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
import com.netflix.asgard.model.InstancePriceType

/**
 * Method contracts and annotations used for the automatic deployment SWF workflow actions.
 */
@Activities(version = "1.3")
@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 10L,
        defaultTaskStartToCloseTimeoutSeconds = 300L)
interface DeploymentActivities {

    /**
     * Constructs names and identification for previous and next ASGs involved when creating a new ASG for a cluster.
     *
     * @param userContext who, where, why
     * @param clusterName where the deployment is taking place
     * @param newSubnetPurpose for the next ASG
     * @param newZones for the next ASG
     * @return names and identification for the ASGs involved in the deployment
     */
    AsgDeploymentNames getAsgDeploymentNames(UserContext userContext, String clusterName, String newSubnetPurpose,
            List<String> newZones)

    /**
     * Creates the launch configuration for the next ASG in the cluster.
     *
     * @param userContext who, where, why
     * @param asgDeploymentNames identification for the previous and next ASGs
     * @param overrides attributes that override values from the template launch configuration
     * @param instancePriceType determines if instance have on demand or spot pricing
     * @return name of the launch configuration
     */
    String createLaunchConfigForNextAsg(UserContext userContext, AsgDeploymentNames asgDeploymentNames,
            LaunchConfigurationOptions overrides, InstancePriceType instancePriceType)

    /**
     * Creates the next ASG in the cluster.
     *
     * @param userContext who, where, why
     * @param asgDeploymentNames identification for the previous and next ASGs
     * @param overrides attributes that override values from the template ASG
     * @param initialTrafficPrevented determines if traffic will be prevented until the first assessment period
     * @param azRebalanceSuspended determines if availability zone rebalancing will be suspended for new ASG
     * @return name of the ASG
     */
    String createNextAsgForCluster(UserContext userContext, AsgDeploymentNames asgDeploymentNames,
            AutoScalingGroupOptions overrides, Boolean initialTrafficPrevented, Boolean azRebalanceSuspended)

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
     */
    void enableAsg(UserContext userContext, String asgName)

    /**
     * Disables scaling behavior for the ASG and traffic to its instances.
     *
     * @param userContext who, where, why
     * @param asgName of the ASG to modify
     */
    void disableAsg(UserContext userContext, String asgName)

    /**
     * Deletes an ASG.
     *
     * @param userContext who, where, why
     * @param asgName of the ASG to modify
     */
    void deleteAsg(UserContext userContext, String asgName)

    /**
     * Runs multiple checks to determine the overall health of an ASG including counting the instances and checking
     * their various health related statuses.
     *
     * @param userContext who, where, why
     * @param asgName of the ASG to modify
     * @param expectedInstances the total number of instances expected in the ASG
     * @return textual description of the reason why an ASG is not at full health, or null if it is healthy
     */
    String reasonAsgIsUnhealthy(UserContext userContext, String asgName, int expectedInstances)

    /**
     * Asks if the deployment should proceed and wait for a reply.
     *
     * @param notificationDestination where deployment notifications will be sent
     * @param asgName of the ASG to modify
     * @param operationDescription describes the current operation of the deployment
     * @param reasonAsgIsUnhealthy textual description of the reason why an ASG is not at full health, or null if it is
     * @return indication on whether to proceed with the deployment
     */
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = 10L,
            defaultTaskStartToCloseTimeoutSeconds = 86400L)
    Boolean askIfDeploymentShouldProceed(String notificationDestination, String asgName, String operationDescription,
            String reasonAsgIsUnhealthy)

    /**
     * Sends a notification about the status of the deployment.
     *
     * @param notificationDestination where deployment notifications will be sent
     * @param asgName of the ASG to modify
     * @param subject of the notification
     * @param reasonAsgIsUnhealthy textual description of the reason why an ASG is not at full health, or null if it is
     */
    void sendNotification(String notificationDestination, String asgName, String subject, String reasonAsgIsUnhealthy)
}
