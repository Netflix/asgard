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

import com.amazonaws.services.ec2.model.InstanceType
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

    def setup() {
        userContext = UserContext.auto(Region.US_EAST_1)
        mockConfigService = Mock(ConfigService)
        mockHardwareProfilesCache = Mock(CachedMap)
        caches = new Caches(new MockCachedMapBuilder([
                (EntityType.hardwareProfile): mockHardwareProfilesCache,
        ]))
        MockUtils.mockLogging(InstanceTypeService)
        instanceTypeService = new InstanceTypeService(caches: caches, configService: mockConfigService)
    }

    @SuppressWarnings("GroovyAccessibility")
    def 'instance types should include unique combo of public and custom instance types'() {

        List<InstanceProductType> products = InstanceProductType.valuesForOnDemandAndReserved()
        Table<InstanceType, InstanceProductType, BigDecimal> pricesByHardwareAndProduct =
            ArrayTable.create(InstanceType.values() as List, products)
        pricesByHardwareAndProduct.put(InstanceType.M1Small, InstanceProductType.LINUX_UNIX, 0.05)
        pricesByHardwareAndProduct.put(InstanceType.M1Medium, InstanceProductType.LINUX_UNIX, 0.23)
        pricesByHardwareAndProduct.put(InstanceType.M1Large, InstanceProductType.LINUX_UNIX, 0.68)
        RegionalInstancePrices regionalInstancePrices = RegionalInstancePrices.create(pricesByHardwareAndProduct)
        caches.allReservedPrices.regionsToRegionalPrices.put(Region.US_EAST_1, regionalInstancePrices)
        caches.allOnDemandPrices.regionsToRegionalPrices.put(Region.US_EAST_1, regionalInstancePrices)
        caches.allSpotPrices.regionsToRegionalPrices.put(Region.US_EAST_1, regionalInstancePrices)

        mockHardwareProfilesCache.list() >> [
                new HardwareProfile(instanceType: 'm1.small', description: 'Small instance'),
                new HardwareProfile(instanceType: 'm1.medium', description: 'Medium instance'),
                new HardwareProfile(instanceType: 'm1.large', description: 'Large instance')
        ]
        mockConfigService.getCustomInstanceTypes() >> [
                new InstanceTypeData(linuxOnDemandPrice: 3.10, hardwareProfile:
                        new HardwareProfile(instanceType: 'hi1.4xlarge', description: 'SSD')),
                new InstanceTypeData(linuxOnDemandPrice: 1.00, hardwareProfile:
                        new HardwareProfile(instanceType: 'm1.medium', description: 'Custom medium description')),
        ]

        when:
        List<InstanceTypeData> instanceTypes = instanceTypeService.buildInstanceTypes(Region.defaultRegion())

        then:
        println instanceTypes*.name
        ['m1.small', 'm1.medium', 'm1.large', 'hi1.4xlarge'] == instanceTypes*.name
        ['Small instance', 'Medium instance', 'Large instance', 'SSD'] == instanceTypes*.hardwareProfile*.description
        [0.05, 0.23, 0.68, 3.10] == instanceTypes*.linuxOnDemandPrice
    }
}
