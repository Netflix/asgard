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
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.netflix.asgard.Relationships
import com.netflix.frigga.ami.AppVersion
import groovy.transform.Canonical

/**
 * Contains ASG related state that is interesting within the context of a Stack.
 */
@Canonical class StackAsg {

    final AutoScalingGroup group
    final LaunchConfiguration launchConfig
    final AppVersion appVersion
    final int healthyInstances

    String getAppName() {
        Relationships.appNameFromGroupName(group.autoScalingGroupName)
    }

    /**
     * Describes the aggregate health of instances in the Auto Scaling Group in textual form.
     * empty - no instances exist
     * fail - instances exist but none are healthy
     * incomplete - there are some healthy instances
     * pass - all instances are healthy
     *
     * @return details about the ASGs that are part of the Stack
     */
    String getHealthDescription() {
        int totalInstances = group.instances.size()
        if (healthyInstances == totalInstances) {
            return healthyInstances == 0 ? 'empty' : 'pass'
        }
        return healthyInstances == 0 ? 'fail' : 'incomplete'
    }
}
