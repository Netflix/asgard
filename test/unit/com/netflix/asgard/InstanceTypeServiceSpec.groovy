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

import com.netflix.asgard.model.InstanceType
import com.google.common.collect.ArrayTable
import com.google.common.collect.Table
import com.netflix.asgard.model.HardwareProfile
import com.netflix.asgard.model.InstanceProductType
import com.netflix.asgard.model.InstanceTypeData
import grails.test.MockUtils
import spock.lang.Specification

class InstanceTypeServiceSpec extends Specification {

    UserContext userContext
    Caches caches
    InstanceTypeService instanceTypeService
    ConfigService mockConfigService
    CachedMap mockHardwareProfilesCache
    CachedMap mockInstanceTypesCache

    def setup() {
        userContext = UserContext.auto()
        mockConfigService = Mock(ConfigService)
        mockHardwareProfilesCache = Mock(CachedMap)
        mockInstanceTypesCache = Mock(CachedMap)
        caches = new Caches(new MockCachedMapBuilder([
                (EntityType.hardwareProfile): mockHardwareProfilesCache,
                (EntityType.instanceType): mockInstanceTypesCache
        ]))
        MockUtils.mockLogging(InstanceTypeService)
        instanceTypeService = new InstanceTypeService(caches: caches, configService: mockConfigService)
    }

    @SuppressWarnings("GroovyAccessibility")
    def 'instance types should include ordered combo of public and custom instance types'() {

        List<InstanceProductType> products = InstanceProductType.values() as List
        Table<InstanceType, InstanceProductType, BigDecimal> pricesByHardwareAndProduct =
            ArrayTable.create(InstanceType.values() as List, products)
        pricesByHardwareAndProduct.put(InstanceType.M1Small, InstanceProductType.LINUX_UNIX, 0.05)
        pricesByHardwareAndProduct.put(InstanceType.M1Medium, InstanceProductType.LINUX_UNIX, 0.23)
        pricesByHardwareAndProduct.put(InstanceType.M1Large, InstanceProductType.LINUX_UNIX, 0.68)
        RegionalInstancePrices regionalInstancePrices = RegionalInstancePrices.create(pricesByHardwareAndProduct)
        caches.allOnDemandPrices.regionsToRegionalPrices.put(Region.US_EAST_1, regionalInstancePrices)

        mockHardwareProfilesCache.list() >> [
                new HardwareProfile(instanceType: 'm1.small', size: 'Small'),
                new HardwareProfile(instanceType: 'm1.medium', size: 'Medium'),
                new HardwareProfile(instanceType: 'm1.large', size: 'Large')
        ]
        mockConfigService.getCustomInstanceTypes() >> [
                new InstanceTypeData(linuxOnDemandPrice: 3.10, hardwareProfile:
                        new HardwareProfile(instanceType: 'superduper.4xlarge', size: 'SSD')),
                new InstanceTypeData(linuxOnDemandPrice: 1.00, hardwareProfile:
                        new HardwareProfile(instanceType: 'm1.medium', size: 'Custom medium')),
        ]

        when:
        List<InstanceTypeData> instanceTypes = instanceTypeService.buildInstanceTypes(Region.defaultRegion())

        then:
        ['c1.medium', 'c1.xlarge', 'c3.2xlarge', 'c3.4xlarge', 'c3.8xlarge', 'c3.large', 'c3.xlarge', 'cc1.4xlarge',
                'cc2.8xlarge', 'cg1.4xlarge', 'cr1.8xlarge', 'g2.2xlarge', 'hi1.4xlarge', 'hs1.8xlarge', 'i2.2xlarge',
                'i2.4xlarge', 'i2.8xlarge', 'i2.xlarge', 'm1.large', 'm1.medium', 'm1.small', 'm1.xlarge', 'm2.2xlarge',
                'm2.4xlarge', 'm2.xlarge', 'm3.2xlarge', 'm3.large', 'm3.medium', 'm3.xlarge', 'r3.2xlarge',
                'r3.4xlarge', 'r3.8xlarge', 'r3.large', 'r3.xlarge', 'superduper.4xlarge',
                't1.micro'] == instanceTypes*.name.sort()
        //each list contains mostly nulls
        def allSizes = instanceTypes*.hardwareProfile*.size
        allSizes.removeAll { it == null }
        ['Large', 'Medium', 'SSD', 'Small'] == allSizes.sort()
        [0.05, 0.23, 0.68, 3.10] == instanceTypes*.linuxOnDemandPrice[-4..-1]
    }

    def 'instance types list should be sorted by price ascending with unpriced types at the end'() {

        UserContext userContext = UserContext.auto(Region.defaultRegion())
        Closure type = { BigDecimal linuxOnDemandPrice, String instanceType ->
            new InstanceTypeData(linuxOnDemandPrice: linuxOnDemandPrice,
                    hardwareProfile: new HardwareProfile(instanceType: instanceType))
        }
        mockInstanceTypesCache.list() >> [
                type(0.68, 'm1.large'),
                type(0.05, 'm1.small'),
                type(null, 'm3.xxlarge'),
                type(null, 'm3.large'),
                type(null, 'm3.medium'),
                type(0.23, 'm1.medium'),
        ]

        when:
        List<InstanceTypeData> instanceTypes = instanceTypeService.getInstanceTypes(userContext)

        then:
        instanceTypes*.name == ['m1.small', 'm1.medium', 'm1.large', 'm3.large', 'm3.medium', 'm3.xxlarge']
        instanceTypes*.linuxOnDemandPrice == [0.05, 0.23, 0.68, null, null, null]
    }
}
