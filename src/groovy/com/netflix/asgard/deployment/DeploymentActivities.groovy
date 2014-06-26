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

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions
import com.netflix.asgard.UserContext
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.ScheduledAsgAnalysis

/**
 * Method contracts and annotations used for the automatic deployment SWF workflow actions.
 */
@Activities(version = "1.9")
@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = -1L,
        defaultTaskStartToCloseTimeoutSeconds = 600L)
interface DeploymentActivities {

    /**
     * Constructs names and identification for previous and next ASGs involved when creating a new ASG for a cluster.
     *
     * @param userContext who, where, why
     * @param clusterName where the deployment is taking place
     * @return names and identification for the ASGs involved in the deployment
     */
    AsgDeploymentNames getAsgDeploymentNames(UserContext userContext, String clusterName)

    /**
     * Creates the launch configuration for the next ASG in the cluster.
     *
     * @param userContext who, where, why
     * @param nextAutoScalingGroup that will use this launch configuration
     * @param inputs for attributes of the new launch configuration
     * @param instancePriceType determines if instance have on demand or spot pricing
     * @return launch configuration attributes
     */
    LaunchConfigurationBeanOptions constructLaunchConfigForNextAsg(UserContext userContext,
            AutoScalingGroupBeanOptions nextAutoScalingGroup, LaunchConfigurationBeanOptions inputs)

    /**
     * Creates the launch configuration for the next ASG in the cluster.
     *
     * @param userContext who, where, why
     * @param autoScalingGroup that will use this launch configuration
     * @param launchConfiguration attributes for the new launch configuration
     * @return name of the launch configuration
     */
    String createLaunchConfigForNextAsg(UserContext userContext, AutoScalingGroupBeanOptions autoScalingGroup,
            LaunchConfigurationBeanOptions launchConfiguration)

    /**
     * Creates the next ASG in the cluster based on the asgOptions but without instances.
     *
     * @param userContext who, where, why
     * @param asgOptions attributes for the new ASG
     * @return name of the ASG
     */
    String createNextAsgForClusterWithoutInstances(UserContext userContext, AutoScalingGroupBeanOptions asgOptions)

    /**
     * Copies scaling policies from the previous ASG to the next ASG.
     *
     * @param userContext who, where, why
     * @param asgDeploymentNames identification for the previous and next ASGs
     * @return number of scaling policies copied
     */
    Integer copyScalingPolicies(UserContext userContext, AsgDeploymentNames asgDeploymentNames)

    /**
     * Copies scheduled actions from the previous ASG to the next ASG.
     *
     * @param userContext who, where, why
     * @param asgDeploymentNames identification for the previous and next ASGs
     * @return number of scheduled actions copied
     */
    Integer copyScheduledActions(UserContext userContext, AsgDeploymentNames asgDeploymentNames)

    /**
     * Changes the instance count and bounds for an ASG.
     *
     * @param userContext who, where, why
     * @param asgName of the ASG to modify
     * @param min number of instances allowed
     * @param desired number of instances allowed
     * @param max number of instances
     */
    void resizeAsg(UserContext userContext, String asgName, int min, int desired, int max)

    /**
     * Enables scaling behavior for the ASG and traffic to its instances.
     *
     * @param userContext who, where, why
     * @param asgName of the ASG to modify
     * @return boolean representing the success of the activity
     */
    Boolean enableAsg(UserContext userContext, String asgName)

    /**
     * Disables scaling behavior for the ASG and traffic to its instances.
     *
     * @param userContext who, where, why
     * @param asgName of the ASG to modify
     * @return boolean representing the success of the activity
     */
    Boolean disableAsg(UserContext userContext, String asgName)

    /**
     * Deletes an ASG.
     *
     * @param userContext who, where, why
     * @param asgName of the ASG to modify
     */
    void deleteAsg(UserContext userContext, String asgName)

    /**
     * Runs multiple checks to determine the overall readiness of an ASG.
     *
     * @param userContext who, where, why
     * @param asgName of the ASG to modify
     * @param expectedInstances the total number of instances expected in the ASG
     * @return textual description of the reason the ASG is not operational, or an empty String if it is
     */
    String reasonAsgIsNotOperational(UserContext userContext, String asgName, int expectedInstances)

    /**
     * Asks if the deployment should proceed and wait for a reply.
     *
     * @param notificationDestination where deployment notifications will be sent
     * @param asgName of the ASG to modify
     * @param operationDescription describes the current operation of the deployment
     * @return indication on whether to proceed with the deployment
     */
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = -1L,
            defaultTaskStartToCloseTimeoutSeconds = 259200L)
    Boolean askIfDeploymentShouldProceed(UserContext userContext, String notificationDestination, String asgName,
            String operationDescription)

    /**
     * Sends a notification about the status of the deployment.
     *
     * @param userContext who, where, why
     * @param notificationDestination where deployment notifications will be sent
     * @param asgName of the ASG to modify
     * @param subject of the notification
     * @param rollbackCause textual description of the reason why an ASG is not operational, or null if it is
     */
    void sendNotification(UserContext userContext, String notificationDestination, String asgName, String subject,
            String rollbackCause)

    /**
     * Starts the analysis of Auto Scaling Groups in a cluster.
     *
     * @param clusterName for the ASGs to be analyzed
     * @param notificationDestination where deployment notifications will be sent
     * @return attributes about the analysis that was started
     */
    ScheduledAsgAnalysis startAsgAnalysis(String clusterName, String notificationDestination)

    /**
     * Stops the analysis of Auto Scaling Groups in a cluster.
     *
     * @param name used to identify the scheduled analysis
     */
    void stopAsgAnalysis(String name)
}
