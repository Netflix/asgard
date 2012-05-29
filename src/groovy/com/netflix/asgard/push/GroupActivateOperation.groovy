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
import com.netflix.asgard.Link
import com.netflix.asgard.Relationships
import com.netflix.asgard.Spring
import com.netflix.asgard.Task
import com.netflix.asgard.Time
import com.netflix.asgard.UserContext
import com.netflix.asgard.model.AutoScalingProcessType
import org.apache.commons.logging.LogFactory
import org.joda.time.Duration

/**
 * A long running process that starts traffic to all instances of an auto scaling group.
 */
class GroupActivateOperation {
    private static final log = LogFactory.getLog(this)

    def applicationService
    def awsAutoScalingService
    def awsLoadBalancerService
    def configService
    def discoveryService
    def taskService
    Task task
    UserContext userContext
    String autoScalingGroupName

    GroupActivateOperation() {
        log.info "Autowiring services in GroupActivateOperation instance"
        Spring.autowire(this)
    }

    String getTaskId() {
        task.id
    }

    void start() {
        def thisOperation = this
        String clusterName = Relationships.clusterFromGroupName(autoScalingGroupName)
        String msg = "Starting traffic to instances of ${autoScalingGroupName}"
        task = taskService.startTask(userContext, msg, { Task task ->
            // Store the new Task object in the operation for more logging later
            thisOperation.task = task
            String appName = Relationships.appNameFromGroupName(autoScalingGroupName)
            task.email = applicationService.getEmailFromApp(userContext, appName)

            AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, autoScalingGroupName)
            awsAutoScalingService.removeExpirationTime(userContext, autoScalingGroupName, task)

            // Ensure processes are enabled, since it was probably disabled when the ASG got "deactivated".
            final Collection<AutoScalingProcessType> suspendedProcessTypesFromDisableProcesses =
                group.suspendedProcessTypes.intersect(AutoScalingProcessType.getDisableProcesses())
            suspendedProcessTypesFromDisableProcesses.each { AutoScalingProcessType processType ->
                awsAutoScalingService.resumeProcess(userContext, processType, autoScalingGroupName, task)
            }

            List<String> instanceIds = group.instances.collect { it.instanceId }

            if (instanceIds.size()) {

                if (group.loadBalancerNames.size()) {
                    task.log("Registering instance${instanceIds.size() == 1 ? '' : 's'} ${instanceIds} with" +
                            " load balancers ${group.loadBalancerNames}")
                    group.loadBalancerNames.eachWithIndex { String loadBalName, int i ->
                        if (i >= 1) { Time.sleepCancellably(250) } // Avoid rate limits when there are dozens of ELBs
                        awsLoadBalancerService.addInstances(userContext, loadBalName, instanceIds, task)
                    }
                }

                if (configService.doesRegionalDiscoveryExist(userContext.region)) {
                    task.log("Registering instance${instanceIds.size() == 1 ? '' : 's'} ${instanceIds} with Discovery")
                    discoveryService.enableAppInstances(userContext, appName, instanceIds, task)
                    Duration waitTime = discoveryService.timeToWaitAfterDiscoveryChange
                    task.log("Waiting ${Time.format(waitTime)} for clients to use new instances")
                    Time.sleepCancellably(waitTime.millis)
                }
            }
            awsAutoScalingService.getAutoScalingGroup(userContext, autoScalingGroupName)
        }, Link.to(EntityType.cluster, clusterName))
    }
}
