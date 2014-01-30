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
        assert 19 < types.size()
        assert 't1.micro' == types[0].name
        InstanceTypeData m1Small = types[1]
        assert 'Small (Default)' == m1Small.hardwareProfile.size
        assert '3.75' == types.find { it.name == 'm1.medium' }.hardwareProfile.mem
        assert '68.4' == types.find { it.name == 'm2.4xlarge' }.hardwareProfile.mem
        InstanceTypeData c1medium = types.find { it.name == 'c1.medium' }
        assert '2' == c1medium.hardwareProfile.vCpu
        assert '64-bit' == types.find { it.name == 'm1.large' }.hardwareProfile.arch
        assert 'Moderate' == types.find { it.name == 'm2.xlarge' }.hardwareProfile.netPerf
        assert 0.060 == m1Small.linuxOnDemandPrice

        assert 4 <= types.findAll{ it.name =~ /i2./ }.size
    }
}
