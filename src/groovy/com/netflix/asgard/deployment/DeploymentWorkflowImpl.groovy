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
package com.netflix.asgard.deployment

import com.amazonaws.services.simpleworkflow.flow.ActivitySchedulingOptions
import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy
import com.netflix.asgard.DiscoveryService
import com.netflix.asgard.GlobalSwfWorkflowAttributes
import com.netflix.asgard.Relationships
import com.netflix.asgard.UserContext
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.push.PushException
import com.netflix.glisten.DoTry
import com.netflix.glisten.WorkflowOperations
import com.netflix.glisten.WorkflowOperator
import com.netflix.glisten.impl.swf.SwfWorkflowOperations

class DeploymentWorkflowImpl implements DeploymentWorkflow, WorkflowOperator<DeploymentActivities> {

    @Delegate WorkflowOperations<DeploymentActivities> workflowOperations = SwfWorkflowOperations.of(
            DeploymentActivities, new ActivitySchedulingOptions(taskList: GlobalSwfWorkflowAttributes.taskList))

    Closure<String> unit = { int count, String unitName ->
        count == 1 ? "1 ${unitName}" : "${count} ${unitName}s" as String
    }

    Closure<Integer> minutesToSeconds = { it * 60 }

    @Override
    void deploy(UserContext userContext, DeploymentWorkflowOptions deploymentOptions,
                LaunchConfigurationBeanOptions lcInputs, AutoScalingGroupBeanOptions asgInputs) {
        if (deploymentOptions.delayDurationMinutes) {
            status "Waiting ${unit(deploymentOptions.delayDurationMinutes, 'minute')} before starting deployment."
        }
        Promise<Void> delay = timer(minutesToSeconds(deploymentOptions.delayDurationMinutes), 'delay')
        Promise<AsgDeploymentNames> asgDeploymentNamesPromise = promiseFor(activities.getAsgDeploymentNames(userContext,
                deploymentOptions.clusterName))
        Throwable rollbackCause = null
        Promise<Void> deploymentComplete = waitFor(allPromises(asgDeploymentNamesPromise, delay)) {
            status "Starting deployment for Cluster '${deploymentOptions.clusterName}'."
            AsgDeploymentNames asgDeploymentNames = asgDeploymentNamesPromise.get()
            AutoScalingGroupBeanOptions nextAsgTemplate = constructNextAsgForCluster(asgDeploymentNames, asgInputs)
            doTry {
                waitFor(activities.constructLaunchConfigForNextAsg(userContext, nextAsgTemplate, lcInputs)) {
                    startDeployment(userContext, deploymentOptions, asgDeploymentNames, nextAsgTemplate, it)
                }
            } withCatch { Throwable e ->
                rollbackCause = e
                rollback(userContext, asgDeploymentNames, deploymentOptions.notificationDestination, rollbackCause)
                promiseFor(false)
            } result
        }
        waitFor(deploymentComplete) {
            if (rollbackCause) {
                if (rollbackCause.getClass() == PushException) {
                    status "Deployment was rolled back. ${rollbackCause.message}"
                } else {
                    status "Deployment was rolled back due to error: ${rollbackCause.message}"
                }
            } else {
                status 'Deployment was successful.'
            }
        }
    }

    private AutoScalingGroupBeanOptions constructNextAsgForCluster(AsgDeploymentNames asgDeploymentNames,
            AutoScalingGroupBeanOptions inputs) {
        AutoScalingGroupBeanOptions autoScalingGroup = AutoScalingGroupBeanOptions.from(inputs)
        autoScalingGroup.with {
            autoScalingGroupName = asgDeploymentNames.nextAsgName
            launchConfigurationName = asgDeploymentNames.nextLaunchConfigName
        }
        autoScalingGroup
    }

    private Promise<Void> startDeployment(UserContext userContext, DeploymentWorkflowOptions deploymentOptions,
            AsgDeploymentNames asgDeploymentNames, AutoScalingGroupBeanOptions nextAsgTemplate,
            LaunchConfigurationBeanOptions nextLcTemplate) {
        status "Creating Launch Configuration '${asgDeploymentNames.nextLaunchConfigName}'."
        Promise<Void> asgCreated = waitFor(activities.createLaunchConfigForNextAsg(userContext,
                nextAsgTemplate, nextLcTemplate)) {
            status "Creating Auto Scaling Group '${asgDeploymentNames.nextAsgName}' initially with 0 instances."
            waitFor(activities.createNextAsgForCluster(userContext, nextAsgTemplate)) {
                status 'Copying Scaling Policies and Scheduled Actions.'
                Promise<Integer> scalingPolicyCount = promiseFor(
                        activities.copyScalingPolicies(userContext, asgDeploymentNames))
                Promise<Integer> scheduledActionCount = promiseFor(
                        activities.copyScheduledActions(userContext, asgDeploymentNames))
                allPromises(scalingPolicyCount, scheduledActionCount)
            }
        }

        Promise<Boolean> scaleToDesiredCapacity = waitFor(asgCreated) {
            status "New ASG '${asgDeploymentNames.nextAsgName}' was successfully created."
            if (!deploymentOptions.doCanary) {
                return promiseFor(true)
            }
            int canaryCapacity = deploymentOptions.canaryCapacity
            Promise<Boolean> scaleAsgPromise = scaleAsg(userContext, asgDeploymentNames,
                    deploymentOptions.canaryStartUpTimeoutMinutes, canaryCapacity, canaryCapacity,
                    canaryCapacity, 'canary capacity')
            waitFor(scaleAsgPromise) {
                determineWhetherToProceedToNextStep(asgDeploymentNames, deploymentOptions.canaryJudgmentPeriodMinutes,
                        deploymentOptions.notificationDestination, deploymentOptions.scaleUp, 'canary capacity')
            }
        }

        Promise<Boolean> isPreviousAsgDisabled = waitFor(scaleToDesiredCapacity) {
            if (!it) { return promiseFor(false) }
            Promise<Boolean> scaleAsgPromise = scaleAsg(userContext,
                    asgDeploymentNames, deploymentOptions.desiredCapacityStartUpTimeoutMinutes, nextAsgTemplate.minSize,
                    nextAsgTemplate.desiredCapacity, nextAsgTemplate.maxSize, 'full capacity')
            Promise<Boolean> proceedToNextStep = waitFor(scaleAsgPromise) {
                determineWhetherToProceedToNextStep(asgDeploymentNames,
                        deploymentOptions.desiredCapacityJudgmentPeriodMinutes,
                        deploymentOptions.notificationDestination, deploymentOptions.disablePreviousAsg,
                        'full capacity')
            }
            waitFor(proceedToNextStep) {
                if (!it) { return promiseFor(false) }
                if (deploymentOptions.disablePreviousAsg) {
                    String previousAsgName = asgDeploymentNames.previousAsgName
                    status "Disabling ASG '${previousAsgName}'."
                    activities.disableAsg(userContext, previousAsgName)
                }
                promiseFor(true)
            }
        }

        waitFor(isPreviousAsgDisabled) {
            String previousAsgName = asgDeploymentNames.previousAsgName
            if (!it) {
                status "ASG '${previousAsgName}' was not disabled. The new ASG is not taking full traffic."
            } else {
                long secondsToWaitAfterEurekaChange = DiscoveryService.SECONDS_TO_WAIT_AFTER_EUREKA_CHANGE
                status "Waiting ${secondsToWaitAfterEurekaChange} seconds for clients to stop using instances."
                waitFor(timer(secondsToWaitAfterEurekaChange, 'secondsToWaitAfterEurekaChange')) {
                    Promise<Boolean> proceedToNextStep = determineWhetherToProceedToNextStep(
                            asgDeploymentNames, deploymentOptions.fullTrafficJudgmentPeriodMinutes,
                            deploymentOptions.notificationDestination, deploymentOptions.deletePreviousAsg,
                            'full traffic')
                    waitFor(proceedToNextStep) {
                        if (it) {
                            activities.deleteAsg(userContext, previousAsgName)
                            status "Deleting ASG '${previousAsgName}'."
                        }
                        Promise.Void()
                    }
                }
            }
        }
    }

    private Promise<Boolean> scaleAsg(UserContext userContext, AsgDeploymentNames asgDeploymentNames,
            int startupLimitMinutes, int min, int capacity, int max, String operationDescription) {
        String nextAsgName = asgDeploymentNames.nextAsgName
        status "Scaling '${nextAsgName}' to ${operationDescription} (${unit(capacity, 'instance')})."
        activities.resizeAsg(userContext, nextAsgName, min, capacity, max)
        status "Waiting up to ${unit(startupLimitMinutes, 'minute')} for ${unit(capacity, 'instance')}."
        RetryPolicy retryPolicy = new ExponentialRetryPolicy(30L).withBackoffCoefficient(1).
                withExceptionsToRetry([PushException])
        DoTry<String> asgIsOperationalPromise = doTry {
            retry(retryPolicy) {
                waitFor(activities.reasonAsgIsNotOperational(userContext, nextAsgName, capacity)) {
                    if (it) {
                        throw new PushException(it)
                    }
                    promiseFor(it)
                }
            }
        }
        DoTry<Void> startupTimeout = cancelableTimer(minutesToSeconds(startupLimitMinutes), 'startupTimeout')
        waitFor(anyPromises(startupTimeout.result, asgIsOperationalPromise.result)) {
            if (asgIsOperationalPromise.result.ready) {
                startupTimeout.cancel(null)
                promiseFor(true)
            } else {
                asgIsOperationalPromise.cancel(null)
                waitFor(activities.reasonAsgIsNotOperational(userContext, nextAsgName, capacity)) {
                    if (it) {
                        String msg = "ASG '${asgDeploymentNames.nextAsgName}' was not at capacity after " +
                                "${unit(startupLimitMinutes, 'minute')}."
                        throw new PushException(msg)
                    }
                    promiseFor(true)
                }
            }
        }
    }

    private Promise<Boolean> determineWhetherToProceedToNextStep(AsgDeploymentNames asgDeploymentNames,
            int judgmentPeriodMinutes, String notificationDestination, ProceedPreference continueWithNextStep,
            String operationDescription) {
        String nextAsgName = asgDeploymentNames.nextAsgName
        if (continueWithNextStep == ProceedPreference.Yes) { return promiseFor(true) }
        if (continueWithNextStep == ProceedPreference.No) { return promiseFor(false) }
        if (judgmentPeriodMinutes) {
            status "ASG will be evaluated for up to ${unit(judgmentPeriodMinutes, 'minute')}."
        }
        String judgmentMessage =
            "${operationDescription.capitalize()} judgment period for ASG '${nextAsgName}' has begun."
        Promise<Boolean> proceed = promiseFor(activities.askIfDeploymentShouldProceed(notificationDestination,
                    nextAsgName, judgmentMessage))
        status judgmentMessage
        status "Awaiting judgment for '${nextAsgName}'."
        DoTry<Void> sendNotificationAtJudgmentTimeout = doTry {
            waitFor(timer(minutesToSeconds(judgmentPeriodMinutes), 'judgmentTimeout')) {
                String clusterName = Relationships.clusterFromGroupName(asgDeploymentNames.nextAsgName)
                String subject = "Judgement period for ASG '${asgDeploymentNames.nextAsgName}' has ended."
                String message = 'Please make a decision to proceed or roll back.'
                activities.sendNotification(notificationDestination, clusterName, subject, message)
                Promise.Void()
            }
        }
        waitFor(proceed) {
            sendNotificationAtJudgmentTimeout.cancel(null)
            if (it) {
                promiseFor(true)
            } else {
                throw new PushException("Judge decided ASG '${asgDeploymentNames.nextAsgName}' was not operational.")
            }
        }
    }

    private void rollback(UserContext userContext, AsgDeploymentNames asgDeploymentNames,
            String notificationDestination, Throwable rollbackCause) {
        String action = "Rolling back to '${asgDeploymentNames.previousAsgName}'."
        status action
        String clusterName = Relationships.clusterFromGroupName(asgDeploymentNames.nextAsgName)
        String status = rollbackCause ? 'not ' : ''
        String message = """\
        Auto Scaling Group '${asgDeploymentNames.nextAsgName}' is ${status}operational.
        ${rollbackCause.message}""".stripIndent()
        activities.sendNotification(notificationDestination, clusterName, action, message)
        activities.enableAsg(userContext, asgDeploymentNames.previousAsgName)
        activities.disableAsg(userContext, asgDeploymentNames.nextAsgName)
    }
}
