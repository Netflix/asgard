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

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing
import com.amazonaws.services.elasticloadbalancing.model.AttachLoadBalancerToSubnetsRequest
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckRequest
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerListenersRequest
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerListenersRequest
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest
import com.amazonaws.services.elasticloadbalancing.model.DetachLoadBalancerFromSubnetsRequest
import com.amazonaws.services.elasticloadbalancing.model.DisableAvailabilityZonesForLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.EnableAvailabilityZonesForLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.Instance
import com.amazonaws.services.elasticloadbalancing.model.InstanceState
import com.amazonaws.services.elasticloadbalancing.model.Listener
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.SourceSecurityGroup
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.netflix.asgard.cache.CacheInitializer
import com.netflix.asgard.model.InstanceStateData
import com.netflix.asgard.model.SubnetTarget
import com.netflix.asgard.model.Subnets
import org.springframework.beans.factory.InitializingBean

class AwsLoadBalancerService implements CacheInitializer, InitializingBean {

    static transactional = false

    MultiRegionAwsClient<AmazonElasticLoadBalancing> awsClient
    def grailsApplication
    def awsClientService
    def awsEc2Service
    Caches caches
    def configService
    def taskService

    void afterPropertiesSet() {
        awsClient = awsClient ?: new MultiRegionAwsClient<AmazonElasticLoadBalancing>( { Region region ->
            AmazonElasticLoadBalancing client = awsClientService.create(AmazonElasticLoadBalancing)
            client.setEndpoint("elasticloadbalancing.${region}.amazonaws.com")
            client
        })
    }

    void initializeCaches() {
        caches.allSourceSecurityGroups.ensureSetUp(
                { Region region -> caches.allLoadBalancers.by(region).list().collect { it.sourceSecurityGroup }
                        .findAll { it != null } } )
        caches.allLoadBalancers.ensureSetUp({ Region region -> retrieveLoadBalancers(region) },
                { Region region -> caches.allSourceSecurityGroups.by(region).fill() })
    }

    // Source Security Groups

    Collection<SourceSecurityGroup> getSourceSecurityGroups(UserContext userContext) {
        caches.allSourceSecurityGroups.by(userContext.region).list()
    }

    SourceSecurityGroup getSourceSecurityGroup(UserContext userContext, String name) {
        caches.allSourceSecurityGroups.by(userContext.region).get(name)
    }

    // Load Balancers

    private List<LoadBalancerDescription> retrieveLoadBalancers(Region region) {
        awsClient.by(region).describeLoadBalancers(new DescribeLoadBalancersRequest()).getLoadBalancerDescriptions()
    }

    Collection<LoadBalancerDescription> getLoadBalancers(UserContext userContext) {
        caches.allLoadBalancers.by(userContext.region).list()
    }

    List<LoadBalancerDescription> getLoadBalancersForApp(UserContext userContext, String appName) {
        getLoadBalancers(userContext).findAll {
            Relationships.appNameFromLaunchConfigName(it.loadBalancerName) == appName
        }
    }

    /**
     * Finds all the load balancers with the specified security group.
     *
     * @param userContext who, where, why
     * @param group the security group for which to find associated load balancers
     * @return the load balancers that have the specified security group
     */
    List<LoadBalancerDescription> getLoadBalancersWithSecurityGroup(UserContext userContext, SecurityGroup group) {
        getLoadBalancers(userContext).findAll {
            group.groupName in it.securityGroups || group.groupId in it.securityGroups
        }
    }

    String getAppNameForLoadBalancer(String name) {
        Relationships.appNameFromLoadBalancerName(name)
    }

    LoadBalancerDescription getLoadBalancer(UserContext userContext, String name, From from = From.AWS) {
        if (!name) { return null }
        if (from == From.CACHE) {
            return caches.allLoadBalancers.by(userContext.region).get(name)
        }
        LoadBalancerDescription loadBalancer
        try {
            def loadBalancers = awsClient.by(userContext.region).describeLoadBalancers(
                    new DescribeLoadBalancersRequest().withLoadBalancerNames([name])).getLoadBalancerDescriptions()
            loadBalancer = Check.lone(loadBalancers, LoadBalancerDescription)
        } catch (AmazonServiceException ignored) {
            loadBalancer = null
        }
        if (from != From.AWS_NOCACHE) {
            caches.allLoadBalancers.by(userContext.region).put(name, loadBalancer)
        }
        loadBalancer
    }

    List<LoadBalancerDescription> getLoadBalancersFor(UserContext userContext, String instanceId) {
        if (!instanceId) { return [] }
        getLoadBalancers(userContext).findAll { it.instances.any { it.instanceId == instanceId } }
    }

    Map<String, Collection<LoadBalancerDescription>> mapInstanceIdsToLoadBalancers(UserContext userContext,
                                                                                   List<String> instanceIds) {
        Collection<LoadBalancerDescription> loadBalancers = getLoadBalancers(userContext)
        Multimap<String, LoadBalancerDescription> instanceIdsToLoadBalancers = ArrayListMultimap.create()
        for (LoadBalancerDescription loadBalancer : loadBalancers) {
            for (Instance instance : loadBalancer.instances) {
                instanceIdsToLoadBalancers.put(instance.instanceId, loadBalancer)
            }
        }
        Map<String, Collection<LoadBalancerDescription>> result = instanceIdsToLoadBalancers.asMap().subMap(instanceIds)
        // subMap() puts missing keys as null values, we want them initialized as empty lists
        result.each { key, value ->
            if (value == null) {
                result[key] = []
            }
        }
    }

    /**
     * Gets the list of instances registered with the specified load balancer, along with the name of the auto scaling
     * group and availability zone of each instance. Results are sorted by availability zone first, then by auto scaling
     * group within a zone.
     *
     * @param userContext who, where, why
     * @param name the name of the load balancer to inspect
     * @param groups the auto scaling groups to check for ownership and zone info for the instances
     * @return list of {@link InstanceStateData} objects associated with the load balancer
     */
    List<InstanceStateData> getInstanceStateDatas(UserContext userContext, String name,
                                                            List<AutoScalingGroup> groups) {
        DescribeInstanceHealthRequest request = new DescribeInstanceHealthRequest().withLoadBalancerName(name)
        List<InstanceState> states = awsClient.by(userContext.region).describeInstanceHealth(request).instanceStates
        Map<String, String> instanceIdsToGroupNames = [:]
        Map<String, String> instanceIdsToZones = [:]
        for (AutoScalingGroup group in groups) {
            for (com.amazonaws.services.autoscaling.model.Instance asgInstance in group.instances) {
                String instanceId = asgInstance.instanceId
                instanceIdsToGroupNames.put(instanceId, group.autoScalingGroupName)
                instanceIdsToZones.put(instanceId, asgInstance.availabilityZone)
            }
        }
        List<InstanceStateData> instanceStateDatas = states.collect { InstanceState instanceState ->
            String instanceId = instanceState.instanceId
            String autoScalingGroupName = instanceIdsToGroupNames[instanceId]
            String availabilityZone = instanceIdsToZones[instanceId]
            new InstanceStateData(instanceId: instanceId, state: instanceState.state,
                    reasonCode: instanceState.reasonCode, description: instanceState.description,
                    autoScalingGroupName: autoScalingGroupName, availabilityZone: availabilityZone)
        }
        // Initial sort by zone. Within zone, sort by ASG.
        instanceStateDatas.sort { it.autoScalingGroupName }.sort { it.availabilityZone }
    }

    // mutators

    LoadBalancerDescription createLoadBalancer(UserContext userContext, String name, List<String> zoneList,
            Collection<Listener> listeners, Collection<String> securityGroups, String subnetPurpose) {
        taskService.runTask(userContext, "Create Load Balancer ${name}", { task ->
            def request = new CreateLoadBalancerRequest(loadBalancerName: name, listeners: listeners,
                    securityGroups: securityGroups)
            if (subnetPurpose) {
                if (subnetPurpose in configService.getInternalSubnetPurposes()) {
                    request.scheme = 'internal'
                }
                // If this is a VPC ELB then we must find the proper subnets and add them.
                Subnets subnets = awsEc2Service.getSubnets(userContext)
                List<String> subnetIds = subnets.getSubnetIdsForZones(zoneList, subnetPurpose, SubnetTarget.ELB)
                request.withSubnets(subnetIds)
            } else {
                request.withAvailabilityZones(zoneList)
            }
            awsClient.by(userContext.region).createLoadBalancer(request)  // has result
        }, Link.to(EntityType.loadBalancer, name))
        getLoadBalancer(userContext, name)
    }

    LoadBalancerDescription addZones(UserContext userContext, String name, Collection<String> zones) {
        taskService.runTask(userContext, "Add zones ${zones} to Load Balancer ${name}", { task ->
            def request = new EnableAvailabilityZonesForLoadBalancerRequest()
                    .withLoadBalancerName(name)
                    .withAvailabilityZones(zones)
            awsClient.by(userContext.region).enableAvailabilityZonesForLoadBalancer(request)  // has result
        }, Link.to(EntityType.loadBalancer, name))
        getLoadBalancer(userContext, name)
    }

    List<String> updateSubnets(UserContext userContext, String name, Collection<String> oldSubnetIds,
            Collection<String> newSubnetIds) {
        Collection<String> addedSubnetIds = newSubnetIds - oldSubnetIds
        Collection<String> removedSubnetIds = oldSubnetIds - newSubnetIds
        List<String> messages = []
        taskService.runTask(userContext, "Updating subnets of Load Balancer '${name}'", { Task task ->
            if (addedSubnetIds) {
                String workDescription = "${displayItems('subnet', addedSubnetIds)} to Load Balancer '${name}'."
                task.log("Add ${workDescription}")
                awsClient.by(userContext.region).attachLoadBalancerToSubnets(
                        new AttachLoadBalancerToSubnetsRequest(loadBalancerName: name, subnets: addedSubnetIds))
                messages << "Added ${workDescription}"
            }
            if (removedSubnetIds) {
                String workDescription = "${displayItems('subnet', removedSubnetIds)} from Load Balancer '${name}'."
                task.log("Remove ${workDescription}")
                awsClient.by(userContext.region).detachLoadBalancerFromSubnets(
                        new DetachLoadBalancerFromSubnetsRequest(loadBalancerName: name, subnets: removedSubnetIds))
                messages << "Removed ${workDescription}"
            }
        }, Link.to(EntityType.loadBalancer, name))
        getLoadBalancer(userContext, name)
        messages
    }

    private String displayItems(String type, Collection<String> items) {
        "${type}${items.size() == 1 ? '' : 's'} ${items}"
    }

    LoadBalancerDescription removeZones(UserContext userContext, String name, Collection<String> zones) {
        taskService.runTask(userContext, "Remove zones ${zones} from Load Balancer ${name}", { task ->
            def request = new DisableAvailabilityZonesForLoadBalancerRequest()
                    .withLoadBalancerName(name)
                    .withAvailabilityZones(zones)
            awsClient.by(userContext.region).disableAvailabilityZonesForLoadBalancer(request)  // has result
        }, Link.to(EntityType.loadBalancer, name))
        getLoadBalancer(userContext, name)
    }

    void removeLoadBalancer(UserContext userContext, String name) {
        taskService.runTask(userContext, "Remove Load Balancer ${name}", { task ->
            def request = new DeleteLoadBalancerRequest()
                    .withLoadBalancerName(name)
            awsClient.by(userContext.region).deleteLoadBalancer(request)  // no result
        }, Link.to(EntityType.loadBalancer, name))
        caches.allLoadBalancers.by(userContext.region).remove(name)
    }

    LoadBalancerDescription configureHealthCheck(UserContext userContext, String name, healthcheck) {
        taskService.runTask(userContext, "Configure Load Balancer ${name} health check ${healthcheck}", { task ->
            def request = new ConfigureHealthCheckRequest()
                    .withLoadBalancerName(name)
                    .withHealthCheck(healthcheck)
            awsClient.by(userContext.region).configureHealthCheck(request)  // has result
        }, Link.to(EntityType.loadBalancer, name))
        getLoadBalancer(userContext, name)
    }

    private Closure checkIfExceptionIsThrottlingError = { Exception e ->
        return (e instanceof AmazonServiceException) && (e.errorCode == 'Throttling')
    }

    LoadBalancerDescription addInstances(UserContext userContext, String name, Collection<String> instanceIds,
                                         Task existingTask = null) {
        // Limit rate of instance changes to avoid Amazon limitation.
        taskService.runTask(userContext, "Add instances ${instanceIds} to Load Balancer ${name}", { Task task ->
            def instances = instanceIds.collect { new Instance().withInstanceId(it) } // elasticloadbalancing.model.Instance type
            RegisterInstancesWithLoadBalancerRequest request = new RegisterInstancesWithLoadBalancerRequest()
                    .withLoadBalancerName(name)
                    .withInstances(instances)
            task.tryUntilSuccessful(
                    { awsClient.by(userContext.region).registerInstancesWithLoadBalancer(request) },
                    checkIfExceptionIsThrottlingError)
            task.log("Registered instances ${instanceIds} in load balancer ${name}")
            Time.sleepCancellably(200)
        }, Link.to(EntityType.loadBalancer, name), existingTask)
        getLoadBalancer(userContext, name)
    }

    LoadBalancerDescription removeInstances(UserContext userContext, String name, Collection<String> instanceIds,
                                            Task existingTask = null) {
        Closure work = { Task task ->
            LoadBalancerDescription loadBalancer = getLoadBalancer(userContext, name)
            List<String> registeredInstanceIds = loadBalancer?.instances*.instanceId
            List<String> instanceIdsToDeregister = registeredInstanceIds?.findAll { it in instanceIds }
            if (!instanceIdsToDeregister) {
                return
            }
            List<Instance> instances = instanceIdsToDeregister.collect { new Instance().withInstanceId(it) } // elasticloadbalancing.model.Instance type
            DeregisterInstancesFromLoadBalancerRequest request = new DeregisterInstancesFromLoadBalancerRequest()
            request.withLoadBalancerName(name).withInstances(instances)
            task.tryUntilSuccessful(
                    { awsClient.by(userContext.region).deregisterInstancesFromLoadBalancer(request) },
                    checkIfExceptionIsThrottlingError)
            task.log("Deregistered instances ${instanceIdsToDeregister} from load balancer ${name}")
        }

        String msg = "Remove instances ${instanceIds} from Load Balancer ${name}"
        taskService.runTask(userContext, msg, work, Link.to(EntityType.loadBalancer, name), existingTask)
        getLoadBalancer(userContext, name)
    }

    void addListeners(UserContext userContext, String lbName, List<Listener> listeners, Task existingTask = null) {
        CreateLoadBalancerListenersRequest request = new CreateLoadBalancerListenersRequest(loadBalancerName: lbName,
                listeners: listeners)
        taskService.runTask(userContext, "Adding listeners to load balancer '${lbName}'", { task ->
            awsClient.by(userContext.region).createLoadBalancerListeners(request)
        }, Link.to(EntityType.loadBalancer, lbName), existingTask)
    }

    void removeListeners(UserContext userContext, String lbName, Collection<Integer> ports, Task existingTask = null) {
        DeleteLoadBalancerListenersRequest request = new DeleteLoadBalancerListenersRequest(loadBalancerName: lbName,
                loadBalancerPorts: ports)
        taskService.runTask(userContext, "Removing listeners from load balancer '${lbName}'", { task ->
            awsClient.by(userContext.region).deleteLoadBalancerListeners(request)
        }, Link.to(EntityType.loadBalancer, lbName), existingTask)
    }

    //CreateAppCookieStickinessPolicy
    //CreateLBCookieStickinessPolicy
    //SetLoadBalancerPoliciesOfListener
    //DeleteLoadBalancerPolicy

}
