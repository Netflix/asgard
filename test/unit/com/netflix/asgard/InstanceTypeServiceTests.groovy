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
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.InstanceProductType
import com.netflix.asgard.model.InstanceTypeData

@SuppressWarnings("GroovyAccessibility")
class InstanceTypeServiceTests extends GroovyTestCase {

    void testGetInstanceTypeData() {
        InstanceTypeService instanceTypeService = Mocks.instanceTypeService()
        List<InstanceTypeData> instanceTypes = instanceTypeService.getInstanceTypes(Mocks.userContext())
        instanceTypes.each{ println it.name }
        assert instanceTypes.any { it.name == 'm1.large' }
        assert instanceTypes.any { it.name == 'i2.xlarge' }
        // Custom instance type from config is included
        assert instanceTypes.any { it.name == 'huge.mainframe' }
    }

    void testRetrieveInstanceTypeOnDemandPricingData() {
        InstanceTypeService instanceTypeService = Mocks.instanceTypeService()
        Map<Region, RegionalInstancePrices> onDemandPricing = instanceTypeService.retrieveInstanceTypeOnDemandPricing()

        // Virginia
        RegionalInstancePrices usEastOnDemandPricing = onDemandPricing.get(Region.defaultRegion())

        // Linux
        assert 0.145 == usEastOnDemandPricing.get(InstanceType.C1Medium, InstanceProductType.LINUX_UNIX)
        assert 0.41 == usEastOnDemandPricing.get(InstanceType.M2Xlarge, InstanceProductType.LINUX_UNIX)
        assert 1.64 == usEastOnDemandPricing.get(InstanceType.M24xlarge, InstanceProductType.LINUX_UNIX)
        assert 2.10 == usEastOnDemandPricing.get(InstanceType.Cg14xlarge, InstanceProductType.LINUX_UNIX)
        assertNull usEastOnDemandPricing.get(InstanceType.Cc14xlarge, InstanceProductType.LINUX_UNIX)

        // Europe
        RegionalInstancePrices euWestOnDemandPricing = onDemandPricing.get(Region.EU_WEST_1)
        assert 0.165 == euWestOnDemandPricing.get(InstanceType.C1Medium, InstanceProductType.LINUX_UNIX)
        assert 0.52 == euWestOnDemandPricing.get(InstanceType.M1Xlarge, InstanceProductType.LINUX_UNIX)
        assert 0.92 == euWestOnDemandPricing.get(InstanceType.M22xlarge, InstanceProductType.LINUX_UNIX)
        assert 1.84 == euWestOnDemandPricing.get(InstanceType.M24xlarge, InstanceProductType.LINUX_UNIX)
        assert 2.36 == euWestOnDemandPricing.get(InstanceType.Cg14xlarge, InstanceProductType.LINUX_UNIX)
        assertNull euWestOnDemandPricing.get(InstanceType.Cc14xlarge, InstanceProductType.LINUX_UNIX)
    }
}
