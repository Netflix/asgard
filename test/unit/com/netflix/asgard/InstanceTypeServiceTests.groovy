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
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.InstanceProductType
import com.netflix.asgard.model.InstanceTypeData

@SuppressWarnings("GroovyAccessibility")
class InstanceTypeServiceTests extends GroovyTestCase {

    void testGetInstanceTypeData() {
        InstanceTypeService instanceTypeService = Mocks.instanceTypeService()
        List<InstanceTypeData> instanceTypes = instanceTypeService.getInstanceTypes(Mocks.userContext())
        assert instanceTypes.any { it.name == 'm1.large' }
        assert instanceTypes.any { it.name == 'i2.xlarge' }
        assert instanceTypes.any { it.name == 'm3.medium' }
        assert instanceTypes.any { it.name == 'r3.xlarge' }
        // Custom instance type from config is included
        assert instanceTypes.any { it.name == 'huge.mainframe' }
    }

    void testRetrieveInstanceTypeOnDemandPricingData() {
        InstanceTypeService instanceTypeService = Mocks.instanceTypeService()
        Map<Region, RegionalInstancePrices> onDemandPricing = instanceTypeService.retrieveInstanceTypeOnDemandPricing()

        // Virginia
        RegionalInstancePrices usEastOnDemandPricing = onDemandPricing.get(Region.defaultRegion())

        // Linux
        assert 0.070 == usEastOnDemandPricing.get(InstanceType.M3Medium, InstanceProductType.LINUX_UNIX)
        assert 0.280 == usEastOnDemandPricing.get(InstanceType.M3Xlarge, InstanceProductType.LINUX_UNIX)
        assert 0.560 == usEastOnDemandPricing.get(InstanceType.M32xlarge, InstanceProductType.LINUX_UNIX)
        assert 2.80 == usEastOnDemandPricing.get(InstanceType.R38xlarge, InstanceProductType.LINUX_UNIX)
        assertNull usEastOnDemandPricing.get(InstanceType.Cc14xlarge, InstanceProductType.LINUX_UNIX)

        // Europe
        RegionalInstancePrices euWestOnDemandPricing = onDemandPricing.get(Region.EU_WEST_1)
        assert 0.077 == euWestOnDemandPricing.get(InstanceType.M3Medium, InstanceProductType.LINUX_UNIX)
        assert 0.308 == euWestOnDemandPricing.get(InstanceType.M3Xlarge, InstanceProductType.LINUX_UNIX)
        assert 0.616 == euWestOnDemandPricing.get(InstanceType.M32xlarge, InstanceProductType.LINUX_UNIX)
        assert 3.120 == euWestOnDemandPricing.get(InstanceType.R38xlarge, InstanceProductType.LINUX_UNIX)
        assertNull euWestOnDemandPricing.get(InstanceType.Cc14xlarge, InstanceProductType.LINUX_UNIX)
    }
}
