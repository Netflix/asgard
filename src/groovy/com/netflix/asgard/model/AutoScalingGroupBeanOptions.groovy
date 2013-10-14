/*
 * Copyright 2013 Netflix, Inc.
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
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest
import com.amazonaws.services.autoscaling.model.Tag
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest
import groovy.transform.Canonical

/**
 * Attributes specified when manipulating auto scaling groups.
 */
@Canonical class AutoScalingGroupBeanOptions extends BeanOptions {

    /** @see AutoScalingGroup#autoScalingGroupName */
    String autoScalingGroupName

    /** @see AutoScalingGroup#launchConfigurationName */
    String launchConfigurationName

    /** @see AutoScalingGroup#minSize */
    Integer minSize

    /** @see AutoScalingGroup#maxSize */
    Integer maxSize

    /** @see AutoScalingGroup#desiredCapacity */
    Integer desiredCapacity

    /** @see AutoScalingGroup#defaultCooldown */
    Integer defaultCooldown

    /** @see AutoScalingGroup#availabilityZones */
    Set<String> availabilityZones

    /** @see AutoScalingGroup#loadBalancerNames */
    Set<String> loadBalancerNames

    /** @see AutoScalingGroup#healthCheckType */
    AutoScalingGroupHealthCheckType healthCheckType

    /** @see AutoScalingGroup#healthCheckGracePeriod */
    Integer healthCheckGracePeriod

    /** @see AutoScalingGroup#placementGroup */
    String placementGroup

    /** Subnet purpose is used to identify the corresponding subnet and generate the VPC zone identifier. */
    String subnetPurpose

    /** @see AutoScalingGroup#terminationPolicies */
    Set<String> terminationPolicies

    /** @see AutoScalingGroup#tags */
    Set<Tag> tags

    /** @see AutoScalingGroup#suspendedProcesses */
    Set<AutoScalingProcessType> suspendedProcesses

    void setAvailabilityZones(Collection<String> availabilityZones) {
        this.availabilityZones = copyNonNullToSet(availabilityZones)
    }

    void setLoadBalancerNames(Collection<String> loadBalancerNames) {
        this.loadBalancerNames = copyNonNullToSet(loadBalancerNames)
    }

    void setTerminationPolicies(Collection<String> terminationPolicies) {
        this.terminationPolicies = copyNonNullToSet(terminationPolicies)
    }

    void setTags(Collection<Tag> tags) {
        this.tags = copyTags(tags)
    }

    void setSuspendedProcesses(Collection<AutoScalingProcessType> suspendedProcesses) {
        this.suspendedProcesses = copyNonNullToSet(suspendedProcesses)
    }

    @SuppressWarnings('ReturnsNullInsteadOfEmptyCollection')
    private static Set<Tag> copyTags(Collection tags) {
        if (tags == null) { return null }
        tags.collect {
            new Tag(resourceId: it.resourceId, resourceType: it.resourceType, key: it.key, value: it.value)
        } as Set
    }

    private String constructVpcZoneIdentifier(Subnets subnets) {
        subnets.constructNewVpcZoneIdentifierForPurposeAndZones(subnetPurpose, availabilityZones)
    }

    /**
     * Clone options.
     *
     * @param source state
     * @return a deep copy of the source state
     */
    static AutoScalingGroupBeanOptions from(AutoScalingGroupBeanOptions source) {
        new AutoScalingGroupBeanOptions(
                autoScalingGroupName: source.autoScalingGroupName,
                launchConfigurationName: source.launchConfigurationName,
                minSize: source.minSize,
                maxSize: source.maxSize,
                desiredCapacity: source.desiredCapacity,
                defaultCooldown: source.defaultCooldown,
                availabilityZones: copyNonNullToSet(source.availabilityZones),
                loadBalancerNames: copyNonNullToSet(source.loadBalancerNames),
                healthCheckType: source.healthCheckType,
                healthCheckGracePeriod: source.healthCheckGracePeriod,
                placementGroup: source.placementGroup,
                subnetPurpose: source.subnetPurpose,
                terminationPolicies: copyNonNullToSet(source.terminationPolicies),
                tags: copyTags(source.tags),
                suspendedProcesses: copyNonNullToSet(source.suspendedProcesses)
        )
    }

    /**
     * Copy options from an AutoScalingGroup.
     *
     * @param group state to copy
     * @param subnets for VPC zone identifier identification
     * @return a deep copy of the group
     */
    static AutoScalingGroupBeanOptions from(AutoScalingGroup group, Subnets subnets) {
        String subnetPurpose = null
        if (group.getVPCZoneIdentifier()) {
            subnetPurpose = subnets.getPurposeFromVpcZoneIdentifier(group.getVPCZoneIdentifier())
        }
        Set<AutoScalingProcessType> suspendedProcesses = group.suspendedProcesses.collect {
            AutoScalingProcessType.parse(it.processName)
        }
        group.with {
            new AutoScalingGroupBeanOptions(
                    autoScalingGroupName: autoScalingGroupName,
                    launchConfigurationName: launchConfigurationName,
                    minSize: minSize,
                    maxSize: maxSize,
                    desiredCapacity: desiredCapacity,
                    defaultCooldown: defaultCooldown,
                    availabilityZones: copyNonNullToSet(availabilityZones),
                    loadBalancerNames: copyNonNullToSet(loadBalancerNames),
                    healthCheckType: AutoScalingGroupHealthCheckType.by(healthCheckType),
                    healthCheckGracePeriod: healthCheckGracePeriod,
                    placementGroup: placementGroup,
                    subnetPurpose: subnetPurpose,
                    terminationPolicies: copyNonNullToSet(terminationPolicies),
                    tags: copyTags(tags),
                    suspendedProcesses: copyNonNullToSet(suspendedProcesses)
            )
        }
    }

    /**
     * Construct CreateAutoScalingGroupRequest.
     *
     * @param subnets for VPC zone identifier generation
     * @return a CreateAutoScalingGroupRequest based on these options
     */
    CreateAutoScalingGroupRequest getCreateAutoScalingGroupRequest(Subnets subnets) {
        String vpcZoneIdentifier = constructVpcZoneIdentifier(subnets)
        new CreateAutoScalingGroupRequest(
                autoScalingGroupName: autoScalingGroupName,
                launchConfigurationName: launchConfigurationName,
                minSize: minSize,
                maxSize: maxSize,
                desiredCapacity: desiredCapacity,
                defaultCooldown: defaultCooldown,
                availabilityZones: copyNonNullToSet(availabilityZones),
                loadBalancerNames: copyNonNullToSet(loadBalancerNames),
                healthCheckType: healthCheckType?.name(),
                healthCheckGracePeriod: healthCheckGracePeriod,
                placementGroup: placementGroup,
                vPCZoneIdentifier: vpcZoneIdentifier,
                terminationPolicies: copyNonNullToSet(terminationPolicies),
                tags: copyTags(tags)
        )
    }

    /**
     * Construct UpdateAutoScalingGroupRequest.
     *
     * @param subnets for VPC zone identifier generation
     * @return a UpdateAutoScalingGroupRequest based on these options
     */
    UpdateAutoScalingGroupRequest getUpdateAutoScalingGroupRequest(Subnets subnets) {
        String vpcZoneIdentifier = null
        if (subnetPurpose != null) {
            vpcZoneIdentifier = constructVpcZoneIdentifier(subnets)
        }
        new UpdateAutoScalingGroupRequest(
                autoScalingGroupName: autoScalingGroupName,
                launchConfigurationName: launchConfigurationName,
                minSize: minSize,
                maxSize: maxSize,
                desiredCapacity: desiredCapacity,
                defaultCooldown: defaultCooldown,
                availabilityZones: copyNonNullToSet(availabilityZones),
                healthCheckType: healthCheckType?.name(),
                healthCheckGracePeriod: healthCheckGracePeriod,
                placementGroup: placementGroup,
                vPCZoneIdentifier: vpcZoneIdentifier,
                terminationPolicies: copyNonNullToSet(terminationPolicies)
        )
    }

    private Collection<String> convertToProcessNames(Collection<AutoScalingProcessType> processes) {
        processes.collect { it.name() }
    }

    private SuspendProcessesRequest constructSuspendProcessesRequest(Collection<AutoScalingProcessType> processes) {
        new SuspendProcessesRequest(
                autoScalingGroupName: autoScalingGroupName,
                scalingProcesses: convertToProcessNames(processes)
        )
    }

    private ResumeProcessesRequest constructResumeProcessesRequest(Collection<AutoScalingProcessType> processes) {
        new ResumeProcessesRequest(
                autoScalingGroupName: autoScalingGroupName,
                scalingProcesses: convertToProcessNames(processes)
        )
    }

    /**
     * Construct SuspendProcessesRequest.
     *
     * @param newSuspendedProcesses used to compare to existing suspended processes
     * @return a SuspendProcessesRequest based on these options
     */
    SuspendProcessesRequest getSuspendProcessesRequestForUpdate(
            Collection<AutoScalingProcessType> newSuspendedProcesses) {
        Collection<AutoScalingProcessType> processes = (newSuspendedProcesses ?: []) - (suspendedProcesses ?: [])
        if (!processes) { return null }
        constructSuspendProcessesRequest(processes)
    }

    /**
     * Construct ResumeProcessesRequest.
     *
     * @param newSuspendedProcesses used to compare to existing suspended processes
     * @return a ResumeProcessesRequest based on these options
     */
    ResumeProcessesRequest getResumeProcessesRequestForUpdate(
            Collection<AutoScalingProcessType> newSuspendedProcesses) {
        Collection<AutoScalingProcessType> processes = (suspendedProcesses ?: []) - (newSuspendedProcesses ?: [])
        if (!processes) { return null }
        constructResumeProcessesRequest(processes)
    }

}
