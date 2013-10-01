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
import grails.test.mixin.TestFor
import org.junit.Before

@TestFor(InstanceTypeController)
class InstanceTypeControllerTests {

    @Before
    void setUp() {
        TestUtils.setUpMockRequest()
        controller.instanceTypeService = Mocks.instanceTypeService()
    }

    void testList() {
        def attrs = controller.list()
        List<InstanceTypeData> types = attrs.instanceTypes
        assert 17 == types.size()
        assert 't1.micro' == types[0].name
        InstanceTypeData m1Small = types[1]
        assert 'M1 Small' == m1Small.hardwareProfile.description
        assert '3.75 GiB' == types.find { it.name == 'm1.medium' }.hardwareProfile.memory
        assert '68.4 GiB' == types.find { it.name == 'm2.4xlarge' }.hardwareProfile.memory
        InstanceTypeData c1medium = types.find { it.name == 'c1.medium' }
        assert '5 EC2 Compute Units (2 virtual cores with 2.5 EC2 Compute Units each)' == c1medium.hardwareProfile.cpu
        assert '5 EC2 Compute Units' == c1medium.hardwareProfile.cpuSummary
        assert '(2 virtual cores with 2.5 EC2 Compute Units each)' == c1medium.hardwareProfile.cpuDetail
        assert '64-bit' == types.find { it.name == 'm1.large' }.hardwareProfile.architecture
        assert 'Moderate' == types.find { it.name == 'm2.xlarge' }.hardwareProfile.ioPerformance

        assert 0.065 == m1Small.linuxOnDemandPrice
        assert 0.03 == m1Small.linuxReservedPrice
        assert 0.007 == m1Small.linuxSpotPrice
        assert 0.115 == m1Small.windowsOnDemandPrice
        assert 0.05 == m1Small.windowsReservedPrice
        assert 0.017 == m1Small.windowsSpotPrice
    }
}
