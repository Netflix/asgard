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
 * A long running process that stops traffic to all instances of an auto scaling group.
 */
class GroupDeactivateOperation extends AbstractPushOperation {

    private static final log = LogFactory.getLog(this)

    def configService
    def discoveryService
    UserContext userContext
    String autoScalingGroupName

    GroupDeactivateOperation() {
        log.info "Autowiring services in GroupDeactivateOperation instance"
        Spring.autowire(this)
    }

    String getTaskId() {
        task.id
    }

    void start() {
        def thisOperation = this
        String clusterName = Relationships.clusterFromGroupName(autoScalingGroupName)
        String msg = "Stopping traffic to instances of ${autoScalingGroupName}"
        task = taskService.startTask(userContext, msg, { Task task ->
            // Store the new Task object in the operation for more logging later
            thisOperation.task = task

            String appName = Relationships.appNameFromGroupName(autoScalingGroupName)
            task.email = applicationService.getEmailFromApp(userContext, appName)

            AutoScalingGroup group = checkGroupStillExists(userContext, autoScalingGroupName)

            // Ensure processes are disabled to avoid accidental launches and terminations while traffic should be off.
            final Set<AutoScalingProcessType> suspendProcessTypes = AutoScalingProcessType.getDisableProcesses() -
                    group.suspendedProcesses
            suspendProcessTypes.each { AutoScalingProcessType processType ->
                awsAutoScalingService.suspendProcess(userContext, processType, autoScalingGroupName, task)
            }
            awsAutoScalingService.setExpirationTime(userContext, autoScalingGroupName, Time.now().plusDays(2), task)
            List<String> instanceIds = group.instances.collect { it.instanceId }

            if (instanceIds.size()) {

                if (group.loadBalancerNames.size()) {
                    task.log("Deregistering instance${instanceIds.size() == 1 ? '' : 's'} ${instanceIds} from" +
                            " load balancers ${group.loadBalancerNames}")
                    group.loadBalancerNames.eachWithIndex { String loadBalName, int i ->
                        if (i >= 1) { Time.sleepCancellably(250) } // Avoid rate limits when there are dozens of ELBs
                        awsLoadBalancerService.removeInstances(userContext, loadBalName, instanceIds, task)
                    }
                }

                if (configService.doesRegionalDiscoveryExist(userContext.region)) {
                    task.log("Taking instance${instanceIds.size() == 1 ? '' : 's'} ${instanceIds} " +
                            "OUT_OF_SERVICE in Discovery")
                    discoveryService.disableAppInstances(userContext, appName, instanceIds, task)
                    Duration waitTime = discoveryService.timeToWaitAfterDiscoveryChange
                    task.log("Waiting ${Time.format(waitTime)} for clients to stop using instances")
                    Time.sleepCancellably(waitTime.millis)
                }
            }
            awsAutoScalingService.getAutoScalingGroup(userContext, autoScalingGroupName)
        }, Link.to(EntityType.cluster, clusterName))
    }
}
