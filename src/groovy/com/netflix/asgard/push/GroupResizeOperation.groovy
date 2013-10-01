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
import com.netflix.asgard.Ensure
import com.netflix.asgard.EntityType
import com.netflix.asgard.Flag
import com.netflix.asgard.From
import com.netflix.asgard.Link
import com.netflix.asgard.Relationships
import com.netflix.asgard.Spring
import com.netflix.asgard.Task
import com.netflix.asgard.Time
import com.netflix.asgard.UserContext
import com.netflix.asgard.model.ApplicationInstance
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.AutoScalingProcessType
import org.apache.commons.logging.LogFactory
import org.joda.time.DateTime
import org.joda.time.Duration

/**
 * A long running process that changes an auto scaling group's size attributes, then waits for the instances list to
 * meet the new requirements. If new instances were needed, this operation waits for the instances to be ready for
 * traffic.
 */
class GroupResizeOperation extends AbstractPushOperation {
    private static final log = LogFactory.getLog(this)

    /**
     * This can be set lower to try to protect brittle external systems from rapid, massive instance creation.
     */
    static final Integer DEFAULT_BATCH_SIZE = 500

    static final Integer MINUTES_BETWEEN_BATCHES = 5

    def awsEc2Service
    def discoveryService
    def flagService
    def restClientService

    UserContext userContext
    String autoScalingGroupName
    InitialTraffic initialTraffic
    Integer eventualMin
    Integer newMin
    Integer desiredCapacity
    Integer newMax
    Integer batchSize
    Boolean checkHealth
    Integer afterBootWait

    Integer recentCount = 0

    GroupResizeOperation() {
        log.info "Autowiring services in GroupResizeOperation instance"
        Spring.autowire(this)
    }

    String getTaskId() {
        task.id
    }

    private String appName
    private GroupResizeOperation thisOperation = this
    private DateTime lastBatchStartTime = Time.now()
    private static Duration MAX_TIME_PER_BATCH = Duration.standardMinutes(25)

    private Duration calculateMaxTimePerBatch() {
        if (flagService.isOn(Flag.SHORT_MAX_TIME_PER_BATCH)) {
            return Duration.standardSeconds(5)
        }
        Duration duration = MAX_TIME_PER_BATCH

        if (eventualMin - recentCount >= 1) {
            Integer additionalNeededCount = eventualMin - recentCount
            // Give an extra minute for each set of 10 because Amazon throttles their own ASG instance launch rate
            Integer additionalMinutes = Ensure.bounded(0, (Integer) (additionalNeededCount / 10), 90)
            duration = duration.withDurationAdded(Duration.standardMinutes(additionalMinutes), 1)
        }
        duration
    }

    Boolean hasTooMuchTimePassedSinceBatchStart() {
        new Duration(lastBatchStartTime, Time.now()).isLongerThan(calculateMaxTimePerBatch())
    }

    Closure work = { Task task ->
        // Store the new Task object in the operation for more logging later
        thisOperation.task = task
        appName = Relationships.appNameFromGroupName(autoScalingGroupName)
        task.email = applicationService.getEmailFromApp(userContext, appName)

        if (eventualMin == 0) {
            // Disable traffic on ELB first because an ELB can send traffic to incorrect instances if you terminate
            // instances that are receiving ELB traffic.
            awsAutoScalingService.deregisterAllInstancesInAutoScalingGroupFromLoadBalancers(userContext,
                    autoScalingGroupName, task)
        }

        while (true) {
            lastBatchStartTime = Time.now()
            AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, autoScalingGroupName)
            if (!group) {
                throw new PushException("Auto scaling group '${group}' does not exist")
            }
            Integer currentCount = group.instances.size()
            recentCount = currentCount

            Integer additional = Math.max(0, eventualMin - currentCount)
            if (additional > batchSize && batchSize > 0) {
                // Increase min in batches and wait for readiness between batches.
                newMin = Ensure.bounded(currentCount, currentCount + batchSize, eventualMin)
                task.log("Starting next batch of ${batchSize} instance${batchSize == 1 ? '' : 's'} for " +
                        "group '${autoScalingGroupName}'")
            } else {
                newMin = eventualMin
            }
            updateAutoScalingGroupSize(group)

            // If current count is too low, wait for new instances. If current count is too high, wait for it to drop.
            if (currentCount < newMin) {
                waitForNewInstances()
            } else if (currentCount > newMax) {
                waitForInstancesToTerminate()
            }

            group = checkGroupStillExists(userContext, autoScalingGroupName)
            currentCount = group.instances.size()
            if (currentCount >= eventualMin && currentCount <= newMax) {
                break
            }

            Duration batchDuration = new Duration(lastBatchStartTime, Time.now())

            Integer secondsToWaitBetweenBatches = flagService.isOn(Flag.SHORT_WAIT_FOR_EXTERNAL_MONITORING_SYSTEMS) ?
                    20 : MINUTES_BETWEEN_BATCHES * 60
            Integer secondsToWait = Math.max(1L,
                    secondsToWaitBetweenBatches - batchDuration.getStandardSeconds() as Integer)
            Duration durationToWait = Duration.standardSeconds(secondsToWait)
            String durationToWaitString = Time.format(durationToWait)
            task.log("Waiting another ${durationToWaitString} for external monitoring systems to be ready for more instances")
            Time.sleepCancellably(secondsToWait * 1000)
        }
    }

    private String describeOperation() {
        "Resizing group '${autoScalingGroupName}' to min ${newMin}, max ${newMax}"
    }

    void start() {
        String clusterName = Relationships.clusterFromGroupName(autoScalingGroupName)
        task = taskService.startTask(userContext, describeOperation(), work, Link.to(EntityType.cluster, clusterName))
    }

    void proceed() {
        task.log(describeOperation())
        taskService.doWork(work, task)
    }

    private void updateAutoScalingGroupSize(AutoScalingGroup group) {
        task.log("Setting group '${autoScalingGroupName}' to min $newMin max $newMax")
        int desiredCapacity = this.desiredCapacity ?: group.desiredCapacity
        int newDesiredCapacity = Ensure.bounded(newMin, desiredCapacity, newMax)

        final Collection<AutoScalingProcessType> suspendedProcessTypes = group.suspendedProcessTypes
        if (group.instances.size() > newMax) {
            suspendedProcessTypes.remove(AutoScalingProcessType.Terminate)
        }
        final AutoScalingGroupData autoScalingGroupData = AutoScalingGroupData.forUpdate(
                group.autoScalingGroupName,
                group.launchConfigurationName, newMin, newDesiredCapacity, newMax, group.defaultCooldown,
                group.healthCheckType, group.healthCheckGracePeriod, group.terminationPolicies,
                group.availabilityZones
        )
        awsAutoScalingService.updateAutoScalingGroup(userContext, autoScalingGroupData, suspendedProcessTypes, [], task)
    }

    private static final Integer ITERATION_WAIT_MILLIS = 300

    private void abortBecauseInstancesHaveNotAppeared(int currentCount, String instanceChangeDescription) {
        throw new PushException("Timeout waiting ${Time.format(calculateMaxTimePerBatch())} for" +
                " instances ${instanceChangeDescription}. Expected ${newMin} instance${newMin == 1 ? '' : 's'}" +
                " by now but auto scaling group '${autoScalingGroupName}' has only ${currentCount}.")
    }

    private void waitForNewInstances() {
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, autoScalingGroupName,
                    From.AWS_NOCACHE)
        if (!group) {
            throw new IllegalStateException("Auto scaling group '${autoScalingGroupName}' not found")
        }

        task.log("Group '${autoScalingGroupName}' has ${group.instances.size()} " +
                "instance${group.instances.size() == 1 ? '' : 's'}. Waiting for ${newMin} to exist.")

        Boolean moreInstancesAreExpected = true
        while (moreInstancesAreExpected) {
            Time.sleepCancellably(ITERATION_WAIT_MILLIS)
            group = checkGroupStillExists(userContext, autoScalingGroupName, From.AWS_NOCACHE)
            Integer currentCount = group.instances.size()
            ensureTrafficIsSuppressedIfAppropriate(group)

            if (currentCount >= newMin) {
                moreInstancesAreExpected = false
                task.log("Group '${autoScalingGroupName}' instance count ${currentCount} is within new " +
                        "bounds $newMin and $newMax")
            } else if (hasTooMuchTimePassedSinceBatchStart()) {
                abortBecauseInstancesHaveNotAppeared(currentCount, 'to be created')
            }
        }
        task.log("Waiting for at least ${newMin} instance${newMin == 1 ? '' : 's'} to be InService in " +
                "group '${autoScalingGroupName}'")
        Boolean waitingForInstancesToGoInService = true
        while (waitingForInstancesToGoInService) {
            Time.sleepCancellably(ITERATION_WAIT_MILLIS)
            group = checkGroupStillExists(userContext, autoScalingGroupName, From.AWS_NOCACHE)
            Integer currentCount = group.findInServiceInstanceIds().size()
            ensureTrafficIsSuppressedIfAppropriate(group)

            if (currentCount >= newMin) {
                waitingForInstancesToGoInService = false
                task.log("Group '${autoScalingGroupName}' has ${currentCount} InService " +
                        "instance${currentCount == 1 ? '' : 's'} which is between min ${newMin} and max ${newMax}")
            } else if (hasTooMuchTimePassedSinceBatchStart()) {
                abortBecauseInstancesHaveNotAppeared(currentCount, 'to go InService')
            }
        }

        if (checkHealth) {
            task.log("Waiting for ${newMin} healthy instance${newMin == 1 ? '' : 's'} in group " +
                    "'${autoScalingGroupName}'")
            group = checkHealthOfInstances()
        } else if (afterBootWait) {
            task.log("Waiting ${afterBootWait} second${afterBootWait == 1 ? '' : 's'} for new instances to be " +
                    "ready in group '${autoScalingGroupName}'")
            Time.sleepCancellably(afterBootWait * 1000)
        }
        task.log("Finishing resize of '${autoScalingGroupName}'")
        // Update the caches for instances
        group?.instances?.collect { it.instanceId }?.each { String id ->
            awsEc2Service.getInstance(userContext, id)
            discoveryService.getAppInstance(userContext, id)
            Time.sleepCancellably(discoveryService.MILLIS_DELAY_BETWEEN_DISCOVERY_CALLS)
        }
        // Update the caches for ASG and cluster
        awsAutoScalingService.getAutoScalingGroup(userContext, autoScalingGroupName)
    }

    private AutoScalingGroup checkHealthOfInstances() {
        AutoScalingGroup group = checkGroupStillExists(userContext, autoScalingGroupName, From.AWS_NOCACHE)
        ensureTrafficIsSuppressedIfAppropriate(group)
        Collection<String> idsOfInstancesThatAreNotYetHealthy = findInstancesNotYetHealthy(group.instances*.instanceId)
        if (idsOfInstancesThatAreNotYetHealthy.empty) {
            // Everything is healthy. Our work is done here.
            return group
        }
        // Loop until everything not yet healthy is healthy
        while (!idsOfInstancesThatAreNotYetHealthy.empty) {
            if (hasTooMuchTimePassedSinceBatchStart()) {
                Integer unhealthyCount = idsOfInstancesThatAreNotYetHealthy.size()
                String regardingUpStatus = initialTraffic == InitialTraffic.ALLOWED ? ' with status "UP"' : ''
                throw new PushException("Timeout waiting ${Time.format(calculateMaxTimePerBatch())} " +
                        "for instances to register with Eureka${regardingUpStatus} and pass a health check. " +
                        "Expected ${newMin} discoverable, healthy instance${newMin == 1 ? '' : 's'}, but " +
                        "auto scaling group '${autoScalingGroupName}' still has ${unhealthyCount} " +
                        "undiscoverable or unhealthy instance${unhealthyCount == 1 ? '' : 's'} " +
                        "including ${idsOfInstancesThatAreNotYetHealthy}. Are the new instances having errors?")
            }
            Time.sleepCancellably(ITERATION_WAIT_MILLIS)
            group = checkGroupStillExists(userContext, autoScalingGroupName, From.AWS_NOCACHE)
            ensureTrafficIsSuppressedIfAppropriate(group)
            idsOfInstancesThatAreNotYetHealthy = findInstancesNotYetHealthy(idsOfInstancesThatAreNotYetHealthy)
        }
        // Check the health of all instances in the ASG, now that the unhealthy instances have become healthy
        checkHealthOfInstances()
    }

    private Collection<String> findInstancesNotYetHealthy(Collection<String> instanceIds) {
        instanceIds.findAll { String id ->

            // Still waiting for Discovery?
            Time.sleepCancellably(discoveryService.MILLIS_DELAY_BETWEEN_DISCOVERY_CALLS)
            ApplicationInstance instance = discoveryService.getAppInstance(userContext, id)
            if (instance == null) {
                return true // Missing in Discovery
            }

            // If traffic is allowed, then any instance that is not "UP" in Eureka should be considered unhealthy
            if (initialTraffic == InitialTraffic.ALLOWED && instance?.status != 'UP') {
                return true
            }

            String healthCheckUrl = instance.healthCheckUrl
            if (healthCheckUrl) {
                Integer responseCode = restClientService.getRepeatedResponseCode(healthCheckUrl)
                return responseCode != 200
            }

            // No health check URL found
            return true
        }
    }

    private void ensureTrafficIsSuppressedIfAppropriate(AutoScalingGroup group) {
        // If initial traffic should be prevented then ensure live instances are not in Discovery.
        if (initialTraffic == InitialTraffic.PREVENTED) {
            Set<String> idsInService = group.findInServiceInstanceIds()
            suppressTraffic(idsInService)
        }
    }

    private void suppressTraffic(Set<String> instanceIds) {
        // Find any instances that are 'UP' in Discovery and disable them.
        List<String> upInstanceIds = instanceIds.findAll {
            discoveryService.getAppInstance(userContext, it)?.status == 'UP'
        } as List
        if (upInstanceIds) {
            discoveryService.disableAppInstances(userContext, appName, upInstanceIds, task)
            task.log("${upInstanceIds} deactivated in Eureka")
        }
    }

    private void waitForInstancesToTerminate() {
        task.log("Waiting for group '${autoScalingGroupName}' to have no more than ${newMax} " +
                "instance${newMax == 1 ? '' : 's'}")
        DateTime terminationStartTime = Time.now()
        Boolean tooManyInstancesExist = true
        while (tooManyInstancesExist) {
            Time.sleepCancellably(1000)
            AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, autoScalingGroupName,
                    From.AWS_NOCACHE)
            Integer currentCount = group.instances.size()
            if (currentCount <= newMax) {
                tooManyInstancesExist = false
                task.log("Group '${autoScalingGroupName}' instance count ${currentCount} is within new " +
                        "bounds ${newMin} to ${newMax}")
            } else if (new Duration(terminationStartTime, Time.now()).isLongerThan(calculateMaxTimePerBatch())) {
                throw new PushException("Timeout waiting ${Time.format(calculateMaxTimePerBatch())} for auto scaling group " +
                        "'${autoScalingGroupName}' to shrink to size ${newMax}. Group still has ${currentCount} " +
                        "instance${currentCount == 1 ? '' : 's'}.")
            }
        }
    }
}
