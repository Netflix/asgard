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
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.InstanceProductType
import com.netflix.asgard.model.InstanceTypeData

@SuppressWarnings("GroovyAccessibility")
class InstanceTypeServiceTests extends GroovyTestCase {

    void testGetInstanceTypeData() {
        InstanceTypeService instanceTypeService = Mocks.instanceTypeService()
        List<InstanceTypeData> instanceTypes = instanceTypeService.getInstanceTypes(Mocks.userContext())
        assert instanceTypes.any { it.name == 'm1.large' }

        // Custom instance type from config is included
        assert instanceTypes.any { it.name == 'huge.mainframe' }
    }

    void testRetrieveInstanceTypeOnDemandPricingData() {
        InstanceTypeService instanceTypeService = Mocks.instanceTypeService()
        Map<Region, RegionalInstancePrices> onDemandPricing = instanceTypeService.retrieveInstanceTypeOnDemandPricing()

        // Virginia
        RegionalInstancePrices usEastOnDemandPricing = onDemandPricing.get(Region.defaultRegion())

        // Linux
        assert 0.17 == usEastOnDemandPricing.get(InstanceType.C1Medium, InstanceProductType.LINUX_UNIX)
        assert 0.34 == usEastOnDemandPricing.get(InstanceType.M1Large, InstanceProductType.LINUX_UNIX)
        assert 0.68 == usEastOnDemandPricing.get(InstanceType.M1Xlarge, InstanceProductType.LINUX_UNIX)
        assert 0.50 == usEastOnDemandPricing.get(InstanceType.M2Xlarge, InstanceProductType.LINUX_UNIX)
        assert 1.00 == usEastOnDemandPricing.get(InstanceType.M22xlarge, InstanceProductType.LINUX_UNIX)
        assert 2.00 == usEastOnDemandPricing.get(InstanceType.M24xlarge, InstanceProductType.LINUX_UNIX)
        assert 2.10 == usEastOnDemandPricing.get(InstanceType.Cg14xlarge, InstanceProductType.LINUX_UNIX)

        // Windows
        assert 0.29 == usEastOnDemandPricing.get(InstanceType.C1Medium, InstanceProductType.WINDOWS)
        assert 0.48 == usEastOnDemandPricing.get(InstanceType.M1Large, InstanceProductType.WINDOWS)
        assert 0.96 == usEastOnDemandPricing.get(InstanceType.M1Xlarge, InstanceProductType.WINDOWS)
        assert 0.62 == usEastOnDemandPricing.get(InstanceType.M2Xlarge, InstanceProductType.WINDOWS)
        assert 1.24 == usEastOnDemandPricing.get(InstanceType.M22xlarge, InstanceProductType.WINDOWS)
        assert 2.48 == usEastOnDemandPricing.get(InstanceType.M24xlarge, InstanceProductType.WINDOWS)
        assert 2.60 == usEastOnDemandPricing.get(InstanceType.Cg14xlarge, InstanceProductType.WINDOWS)

        // Europe
        RegionalInstancePrices euWestOnDemandPricing = onDemandPricing.get(Region.EU_WEST_1)
        assert 0.19 == euWestOnDemandPricing.get(InstanceType.C1Medium, InstanceProductType.LINUX_UNIX)
        assert 0.76 == euWestOnDemandPricing.get(InstanceType.C1Xlarge, InstanceProductType.LINUX_UNIX)
        assert 0.38 == euWestOnDemandPricing.get(InstanceType.M1Large, InstanceProductType.LINUX_UNIX)
        assert 0.76 == euWestOnDemandPricing.get(InstanceType.M1Xlarge, InstanceProductType.LINUX_UNIX)
        assert 1.14 == euWestOnDemandPricing.get(InstanceType.M22xlarge, InstanceProductType.LINUX_UNIX)
        assert 2.28 == euWestOnDemandPricing.get(InstanceType.M24xlarge, InstanceProductType.LINUX_UNIX)
        assertNull euWestOnDemandPricing.get(InstanceType.Cg14xlarge, InstanceProductType.LINUX_UNIX)
    }

    void testRetrieveInstanceTypeReservedPricingData() {
        InstanceTypeService instanceTypeService = Mocks.instanceTypeService()
        Map<Region, RegionalInstancePrices> reservedPricing = instanceTypeService.retrieveInstanceTypeReservedPricing()

        // Virginia
        RegionalInstancePrices usEastReservedPricing = reservedPricing.get(Region.defaultRegion())

        // Linux
        assert 0.06 == usEastReservedPricing.get(InstanceType.C1Medium, InstanceProductType.LINUX_UNIX)
        assert 0.12 == usEastReservedPricing.get(InstanceType.M1Large, InstanceProductType.LINUX_UNIX)
        assert 0.24 == usEastReservedPricing.get(InstanceType.M1Xlarge, InstanceProductType.LINUX_UNIX)
        assert 0.17 == usEastReservedPricing.get(InstanceType.M2Xlarge, InstanceProductType.LINUX_UNIX)
        assert 0.34 == usEastReservedPricing.get(InstanceType.M22xlarge, InstanceProductType.LINUX_UNIX)
        assert 0.68 == usEastReservedPricing.get(InstanceType.M24xlarge, InstanceProductType.LINUX_UNIX)
        assert 0.74 == usEastReservedPricing.get(InstanceType.Cg14xlarge, InstanceProductType.LINUX_UNIX)

        // Windows
        assert 0.125 == usEastReservedPricing.get(InstanceType.C1Medium, InstanceProductType.WINDOWS)
        assert 0.20 == usEastReservedPricing.get(InstanceType.M1Large, InstanceProductType.WINDOWS)
        assert 0.40 == usEastReservedPricing.get(InstanceType.M1Xlarge, InstanceProductType.WINDOWS)
        assert 0.24 == usEastReservedPricing.get(InstanceType.M2Xlarge, InstanceProductType.WINDOWS)
        assert 0.48 == usEastReservedPricing.get(InstanceType.M22xlarge, InstanceProductType.WINDOWS)
        assert 0.96 == usEastReservedPricing.get(InstanceType.M24xlarge, InstanceProductType.WINDOWS)
        assert 1.04 == usEastReservedPricing.get(InstanceType.Cg14xlarge, InstanceProductType.WINDOWS)

        // Europe
        RegionalInstancePrices euWestReservedPricing = reservedPricing.get(Region.EU_WEST_1)
        assert 0.08 == euWestReservedPricing.get(InstanceType.C1Medium, InstanceProductType.LINUX_UNIX)
        assert 0.32 == euWestReservedPricing.get(InstanceType.C1Xlarge, InstanceProductType.LINUX_UNIX)
        assert 0.16 == euWestReservedPricing.get(InstanceType.M1Large, InstanceProductType.LINUX_UNIX)
        assert 0.32 == euWestReservedPricing.get(InstanceType.M1Xlarge, InstanceProductType.LINUX_UNIX)
        assert 0.48 == euWestReservedPricing.get(InstanceType.M22xlarge, InstanceProductType.LINUX_UNIX)
        assert 0.96 == euWestReservedPricing.get(InstanceType.M24xlarge, InstanceProductType.LINUX_UNIX)
        assertNull euWestReservedPricing.get(InstanceType.Cg14xlarge, InstanceProductType.LINUX_UNIX)
    }

    void testFindRelevantInstanceTypesFor64BitImage() {
        InstanceTypeService instanceTypeService = Mocks.instanceTypeService()
        Image image = new Image(architecture: 'x86_64')
        List<String> expected64BitInstanceTypes = ['t1.micro', 'm1.small', 'c1.medium', 'm1.large', 'm2.xlarge',
                'c1.xlarge', 'm1.xlarge', 'm2.2xlarge', 'cc1.4xlarge', 'm2.4xlarge', 'cg1.4xlarge', 'cc2.8xlarge',
                'huge.mainframe']

        assertEquals(expected64BitInstanceTypes,
                instanceTypeService.findRelevantInstanceTypesForImage(Mocks.userContext(), image)*.name)
    }

    void testFindRelevantInstanceTypesFor32BitImage() {
        InstanceTypeService instanceTypeService = Mocks.instanceTypeService()
        Image image = new Image(architecture: 'i386')
        List<String> expected32BitInstaceTypes = ['t1.micro', 'm1.small', 'c1.medium']
        assertEquals(expected32BitInstaceTypes,
            instanceTypeService.findRelevantInstanceTypesForImage(Mocks.userContext(), image)*.name)
    }
}
