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
import com.netflix.asgard.DiscoveryService
import com.netflix.asgard.UserContext
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.push.PushException
import com.netflix.glisten.DoTry
import com.netflix.glisten.GlobalWorkflowAttributes
import com.netflix.glisten.SwfWorkflowOperations
import com.netflix.glisten.WorkflowOperations

class DeploymentWorkflowImpl implements DeploymentWorkflow {

    @Delegate WorkflowOperations<DeploymentActivities> workflow = SwfWorkflowOperations.of(DeploymentActivities,
            new ActivitySchedulingOptions(taskList: GlobalWorkflowAttributes.taskList))

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
        Promise<AsgDeploymentNames> asgDeploymentNames = waitFor(timer(minutesToSeconds(deploymentOptions.
                delayDurationMinutes))) {
            status "Starting deployment for Cluster '${deploymentOptions.clusterName}'."
            promiseFor(activities.getAsgDeploymentNames(userContext, deploymentOptions.clusterName))
        }

        Promise<AutoScalingGroupBeanOptions> nextAsgTemplate = waitFor(asgDeploymentNames) {
            promiseFor(activities.constructNextAsgForCluster(userContext, asgDeploymentNames.get(), asgInputs))
        }

        Promise<LaunchConfigurationBeanOptions> nextLcTemplate = waitFor(nextAsgTemplate) {
            promiseFor(activities.constructLaunchConfigForNextAsg(userContext, nextAsgTemplate.get(), lcInputs))
        }

        Promise<String> nextLcName = waitFor(allPromises(asgDeploymentNames, nextLcTemplate)) {
            status "Creating Launch Configuration '${asgDeploymentNames.get().nextLaunchConfigName}'."
            promiseFor(activities.createLaunchConfigForNextAsg(userContext, nextAsgTemplate.get(),
                    nextLcTemplate.get()))
        }

        Promise<String> nextAsgName = waitFor(nextLcName) {
            status "Creating Auto Scaling Group '${asgDeploymentNames.get().nextAsgName}' initially with 0 instances."
            promiseFor(activities.createNextAsgForCluster(userContext, nextAsgTemplate.get()))
        }

        Promise<Integer> scalingPolicyCount = waitFor(nextAsgName) {
            status 'Copying Scaling Policies.'
            promiseFor(activities.copyScalingPolicies(userContext, asgDeploymentNames.get()))
        }

        Promise<Integer> scheduledActionCount = waitFor(nextAsgName) {
            status 'Copying Scheduled Actions.'
            promiseFor(activities.copyScheduledActions(userContext, asgDeploymentNames.get()))
        }

        Promise<Boolean> shouldScaleToDesiredCapacity = waitFor(allPromises(scalingPolicyCount,
                scheduledActionCount)) {
            status "New ASG '${nextAsgName.get()}' was successfully created."
            if (!deploymentOptions.doCanary) {
                return promiseFor(true)
            }
            int canaryCapacity = deploymentOptions.canaryCapacity
            status 'Canary testing will now be performed.'
            scaleAsgAndWaitForDecision(userContext, nextAsgName.get(), deploymentOptions.canaryStartUpTimeoutMinutes,
                    canaryCapacity, canaryCapacity, canaryCapacity, deploymentOptions.canaryAssessmentDurationMinutes,
                    deploymentOptions.notificationDestination, deploymentOptions.scaleUp, 'canary capacity')
        }

        Promise<Boolean> isPreviousAsgDisabled = waitFor(shouldScaleToDesiredCapacity) {
            if (!shouldScaleToDesiredCapacity.get()) {
                rollback(userContext, asgDeploymentNames.get())
                return promiseFor(false)
            }
            status 'Scaling to full capacity.'
            Promise<Boolean> isNewAsgHealthyAtDesiredCapacity = scaleAsgAndWaitForDecision(userContext, nextAsgName.get(),
                    deploymentOptions.desiredCapacityStartUpTimeoutMinutes, asgInputs.minSize,
                    asgInputs.desiredCapacity, asgInputs.maxSize,
                    deploymentOptions.desiredCapacityAssessmentDurationMinutes,
                    deploymentOptions.notificationDestination, deploymentOptions.disablePreviousAsg,
                    'full capacity')
            waitFor(isNewAsgHealthyAtDesiredCapacity) {
                if (!isNewAsgHealthyAtDesiredCapacity.get()) {
                    if (deploymentOptions.disablePreviousAsg != ProceedPreference.No) {
                        rollback(userContext, asgDeploymentNames.get())
                    }
                    return promiseFor(false)
                }
                if (deploymentOptions.disablePreviousAsg) {
                    String previousAsgName = asgDeploymentNames.get().previousAsgName
                    status "Disabling ASG '${previousAsgName}'."
                    activities.disableAsg(userContext, previousAsgName)
                }
                promiseFor(deploymentOptions.disablePreviousAsg)
            }
        }

        waitFor(isPreviousAsgDisabled) {
            String previousAsgName = asgDeploymentNames.get().previousAsgName
            if (!isPreviousAsgDisabled.get()) {
                status "ASG '${previousAsgName}' was not disabled. Full traffic health check will not take place."
            } else {
                long secondsToWaitAfterEurekaChange = DiscoveryService.SECONDS_TO_WAIT_AFTER_EUREKA_CHANGE
                status "Waiting ${secondsToWaitAfterEurekaChange} seconds for clients to stop using instances."
                waitFor(timer(secondsToWaitAfterEurekaChange)) {
                    Promise<Boolean> isNewAsgHealthyWithFullTraffic = startAssessmentPeriodWaitForDecision(userContext,
                            nextAsgName.get(), deploymentOptions.fullTrafficAssessmentDurationMinutes,
                            deploymentOptions.notificationDestination, deploymentOptions.deletePreviousAsg,
                            asgInputs.desiredCapacity, 'full traffic')
                    waitFor(isNewAsgHealthyWithFullTraffic) {
                        boolean isAsgHealthy = isNewAsgHealthyWithFullTraffic.get()
                        if (isAsgHealthy) {
                            if (deploymentOptions.deletePreviousAsg) {
                                status "Deleting ASG '${previousAsgName}'."
                                activities.deleteAsg(userContext, previousAsgName)
                            }
                        } else {
                            if (deploymentOptions.deletePreviousAsg != ProceedPreference.No) {
                                rollback(userContext, asgDeploymentNames.get())
                            }
                        }
                        Promise.Void()
                    }
                }
            }
            Promise.Void()
        }

    }

    private Promise<Boolean> scaleAsgAndWaitForDecision(UserContext userContext, String nextAsgName, int startupLimit,
            int min, int capacity, int max, int assessmentDuration, String notificationDestination,
            ProceedPreference continueWithNextStep, String operationDescription) {
        status "Scaling '${nextAsgName}' to ${unit(capacity, 'instance')}."
        activities.resizeAsg(userContext, nextAsgName, min, capacity, max)

        DoTry<String> repeatingHealthCheck = checkAsgHealth(userContext, nextAsgName, capacity,
                new ExponentialRetryPolicy(180L).withMaximumRetryIntervalSeconds(60L))
        DoTry<Void> healthCheckTimeout = cancellableTimer(minutesToSeconds(startupLimit))
        status "Waiting up to ${unit(startupLimit, 'minute')} for ${unit(capacity, 'instance')}."
        waitFor(anyPromises(repeatingHealthCheck.result, healthCheckTimeout.result)) {
            if (repeatingHealthCheck.result.isReady()) {
                status "ASG '${nextAsgName}' is at ${operationDescription}."
                healthCheckTimeout.cancel(null)
                return startAssessmentPeriodWaitForDecision(userContext, nextAsgName, assessmentDuration,
                        notificationDestination, continueWithNextStep, capacity, operationDescription)
            } else {
                String msg = "ASG '${nextAsgName}' was not at capacity after ${unit(startupLimit, 'minute')}."
                status msg
                repeatingHealthCheck.cancel(new PushException(msg))
                return promiseFor(false)
            }
        }
    }

    private Promise<Boolean> startAssessmentPeriodWaitForDecision(UserContext userContext, String nextAsgName,
            int assessmentDuration, String notificationDestination, ProceedPreference continueWithNextStep, int capacity,
            String operationDescription) {
        if (assessmentDuration) {
            status "ASG health will be evaluated after ${unit(assessmentDuration, 'minute')}."
        }
        waitFor(timer(minutesToSeconds(assessmentDuration))) {
            // To get to this point the ASG has been seen as healthy once somewhere. Let's retry for the sake of
            // "eventual consistency" issues like the cache.
            DoTry<String> reasonAsgIsUnhealthy = checkAsgHealth(userContext, nextAsgName, capacity,
                    new ExponentialRetryPolicy(5L).withMaximumAttempts(5))
            waitFor(reasonAsgIsUnhealthy.result) {
                String assessmentCompleteMessage =
                    "${operationDescription.capitalize()} assessment period for ASG '${nextAsgName}' has completed."
                status assessmentCompleteMessage
                Map<ProceedPreference, Closure<Promise<Boolean>>> preferenceToAction = [
                        (ProceedPreference.Yes): {
                            if (reasonAsgIsUnhealthy.result.get()) {
                                String subject = "Asgard deployment for '${nextAsgName}' will not proceed due to error."
                                activities.sendNotification(notificationDestination, nextAsgName, subject,
                                        reasonAsgIsUnhealthy.result.get())
                                return Promise.asPromise(false)
                            }
                            Promise.asPromise(true)
                        },
                        (ProceedPreference.No): {
                            Promise.asPromise(false)
                        },
                        (ProceedPreference.Ask): {
                            status "Awaiting health decision for '${nextAsgName}'."
                            (Promise<Boolean>) promiseFor(activities.askIfDeploymentShouldProceed(
                                    notificationDestination, nextAsgName, assessmentCompleteMessage,
                                    reasonAsgIsUnhealthy.result.get()))
                        }
                ]
                preferenceToAction[continueWithNextStep]()
            }
        }
    }

    private DoTry<String> checkAsgHealth(UserContext userContext, String asgName, int expectedInstances,
            ExponentialRetryPolicy retryPolicy) {
        retryPolicy.withExceptionsToRetry([PushException])
        doTry() {
            retry(retryPolicy) {
                (Promise<String>) promiseFor(activities.reasonAsgIsUnhealthy(userContext, asgName, expectedInstances))
            }
        } withCatch { Throwable e ->
            status e.message
            if (e instanceof PushException) {
                return Promise.asPromise(e.message)
            }
            (Promise<String>) promiseFor(null)
        }
    }

    private void rollback(UserContext userContext, AsgDeploymentNames asgDeploymentNames) {
        status "Rolling back to '${asgDeploymentNames.previousAsgName}'."
        activities.enableAsg(userContext, asgDeploymentNames.previousAsgName)
        activities.disableAsg(userContext, asgDeploymentNames.nextAsgName)
    }

}
