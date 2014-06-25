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
import com.amazonaws.services.simpleworkflow.flow.core.Settable
import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy
import com.netflix.asgard.DiscoveryService
import com.netflix.asgard.GlobalSwfWorkflowAttributes
import com.netflix.asgard.Relationships
import com.netflix.asgard.ServiceUnavailableException
import com.netflix.asgard.UserContext
import com.netflix.asgard.deployment.steps.CreateAsgStep
import com.netflix.asgard.deployment.steps.DeleteAsgStep
import com.netflix.asgard.deployment.steps.DeploymentStep
import com.netflix.asgard.deployment.steps.DisableAsgStep
import com.netflix.asgard.deployment.steps.EnableAsgStep
import com.netflix.asgard.deployment.steps.JudgmentStep
import com.netflix.asgard.deployment.steps.ResizeStep
import com.netflix.asgard.deployment.steps.WaitStep
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.AutoScalingProcessType
import com.netflix.asgard.model.Deployment
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.ScheduledAsgAnalysis
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

    RetryPolicy remoteServiceRetryPolicy = new ExponentialRetryPolicy(5).withMaximumAttempts(3).
            withExceptionsToRetry([ServiceUnavailableException])

    @Override
    void deploy(UserContext userContext, DeploymentWorkflowOptions deploymentOptions,
                LaunchConfigurationBeanOptions lcInputs, AutoScalingGroupBeanOptions asgInputs) {
        String clusterName = deploymentOptions.clusterName
        Promise<AsgDeploymentNames> asgDeploymentNamesPromise = promiseFor(activities.getAsgDeploymentNames(userContext,
                clusterName))
        Throwable rollbackCause = null
        List<DoTry<ScheduledAsgAnalysis>> runningAsgAnalyses = []
        Promise<Void> deploymentComplete = waitFor(asgDeploymentNamesPromise) { AsgDeploymentNames asgDeploymentNames ->
            doTry {
                AutoScalingGroupBeanOptions nextAsgTemplate = AutoScalingGroupBeanOptions.from(asgInputs)
                nextAsgTemplate.with {
                    autoScalingGroupName = asgDeploymentNames.nextAsgName
                    launchConfigurationName = asgDeploymentNames.nextLaunchConfigName
                    suspendedProcesses?.removeAll(AutoScalingProcessType.getDisableProcesses())
                }
                Promise<LaunchConfigurationBeanOptions> nextLcTemplateConstructed = promiseFor(activities.
                        constructLaunchConfigForNextAsg(userContext, nextAsgTemplate, lcInputs))
                waitFor(nextLcTemplateConstructed) { LaunchConfigurationBeanOptions nextLcTemplate ->
                    startDeployment(userContext, deploymentOptions, asgDeploymentNames, nextAsgTemplate, nextLcTemplate,
                            runningAsgAnalyses)
                }
            } withCatch { Throwable e ->
                rollbackCause = e
                rollback(userContext, asgDeploymentNames)
                Promise.Void()
            } result
        }
        waitFor(deploymentComplete) {
            for (DoTry runningAsgAnalysis : runningAsgAnalyses) {
                stopScheduledAsgAnalysis(runningAsgAnalysis) // ensure that ASG Analysis is stopped
            }
            String notificationDestination  = deploymentOptions.notificationDestination
            AsgDeploymentNames asgDeploymentNames = asgDeploymentNamesPromise.get()
            String deploymentCompleteMessage = 'Deployment was successful.'
            String subject = "Deployment succeeded for ASG '${asgDeploymentNames.nextAsgName}'."
            if (rollbackCause) {
                if (rollbackCause.getClass() == PushException) {
                    deploymentCompleteMessage = "Deployment was rolled back. ${rollbackCause.message}"
                } else {
                    deploymentCompleteMessage = "Deployment was rolled back due to error: ${rollbackCause}"
                }
                subject = "Deployment failed for ASG '${asgDeploymentNames.nextAsgName}'."
            }
            activities.sendNotification(userContext, notificationDestination, clusterName, subject,
                    deploymentCompleteMessage)
            status deploymentCompleteMessage
        }
    }

    private Promise<Void> startDeployment(UserContext userContext, DeploymentWorkflowOptions deploymentOptions,
            AsgDeploymentNames asgDeploymentNames, AutoScalingGroupBeanOptions nextAsgTemplate,
            LaunchConfigurationBeanOptions nextLcTemplate, List<DoTry<ScheduledAsgAnalysis>> runningAsgAnalyses) {

        Map<Class<? extends DeploymentStep>, Closure<Promise>> stepsToOperations = [
                (WaitStep): { WaitStep step ->
                    status "Waiting ${unit(step.durationMinutes, 'minute')} before next step."
                    timer(minutesToSeconds(step.durationMinutes), step.description)
                },
                (CreateAsgStep): { CreateAsgStep step ->
                    status "Creating Launch Configuration '${asgDeploymentNames.nextLaunchConfigName}'."
                    Promise<String> launchConfigCreated = promiseFor(
                            activities.createLaunchConfigForNextAsg(userContext, nextAsgTemplate, nextLcTemplate))
                    waitFor(launchConfigCreated) {
                        status "Creating Auto Scaling Group '${asgDeploymentNames.nextAsgName}' \
initially with 0 instances."
                        waitFor(activities.createNextAsgForClusterWithoutInstances(userContext, nextAsgTemplate)) {
                            status 'Copying Scaling Policies and Scheduled Actions.'
                            Promise<Integer> scalingPolicyCount = promiseFor(
                                    activities.copyScalingPolicies(userContext, asgDeploymentNames))
                            Promise<Integer> scheduledActionCount = promiseFor(
                                    activities.copyScheduledActions(userContext, asgDeploymentNames))
                            allPromises(scalingPolicyCount, scheduledActionCount)
                        }
                    }
                },
                (ResizeStep): { ResizeStep step ->
                    scaleAsg(userContext, asgDeploymentNames.nextAsgName, step.startUpTimeoutMinutes,
                            nextAsgTemplate.minSize, step.capacity, nextAsgTemplate.maxSize)
                },
                (JudgmentStep): { JudgmentStep step ->
                    DoTry<ScheduledAsgAnalysis> startAsgAnalysis = startScheduledAsgAnalysis(
                            deploymentOptions.clusterName, deploymentOptions.notificationDestination)
                    runningAsgAnalyses << startAsgAnalysis
                    Promise<Boolean> judgment = determineWhetherToProceedToNextStep(userContext,
                            asgDeploymentNames.nextAsgName, step.durationMinutes,
                            deploymentOptions.notificationDestination)
                    waitFor(judgment) {
                        stopScheduledAsgAnalysis(startAsgAnalysis)
                        runningAsgAnalyses.remove(startAsgAnalysis)
                        judgment
                    }
                },
                (DisableAsgStep): { DisableAsgStep step ->
                    String asgName = asgDeploymentNames.getAsgName(step.targetAsg)
                    status "Disabling ASG '${asgName}'."
                    activities.disableAsg(userContext, asgName)
                    long secondsToWaitAfterEurekaChange = DiscoveryService.SECONDS_TO_WAIT_AFTER_EUREKA_CHANGE
                    status "Waiting ${secondsToWaitAfterEurekaChange} seconds for clients to stop using instances."
                    timer(secondsToWaitAfterEurekaChange, 'waitAfterEurekaChange')
                },
                (EnableAsgStep): { EnableAsgStep step ->
                    String asgName = asgDeploymentNames.getAsgName(step.targetAsg)
                    status "Enabling ASG '${asgName}'."
                    activities.enableAsg(userContext, asgName)
                    Promise.Void()
                },
                (DeleteAsgStep): { DeleteAsgStep step ->
                    String asgName = asgDeploymentNames.getAsgName(step.targetAsg)
                    activities.deleteAsg(userContext, asgName)
                    status "Deleting ASG '${asgName}'."
                }
        ]

        List<Settable> stepPromises = []
        (deploymentOptions.steps.size() + 1).times { stepPromises.add(new Settable()) }
        stepPromises[0].chain(Promise.Void())
        (0..(deploymentOptions.steps.size() - 1)).each { stepIndex ->
            waitFor(stepPromises[stepIndex]) {
                status Deployment.constructStepJson(stepIndex)
                DeploymentStep step = deploymentOptions.steps[stepIndex]
                Settable stepPromise = stepPromises[stepIndex + 1]
                stepPromise.chain(stepsToOperations[step.class](step))
                Promise.Void()
            }
        }
        stepPromises[deploymentOptions.steps.size()]
    }

    private DoTry<ScheduledAsgAnalysis> startScheduledAsgAnalysis(String clusterName, String notificationDestination) {
        doTry {
            retry(getRemoteServiceRetryPolicy()) {
                promiseFor(activities.startAsgAnalysis(clusterName, notificationDestination))
            }
        } withCatch { Throwable t ->
            status "Error starting ASG analyzer: ${t}"
        }
    }

    private void stopScheduledAsgAnalysis(DoTry<ScheduledAsgAnalysis> startAsgAnalysis) {
        if (null == startAsgAnalysis) { return }
        startAsgAnalysis.cancel(null) // cancel analysis launch in case it is still trying to start
        waitFor(startAsgAnalysis.result) { ScheduledAsgAnalysis scheduledAnalysis ->
            doTry {
                retry(getRemoteServiceRetryPolicy()) {
                    activities.stopAsgAnalysis(scheduledAnalysis?.name)
                    Promise.Void()
                }
            } withCatch { Throwable t ->
                status "Error stopping ASG analyzer: ${t}"
            } result
        }
    }

    private Promise<Boolean> scaleAsg(UserContext userContext, String asgName, int startupLimitMinutes, int min,
            int capacity, int max) {
        status "Waiting up to ${unit(startupLimitMinutes, 'minute')} while resizing to ${unit(capacity, 'instance')}."
        activities.resizeAsg(userContext, asgName, min, capacity, max)
        RetryPolicy retryPolicy = new ExponentialRetryPolicy(30L).withBackoffCoefficient(1).
                withExceptionsToRetry([PushException])
        DoTry<Void> asgIsOperational = doTry {
            retry(retryPolicy) {
                waitFor(activities.reasonAsgIsNotOperational(userContext, asgName, capacity)) { String reason ->
                    if (reason) {
                        throw new PushException(reason)
                    }
                    Promise.Void()
                }
            }
        }
        DoTry<Void> startupTimeout = cancelableTimer(minutesToSeconds(startupLimitMinutes), 'startupTimeout')
        waitFor(anyPromises(startupTimeout.result, asgIsOperational.result)) {
            if (asgIsOperational.result.ready) {
                startupTimeout.cancel(null)
                promiseFor(true)
            } else {
                asgIsOperational.cancel(null)
                waitFor(activities.reasonAsgIsNotOperational(userContext, asgName, capacity)) {
                    if (it) {
                        String msg = "ASG '${asgName}' was not at capacity after " +
                                "${unit(startupLimitMinutes, 'minute')}."
                        throw new PushException(msg)
                    }
                    promiseFor(true)
                }
            }
        }
    }

    private Promise<Boolean> determineWhetherToProceedToNextStep(UserContext userContext, String asgName,
            int judgmentPeriodMinutes, String notificationDestination) {
        String judgmentMessage = "ASG will now be evaluated for up to ${unit(judgmentPeriodMinutes, 'minute')}" +
                " during the judgment period."
        status judgmentMessage
        Promise<Boolean> proceed = promiseFor(activities.askIfDeploymentShouldProceed(userContext,
                notificationDestination, asgName, judgmentMessage))
        DoTry<Void> sendNotificationAtJudgmentTimeout = doTry {
            Promise<Void> judgmentTimeout = timer(minutesToSeconds(judgmentPeriodMinutes), 'judgmentTimeout')
            waitFor(judgmentTimeout) {
                String clusterName = Relationships.clusterFromGroupName(asgName)
                String subject = "Judgment period for ASG '${asgName}' has ended."
                String message = 'Please make a decision to proceed or roll back.'
                activities.sendNotification(userContext, notificationDestination, clusterName, subject, message)
                Promise.Void()
            }
        }
        waitFor(proceed) {
            sendNotificationAtJudgmentTimeout.cancel(null)
            if (it) {
                promiseFor(true)
            } else {
                throw new PushException("Judge decided ASG '${asgName}' was not viable.")
            }
        }
    }

    private void rollback(UserContext userContext, AsgDeploymentNames asgDeploymentNames) {
        Promise<Boolean> previousAsgEnabled = doTry {
            promiseFor(activities.enableAsg(userContext, asgDeploymentNames.previousAsgName))
        } withCatch { Throwable t ->
            status "Previous ASG '${asgDeploymentNames.previousAsgName}' could not be enabled."
            promiseFor(false)
        } result
        waitFor(previousAsgEnabled) {
            if (it) {
                activities.disableAsg(userContext, asgDeploymentNames.nextAsgName)
            }
            Promise.Void()
        }
    }
}
