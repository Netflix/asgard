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

import com.amazonaws.services.elasticloadbalancing.model.Instance
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.netflix.asgard.mock.Mocks
import grails.test.GrailsUnitTestCase

class AwsLoadBalancerServiceTests extends GrailsUnitTestCase {

    AwsLoadBalancerService elbService

    void setUp() {
        elbService = Mocks.awsLoadBalancerService()
    }

    void testGetLoadBalancerNonExistent() {
        assertNull elbService.getLoadBalancer(Mocks.userContext(), "doesn't exist")
    }

    void testMapInstanceIdsToLoadBalancers() {

        String ideadbeef = 'i-deadbeef'
        String i1234beef = 'i-1234beef'
        String idead5678 = 'i-dead5678'
        String i34343434 = 'i-34343434'
        String i33333333 = 'i-33333333'
        String i44004400 = 'i-44004400'
        String i10101010 = 'i-10101010'
        String i00000000 = 'i-00000000'
        List<String> instanceIds = [ideadbeef, i1234beef, idead5678, i34343434, i33333333, i44004400, i10101010,
                i00000000]

        LoadBalancerDescription helloworldLoadBalancer = new LoadBalancerDescription().
                withLoadBalancerName("helloworld-frontend").withInstances([
            new Instance().withInstanceId(ideadbeef),
            new Instance().withInstanceId(i1234beef),
            new Instance().withInstanceId(idead5678)
        ])
        LoadBalancerDescription apiLoadBalancer = new LoadBalancerDescription().withLoadBalancerName("api-frontend").
                withInstances([
            new Instance().withInstanceId(i34343434),
            new Instance().withInstanceId(i33333333),
            new Instance().withInstanceId(i44004400)
        ])
        LoadBalancerDescription apiProxyLoadBalancer = new LoadBalancerDescription().
                withLoadBalancerName("api-proxy-frontend").withInstances([
            new Instance().withInstanceId(i34343434),
            new Instance().withInstanceId(i1234beef),
            new Instance().withInstanceId(i10101010)
        ])

        AwsLoadBalancerService service = new AwsLoadBalancerService()
        service.metaClass.getLoadBalancers = { UserContext userContext ->
            [helloworldLoadBalancer, apiLoadBalancer, apiProxyLoadBalancer]
        }

        Map instanceIdsToLoadBalancerLists = service.mapInstanceIdsToLoadBalancers(Mocks.userContext(), instanceIds)
        assert 8 == instanceIdsToLoadBalancerLists.size()

        assert 1 == instanceIdsToLoadBalancerLists[ideadbeef].size()
        assert instanceIdsToLoadBalancerLists[ideadbeef].contains(helloworldLoadBalancer)

        assert 2 == instanceIdsToLoadBalancerLists[i1234beef].size()
        assert instanceIdsToLoadBalancerLists[i1234beef].contains(helloworldLoadBalancer)
        assert instanceIdsToLoadBalancerLists[i1234beef].contains(apiProxyLoadBalancer)

        assert 1 == instanceIdsToLoadBalancerLists[idead5678].size()
        assert instanceIdsToLoadBalancerLists[idead5678].contains(helloworldLoadBalancer)

        assert 2 == instanceIdsToLoadBalancerLists[i34343434].size()
        assert instanceIdsToLoadBalancerLists[i34343434].contains(apiLoadBalancer)
        assert instanceIdsToLoadBalancerLists[i34343434].contains(apiProxyLoadBalancer)

        assert 1 == instanceIdsToLoadBalancerLists[i33333333].size()
        assert instanceIdsToLoadBalancerLists[i33333333].contains(apiLoadBalancer)

        assert 1 == instanceIdsToLoadBalancerLists[i44004400].size()
        assert instanceIdsToLoadBalancerLists[i44004400].contains(apiLoadBalancer)

        assert 1 == instanceIdsToLoadBalancerLists[i10101010].size()
        assert instanceIdsToLoadBalancerLists[i10101010].contains(apiProxyLoadBalancer)

        assert 0 == instanceIdsToLoadBalancerLists[i00000000].size()
    }

    void testAddInstances() {
        UserContext userContext = Mocks.userContext()
        LoadBalancerDescription balancer = elbService.getLoadBalancer(userContext, 'helloworld--frontend')
        assertFalse 'i-e268698d' in balancer.instances.collect {
            it.instanceId
        }
        elbService.addInstances(Mocks.userContext(), 'helloworld--frontend', ['i-e268698d'])
        assert 'i-e268698d' in elbService.getLoadBalancer(userContext, 'helloworld--frontend').instances.collect {
            it.instanceId
        }
    }
}
