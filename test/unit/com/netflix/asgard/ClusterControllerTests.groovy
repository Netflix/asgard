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
import com.netflix.asgard.push.Cluster
import org.junit.Before

class ClusterControllerTests {

    @Before
    void setUp() {
        Mocks.monkeyPatcherService().createDynamicMethods()
        TestUtils.setUpMockRequest()
        controller.grailsApplication = Mocks.grailsApplication()
        controller.awsAutoScalingService = Mocks.awsAutoScalingService()
        controller.mergedInstanceService = Mocks.mergedInstanceService()
        controller.pushService = Mocks.pushService()
        controller.taskService = Mocks.taskService()
        controller.awsEc2Service = Mocks.awsEc2Service()
        controller.awsLoadBalancerService = Mocks.awsLoadBalancerService()
    }

    void testShow() {
        def p = controller.params
        p.name = 'helloworld-example'
        def attrs = controller.show()
        assert 'helloworld-example-v015' == attrs.group.autoScalingGroupName
        Cluster cluster = attrs.cluster
        assert 'helloworld-example' == cluster.name
        assert 'helloworld-example-v015' == cluster.last().autoScalingGroupName
        assert ['i-8ee4eeee', 'i-6ef9f30e', 'i-95fe1df6'] == cluster.instances*.instanceId
        assert 'helloworld-example-v016' == attrs.nextGroupName
        assert attrs.okayToCreateGroup
        assert 'Create a new group and switch traffic to it' == attrs.recommendedNextStep
    }

    void testShowNonExistent() {
        def p = controller.params
        p.name ='doesntexist'
        controller.show()
        assert '/error/missing' == view
        assert "Cluster 'doesntexist' not found in us-east-1 test" == controller.flash.message
    }
}
