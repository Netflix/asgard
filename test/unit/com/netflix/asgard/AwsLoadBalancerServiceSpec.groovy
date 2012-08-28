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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.Instance
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing
import com.amazonaws.services.elasticloadbalancing.model.AttachLoadBalancerToSubnetsRequest
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult
import com.amazonaws.services.elasticloadbalancing.model.DetachLoadBalancerFromSubnetsRequest
import com.amazonaws.services.elasticloadbalancing.model.InstanceState
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.netflix.asgard.model.InstanceStateData
import spock.lang.Specification

@SuppressWarnings("GroovyPointlessArithmetic")
class AwsLoadBalancerServiceSpec extends Specification {

    UserContext userContext
    AmazonElasticLoadBalancing mockAmazonElasticLoadBalancing
    AwsLoadBalancerService awsLoadBalancerService
    CachedMap cachedMap = Mock(CachedMap)

    def setup() {
        userContext = UserContext.auto(Region.US_EAST_1)
        mockAmazonElasticLoadBalancing = Mock(AmazonElasticLoadBalancing)
        MultiRegionAwsClient awsClient = new MultiRegionAwsClient({ mockAmazonElasticLoadBalancing })
        TaskService taskService = new TaskService() {
            def runTask(UserContext userContext, String name, Closure work, Link link = null) {
                work(new Task())
            }
        }
        Caches caches = new Caches(new MockCachedMapBuilder([
                (EntityType.loadBalancer): cachedMap
        ]))
        awsLoadBalancerService = new AwsLoadBalancerService(awsClient: awsClient, taskService: taskService,
                caches: caches)
    }

    def 'instance state data should include and be sorted by availability zone and auto scaling group'() {
        List<AutoScalingGroup> groups = [
                new AutoScalingGroup(autoScalingGroupName: 'autocomplete-v105', instances: [
                        new Instance(instanceId: 'i-facedeed', availabilityZone: 'us-east-1a'),
                        new Instance(instanceId: 'i-87654321', availabilityZone: 'us-east-1b')
                ]),
                new AutoScalingGroup(autoScalingGroupName: 'autocomplete-v106', instances: [
                        new Instance(instanceId: 'i-deadbeef', availabilityZone: 'us-east-1a')
                ])
        ]
        String unhealthy = 'Instance has failed at least the UnhealthyThreshold number of health checks consecutively.'
        String terminated = 'Instance is in terminated state.'
        String transientError = 'A transient error occurred. Please try again later.'
        mockAmazonElasticLoadBalancing.describeInstanceHealth(_) >> new DescribeInstanceHealthResult(instanceStates: [
                [instanceId: 'i-facedeed', state: 'InService', reasonCode: 'N/A', description: 'N/A'],
                [instanceId: 'i-deadbeef', state: 'InService', reasonCode: 'ELB', description: transientError],
                [instanceId: 'i-12345678', state: 'OutOfService', reasonCode: 'Instance', description: terminated],
                [instanceId: 'i-87654321', state: 'OutOfService', reasonCode: 'Instance', description: unhealthy]
        ].collect { new InstanceState(it) })

        when:
        List<InstanceStateData> instanceStateDatas = awsLoadBalancerService.getInstanceStateDatas(userContext,
                'autocomplete-frontend', groups)

        then:
        instanceStateDatas == [
                [instanceId: 'i-12345678', state: 'OutOfService', reasonCode: 'Instance', description: terminated],
                [instanceId: 'i-facedeed', state: 'InService', reasonCode: 'N/A', description: 'N/A',
                        availabilityZone: 'us-east-1a', autoScalingGroupName: 'autocomplete-v105'],
                [instanceId: 'i-deadbeef', state: 'InService', reasonCode: 'ELB', description: transientError,
                        availabilityZone: 'us-east-1a', autoScalingGroupName: 'autocomplete-v106'],
                [instanceId: 'i-87654321', state: 'OutOfService', reasonCode: 'Instance', description: unhealthy,
                        availabilityZone: 'us-east-1b', autoScalingGroupName: 'autocomplete-v105']
        ].collect { new InstanceStateData(it) }
    }

    def 'should update subnets'() {
        Collection<String> oldSubnets = ['subnet-101', 'subnet-102', 'subnet-103']
        Collection<String> newSubnets = ['subnet-103', 'subnet-104']

        when:
        List<String> messages = awsLoadBalancerService.updateSubnets(userContext, 'lb1', oldSubnets, newSubnets)

        then:
        messages == [
                "Added subnet [subnet-104] to Load Balancer 'lb1'.",
                "Removed subnets [subnet-101, subnet-102] from Load Balancer 'lb1'."
        ]
        1 * mockAmazonElasticLoadBalancing.attachLoadBalancerToSubnets(new AttachLoadBalancerToSubnetsRequest(
                loadBalancerName: 'lb1', subnets: ['subnet-104']))
        1 * mockAmazonElasticLoadBalancing.detachLoadBalancerFromSubnets(new DetachLoadBalancerFromSubnetsRequest(
                loadBalancerName: 'lb1', subnets: ['subnet-101', 'subnet-102']))
        1 * mockAmazonElasticLoadBalancing.describeLoadBalancers(new DescribeLoadBalancersRequest(
                loadBalancerNames: ['lb1'])) >> new DescribeLoadBalancersResult(loadBalancerDescriptions: [
                        new LoadBalancerDescription()
                ])
        1 * cachedMap.put('lb1', _)
        0 * _._
    }

    def 'should not update subnets if nothing changed'() {
        Collection<String> oldSubnets = ['subnet-101', 'subnet-102', 'subnet-103']
        Collection<String> newSubnets = ['subnet-101', 'subnet-102', 'subnet-103']

        when:
        List<String> messages = awsLoadBalancerService.updateSubnets(userContext, 'lb1', oldSubnets, newSubnets)

        then:
        messages == []
        1 * mockAmazonElasticLoadBalancing.describeLoadBalancers(new DescribeLoadBalancersRequest(
                loadBalancerNames: ['lb1'])) >> new DescribeLoadBalancersResult(loadBalancerDescriptions: [
                new LoadBalancerDescription(loadBalancerName: 'lb1')
        ])
        1 * cachedMap.put('lb1', _)
        0 * _._
    }
}
