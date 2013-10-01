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
import org.junit.Before

class LaunchConfigurationControllerTests {

    @Before
    void setUp() {
        Mocks.createDynamicMethods()
        TestUtils.setUpMockRequest()
        controller.awsAutoScalingService = Mocks.awsAutoScalingService()
        controller.applicationService = Mocks.applicationService()
        controller.awsEc2Service = Mocks.awsEc2Service()
    }

    void testShow() {
        def p = controller.params
        p.name = 'helloworld-example-v015-20111014165240'
        def attrs = controller.show()
        assert 'helloworld-example-v015-20111014165240' == attrs.lc.launchConfigurationName
        assert 'helloworld-example-v015' == attrs.group.autoScalingGroupName
        assert 'helloworld' == attrs.app.name
        assert 'ami-4775b32e' == attrs.image.imageId
    }

    void testShowNonExistent() {
        def p = controller.params
        p.name = 'doesntexist'
        controller.show()
        assert '/error/missing' == view
        assert "Launch Configuration 'doesntexist' not found in us-east-1 test" == controller.flash.message
    }
}
