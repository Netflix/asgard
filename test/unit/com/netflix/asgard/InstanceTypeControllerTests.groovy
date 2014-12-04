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
        List<InstanceTypeData> types = attrs.instanceTypes.sort { it.name }
        assert 31 <= types.size()
        assert 'c1.medium' == types[0].name
        InstanceTypeData m1Small = types.find { it.name == 'm1.small' }
        assert 'Small (Default)' == m1Small.hardwareProfile.size
        assert '3.75' == types.find { it.name == 'm1.medium' }.hardwareProfile.mem
        assert '68.4' == types.find { it.name == 'm2.4xlarge' }.hardwareProfile.mem
        InstanceTypeData c1medium = types.find { it.name == 'c1.medium' }
        assert '2' == c1medium.hardwareProfile.vCpu
        assert '64-bit' == types.find { it.name == 'm1.large' }.hardwareProfile.arch
        assert 'Moderate' == types.find { it.name == 'm2.xlarge' }.hardwareProfile.netPerf
        assert null == m1Small.linuxOnDemandPrice
        assert 0.280 == types.find { it.name == 'm3.xlarge' }.linuxOnDemandPrice

        assert 4 <= types.findAll{ it.name =~ /i2./ }.size
    }
}
