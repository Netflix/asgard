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

import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.InstanceTypeData
import org.junit.Before

class InstanceTypeControllerTests {

    @Before
    void setUp() {
        TestUtils.setUpMockRequest()
        controller.instanceTypeService = Mocks.instanceTypeService()
    }

    void testList() {
        def attrs = controller.list()
        List<InstanceTypeData> types = attrs.instanceTypes
        assert 12 == types.size()
        assert 't1.micro' == types[0].name
        assert 'Small' == types[1].hardwareProfile.description
        assert '1.7 GB' == types[2].hardwareProfile.memory
        assert '22 GB' == types[10].hardwareProfile.memory
        assert '4 EC2 Compute Units (2 virtual cores with 2 EC2 Compute Units each)' == types[3].hardwareProfile.cpu
        assert '4 EC2 Compute Units' == types[3].hardwareProfile.cpuSummary
        assert '(2 virtual cores with 2 EC2 Compute Units each)' == types[3].hardwareProfile.cpuDetail
        assert '64-bit' == types[4].hardwareProfile.architecture
        assert 'High' == types[5].hardwareProfile.ioPerformance

        assert 0.085 == types[1].linuxOnDemandPrice
        assert 0.03 == types[1].linuxReservedPrice
        assert -1 == types[1].linuxSpotPrice
        assert 0.12 == types[1].windowsOnDemandPrice
        assert 0.05 == types[1].windowsReservedPrice
        assert -1 == types[1].windowsSpotPrice
    }
}
