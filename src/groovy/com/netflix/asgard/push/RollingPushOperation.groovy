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
import com.amazonaws.services.autoscaling.model.Instance
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.ec2.model.Image
import com.netflix.asgard.EntityType
import com.netflix.asgard.From
import com.netflix.asgard.Link
import com.netflix.asgard.Region
import com.netflix.asgard.Relationships
import com.netflix.asgard.Spring
import com.netflix.asgard.Time
import com.netflix.asgard.UserContext
import com.netflix.asgard.model.ApplicationInstance
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.Subnets
import org.apache.commons.logging.LogFactory
import org.joda.time.Duration

/**
 * A complex operation that involves pushing an AMI image to many new instances within an auto scaling group, and
 * monitoring the status of the instances for real time user feedback.
 */
class RollingPushOperation extends AbstractPushOperation {
    private static final log = LogFactory.getLog(this)

    def awsEc2Service
    def configService
    def discoveryService
    def launchTemplateService
    def restClientService
    private List<Slot> relaunchSlots = []
    List<String> loadBalancerNames = []
    private final RollingPushOptions options
    PushStatus pushStatus

    /**
     * If the timeout durations turn out to cause trouble in prod then this flag provides a way to prevent timing out.
     */
    static Boolean timeoutsEnabled = true

    RollingPushOperation(RollingPushOptions options) {
        this.options = options
        log.info "Autowiring services in RollingPushOperation instance"
        Spring.autowire(this)
    }

    String getTaskId() {
        task.id
    }

    void start() {

        def thisPushOperation = this
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(options.userContext, options.groupName)
        Image image = awsEc2Service.getImage(options.userContext, options.imageId)
        String appversion = image.getAppVersion()
        task = taskService.startTask(options.userContext, "Pushing $options.imageId " +
                (appversion ? "with package $appversion " : '') +
                "into group $options.groupName for app $options.appName", { task ->

            task.email = applicationService.getEmailFromApp(options.common.userContext, options.common.appName)

            // Store the new Task object in the RollingPushOperation
            thisPushOperation.task = task

            prepareGroup(group)
            restartInstances(group)
        }, Link.to(EntityType.autoScaling, options.groupName))
    }

    List<com.amazonaws.services.ec2.model.Instance> getSortedEc2Instances(List<Instance> asgInstances) {
        Closure sortAlgorithm = options.newestFirst ? { a, b -> b.launchTime <=> a.launchTime } : { it.launchTime }
        UserContext userContext = options.common.userContext
        List<com.amazonaws.services.ec2.model.Instance> ec2Instances =
                asgInstances.collect { awsEc2Service.getInstance(userContext, it.instanceId) }
                        .findAll{ it != null }
                        .sort(sortAlgorithm)
        task.log("Sorted ${options.common.appName} instances in ${options.common.groupName} by launch time with " +
                "${options.newestFirst ? 'newest' : 'oldest'} first")
        ec2Instances
    }

    private void prepareGroup(AutoScalingGroup group) {
        LaunchConfiguration oldLaunch = awsAutoScalingService.getLaunchConfiguration(options.userContext,
                group.launchConfigurationName)
        String groupName = group.autoScalingGroupName
        loadBalancerNames = group.loadBalancerNames
        String newLaunchName = Relationships.buildLaunchConfigurationName(groupName)
        Time.sleepCancellably 100 // tiny pause before LC create to avoid rate limiting
        Collection<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups(options.securityGroups,
                group.VPCZoneIdentifier as boolean, options.userContext.region)
        task.log("Updating launch from ${oldLaunch.launchConfigurationName} with ${options.imageId} into ${newLaunchName}")
        String iamInstanceProfile = options.iamInstanceProfile ?: null
        LaunchConfigurationBeanOptions launchConfig = new LaunchConfigurationBeanOptions(
                launchConfigurationName: newLaunchName, imageId: options.imageId, keyName: options.keyName,
                securityGroups: securityGroups, instanceType: options.instanceType,
                kernelId: oldLaunch.kernelId, ramdiskId: oldLaunch.ramdiskId, iamInstanceProfile: iamInstanceProfile,
                ebsOptimized: oldLaunch.ebsOptimized
        )
        UserContext userContext = options.common.userContext
        Subnets subnets = awsEc2Service.getSubnets(userContext)
        AutoScalingGroupBeanOptions groupForUserData = AutoScalingGroupBeanOptions.from(group, subnets)
        groupForUserData.launchConfigurationName = newLaunchName
        launchConfig.userData = launchTemplateService.buildUserData(options.common.userContext, groupForUserData,
                launchConfig)
        awsAutoScalingService.createLaunchConfiguration(options.common.userContext, launchConfig, task)

        Time.sleepCancellably 200 // small pause before ASG update to avoid rate limiting
        task.log("Updating group ${groupName} to use launch config ${newLaunchName}")
        final AutoScalingGroupData autoScalingGroupData = AutoScalingGroupData.forUpdate(
                groupName, newLaunchName,
                group.minSize, group.desiredCapacity, group.maxSize, group.defaultCooldown,
                group.healthCheckType, group.healthCheckGracePeriod, group.terminationPolicies, group.availabilityZones
        )
        awsAutoScalingService.updateAutoScalingGroup(options.common.userContext, autoScalingGroupData, [], [], task)
    }

    private void decideActionsForSlot(Slot slot) {

        // Stagger external requests by a few milliseconds to avoid rate limiting and excessive object creation
        Time.sleepCancellably 300

        // Are we acting on the old instance or the fresh instance?
        InstanceMetaData instanceInfo = slot.current

        // How long has it been since the targeted instance state changed?
        Duration timeSinceChange = instanceInfo.timeSinceChange

        Boolean timeForPeriodicLogging = instanceInfo.isItTimeForPeriodicLogging()

        Region region = options.common.userContext.region
        boolean discoveryExists = configService.doesRegionalDiscoveryExist(region)
        Duration afterDiscovery = discoveryExists ? discoveryService.timeToWaitAfterDiscoveryChange : Duration.ZERO

        UserContext userContext = options.common.userContext

        switch (instanceInfo.state) {

            // Starting here we're looking at the state of the old instance
            case InstanceState.initial:
                // If too many other slots are still in progress, then skip this unstarted slot for now.
                if (areTooManyInProgress()) { break }

                handleInitialPhase(instanceInfo, userContext, discoveryExists, afterDiscovery)
                break

            case InstanceState.unregistered:
                // Shut down abruptly or wait for clients of the instance to adjust to the change.
                handleUnregisteredPhase(timeSinceChange, afterDiscovery, instanceInfo, userContext)
                break

            case InstanceState.terminated:
                handleTerminatedPhase(userContext, instanceInfo, slot)
                break

            // After here instanceInfo holds the state of the fresh instance, not the old instance
            case InstanceState.pending:
                //if (state == InstanceState.pending) {
                handlePendingPhase(userContext, instanceInfo, timeForPeriodicLogging, slot)
                break

            case InstanceState.running:
                handleRunningPhase(userContext, instanceInfo)
                break

            case InstanceState.registered:
                handleRegisteredPhase(instanceInfo, timeForPeriodicLogging)
                break

            case InstanceState.snoozing:
                handleSnoozingPhase(timeSinceChange, instanceInfo)
                break
        }

        // If a change should have happened by now then something is wrong so stop the push.
        if (timeoutsEnabled && timeSinceChange.isLongerThan(instanceInfo.state.timeOutToExitState)) {
            String err = "${reportSummary()} Timeout waiting ${Time.format(timeSinceChange)} for ${instanceInfo.id} to progress "
            err += "from ${instanceInfo.state?.name()} state. "
            err += "(Maximum ${Time.format(instanceInfo.state.timeOutToExitState)} allowed)."
            fail(err)
        }
    }

    private void handleInitialPhase(InstanceMetaData instanceInfo, UserContext userContext, boolean discoveryExists, Duration afterDiscovery) {
        // Disable the app in discovery and ELBs so that clients don't try to talk to it
        String appName = options.common.appName
        String appInstanceId = "${appName} / ${instanceInfo.id}"
        if (loadBalancerNames) {
            task.log("Disabling ${appInstanceId} in ${loadBalancerNames.size()} ELBs.")
            for (String loadBalancerName in loadBalancerNames) {
                awsLoadBalancerService.removeInstances(userContext, loadBalancerName, [instanceInfo.id], task)
            }
        }
        if (discoveryExists) {
            discoveryService.disableAppInstances(userContext, appName, [instanceInfo.id], task)
            if (options.rudeShutdown) {
                task.log("Rude shutdown mode. Not waiting for clients to stop using ${appInstanceId}")
            } else {
                task.log("Waiting ${Time.format(afterDiscovery)} for clients to stop using ${appInstanceId}")
            }
        }
        instanceInfo.state = InstanceState.unregistered
    }

    private void handleUnregisteredPhase(Duration timeSinceChange, Duration afterDiscovery, InstanceMetaData instanceInfo, UserContext userContext) {
        if (options.rudeShutdown || timeSinceChange.isLongerThan(afterDiscovery)) {
            task.log("Terminating instance ${instanceInfo.id}")
            if (awsEc2Service.terminateInstances(userContext, [instanceInfo.id], task) == null) {
                fail("${reportSummary()} Instance ${instanceInfo.id} failed to terminate. Aborting push.")
            }
            instanceInfo.state = InstanceState.terminated
            String duration = Time.format(InstanceState.terminated.timeOutToExitState)
            task.log("Waiting up to ${duration} for new instance of ${options.groupName} to become Pending.")
        }
    }

    private void handleTerminatedPhase(UserContext userContext, InstanceMetaData instanceInfo, Slot slot) {
        AutoScalingGroup freshGroup = checkGroupStillExists(userContext, options.groupName, From.AWS_NOCACHE)
        // See if new ASG instance can be found yet
        def newInst = freshGroup.instances.find {
            def instanceFromGroup = it
            def matchingSlot = relaunchSlots.find { theSlot ->
                instanceFromGroup.instanceId == theSlot.fresh.id
            }
            // AWS lifecycleState values are Pending, InService, Terminating and Terminated
            // http://docs.amazonwebservices.com/AutoScaling/latest/DeveloperGuide/CHAP_Glossary.html
            (instanceFromGroup?.lifecycleState?.equals("Pending")) && !matchingSlot
        }
        if (newInst) {
            // Get the EC2 Instance object based on the ASG Instance object
            com.amazonaws.services.ec2.model.Instance instance = awsEc2Service.getInstance(userContext, newInst.instanceId)
            if (instance) {
                task.log("It took ${Time.format(instanceInfo.timeSinceChange)} for instance ${instanceInfo.id} to terminate and be replaced by ${instance.instanceId}")
                slot.fresh.instance = instance
                slot.fresh.state = InstanceState.pending
                task.log("Waiting up to ${Time.format(InstanceState.pending.timeOutToExitState)} for Pending ${instance.instanceId} to go InService.")
            }
        }
    }

    private void handlePendingPhase(UserContext userContext, InstanceMetaData instanceInfo, boolean timeForPeriodicLogging, Slot slot) {
        def freshGroup = checkGroupStillExists(userContext, options.groupName, From.AWS_NOCACHE)
        def newInst = freshGroup.instances.find { it.instanceId == instanceInfo.id }
        String lifecycleState = newInst?.lifecycleState
        Boolean freshInstanceFailed = !newInst || "Terminated" == lifecycleState
        if (timeForPeriodicLogging || freshInstanceFailed) {
            task.log("Instance of ${options.appName} on ${instanceInfo.id} is in lifecycle state ${lifecycleState ?: 'Not Found'}")
        }

        if (freshInstanceFailed) {
            Integer startupTriesDone = slot.replaceFreshInstance()
            if (startupTriesDone > options.maxStartupRetries) {
                fail("${reportSummary()} Startup failed ${startupTriesDone} times for one slot. " +
                        "Max ${options.maxStartupRetries} tries allowed. Aborting push.")
            } else {
                task.log("Startup failed on ${instanceInfo.id} so Amazon terminated it. Waiting up to ${Time.format(InstanceState.terminated.timeOutToExitState)} for another instance.")
            }
        }

        // If no longer pending, change state
        if (newInst?.lifecycleState?.equals("InService")) {
            task.log("It took ${Time.format(instanceInfo.timeSinceChange)} for instance ${instanceInfo.id} to go from Pending to InService")
            instanceInfo.state = InstanceState.running
            if (options.checkHealth) {
                task.log("Waiting up to ${Time.format(InstanceState.running.timeOutToExitState)} for Eureka registration of ${options.appName} on ${instanceInfo.id}")
            }
        }
    }

    private void handleRunningPhase(UserContext userContext, InstanceMetaData instanceInfo) {
        if (options.checkHealth) {

            // Eureka isn't yet strong enough to handle sustained rapid fire requests.
            Time.sleepCancellably(discoveryService.MILLIS_DELAY_BETWEEN_DISCOVERY_CALLS as Integer)

            ApplicationInstance appInst = discoveryService.getAppInstance(userContext,
                    options.common.appName, instanceInfo.id)
            if (appInst) {
                task.log("It took ${Time.format(instanceInfo.timeSinceChange)} for instance " +
                        "${instanceInfo.id} to go from InService to registered with Eureka")
                instanceInfo.state = InstanceState.registered
                def healthCheckUrl = appInst.healthCheckUrl
                if (healthCheckUrl) {
                    instanceInfo.healthCheckUrl = healthCheckUrl
                    task.log("Waiting up to ${Time.format(InstanceState.registered.timeOutToExitState)} for health check pass at ${healthCheckUrl}")
                }
            }
        }
        // If check health is off, prepare for final wait
        else {
            startSnoozing(instanceInfo)
        }
    }

    private void handleRegisteredPhase(InstanceMetaData instanceInfo, boolean timeForPeriodicLogging) {
        // If there's a health check URL then check it before preparing for final wait
        if (instanceInfo.healthCheckUrl) {
            Integer responseCode = restClientService.getRepeatedResponseCode(instanceInfo.healthCheckUrl)
            String message = "Health check response code is ${responseCode} for application ${options.appName} on instance ${instanceInfo.id}"
            if (responseCode >= 400) {
                fail("${reportSummary()} ${message}. Push was aborted because ${instanceInfo.healthCheckUrl} " +
                        "shows a sick instance. See http://go/healthcheck for guidelines.")
            }
            Boolean healthy = responseCode == 200
            if (timeForPeriodicLogging || healthy) {
                task.log(message)
            }
            if (healthy) {
                task.log("It took ${Time.format(instanceInfo.timeSinceChange)} for instance " +
                        "${instanceInfo.id} to go from registered to healthy")
                startSnoozing(instanceInfo)
            }
        }
        // If there is no health check URL, assume instance is ready for final wait
        else {
            task.log("Can't check health of ${options.appName} on ${instanceInfo.id} since no URL is registered.")
            startSnoozing(instanceInfo)
        }
    }

    private void handleSnoozingPhase(Duration timeSinceChange, InstanceMetaData instanceInfo) {
        Boolean ready = true
        if (options.shouldWaitAfterBoot()
                && timeSinceChange.isShorterThan(Duration.standardSeconds(options.common.afterBootWait))) {
            ready = false
        }
        if (ready) {
            instanceInfo.state = InstanceState.ready
            task.log("Instance ${options.appName} on ${instanceInfo.id} is ready for use. ${reportSummary()}")
        }
    }

    private void fail(String message) {
        // Refresh AutoScalingGroup and Cluster cache objects when push fails.
        awsAutoScalingService.getAutoScalingGroup(options.userContext, options.groupName)
        throw new PushException(message)
    }

    private String reportSummary() {
        replacePushStatus()
        return "${pushStatus.countReadyNewInstances()} of ${options.relaunchCount} instance relaunches done."
    }

    private void startSnoozing(InstanceMetaData instanceInfo) {
        if (options.shouldWaitAfterBoot()) {
            task.log("Waiting ${options.afterBootWait} second${options.afterBootWait == 1 ? "" : "s"} for ${options.appName} on ${instanceInfo.id} to be ready.")
        }
        instanceInfo.state = InstanceState.snoozing
    }

    private boolean areTooManyInProgress() {
        Integer concurrentRelaunches = Math.min(options.concurrentRelaunches, relaunchSlots.size())
        def slotsInProgress = relaunchSlots.findAll { it.inProgress() }
        return slotsInProgress.size() >= concurrentRelaunches
    }

    private void restartInstances(AutoScalingGroup group) {

        List<com.amazonaws.services.ec2.model.Instance> ec2Instances = getSortedEc2Instances(group.instances)

        // Create initial slots
        ec2Instances.each { relaunchSlots << new Slot(it) }
        for (Integer i = 0; i < options.relaunchCount; i++) {
            relaunchSlots[i].shouldRelaunch = true
        }
        Integer total = ec2Instances.size()
        task.log "The group ${options.common.groupName} has ${total} instance${total == 1 ? "" : "s"}. " +
                "${options.relaunchCount} will be replaced, ${options.concurrentRelaunches} at a time."

        // Iterate through the slots that are marked for relaunch and choose the next action to take for each one.
        Boolean allDone = false
        while (!allDone && task.status == "running") {
            relaunchSlots.findAll { it.shouldRelaunch }.each { Slot slot -> decideActionsForSlot(slot) }
            replacePushStatus()
            allDone = pushStatus.allDone

            // Stagger iterations to avoid AWS rate limiting and excessive object creation
            Time.sleepCancellably 1000
        }

        // Refresh AutoScalingGroup and Cluster cache objects when push succeeds.
        awsAutoScalingService.getAutoScalingGroup(options.userContext, options.groupName)
    }

    private void replacePushStatus() {
        pushStatus = new PushStatus(relaunchSlots, options)
    }
}
