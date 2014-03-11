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

import com.amazonaws.services.autoscaling.model.Instance
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.netflix.asgard.MergedInstance
import com.netflix.asgard.Relationships
import com.netflix.frigga.ami.AppVersion
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Immutable custom representation of an AWS instance in an AutoScalingGroupData.
 */
@EqualsAndHashCode
@ToString
class GroupedInstance {

    final String instanceId
    final String availabilityZone
    final String lifecycleState
    final String healthStatus
    final String launchConfigurationName
    final Date launchTime
    final List<String> loadBalancers
    final AppVersion appVersion
    final String buildJobName
    final String buildNumber
    final String imageId
    final String discoveryStatus
    final String healthCheckUrl

    static GroupedInstance from(Instance asgInstance, Collection<LoadBalancerDescription> loadBalancersForInstance,
                                   MergedInstance mergedInstance, Image image) {
        AppVersion appVersion = Relationships.dissectAppVersion(image?.appVersion as String)
        new GroupedInstance(
                asgInstance.instanceId,
                asgInstance.availabilityZone,
                asgInstance.lifecycleState,
                asgInstance.healthStatus,
                asgInstance.launchConfigurationName,
                mergedInstance?.launchTime,
                loadBalancersForInstance.collect { it.loadBalancerName },
                appVersion,
                appVersion?.buildJobName,
                appVersion?.buildNumber,
                image?.imageId,
                mergedInstance?.appInstance?.status,
                mergedInstance?.appInstance?.healthCheckUrl
        )
    }

    private GroupedInstance(
            String instanceId,
            String availabilityZone,
            String lifecycleState,
            String healthStatus,
            String launchConfigurationName,
            Date launchTime,
            List<String> loadBalancers,
            AppVersion appVersion,
            String buildJobName,
            String buildNumber,
            String imageId,
            String discoveryStatus,
            String healthCheckUrl) {

        this.instanceId = instanceId
        this.availabilityZone = availabilityZone
        this.lifecycleState = lifecycleState
        this.healthStatus = healthStatus
        this.launchConfigurationName = launchConfigurationName
        this.launchTime = launchTime ? new Date(launchTime.time) : null
        this.loadBalancers = Collections.unmodifiableList(loadBalancers ? new ArrayList<String>(loadBalancers) : [])
        this.appVersion = appVersion
        this.buildJobName = buildJobName
        this.buildNumber = buildNumber
        this.imageId = imageId
        this.discoveryStatus = discoveryStatus
        this.healthCheckUrl = healthCheckUrl
    }
}
