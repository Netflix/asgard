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
package com.netflix.asgard.push

import com.amazonaws.services.autoscaling.model.ScheduledUpdateGroupAction
import com.netflix.asgard.model.ScalingPolicyData
import groovy.transform.Immutable

@Immutable final class GroupCreateOptions {
    CommonPushOptions common
    InitialTraffic initialTraffic
    Integer minSize
    Integer desiredCapacity
    Integer maxSize
    Integer defaultCooldown
    String healthCheckType
    Integer healthCheckGracePeriod
    List<String> terminationPolicies
    List<String> loadBalancerNames
    List<String> availabilityZones
    String vpcZoneIdentifier
    boolean zoneRebalancingSuspended
    Collection<ScalingPolicyData> scalingPolicies
    Collection<ScheduledUpdateGroupAction> scheduledActions
    String spotPrice

    /** The number of instances to create at a time while inflating the auto scaling group. */
    Integer batchSize

    String kernelId
    String ramdiskId
    String iamInstanceProfile
    String keyName

    def propertyMissing(String name) { common[name] }
}
