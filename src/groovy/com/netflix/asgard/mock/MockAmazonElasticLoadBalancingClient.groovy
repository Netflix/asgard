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
package com.netflix.asgard.mock

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.ClientConfiguration
import com.amazonaws.ResponseMetadata
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckRequest
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckResult
import com.amazonaws.services.elasticloadbalancing.model.CreateAppCookieStickinessPolicyRequest
import com.amazonaws.services.elasticloadbalancing.model.CreateAppCookieStickinessPolicyResult
import com.amazonaws.services.elasticloadbalancing.model.CreateLBCookieStickinessPolicyRequest
import com.amazonaws.services.elasticloadbalancing.model.CreateLBCookieStickinessPolicyResult
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerListenersRequest
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerListenersRequest
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerPolicyRequest
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerPolicyResult
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerResult
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult
import com.amazonaws.services.elasticloadbalancing.model.DisableAvailabilityZonesForLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.DisableAvailabilityZonesForLoadBalancerResult
import com.amazonaws.services.elasticloadbalancing.model.EnableAvailabilityZonesForLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.EnableAvailabilityZonesForLoadBalancerResult
import com.amazonaws.services.elasticloadbalancing.model.HealthCheck
import com.amazonaws.services.elasticloadbalancing.model.Instance
import com.amazonaws.services.elasticloadbalancing.model.InstanceState
import com.amazonaws.services.elasticloadbalancing.model.Listener
import com.amazonaws.services.elasticloadbalancing.model.ListenerDescription
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.amazonaws.services.elasticloadbalancing.model.Policies
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerResult
import com.amazonaws.services.elasticloadbalancing.model.SetLoadBalancerListenerSSLCertificateRequest
import com.amazonaws.services.elasticloadbalancing.model.SetLoadBalancerPoliciesOfListenerRequest
import com.amazonaws.services.elasticloadbalancing.model.SetLoadBalancerPoliciesOfListenerResult
import com.amazonaws.services.elasticloadbalancing.model.SourceSecurityGroup
import org.codehaus.groovy.grails.web.json.JSONArray
import org.joda.time.format.ISODateTimeFormat

class MockAmazonElasticLoadBalancingClient extends AmazonElasticLoadBalancingClient {

    List<LoadBalancerDescription> mockLoadBalancers

    private List<LoadBalancerDescription> loadMockLoadBalancers() {
        JSONArray jsonArray = Mocks.parseJsonString(MockLoadBalancers.DATA)
        return jsonArray.collect {
            new LoadBalancerDescription().withLoadBalancerName(it.loadBalancerName).
            withAvailabilityZones(it.availabilityZones as List).
            withCanonicalHostedZoneName(it.canonicalHostedZoneName).
            withCanonicalHostedZoneNameID(it.canonicalHostedZoneNameID).
            withCreatedTime(ISODateTimeFormat.dateTimeParser().parseDateTime(it.createdTime).toDate()).
            withDNSName(it.DNSName).
            withSourceSecurityGroup(new SourceSecurityGroup().withGroupName(it.sourceSecurityGroup.groupName).
                    withOwnerAlias(it.sourceSecurityGroup.ownerAlias)
            ).
            withHealthCheck(new HealthCheck().withHealthyThreshold(it.healthCheck.healthyThreshold).
                    withInterval(it.healthCheck.interval as Integer).withTarget(it.healthCheck.target).
                    withTimeout(it.healthCheck.timeout as Integer).
                    withUnhealthyThreshold(it.healthCheck.unhealthyThreshold)
            ).
            withInstances(it.instances.collect { def inst -> new Instance().withInstanceId(inst.instanceId) }).
            withListenerDescriptions(it.listenerDescriptions.collect { def listenerDesc ->
                new ListenerDescription().withPolicyNames(listenerDesc.policyNames as List).
                        withListener(new Listener().withInstancePort(listenerDesc.listener.instancePort as Integer).
                                withLoadBalancerPort(listenerDesc.listener.loadBalancerPort as Integer).
                                withProtocol(listenerDesc.listener.protocol).
                                withSSLCertificateId(Mocks.jsonNullable(listenerDesc.listener.SSLCertificateId))
                        )
            }).
            withPolicies(new Policies().
                    withAppCookieStickinessPolicies(it.policies.appCookieStickinessPolicies as List).
                    withLBCookieStickinessPolicies(it.policies.LBCookieStickinessPolicies as List).
                    withOtherPolicies(it.policies.otherPolicies as List))
        }
    }

    MockAmazonElasticLoadBalancingClient(BasicAWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        super(awsCredentials as BasicAWSCredentials, clientConfiguration)
        mockLoadBalancers = loadMockLoadBalancers()
    }

    void setEndpoint(String s) { }

    void createLoadBalancerListeners(CreateLoadBalancerListenersRequest createLoadBalancerListenersRequest) { }

    CreateLBCookieStickinessPolicyResult createLBCookieStickinessPolicy(
            CreateLBCookieStickinessPolicyRequest createLBCookieStickinessPolicyRequest) { null }

    ConfigureHealthCheckResult configureHealthCheck(ConfigureHealthCheckRequest configureHealthCheckRequest) { null }

    DescribeLoadBalancersResult describeLoadBalancers(DescribeLoadBalancersRequest describeLoadBalancersRequest) {
        List<String> requestedNames = describeLoadBalancersRequest.loadBalancerNames

        Collection<LoadBalancerDescription> loadBalancers = mockLoadBalancers
        if (requestedNames.size() >= 1) {
            Boolean everyRequestedNameFound = requestedNames.every { req ->
                mockLoadBalancers.find { it.loadBalancerName == req }
            }
            if (!everyRequestedNameFound) {
                throw Mocks.makeAmazonServiceException(null, 400, 'LoadBalancerNotFound', '123unittest')
            }
            loadBalancers = requestedNames.collect { String requestedName ->
                mockLoadBalancers.find { it.loadBalancerName == requestedName }
            }
        }
        return new DescribeLoadBalancersResult().withLoadBalancerDescriptions(loadBalancers)
    }

    void setLoadBalancerListenerSSLCertificate(
            SetLoadBalancerListenerSSLCertificateRequest setLoadBalancerListenerSSLCertificateRequest) { }

    CreateLoadBalancerResult createLoadBalancer(CreateLoadBalancerRequest createLoadBalancerRequest) { null }

    EnableAvailabilityZonesForLoadBalancerResult enableAvailabilityZonesForLoadBalancer(
            EnableAvailabilityZonesForLoadBalancerRequest enableAvailabilityZonesForLoadBalancerRequest) { null }

    DescribeInstanceHealthResult describeInstanceHealth(DescribeInstanceHealthRequest describeInstanceHealthRequest) {
        String loadBalancerName = describeInstanceHealthRequest.loadBalancerName
        LoadBalancerDescription loadBalancer = mockLoadBalancers.find { it.loadBalancerName == loadBalancerName }
        Collection<InstanceState> instanceStates = loadBalancer.instances.collect {
            new InstanceState().withInstanceId(it.instanceId).withState('InService')
        }
        new DescribeInstanceHealthResult().withInstanceStates(instanceStates)
    }

    DeleteLoadBalancerPolicyResult deleteLoadBalancerPolicy(
            DeleteLoadBalancerPolicyRequest deleteLoadBalancerPolicyRequest) { null }

    DisableAvailabilityZonesForLoadBalancerResult disableAvailabilityZonesForLoadBalancer(
            DisableAvailabilityZonesForLoadBalancerRequest disableAvailabilityZonesForLoadBalancerRequest) { null }

    DeregisterInstancesFromLoadBalancerResult deregisterInstancesFromLoadBalancer(
            DeregisterInstancesFromLoadBalancerRequest deregisterInstancesFromLoadBalancerRequest) { null }

    void deleteLoadBalancerListeners(DeleteLoadBalancerListenersRequest deleteLoadBalancerListenersRequest) { }

    void deleteLoadBalancer(DeleteLoadBalancerRequest deleteLoadBalancerRequest) { }

    CreateAppCookieStickinessPolicyResult createAppCookieStickinessPolicy(
            CreateAppCookieStickinessPolicyRequest createAppCookieStickinessPolicyRequest) { null }

    int timesRegisterWasCalled = 0

    RegisterInstancesWithLoadBalancerResult registerInstancesWithLoadBalancer(
            RegisterInstancesWithLoadBalancerRequest registerInstancesWithLoadBalancerRequest) {
        // Emulate the current known behavior of Amazon's server, which is to respond occasionally with an
        // incorrect "Throttling" message and an error. Amazon reps have told us that the appropriate response is to
        // retry, at least until they fix the problem on their end.
        if (timesRegisterWasCalled <= 0) {
            timesRegisterWasCalled++
            throw Mocks.makeAmazonServiceException(null, 400, 'Throttling', 'c13ff8ac-c469-11e0-a75c-5126cc07407e')
        }
        String loadBalancerName = registerInstancesWithLoadBalancerRequest.loadBalancerName
        LoadBalancerDescription loadBalancer = mockLoadBalancers.find { it.loadBalancerName == loadBalancerName }
        Set<String> instanceIds = new HashSet<String>(loadBalancer.instances*.instanceId)
        instanceIds.addAll(registerInstancesWithLoadBalancerRequest.instances*.instanceId)
        loadBalancer.setInstances(instanceIds.collect { new Instance().withInstanceId(it) } )
        new RegisterInstancesWithLoadBalancerResult().withInstances(loadBalancer.instances)
    }

    SetLoadBalancerPoliciesOfListenerResult setLoadBalancerPoliciesOfListener(
            SetLoadBalancerPoliciesOfListenerRequest setLoadBalancerPoliciesOfListenerRequest) { null }

    DescribeLoadBalancersResult describeLoadBalancers() { new DescribeLoadBalancersResult() }

    void shutdown() { }

    ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest amazonWebServiceRequest) { null }
}
