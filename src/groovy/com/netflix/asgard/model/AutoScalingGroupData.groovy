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
package com.netflix.asgard.model

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.Instance
import com.amazonaws.services.autoscaling.model.TagDescription
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.netflix.asgard.MergedInstance
import com.netflix.asgard.Relationships
import com.netflix.asgard.TagNames
import com.netflix.asgard.Time
import com.netflix.frigga.ami.AppVersion
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.apache.commons.collections.Bag
import org.apache.commons.collections.bag.HashBag
import org.joda.time.DateTime
import org.joda.time.Duration

/**
 * Immutable custom representation of an auto scaling group.
 */
@EqualsAndHashCode
@ToString
class AutoScalingGroupData {

    static final Duration MAX_EXPIRATION_DURATION = Duration.standardDays(7)

    final String autoScalingGroupName
    final Date createdTime
    final Integer minSize
    final Integer maxSize
    final ImmutableList<TagDescription> tags
    final AutoScalingGroupHealthCheckType healthCheckType
    final Integer healthCheckGracePeriod
    final List<String> terminationPolicies
    final ImmutableSet<AutoScalingProcessType> suspendedProcessTypes
    final Integer desiredCapacity
    final String status
    final List<String> availabilityZones
    final Integer defaultCooldown
    final List<GroupedInstance> instances
    final String launchConfigurationName
    final List<String> loadBalancerNames
    final Map<GroupedInstanceState, List<GroupedInstance>> statesToInstanceList
    final Date expirationTime
    final String expirationDurationString
    final Collection<ScalingPolicyData> scalingPolicies
    final String vpcZoneIdentifier

    static AutoScalingGroupData from(AutoScalingGroup awsAutoScalingGroup,
                                     Map<String, Collection<LoadBalancerDescription>> instanceIdsToLoadBalancerLists,
                                     List<MergedInstance> mergedInstances, Map<String, Image> imageIdsToImages,
                                     Collection<ScalingPolicyData> scalingPolicies) {
        new AutoScalingGroupData(
                awsAutoScalingGroup.autoScalingGroupName,
                awsAutoScalingGroup.createdTime,
                awsAutoScalingGroup.minSize,
                awsAutoScalingGroup.maxSize,
                awsAutoScalingGroup.tags,
                awsAutoScalingGroup.healthCheckType,
                awsAutoScalingGroup.healthCheckGracePeriod,
                awsAutoScalingGroup.terminationPolicies,
                (Collection) awsAutoScalingGroup.suspendedProcessTypes,
                awsAutoScalingGroup.desiredCapacity,
                awsAutoScalingGroup.status,
                awsAutoScalingGroup.availabilityZones,
                awsAutoScalingGroup.defaultCooldown,
                awsAutoScalingGroup.instances,
                awsAutoScalingGroup.launchConfigurationName,
                awsAutoScalingGroup.loadBalancerNames,
                instanceIdsToLoadBalancerLists,
                mergedInstances,
                imageIdsToImages,
                scalingPolicies,
                ensureValidVpcZoneIdentifier(awsAutoScalingGroup.VPCZoneIdentifier)
        )
    }

    /** The VPC Zone ID from AWS will be an empty String for non-VPC ASGs. Sending it back to AWS results in an error.*/
    private static ensureValidVpcZoneIdentifier(String vpcZoneIdentifier) {
        vpcZoneIdentifier ?: null
    }

    static AutoScalingGroupData forUpdate(String name, String launchConfigurationName, Integer minSize,
                                          Integer desiredCapacity, Integer maxSize, Integer defaultCooldown,
                                          String healthCheckType, Integer healthCheckGracePeriod,
                                          List<String> terminationPolicies, Collection<String> availabilityZones) {

        new AutoScalingGroupData(
                name,
                null,
                minSize,
                maxSize,
                [],
                healthCheckType,
                healthCheckGracePeriod,
                terminationPolicies,
                [],
                desiredCapacity,
                null,
                availabilityZones,
                defaultCooldown,
                [],
                launchConfigurationName,
                [], [:], [], [:], []
        )
    }

    private AutoScalingGroupData(String autoScalingGroupName, Date createdTime, Integer minSize, Integer maxSize,
            List<TagDescription> tags, String healthCheckType, Integer healthCheckGracePeriod,
            List<String> terminationPolicies, Collection<AutoScalingProcessType> suspendedProcessTypes,
            Integer desiredCapacity, String status, Collection<String> availabilityZones, Integer defaultCooldown,
            List<Instance> instances, String launchConfigurationName, List<String> loadBalancerNames,
            Map<String, Collection<LoadBalancerDescription>> instanceIdsToLoadBalancerLists,
            List<MergedInstance> mergedInstances, Map<String, Image> imageIdsToImages,
            Collection<ScalingPolicyData> scalingPolicies, String vpcZoneIdentifier = null) {

        this.autoScalingGroupName = autoScalingGroupName
        this.createdTime = createdTime ? new Date(createdTime.time) : null
        this.minSize = minSize
        this.maxSize = maxSize
        List newTags = tags.collect {
            new TagDescription(key: it.key, value: it.value, propagateAtLaunch: it.propagateAtLaunch)
        }
        this.tags = ImmutableList.copyOf(newTags)
        this.expirationTime = Time.parse(tags.find { it.key == TagNames.EXPIRATION_TIME }?.value)?.toDate()
        this.expirationDurationString = determineExpirationDurationString()
        this.healthCheckType = AutoScalingGroupHealthCheckType.by(healthCheckType)
        this.healthCheckGracePeriod = healthCheckGracePeriod
        this.terminationPolicies = ImmutableList.copyOf(terminationPolicies)
        this.desiredCapacity = desiredCapacity
        this.status = status
        this.availabilityZones = Collections.unmodifiableList(new ArrayList<String>(availabilityZones).sort())

        this.defaultCooldown = defaultCooldown
        this.suspendedProcessTypes = ImmutableSet.copyOf(suspendedProcessTypes)

        List<GroupedInstance> groupedInstances = instances.collect { Instance inst ->
            String instanceId = inst.instanceId
            Collection<LoadBalancerDescription> lbsForInstance = instanceIdsToLoadBalancerLists?.get(instanceId)
            MergedInstance mergedInstance = mergedInstances?.find { it.instanceId == instanceId }
            Image image = imageIdsToImages?.get(mergedInstance?.amiId)
            GroupedInstance.from(inst, lbsForInstance, mergedInstance, image)
        }.sort { GroupedInstance inst -> inst.launchTime }
        this.instances = Collections.unmodifiableList(groupedInstances)

        this.launchConfigurationName = launchConfigurationName
        this.loadBalancerNames = Collections.unmodifiableList(new ArrayList<String>(loadBalancerNames))
        this.statesToInstanceList = calculateStatesToInstanceList()
        this.scalingPolicies = ImmutableSet.copyOf(scalingPolicies)
        this.vpcZoneIdentifier = vpcZoneIdentifier
    }

    boolean isProcessSuspended(AutoScalingProcessType autoScalingProcessType) {
        autoScalingProcessType in suspendedProcessTypes
    }

    @SuppressWarnings(["GroovyUnusedDeclaration", "Used by front end code"])
    Boolean isDeleteInProgress() {
        status == 'Delete in progress'
    }

    Map<String, String> getVariables() {
        Relationships.parts(autoScalingGroupName)
    }

    @SuppressWarnings(["GroovyUnusedDeclaration", "Used by front end code"])
    AppVersion getMostCommonAppVersion() {

        // Identify the most popular AppVersion
        Bag countsOfAppVersions = new HashBag()
        instances*.appVersion.findAll { it != null }.each { countsOfAppVersions.add(it) }
        Set<AppVersion> appVersions = countsOfAppVersions.uniqueSet()
        AppVersion mostCommon = appVersions.size() > 0 ? appVersions.iterator().next() : null
        if (appVersions.size() > 1) {
            Integer max = 0
            appVersions.each {
                Integer count = countsOfAppVersions.count(it)
                max = Math.max(max, count)
            }
            appVersions.each {
                if (countsOfAppVersions.count(it) == max) {
                    mostCommon = it
                }
            }
        }
        mostCommon
    }

    private Map<GroupedInstanceState, List<GroupedInstance>> calculateStatesToInstanceList() {
        Map<GroupedInstanceState, List<GroupedInstance>> statesToCounts = [:]
        for (GroupedInstance instance in instances) {
            GroupedInstanceState instanceState = new GroupedInstanceState([
                    discoveryStatus: instance.discoveryStatus,
                    imageId: instance.imageId,
                    buildJobName: instance.buildJobName,
                    buildNumber: instance.buildNumber,
                    loadBalancers: instance.loadBalancers,
                    lifecycleState: instance.lifecycleState])

            if (!statesToCounts.containsKey(instanceState)) {
                statesToCounts.put(instanceState, [])
            }
            List<GroupedInstance> instances = statesToCounts.get(instanceState)
            instances.add(instance)
        }
        return statesToCounts.sort { it.value.size() }.asImmutable()
    }

    private String determineExpirationDurationString() {
        DateTime expirationTime = expirationTimeAsDateTime()
        if (!expirationTime) { return null }

        DateTime now = Time.now()
        if (now.isAfter(expirationTime)) { return 'the past' }

        Time.format(now, expirationTime, Time.DAY_HR_MIN)
    }

    DateTime expirationTimeAsDateTime() {
        expirationTime ? new DateTime(expirationTime) : null
    }

    Collection<AutoScalingProcessType> getSuspendedPrimaryProcessTypes() {
        AutoScalingProcessType.with { [Launch, Terminate] }.intersect(suspendedProcessTypes)
    }

    @SuppressWarnings(["GroovyUnusedDeclaration", "Used by front end code"])
    Boolean seemsDisabled() {
        return (getSuspendedPrimaryProcessTypes() || expirationTime)
    }

    @SuppressWarnings(["GroovyUnusedDeclaration", "Used by front end code"])
    boolean isZoneRebalancingSuspended() {
        isProcessSuspended(AutoScalingProcessType.AZRebalance)
    }

    boolean isLaunchingSuspended() {
        isProcessSuspended(AutoScalingProcessType.Launch)
    }

    boolean isTerminatingSuspended() {
        isProcessSuspended(AutoScalingProcessType.Terminate)
    }

    boolean isAddingToLoadBalancerSuspended() {
        isProcessSuspended(AutoScalingProcessType.AddToLoadBalancer)
    }
}
