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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.netflix.asgard.EntityType
import com.netflix.asgard.From
import com.netflix.asgard.Link
import com.netflix.asgard.Relationships
import com.netflix.asgard.Spring
import com.netflix.asgard.Task
import com.netflix.asgard.Time
import com.netflix.asgard.UserContext
import com.netflix.frigga.Names
import org.apache.commons.logging.LogFactory
import org.joda.time.DateTime
import org.joda.time.Duration

/**
 * A long running process that sets an auto scaling group to size 0, waits for all the instances to terminate, then
 * deletes the auto scaling group and all its launch configurations.
 */
class GroupDeleteOperation extends AbstractPushOperation {
    private static final log = LogFactory.getLog(this)

    UserContext userContext
    AutoScalingGroup autoScalingGroup

    private static Duration MAX_TIME_FOR_DELETION = Duration.standardMinutes(10)

    GroupDeleteOperation() {
        log.info "Autowiring services in GroupDeleteOperation instance"
        Spring.autowire(this)
    }

    String getTaskId() {
        task.id
    }

    void start() {
        def thisOperation = this
        String groupName = autoScalingGroup.autoScalingGroupName
        Names names = Relationships.dissectCompoundName(groupName)
        String clusterName = names.cluster
        task = taskService.startTask(userContext, "Force Delete Auto Scaling Group '${groupName}'", { Task task ->
            // Store the new Task object in the operation for more logging later
            thisOperation.task = task
            String appName = names.app
            task.email = applicationService.getEmailFromApp(userContext, appName)

            AutoScalingGroup group = checkGroupStillExists(userContext, groupName)
            List<String> oldLaunchConfigNames = awsAutoScalingService.getLaunchConfigurationNamesForAutoScalingGroup(
                userContext, groupName).findAll { it != group.launchConfigurationName }

            deregisterAllInstancesInAutoScalingGroupFromLoadBalancers()
            forceDeleteAutoScalingGroup()
            task.log("Auto scaling group '${groupName}' will be deleted after deflation finishes")
            waitForDeletion()
            deleteLaunchConfigs(oldLaunchConfigNames)
            task.log("Finished deletion of '${autoScalingGroup.autoScalingGroupName}'")
        }, Link.to(EntityType.cluster, clusterName))
    }

    /**
     * Disables traffic on ELB because an ELB can send traffic to incorrect instances if you terminate instances that
     * are receiving ELB traffic.
     */
    private void deregisterAllInstancesInAutoScalingGroupFromLoadBalancers() {
        task.log("Deregistering all instances in '${autoScalingGroup.autoScalingGroupName}' from load balancers")
        awsAutoScalingService.deregisterAllInstancesInAutoScalingGroupFromLoadBalancers(userContext,
                autoScalingGroup.autoScalingGroupName, task)
    }

    private void forceDeleteAutoScalingGroup() {
        task.log("Deleting auto scaling group '${autoScalingGroup.autoScalingGroupName}'")
        awsAutoScalingService.deleteAutoScalingGroup(userContext, autoScalingGroup.autoScalingGroupName,
                AsgDeletionMode.FORCE, task)
    }

    private void waitForDeletion() {
        DateTime terminationStartTime = Time.now()
        boolean asgStillExists = true
        while (asgStillExists) {
            Time.sleepCancellably(5000)
            AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext,
                    autoScalingGroup.autoScalingGroupName, From.AWS_NOCACHE)
            if (!group) {
                asgStillExists = false
            } else if (new Duration(terminationStartTime, Time.now()).isLongerThan(MAX_TIME_FOR_DELETION)) {
                throw new PushException("Timeout waiting ${Time.format(MAX_TIME_FOR_DELETION)} for auto scaling group " +
                        "'${autoScalingGroup.autoScalingGroupName}' to disappear from AWS.")
            }
        }
    }

    private void deleteLaunchConfigs(List<String> launchConfigNames) {
        launchConfigNames.each {
            task.log("Deleting launch configuration '${it}'")
            awsAutoScalingService.deleteLaunchConfiguration(userContext, it, task)
        }
    }
}
