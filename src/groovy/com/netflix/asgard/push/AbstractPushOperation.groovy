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
package com.netflix.asgard.push

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.netflix.asgard.From
import com.netflix.asgard.Task
import com.netflix.asgard.UserContext

abstract class AbstractPushOperation {

    def applicationService
    def awsAutoScalingService
    def awsLoadBalancerService
    def taskService
    Task task

    protected final AutoScalingGroup checkGroupStillExists(UserContext userContext, String autoScalingGroupName,
            From from=From.AWS) {
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, autoScalingGroupName, from)
        if (!group) {
            abortBecauseGroupDisappeared(autoScalingGroupName)
        }
        group
    }

    private void abortBecauseGroupDisappeared(String autoScalingGroupName) {
        throw new PushException("Group '${autoScalingGroupName}' can no longer be found.")
    }

}
