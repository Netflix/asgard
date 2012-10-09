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

import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerListenersRequest
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerListenersRequest
import com.amazonaws.services.elasticloadbalancing.model.Listener
import com.netflix.asgard.mock.Mocks
import grails.test.MockUtils
import grails.test.mixin.TestFor
import spock.lang.Specification

@SuppressWarnings("GroovyPointlessArithmetic")

@TestFor(LoadBalancerController)
class LoadBalancerControllerSpec extends Specification {

    AmazonElasticLoadBalancing mockElb = Mock(AmazonElasticLoadBalancing)

    void setup() {
        TestUtils.setUpMockRequest()
        request.region = Region.defaultRegion()
        [AddListenerCommand, RemoveListenerCommand].each {
            MockUtils.prepareForConstraintsTests(it)
        }
        controller.awsLoadBalancerService = Mocks.newAwsLoadBalancerService(mockElb)
        mockElb.describeLoadBalancers(_) >> { [] }
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
        mockElb.createLoadBalancerListeners(_) >> {
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
        1 * mockElb.createLoadBalancerListeners(new CreateLoadBalancerListenersRequest(loadBalancerName: 'app--test',
                listeners: [new Listener(protocol: 'http', loadBalancerPort: 80, instancePort: 7001)]))
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
        1 * mockElb.deleteLoadBalancerListeners(new DeleteLoadBalancerListenersRequest(loadBalancerName: 'app--test',
                loadBalancerPorts: [80]))
        0 * _._
    }
}
