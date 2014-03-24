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

import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.google.common.collect.Lists
import com.netflix.asgard.MergedInstance
import com.netflix.asgard.Relationships
import com.netflix.frigga.ami.AppVersion
import groovy.transform.Canonical

/**
 * Immutable custom representation of an AWS instance in an AutoScalingGroupData.
 */
@Canonical
class GroupedInstance {

    final String instanceId
    final String availabilityZone
    final String lifecycleState
    final String healthStatus
    final String launchConfigurationName
    final Date launchTime
    final String hostName
    final String port
    final String publicDnsName
    final String publicIpAddress
    final String privateDnsName
    final String privateIpAddress
    final List<String> loadBalancers
    final AppVersion appVersion
    final String buildJobName
    final String buildNumber
    final String imageId
    final String discoveryStatus
    final String healthCheckUrl

    static GroupedInstance from(com.amazonaws.services.autoscaling.model.Instance asgInstance,
                                Collection<LoadBalancerDescription> loadBalancersForInstance,
                                MergedInstance mergedInstance, Image image) {
        AppVersion appVersion = Relationships.dissectAppVersion(image?.appVersion as String)
        List<String> loadBalancerNames = loadBalancersForInstance.collect { it.loadBalancerName }
        ApplicationInstance appInstance = mergedInstance?.appInstance
        Instance ec2Instance = mergedInstance?.ec2Instance
        new GroupedInstance(asgInstance, loadBalancerNames, appInstance, ec2Instance, appVersion, image?.imageId)
    }

    private GroupedInstance(com.amazonaws.services.autoscaling.model.Instance asgInstance,
                            Collection<String> loadBalancerNames, ApplicationInstance appInstance, Instance ec2Instance,
                            AppVersion appVersion, String imageId) {

        this.instanceId = asgInstance.instanceId
        this.availabilityZone = asgInstance.availabilityZone
        this.lifecycleState = asgInstance.lifecycleState
        this.healthStatus = asgInstance.healthStatus
        this.launchConfigurationName = asgInstance.launchConfigurationName
        this.hostName = appInstance?.hostName
        this.port = appInstance?.port
        this.discoveryStatus = appInstance?.status
        this.healthCheckUrl = appInstance?.healthCheckUrl
        this.launchTime = ec2Instance?.launchTime
        this.publicDnsName = ec2Instance?.publicDnsName
        this.publicIpAddress = ec2Instance?.publicIpAddress
        this.privateDnsName = ec2Instance?.privateDnsName
        this.privateIpAddress = ec2Instance?.privateIpAddress
        this.loadBalancers = Collections.unmodifiableList(Lists.newArrayList(loadBalancerNames ?: []))
        this.appVersion = appVersion
        this.buildJobName = appVersion?.buildJobName
        this.buildNumber = appVersion?.buildNumber
        this.imageId = imageId
    }
}
