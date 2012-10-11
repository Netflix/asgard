/*
 * Copyright 2012 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.asgard

import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.InstanceType
import com.google.common.collect.ArrayTable
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Table
import com.netflix.asgard.cache.CacheInitializer
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.HardwareProfile
import com.netflix.asgard.model.InstancePriceType
import com.netflix.asgard.model.InstanceProductType
import com.netflix.asgard.model.InstanceTypeData
import groovy.transform.Immutable
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONElement
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.Elements

/**
 * Scrapes web pages and json feeds from Amazon to parse technical and financial information about instance types.
 */
class InstanceTypeService implements CacheInitializer {

    static transactional = false

    final String instanceTypesUrl = 'http://aws.amazon.com/ec2/instance-types/'
    final Map<JsonTypeSizeCombo, InstanceType> typeSizeCodesToInstanceTypes = buildTypeSizeCodesToInstanceTypes()

    final BigDecimal lowPrioritySpotPriceFactor = 1.0
    final BigDecimal highPrioritySpotPriceFactor = 1.04
    final Map<String, String> imageArchToInstanceTypeArch = ImmutableMap.copyOf([x86_64: '64-bit', i386: '32-bit'])

    def grailsApplication
    def awsEc2Service
    Caches caches
    def configService
    def emailerService
    def restClientService

    void initializeCaches() {
        // Use one thread for all these data sources. None of these need updating more than once an hour.
        caches.allOnDemandPrices.ensureSetUp({ retrieveInstanceTypeOnDemandPricing() })
        caches.allReservedPrices.ensureSetUp({ retrieveInstanceTypeReservedPricing() })
        caches.allSpotPrices.ensureSetUp({ retrieveInstanceTypeSpotPricing() })
        caches.allInstanceTypes.ensureSetUp({ Region region -> buildInstanceTypes(region) })
        caches.allHardwareProfiles.ensureSetUp({ retrieveHardwareProfiles() }, {
            caches.allOnDemandPrices.fill()
            caches.allReservedPrices.fill()
            caches.allSpotPrices.fill()
            caches.allInstanceTypes.fill()
        })
    }

    /** Costs less, can take longer to start, can terminate sooner */
    BigDecimal calculateLeisureLinuxSpotBid(UserContext userContext, String instanceTypeName) {
        InstanceTypeData instanceType = getInstanceType(userContext, instanceTypeName)
        instanceType.linuxOnDemandPrice * lowPrioritySpotPriceFactor
    }

    /** Costs more, should start soon, should terminate rarely */
    BigDecimal calculateUrgentLinuxSpotBid(UserContext userContext, String instanceTypeName) {
        InstanceTypeData instanceType = getInstanceType(userContext, instanceTypeName)
        instanceType.linuxOnDemandPrice * highPrioritySpotPriceFactor
    }

    Collection<InstanceTypeData> findRelevantInstanceTypesForImage(UserContext userContext, Image image) {
        Collection<InstanceTypeData> instanceTypes = getInstanceTypes(userContext)
        String instanceTypeArch = imageArchToInstanceTypeArch[image.architecture]
        instanceTypes.findAll { it.hardwareProfile.architecture.contains(instanceTypeArch) }
    }

    InstanceTypeData getInstanceType(UserContext userContext, String instanceTypeName) {
        caches.allInstanceTypes.by(userContext.region).get(instanceTypeName)
    }

    Collection<InstanceTypeData> getInstanceTypes(UserContext userContext) {
        caches.allInstanceTypes.by(userContext.region).list().sort { it.linuxOnDemandPrice }
    }

    private Document fetchInstanceTypesDocument() {
        if (configService.online) {
            Jsoup.parse(restClientService.getAsText(instanceTypesUrl))
        } else {
            fetchLocalInstanceTypesDocument()
        }
    }

    private JSONElement fetchPricingJsonData(InstancePriceType instancePriceType) {
        Boolean online = grailsApplication.config.server.online
        String pricingJsonUrl = instancePriceType.url
        online ? restClientService.getAsJson(pricingJsonUrl) : Mocks.parseJsonFile(instancePriceType.dataSourceFileName)
    }

    private Collection<HardwareProfile> getHardwareProfiles() {
        caches.allHardwareProfiles.list()
    }

    private RegionalInstancePrices getOnDemandPrices(Region region) {
        caches.allOnDemandPrices.by(region)
    }

    private RegionalInstancePrices getReservedPrices(Region region) {
        caches.allReservedPrices.by(region)
    }

    private RegionalInstancePrices getSpotPrices(Region region) {
        caches.allSpotPrices.by(region)
    }

    private List<InstanceTypeData> buildInstanceTypes(Region region) {
        try {
            Collection<HardwareProfile> hardwareProfiles = getHardwareProfiles()
            RegionalInstancePrices onDemandPrices = getOnDemandPrices(region)
            RegionalInstancePrices reservedPrices = getReservedPrices(region)
            RegionalInstancePrices spotPrices = getSpotPrices(region)

            List<InstanceTypeData> instanceTypes = InstanceType.values().collect { InstanceType instanceType ->
                HardwareProfile hardwareProfile = hardwareProfiles.find { it.instanceType == instanceType.toString() }
                new InstanceTypeData(
                        hardwareProfile: hardwareProfile,
                        linuxOnDemandPrice: onDemandPrices.get(instanceType, InstanceProductType.LINUX_UNIX),
                        linuxReservedPrice: reservedPrices.get(instanceType, InstanceProductType.LINUX_UNIX),
                        linuxSpotPrice: spotPrices.get(instanceType, InstanceProductType.LINUX_UNIX),
                        windowsOnDemandPrice: onDemandPrices.get(instanceType, InstanceProductType.WINDOWS),
                        windowsReservedPrice: reservedPrices.get(instanceType, InstanceProductType.WINDOWS),
                        windowsSpotPrice: spotPrices.get(instanceType, InstanceProductType.WINDOWS)
                )
            }
            // Only include types that have prices listed for this region
            Collection<InstanceTypeData> relevantInstanceTypes = instanceTypes.findAll { it.linuxOnDemandPrice }
            Collection<String> foundInstanceTypeNames = relevantInstanceTypes*.name

            // Add any custom instance types that are still missing from the InstanceType enum.
            List<InstanceTypeData> customInstanceTypes = configService.customInstanceTypes.findAll {
                !(it.name in foundInstanceTypeNames)
            }
            return relevantInstanceTypes + customInstanceTypes
        } catch (Exception e) {
            log.error(e)
            emailerService.sendExceptionEmail('Error parsing Amazon instance data', e)
            return []
        }
    }

    private Document fetchLocalInstanceTypesDocument() {
        Mocks.parseHtmlFile('instance-types.html')
    }

    private List<HardwareProfile> retrieveHardwareProfiles() {

        // http://imediava.wordpress.com/2011/09/24/web-scraping-with-groovy-3-of-3/
        try {
            return parseHardwareProfilesDocument(fetchInstanceTypesDocument())
        } catch (Exception e) {
            String msg = "Using old hardware profiles document because of an unexpected format at ${instanceTypesUrl}"
            log.error msg
            emailerService.sendExceptionEmail(msg, e)
        }
        parseHardwareProfilesDocument(fetchLocalInstanceTypesDocument())
    }

    private List<HardwareProfile> parseHardwareProfilesDocument(Document instanceTypesDoc) {

        List<HardwareProfile> hardwareProfiles = []

        Elements titleParagraphs = instanceTypesDoc.select('p strong')
        Elements paragraphsWithBreaks = instanceTypesDoc.select('p:has(br)')
        Elements dataParagraphs = paragraphsWithBreaks.select(':matches([a-z])') // Skip the empty paragraph

        // Send an alert email when Amazon edits the instance types page in a way that breaks this web scraper.
        if (titleParagraphs.size() <= dataParagraphs.size() && titleParagraphs.size() >= InstanceType.values().size()) {
            for (Integer i = 0; i < titleParagraphs.size(); i++) {
                Element dataParagraph = dataParagraphs.get(i)
                List<Node> dataChildNodes = dataParagraph.childNodes().findAll { it.nodeName() != 'br' }
                Collection<String> textNodes = dataChildNodes.collect { it.text().trim() }
                String name = Check.notEmpty(textNodes.find { it.startsWith('API name: ')}, 'name') - 'API name: '

                // Skip any instance type Amazon has added to their web page that is not yet in their Java SDK
                if (InstanceType.values().any { it.toString() == name }) {

                    String memoryRaw = Check.notEmpty(textNodes.find { it.endsWith(' memory')}, 'memory')
                    String memory = (memoryRaw - ' of memory' - ' memory').trim()

                    String cpu = (Check.notEmpty(textNodes.find { it.contains('Compute Unit')}, 'cpu')).trim()

                    String storageRaw = Check.notEmpty(textNodes.find { it.contains('storage')}, 'storage')
                    String storage = (storageRaw - ' of instance storage' - ' instance storage' - ' storage').trim()

                    String archRaw = textNodes.find { it.endsWith('platform')}
                    String validArchRaw = Check.notEmpty(archRaw, 'architecture')
                    String architecture = (validArchRaw - ' platform').trim()

                    String ioPerfRaw = textNodes.find { it.startsWith('I/O Performance: ')}
                    String validIoPerfRaw = Check.notEmpty(ioPerfRaw, 'ioPerformance')
                    String ioPerformance = (validIoPerfRaw - 'I/O Performance: ').trim()

                    String descriptionRaw = Check.notEmpty(titleParagraphs.get(i).text(), 'description')
                    String description = (descriptionRaw - ' Instance').trim()

                    HardwareProfile hardwareProfile = new HardwareProfile(
                            instanceType: name,
                            description: description,
                            memory: memory,
                            cpu: cpu,
                            storage: storage,
                            architecture: architecture,
                            ioPerformance: ioPerformance
                    )
                    hardwareProfiles << hardwareProfile
                }
            }
        } else {
            throw new Exception("Unexpected format of HTML on ${instanceTypesUrl}")
        }
        hardwareProfiles
    }

    private Map<Region, RegionalInstancePrices> retrieveInstanceTypePricing(InstancePriceType priceType) {
        Map<Region, RegionalInstancePrices> regionsToPricingData = [:]
        JSONElement pricingJson = fetchPricingJsonData(priceType)
        JSONElement config = pricingJson.config
        JSONArray regionJsonArray = config.regions
        for (JSONElement regionJsonObject in regionJsonArray) {
            Region region = Region.withPricingJsonCode(regionJsonObject.region)
            List<InstanceType> instanceTypes = InstanceType.values() as List
            List<InstanceProductType> products = InstanceProductType.valuesForOnDemandAndReserved()
            Table<InstanceType, InstanceProductType, BigDecimal> pricesByHardwareAndProduct =
                    ArrayTable.create(instanceTypes, products)
            JSONArray typesJsonArray = regionJsonObject.instanceTypes
            for (JSONElement typeJsonObject in typesJsonArray) {
                String typeCode = typeJsonObject.type // Name of group of hardware instance types like hiMemODI
                JSONArray sizes = typeJsonObject.sizes
                for (JSONElement sizeObject in sizes) {
                    String sizeCode = sizeObject.size
                    InstanceType instanceType = determineInstanceType(typeCode, sizeCode)
                    if (instanceType) {
                        JSONArray valueColumns = sizeObject.valueColumns
                        for (JSONElement valueColumn in valueColumns) {
                            String productTypeString = valueColumn.name
                            InstanceProductType product = products.find { it.jsonPricingName == productTypeString }
                            if (product) {
                                String priceString = valueColumn.prices.USD
                                if (priceString?.isBigDecimal()) {
                                    BigDecimal price = priceString.toBigDecimal()
                                    pricesByHardwareAndProduct.put(instanceType, product, price)
                                }
                            }
                        }
                    }
                }
            }
            regionsToPricingData[region] = RegionalInstancePrices.create(pricesByHardwareAndProduct)
        }
        return regionsToPricingData
    }

    private Map<Region, RegionalInstancePrices> retrieveInstanceTypeOnDemandPricing() {
        retrieveInstanceTypePricing(InstancePriceType.ON_DEMAND)
    }

    private Map<Region, RegionalInstancePrices> retrieveInstanceTypeReservedPricing() {
        retrieveInstanceTypePricing(InstancePriceType.RESERVED)
    }

    private Map<Region, RegionalInstancePrices> retrieveInstanceTypeSpotPricing() {
        retrieveInstanceTypePricing(InstancePriceType.SPOT)
    }

    private Map<JsonTypeSizeCombo, InstanceType> buildTypeSizeCodesToInstanceTypes() {

        Map<JsonTypeSizeCombo, InstanceType> typeSizeCodesToInstanceTypes = [:]

        // Reservation json compound code names
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('stdResI', 'sm'), InstanceType.M1Small)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('stdResI', 'lg'), InstanceType.M1Large)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('stdResI', 'xl'), InstanceType.M1Xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('uResI', 'u'), InstanceType.T1Micro)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiMemResI', 'xl'), InstanceType.M2Xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiMemResI', 'xxl'), InstanceType.M22xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiMemResI', 'xxxxl'), InstanceType.M24xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiCPUResI', 'med'), InstanceType.C1Medium)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiCPUResI', 'xl'), InstanceType.C1Xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('clusterCompResI', 'xxxxl'), InstanceType.Cc14xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('clusterCompResI', 'xxxxxxxxl'), InstanceType.Cc28xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('clusterGPUResI', 'xxxxl'), InstanceType.Cg14xlarge)

        // On-demand json compound code names
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('stdODI', 'sm'), InstanceType.M1Small)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('stdODI', 'med'), InstanceType.M1Medium)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('stdODI', 'lg'), InstanceType.M1Large)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('stdODI', 'xl'), InstanceType.M1Xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('uODI', 'u'), InstanceType.T1Micro)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiMemODI', 'xl'), InstanceType.M2Xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiMemODI', 'xxl'), InstanceType.M22xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiMemODI', 'xxxxl'), InstanceType.M24xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiCPUODI', 'med'), InstanceType.C1Medium)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiCPUODI', 'xl'), InstanceType.C1Xlarge)

        // Spot json compound code names
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('stdSpot', 'sm'), InstanceType.M1Small)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('stdSpot', 'med'), InstanceType.M1Medium)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('stdSpot', 'lg'), InstanceType.M1Large)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('stdSpot', 'xl'), InstanceType.M1Xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('uSpot', 'u'), InstanceType.T1Micro)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiMemSpot', 'xl'), InstanceType.M2Xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiMemSpot', 'xxl'), InstanceType.M22xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiMemSpot', 'xxxxl'), InstanceType.M24xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiCPUSpot', 'med'), InstanceType.C1Medium)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('hiCPUSpot', 'xl'), InstanceType.C1Xlarge)

        // On demand and spot share these identifiers
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('clusterComputeI', 'xxxxl'), InstanceType.Cc14xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('clusterComputeI', 'xxxxxxxxl'), InstanceType.Cc28xlarge)
        typeSizeCodesToInstanceTypes.put(new JsonTypeSizeCombo('clusterGPUI', 'xxxxl'), InstanceType.Cg14xlarge)

        return typeSizeCodesToInstanceTypes.asImmutable()
    }

    private InstanceType determineInstanceType(String jsonTypeCode, String jsonSizeCode) {
        typeSizeCodesToInstanceTypes[new JsonTypeSizeCombo(jsonTypeCode, jsonSizeCode)]
    }
}

@Immutable final class JsonTypeSizeCombo {
    String type
    String size
}

class RegionalInstancePrices {
    final Table<InstanceType, InstanceProductType, BigDecimal> pricesByHardwareAndProduct

    private RegionalInstancePrices(Table<InstanceType, InstanceProductType, BigDecimal> pricesByHardwareAndProduct) {
        this.pricesByHardwareAndProduct = pricesByHardwareAndProduct
    }

    static create(Table<InstanceType, InstanceProductType, BigDecimal> pricesByHardwareAndProduct) {
        new RegionalInstancePrices(pricesByHardwareAndProduct)
    }

    BigDecimal get(InstanceType instanceType, InstanceProductType instanceProductType) {
        pricesByHardwareAndProduct.get(instanceType, instanceProductType)
    }

    String toString() {
        pricesByHardwareAndProduct.toString()
    }
}
