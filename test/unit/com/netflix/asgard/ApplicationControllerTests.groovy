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
import grails.test.mixin.TestFor
import org.junit.Before

@TestFor(ApplicationController)
class ApplicationControllerTests {

    @Before
    void setUp() {
        Mocks.createDynamicMethods()
        TestUtils.setUpMockRequest()
        controller.awsEc2Service = Mocks.awsEc2Service()
        controller.awsAutoScalingService = Mocks.awsAutoScalingService()
        controller.applicationService = Mocks.applicationService()
        controller.awsLoadBalancerService = Mocks.awsLoadBalancerService()
        controller.configService = [alertingServiceConfigUrl: 'alertingServiceUrl']
        controller.cloudReadyService = new CloudReadyService(configService: Mocks.configService())
    }

    void testShow() {
        def params = controller.params
        params.id = 'helloworld'
        def attrs = controller.show()
        assert 'helloworld' == attrs.app.name
        assert attrs.strictName
        assert ['helloworld-example'] == attrs.clusters
        assert ['helloworld-example-v015'] == attrs.groups*.autoScalingGroupName
        assert ['helloworld--frontend'] == attrs.balancers*.loadBalancerName
        assert ['helloworld-frontend', 'helloworld'] == attrs.securities*.groupName
    }

    void testShowNonExistent() {
        def p = controller.params
        p.name = 'doesntexist'
        controller.show()
        assert '/error/missing' == view
        assert "Application 'doesntexist' not found in us-east-1 test" == controller.flash.message
    }
}
