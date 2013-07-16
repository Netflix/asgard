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
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.autoscaling.model.ScalingPolicy
import com.amazonaws.services.autoscaling.model.ScheduledUpdateGroupAction
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.amazonaws.services.ec2.model.AvailabilityZone
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.KeyPairInfo
import com.amazonaws.services.ec2.model.ReservedInstances
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.ec2.model.Snapshot
import com.amazonaws.services.ec2.model.SpotInstanceRequest
import com.amazonaws.services.ec2.model.Subnet
import com.amazonaws.services.ec2.model.Volume
import com.amazonaws.services.ec2.model.Vpc
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.amazonaws.services.elasticloadbalancing.model.SourceSecurityGroup
import com.amazonaws.services.rds.model.DBInstance
import com.amazonaws.services.rds.model.DBSecurityGroup
import com.amazonaws.services.rds.model.DBSnapshot
import com.amazonaws.services.simpleworkflow.model.ActivityTypeInfo
import com.amazonaws.services.simpleworkflow.model.DomainInfo
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfo
import com.amazonaws.services.simpleworkflow.model.WorkflowTypeInfo
import com.netflix.asgard.model.ApplicationInstance
import com.netflix.asgard.model.HardwareProfile
import com.netflix.asgard.model.InstanceHealth
import com.netflix.asgard.model.InstanceTypeData
import com.netflix.asgard.model.MetricId
import com.netflix.asgard.model.MultiRegionInstancePrices
import com.netflix.asgard.model.SimpleQueue
import com.netflix.asgard.model.TopicData
import com.netflix.asgard.push.Cluster

/**
 * By creating all these object upfront and letting other services initialize them only if needed, we remove the problem
 * of repeatedly needing to recreate and reload the caches during development of service classes.
 */
class Caches {

    final CachedMap<ActivityTypeInfo> allActivityTypes
    final CachedMap<WorkflowExecutionInfo> allClosedWorkflowExecutions
    final CachedMap<WorkflowExecutionInfo> allOpenWorkflowExecutions
    final CachedMap<AppRegistration> allApplications
    final CachedMap<MetricId> allCustomMetrics
    final CachedMap<HardwareProfile> allHardwareProfiles
    final CachedMap<String> allTerminationPolicyTypes
    final CachedMap<WorkflowTypeInfo> allWorkflowTypes
    final CachedMap<DomainInfo> allWorkflowDomains

    final MultiRegionCachedMap<MetricAlarm> allAlarms
    final MultiRegionCachedMap<ApplicationInstance> allApplicationInstances
    final MultiRegionCachedMap<AutoScalingGroup> allAutoScalingGroups
    final MultiRegionCachedMap<AvailabilityZone> allAvailabilityZones
    final MultiRegionCachedMap<Cluster> allClusters
    final MultiRegionCachedMap<DBInstance> allDBInstances
    final MultiRegionCachedMap<DBSecurityGroup> allDBSecurityGroups
    final MultiRegionCachedMap<DBSnapshot> allDBSnapshots
    final MultiRegionCachedMap<String> allDomains
    final MultiRegionCachedMap<String> allEurekaAddresses
    final MultiRegionCachedMap<FastProperty> allFastProperties
    final MultiRegionCachedMap<Image> allImages
    final MultiRegionCachedMap<Instance> allInstances
    final MultiRegionCachedMap<InstanceHealth> allSignificantStackInstanceHealthChecks
    final MultiRegionCachedMap<InstanceTypeData> allInstanceTypes
    final MultiRegionCachedMap<KeyPairInfo> allKeyPairs
    final MultiRegionCachedMap<LaunchConfiguration> allLaunchConfigurations
    final MultiRegionCachedMap<LoadBalancerDescription> allLoadBalancers
    final MultiRegionCachedMap<SimpleQueue> allQueues
    final MultiRegionCachedMap<ReservedInstances> allReservedInstancesGroups
    final MultiRegionCachedMap<ScalingPolicy> allScalingPolicies
    final MultiRegionCachedMap<ScheduledUpdateGroupAction> allScheduledActions
    final MultiRegionCachedMap<SecurityGroup> allSecurityGroups
    final MultiRegionCachedMap<Snapshot> allSnapshots
    final MultiRegionCachedMap<SourceSecurityGroup> allSourceSecurityGroups
    final MultiRegionCachedMap<SpotInstanceRequest> allSpotInstanceRequests
    final MultiRegionCachedMap<Subnet> allSubnets
    final MultiRegionCachedMap<TopicData> allTopics
    final MultiRegionCachedMap<Volume> allVolumes
    final MultiRegionCachedMap<Vpc> allVpcs

    final MultiRegionInstancePrices allOnDemandPrices
    final MultiRegionInstancePrices allReservedPrices
    final MultiRegionInstancePrices allSpotPrices

    Caches(CachedMapBuilder cachedMapBuilder, ConfigService configService = null) {

        allClusters = cachedMapBuilder.of(EntityType.cluster).buildMultiRegionCachedMap()
        allAutoScalingGroups = cachedMapBuilder.of(EntityType.autoScaling, 120).buildMultiRegionCachedMap()
        allLaunchConfigurations = cachedMapBuilder.of(EntityType.launchConfiguration, 180).buildMultiRegionCachedMap()
        allLoadBalancers = cachedMapBuilder.of(EntityType.loadBalancer, 120).buildMultiRegionCachedMap()
        allSourceSecurityGroups = cachedMapBuilder.of(EntityType.sourceSecurityGroup).buildMultiRegionCachedMap()
        allAvailabilityZones = cachedMapBuilder.of(EntityType.availabilityZone, 3600).buildMultiRegionCachedMap()
        allSubnets = cachedMapBuilder.of(EntityType.subnet, 3600).buildMultiRegionCachedMap()
        allVpcs = cachedMapBuilder.of(EntityType.vpc, 3600).buildMultiRegionCachedMap()
        allKeyPairs = cachedMapBuilder.of(EntityType.keyPair).buildMultiRegionCachedMap()
        allImages = cachedMapBuilder.of(EntityType.image, 120).buildMultiRegionCachedMap()
        allInstances = cachedMapBuilder.of(EntityType.instance, 120).buildMultiRegionCachedMap()
        allSpotInstanceRequests = cachedMapBuilder.of(EntityType.spotInstanceRequest, 120).buildMultiRegionCachedMap()
        allApplicationInstances = cachedMapBuilder.of(EntityType.applicationInstance, 60).buildMultiRegionCachedMap()
        allReservedInstancesGroups = cachedMapBuilder.of(EntityType.reservation, 3600).buildMultiRegionCachedMap()
        allSecurityGroups = cachedMapBuilder.of(EntityType.security, 120).buildMultiRegionCachedMap()
        allSnapshots = cachedMapBuilder.of(EntityType.snapshot, 300).buildMultiRegionCachedMap()
        allVolumes = cachedMapBuilder.of(EntityType.volume, 300).buildMultiRegionCachedMap()
        allDomains = cachedMapBuilder.of(EntityType.domain, 120).buildMultiRegionCachedMap()
        allEurekaAddresses = cachedMapBuilder.of(EntityType.eurekaAddress, 120).buildMultiRegionCachedMap()
        allTopics = cachedMapBuilder.of(EntityType.topic, 120).buildMultiRegionCachedMap()
        allQueues = cachedMapBuilder.of(EntityType.queue, 120).buildMultiRegionCachedMap()
        allAlarms = cachedMapBuilder.of(EntityType.alarm, 120).buildMultiRegionCachedMap()
        allDBInstances = cachedMapBuilder.of(EntityType.rdsInstance, 120).buildMultiRegionCachedMap()
        allDBSecurityGroups = cachedMapBuilder.of(EntityType.dbSecurity, 120).buildMultiRegionCachedMap()
        allDBSnapshots = cachedMapBuilder.of(EntityType.dbSnapshot, 120).buildMultiRegionCachedMap()
        allFastProperties = cachedMapBuilder.of(EntityType.fastProperty, 180).buildMultiRegionCachedMap(configService?.
                platformServiceRegions)
        allScalingPolicies = cachedMapBuilder.of(EntityType.scalingPolicy, 120).buildMultiRegionCachedMap()
        allScheduledActions = cachedMapBuilder.of(EntityType.scheduledAction, 120).buildMultiRegionCachedMap()
        allSignificantStackInstanceHealthChecks = cachedMapBuilder.of(EntityType.instanceHealth, 300).
                buildMultiRegionCachedMap()
        allActivityTypes = cachedMapBuilder.of(EntityType.activityType, 120).buildCachedMap()
        allOpenWorkflowExecutions = cachedMapBuilder.of(EntityType.workflowExecution, 30).buildCachedMap()
        allClosedWorkflowExecutions = cachedMapBuilder.of(EntityType.workflowExecution, 30).buildCachedMap()
        allApplications = cachedMapBuilder.of(EntityType.application, 120).buildCachedMap()
        allCustomMetrics = cachedMapBuilder.of(EntityType.metric, 120).buildCachedMap()
        allWorkflowTypes = cachedMapBuilder.of(EntityType.workflowType, 120).buildCachedMap()
        allWorkflowDomains = cachedMapBuilder.of(EntityType.workflowDomain, 3600).buildCachedMap()

        // Use one thread for all instance type and pricing caches. None of these need updating more than once an hour.
        allHardwareProfiles = cachedMapBuilder.of(EntityType.hardwareProfile, 3600).buildCachedMap()
        allOnDemandPrices = MultiRegionInstancePrices.create('On Demand Prices')
        allReservedPrices = MultiRegionInstancePrices.create('Reserved Prices')
        allSpotPrices = MultiRegionInstancePrices.create('Spot Prices')
        allInstanceTypes = cachedMapBuilder.of(EntityType.instanceType).buildMultiRegionCachedMap()
        allTerminationPolicyTypes = cachedMapBuilder.of(EntityType.terminationPolicyType, 3600).buildCachedMap()
    }
}
