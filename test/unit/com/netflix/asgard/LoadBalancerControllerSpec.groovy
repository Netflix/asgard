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

import com.amazonaws.services.ec2.model.Subnet
import com.amazonaws.services.ec2.model.Tag
import com.amazonaws.services.elasticloadbalancing.model.HealthCheck
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing
import com.amazonaws.services.elasticloadbalancing.model.Listener
import com.netflix.asgard.model.Subnets
import grails.test.MockUtils
import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings("GroovyAssignabilityCheck")
@TestFor(LoadBalancerController)
class LoadBalancerControllerSpec extends Specification {

    AmazonElasticLoadBalancing mockElb = Mock(AmazonElasticLoadBalancing)
    AwsLoadBalancerService awsLoadBalancerService = Mock(AwsLoadBalancerService)
    AwsEc2Service awsEc2Service = Mock(AwsEc2Service)

    void setup() {
        TestUtils.setUpMockRequest()
        request.region = Region.defaultRegion()
        [AddListenerCommand, RemoveListenerCommand].each {
            MockUtils.prepareForConstraintsTests(it)
        }
        controller.awsLoadBalancerService = awsLoadBalancerService
        awsLoadBalancerService.awsClient = new MultiRegionAwsClient({ mockElb })
        controller.awsEc2Service = awsEc2Service
        mockElb.describeLoadBalancers(_) >> { [] }
    }

    private void setUpHealthCheckParams() {
        params.target = 'HTTP:8080/'
        params.interval = '5'
        params.timeout = '10'
        params.unhealthy = '3'
        params.healthy = '4'
    }

    @Unroll('update should change zones but not subnets when default VPC subnets #defaultVpcSubnetsCondition')
    def 'update without custom VPC subnets should change zones but not subnets'() {
        params.name = 'hello'
        params.selectedZones = ['us-east-1b', 'us-east-1c']
        setUpHealthCheckParams()

        when:
        controller.update()

        then:
        flash.message == "Added zone [us-east-1c] to load balancer. Removed zone [us-east-1a] from load balancer. " +
                "Load Balancer 'hello' health check has been updated. "
        1 * awsLoadBalancerService.getLoadBalancer(_, 'hello') >>
                new LoadBalancerDescription(availabilityZones: ['us-east-1a', 'us-east-1b'], subnets: lbSubnetIds)
        1 * awsEc2Service.getDefaultVpcSubnetIds(_) >> defaultVpcSubnetIds
        1 * awsLoadBalancerService.addZones(_, 'hello', ['us-east-1c'])
        1 * awsLoadBalancerService.removeZones(_, 'hello', ['us-east-1a'])
        1 * awsLoadBalancerService.configureHealthCheck(_, 'hello', new HealthCheck(target: 'HTTP:8080/', interval: 5,
                timeout: 10, unhealthyThreshold: 3, healthyThreshold: 4))
        0 * _

        where:
        defaultVpcSubnetIds          | lbSubnetIds                  | defaultVpcSubnetsCondition
        []                           | []                           | 'do not exist and load balancer has no subnets'
        ['subnet-123', 'subnet-456'] | []                           | 'exist but load balancer has no subnets'
        ['subnet-123', 'subnet-456'] | ['subnet-123']               | 'are partially used by load balancer'
        ['subnet-123', 'subnet-456'] | ['subnet-123', 'subnet-456'] | 'are all used by load balancer'
    }

    @Unroll('update should change subnets but not zones when default VPC subnets #defaultVpcSubnetsCondition')
    def 'update with custom VPC subnets should change subnets but not zones'() {
        params.name = 'hello'
        params.selectedZones = ['us-east-1b', 'us-east-1c']
        setUpHealthCheckParams()

        Subnets allSubnets = Subnets.from([
                new Subnet(subnetId: 'sn-123', vpcId: 'vpc-def', availabilityZone: 'us-east-1a'),
                new Subnet(subnetId: 'sn-456', vpcId: 'vpc-def', availabilityZone: 'us-east-1b'),
                new Subnet(subnetId: 'sn-789', vpcId: 'vpc-def', availabilityZone: 'us-east-1c'),
                new Subnet(subnetId: 'sn-ant', vpcId: 'vpc-custom', availabilityZone: 'us-east-1a',
                        tags: [new Tag(key: 'immutable_metadata', value: '{"purpose":"external","target":"elb"}')]),
                new Subnet(subnetId: 'sn-bat', vpcId: 'vpc-custom', availabilityZone: 'us-east-1b',
                        tags: [new Tag(key: 'immutable_metadata', value: '{"purpose":"external","target":"elb"}')]),
                new Subnet(subnetId: 'sn-cat', vpcId: 'vpc-custom', availabilityZone: 'us-east-1c',
                        tags: [new Tag(key: 'immutable_metadata', value: '{"purpose":"external","target":"elb"}')]),
        ])

        when:
        controller.update()

        then:
        flash.message == "Load Balancer 'hello' health check has been updated. "
        1 * awsLoadBalancerService.getLoadBalancer(_, 'hello') >> new LoadBalancerDescription(
                availabilityZones: ['us-east-1a', 'us-east-1b'], subnets: ['sn-ant', 'sn-bat'])
        1 * awsEc2Service.getDefaultVpcSubnetIds(_) >> defaultVpcSubnetIds
        1 * awsEc2Service.getSubnets(_) >> allSubnets

        1 * awsLoadBalancerService.updateSubnets(_, 'hello', ['sn-ant', 'sn-bat'], ['sn-bat', 'sn-cat'])
        1 * awsLoadBalancerService.configureHealthCheck(_, 'hello', new HealthCheck(target: 'HTTP:8080/',
                interval: 5, timeout: 10, unhealthyThreshold: 3, healthyThreshold: 4))
        0 * _

        where:
        defaultVpcSubnetIds            | defaultVpcSubnetsCondition
        []                             | 'do not exist and load balancer has subnets'
        ['sn-123', 'sn-456', 'sn-789'] | 'exist but load balancer has other subnets'
    }

    def 'addListener should fail without instance port'() {
        final cmd = new AddListenerCommand(name: 'app--test')
        cmd.validate()

        when:
        controller.addListener(cmd)

        then:
        response.redirectUrl == '/loadBalancer/prepareListener'
        flash.chainModel.cmd.name == 'app--test'
    }

    def 'addListener should fail with error'() {
        final cmd = new AddListenerCommand(name: 'app--test', protocol: 'http', lbPort: 80, instancePort: 7001)
        cmd.validate()
        controller.awsLoadBalancerService.addListeners(* _) >> {
            throw new IllegalArgumentException("ELB service problems!")
        }

        when:
        controller.addListener(cmd)

        then:
        controller.flash.message == "Could not add listener: java.lang.IllegalArgumentException: ELB service problems!"
        response.redirectUrl == '/loadBalancer/prepareListener'
    }

    def 'addListener should create listener'() {
        final cmd = new AddListenerCommand(name: 'app--test', protocol: 'http', lbPort: 80, instancePort: 7001)
        cmd.validate()

        when:
        controller.addListener(cmd)

        then:
        response.redirectUrl == '/loadBalancer/show/app--test'
        controller.flash.message == "Listener has been added to port 80."
        1 * controller.awsLoadBalancerService.addListeners(_, 'app--test',
                [new Listener(protocol: 'http', loadBalancerPort: 80, instancePort: 7001)])
        0 * _._
    }

    def 'removeListener should delete Listener'() {
        final cmd = new RemoveListenerCommand(name: 'app--test', lbPort: 80)
        cmd.validate()

        when:
        controller.removeListener(cmd)

        then:
        response.redirectUrl == '/loadBalancer/show/app--test'
        controller.flash.message == "Listener on port 80 has been removed."
        1 * controller.awsLoadBalancerService.removeListeners(_, 'app--test', [80])
        0 * _._
    }
}
