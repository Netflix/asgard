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
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.google.common.collect.Sets
import com.netflix.asgard.CreateAutoScalingGroupResult
import com.netflix.asgard.EntityType
import com.netflix.asgard.Link
import com.netflix.asgard.Relationships
import com.netflix.asgard.Spring
import com.netflix.asgard.Task
import com.netflix.asgard.model.AutoScalingProcessType
import org.apache.commons.logging.LogFactory

/**
 * A long running operation that creates an auto scaling group and monitors the status of the new instances.
 * Optionally this operation can try to prevent traffic from reaching the instances as they come up.
 * This operation finishes after all instances are in their expected stable state (either with or without traffic).
 */
class GroupCreateOperation extends AbstractPushOperation {
    private static final log = LogFactory.getLog(this)

    def awsEc2Service
    def discoveryService
    private final GroupCreateOptions options
    Task task

    GroupCreateOperation(GroupCreateOptions options) {
        this.options = options
        log.info "Autowiring services in GroupCreateOperation instance"
        Spring.autowire(this)
    }

    String getTaskId() {
        task.id
    }

    private void fail(String message) {
        // Refresh AutoScalingGroup and Cluster cache objects when push fails.
        awsAutoScalingService.getAutoScalingGroup(options.common.userContext, options.common.groupName)
        throw new PushException(message)
    }

    void start() {

        def thisOperation = this
        String clusterName = Relationships.clusterFromGroupName(options.common.groupName)
        String msg = "Creating auto scaling group '$options.common.groupName', " +
                "min $options.minSize, max $options.maxSize, traffic ${options.initialTraffic.name().toLowerCase()}"
        task = taskService.startTask(options.common.userContext, msg, { Task task ->

            task.email = applicationService.getEmailFromApp(options.common.userContext, options.common.appName)
            thisOperation.task = task
            task.log("Group '${options.common.groupName}' will start with 0 instances")

            AutoScalingGroup groupTemplate = new AutoScalingGroup().withAutoScalingGroupName(options.common.groupName).
                    withAvailabilityZones(options.availabilityZones).withLoadBalancerNames(options.loadBalancerNames).
                    withMinSize(0).withDesiredCapacity(0).withMaxSize(options.maxSize).
                    withDefaultCooldown(options.defaultCooldown).withHealthCheckType(options.healthCheckType).
                    withHealthCheckGracePeriod(options.healthCheckGracePeriod).
                    withTerminationPolicies(options.terminationPolicies).
                    withVPCZoneIdentifier(options.vpcZoneIdentifier)
            LaunchConfiguration launchConfigTemplate = new LaunchConfiguration().withImageId(options.common.imageId).
                    withKernelId(options.kernelId).withInstanceType(options.common.instanceType).
                    withKeyName(options.keyName).withRamdiskId(options.ramdiskId).
                    withSecurityGroups(options.common.securityGroups).
                    withIamInstanceProfile(options.iamInstanceProfile).
                    withSpotPrice(options.spotPrice)

            final Collection<AutoScalingProcessType> suspendedProcesses = Sets.newHashSet()
            if (options.zoneRebalancingSuspended) {
                suspendedProcesses << AutoScalingProcessType.AZRebalance
            }
            if (options.initialTraffic == InitialTraffic.PREVENTED) {
                suspendedProcesses << AutoScalingProcessType.AddToLoadBalancer
            }

            CreateAutoScalingGroupResult result = awsAutoScalingService.createLaunchConfigAndAutoScalingGroup(
                    options.common.userContext, groupTemplate, launchConfigTemplate, suspendedProcesses,
                    task)
            task.log(result.toString())
            if (result.succeeded()) {
                // Add scalingPolicies to ASG. In the future this might need to be its own operation for reuse.
                awsAutoScalingService.createScalingPolicies(options.common.userContext, options.scalingPolicies, task)
                awsAutoScalingService.createScheduledActions(options.common.userContext, options.scheduledActions, task)

                // If the user wanted any instances then start a resize operation.
                if (options.minSize > 0) {
                    GroupResizeOperation operation = new GroupResizeOperation(userContext: options.common.userContext,
                            autoScalingGroupName: options.common.groupName,
                            eventualMin: options.minSize, newMin: options.minSize,
                            desiredCapacity: options.desiredCapacity, newMax: options.maxSize,
                            batchSize: options.batchSize, initialTraffic: options.initialTraffic,
                            checkHealth: options.common.checkHealth, afterBootWait: options.common.afterBootWait,
                            task: task
                    )
                    operation.proceed()
                }

                if (options.initialTraffic == InitialTraffic.PREVENTED) {
                    // Prevent Discovery traffic from going to newly launched instances.
                    AutoScalingProcessType.getPrimaryProcesses().each {
                        awsAutoScalingService.suspendProcess(options.common.userContext, it, options.common.groupName, task)
                    }
                }

            } else {
                fail(result.toString())
            }
        }, Link.to(EntityType.cluster, clusterName))
    }
}
