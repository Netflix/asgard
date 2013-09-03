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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.autoscaling.model.ScheduledUpdateGroupAction
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.simpleworkflow.flow.annotations.ManualActivityCompletion
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.google.common.collect.Sets
import com.netflix.asgard.AppRegistration
import com.netflix.asgard.ApplicationService
import com.netflix.asgard.AwsAutoScalingService
import com.netflix.asgard.AwsEc2Service
import com.netflix.asgard.AwsLoadBalancerService
import com.netflix.asgard.BeanState
import com.netflix.asgard.Caches
import com.netflix.asgard.CloudReadyService
import com.netflix.asgard.ConfigService
import com.netflix.asgard.DiscoveryService
import com.netflix.asgard.EmailerService
import com.netflix.asgard.LaunchTemplateService
import com.netflix.asgard.Relationships
import com.netflix.asgard.SpotInstanceRequestService
import com.netflix.asgard.Task
import com.netflix.asgard.Time
import com.netflix.asgard.UserContext
import com.netflix.asgard.flow.Activity
import com.netflix.asgard.flow.SwfActivity
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.AutoScalingProcessType
import com.netflix.asgard.model.InstancePriceType
import com.netflix.asgard.model.ScalingPolicyData
import com.netflix.asgard.model.Subnets
import com.netflix.asgard.push.AsgDeletionMode
import com.netflix.asgard.push.PushException
import org.codehaus.groovy.grails.web.mapping.LinkGenerator

class DeploymentActivitiesImpl implements DeploymentActivities {

    @Delegate Activity activity = new SwfActivity()

    ApplicationService applicationService
    AwsAutoScalingService awsAutoScalingService
    AwsEc2Service awsEc2Service
    AwsLoadBalancerService awsLoadBalancerService
    Caches caches
    CloudReadyService cloudReadyService
    ConfigService configService
    DiscoveryService discoveryService
    EmailerService emailerService
    LaunchTemplateService launchTemplateService
    LinkGenerator grailsLinkGenerator
    SpotInstanceRequestService spotInstanceRequestService

    @Override
    AsgDeploymentNames getAsgDeploymentNames(UserContext userContext, String clusterName, String newSubnetPurpose,
            List<String> newZones) {
        AutoScalingGroupData lastAsg = awsAutoScalingService.getCluster(userContext, clusterName).last()
        String nextAsgName = Relationships.buildNextAutoScalingGroupName(lastAsg.autoScalingGroupName)

        Subnets subnets = awsEc2Service.getSubnets(userContext)
        String newVpcZoneIdentifier
        if (newSubnetPurpose == null) {
            newVpcZoneIdentifier = subnets.constructNewVpcZoneIdentifierForZones(lastAsg.vpcZoneIdentifier, newZones)
        } else {
            newVpcZoneIdentifier = subnets.constructNewVpcZoneIdentifierForPurposeAndZones(newSubnetPurpose, newZones)
        }
        new AsgDeploymentNames(
                previousAsgName: lastAsg.autoScalingGroupName,
                previousLaunchConfigName: lastAsg.launchConfigurationName,
                previousVpcZoneIdentifier: lastAsg.vpcZoneIdentifier,
                nextAsgName: nextAsgName,
                nextLaunchConfigName: Relationships.buildLaunchConfigurationName(nextAsgName),
                nextVpcZoneIdentifier: newVpcZoneIdentifier
        )
    }

    @Override
    String createLaunchConfigForNextAsg(UserContext userContext, AsgDeploymentNames asgDeploymentNames,
            LaunchConfigurationOptions overrides, InstancePriceType instancePriceType) {

        LaunchConfiguration templateLaunchConfiguration = awsAutoScalingService.getLaunchConfiguration(
                userContext, asgDeploymentNames.previousLaunchConfigName)
        LaunchConfiguration launchConfiguration = new LaunchConfiguration()
        BeanState.ofSourceBean(templateLaunchConfiguration).injectState(launchConfiguration)
        BeanState.ofSourceBean(overrides).injectState(launchConfiguration)
        launchConfiguration.launchConfigurationName = asgDeploymentNames.nextLaunchConfigName
        Collection<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups(
                launchConfiguration.securityGroups, asgDeploymentNames.nextVpcZoneIdentifier, userContext.region)
        launchConfiguration.securityGroups = securityGroups

        AutoScalingGroup templateAutoScalingGroup = awsAutoScalingService.getAutoScalingGroup(userContext,
                asgDeploymentNames.previousAsgName)
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup()
        BeanState.ofSourceBean(templateAutoScalingGroup).injectState(autoScalingGroup)
        autoScalingGroup.autoScalingGroupName = asgDeploymentNames.nextAsgName

        launchConfiguration.iamInstanceProfile = launchConfiguration.iamInstanceProfile ?: configService.defaultIamRole
        if (instancePriceType == InstancePriceType.SPOT ||
                (!instancePriceType && templateLaunchConfiguration.spotPrice)) {
            launchConfiguration.spotPrice = spotInstanceRequestService.recommendSpotPrice(userContext,
                    launchConfiguration.instanceType)
        }

        String appName = Relationships.appNameFromGroupName(asgDeploymentNames.nextAsgName)
        AppRegistration app = applicationService.getRegisteredApplication(userContext, appName)
        Image image = awsEc2Service.getImage(userContext, overrides.imageId)
        launchConfiguration.userData = launchTemplateService.buildUserData(userContext, app, image, autoScalingGroup,
                launchConfiguration)
        awsAutoScalingService.createLaunchConfiguration(userContext, launchConfiguration)
        launchConfiguration.launchConfigurationName
    }

    @Override
    String createNextAsgForCluster(UserContext userContext, AsgDeploymentNames asgDeploymentNames,
            AutoScalingGroupOptions overrides, Boolean initialTrafficPrevented, Boolean azRebalanceSuspended) {
        Task task = new Task()
        AutoScalingGroup templateAsg = awsAutoScalingService.getAutoScalingGroup(userContext,
                asgDeploymentNames.previousAsgName)
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup()
        BeanState.ofSourceBean(templateAsg).injectState(autoScalingGroup)
        BeanState.ofSourceBean(overrides).injectState(autoScalingGroup)
        autoScalingGroup.withAutoScalingGroupName(asgDeploymentNames.nextAsgName).withMinSize(0).withDesiredCapacity(0).
                withVPCZoneIdentifier(asgDeploymentNames.nextVpcZoneIdentifier)
        Collection<AutoScalingProcessType> suspendedProcesses = Sets.newHashSet()
        boolean lastRebalanceSuspended = templateAsg.isProcessSuspended(AutoScalingProcessType.AZRebalance)
        if ((azRebalanceSuspended == null) ? lastRebalanceSuspended : azRebalanceSuspended) {
            suspendedProcesses << AutoScalingProcessType.AZRebalance
        }
        if (initialTrafficPrevented) {
            suspendedProcesses << AutoScalingProcessType.AddToLoadBalancer
        }
        AutoScalingGroup resultingAutoScalingGroup = awsAutoScalingService.createAutoScalingGroup(userContext,
                autoScalingGroup, asgDeploymentNames.nextLaunchConfigName, suspendedProcesses, task)
        resultingAutoScalingGroup?.autoScalingGroupName
    }

    @Override
    Integer copyScalingPolicies(UserContext userContext, AsgDeploymentNames asgDeploymentNames) {
        List<ScalingPolicyData> newScalingPolicies = awsAutoScalingService.getScalingPolicyDatas(userContext,
                asgDeploymentNames.previousAsgName).collect { it.copyForAsg(asgDeploymentNames.nextAsgName) }
        awsAutoScalingService.createScalingPolicies(userContext, newScalingPolicies, new Task())
        newScalingPolicies.size()
    }

    @Override
    Integer copyScheduledActions(UserContext userContext, AsgDeploymentNames asgDeploymentNames) {
        List<ScheduledUpdateGroupAction> lastScheduledActions = awsAutoScalingService.getScheduledActionsForGroup(
                userContext, asgDeploymentNames.previousAsgName)
        List<ScheduledUpdateGroupAction> newScheduledActions = awsAutoScalingService.copyScheduledActionsForNewAsg(
                userContext, asgDeploymentNames.nextAsgName, lastScheduledActions)
        awsAutoScalingService.createScheduledActions(userContext, newScheduledActions, new Task())
        lastScheduledActions.size()
    }

    @Override
    void resizeAsg(UserContext userContext, String asgName, int min, int desired, int max) {
        awsAutoScalingService.resizeAutoScalingGroup(userContext, asgName, min, desired, max)
    }

    @Override
    String reasonAsgIsUnhealthy(UserContext userContext, String asgName, int expectedSize) {
        String reasonAsgIsUnhealthy = awsAutoScalingService.reasonAsgIsUnhealthy(userContext, asgName, expectedSize)
        if (reasonAsgIsUnhealthy) {
            throw new PushException(reasonAsgIsUnhealthy)
        }
        null
    }

    @Override
    void enableAsg(UserContext userContext, String asgName) {
        Task task = new Task()
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, asgName)
        String appName = Relationships.appNameFromGroupName(asgName)
        awsAutoScalingService.removeExpirationTime(userContext, asgName, task)
        AutoScalingProcessType.getDisableProcesses().each { AutoScalingProcessType processType ->
            awsAutoScalingService.resumeProcess(userContext, processType, asgName, task)
        }
        List<String> instanceIds = group.instances.collect { it.instanceId }
        if (instanceIds.size()) {
            group.loadBalancerNames.eachWithIndex { String loadBalName, int i ->
                if (i >= 1) { Time.sleepCancellably(250) } // Avoid rate limits when there are dozens of ELBs
                awsLoadBalancerService.addInstances(userContext, loadBalName, instanceIds, task)
            }
            if (configService.doesRegionalDiscoveryExist(userContext.region)) {
                discoveryService.enableAppInstances(userContext, appName, instanceIds, task)
            }
        }
    }

    @Override
    void disableAsg(UserContext userContext, String asgName) {
        Task task = new Task()
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, asgName)
        String appName = Relationships.appNameFromGroupName(asgName)
        AutoScalingProcessType.getDisableProcesses().each { AutoScalingProcessType processType ->
            awsAutoScalingService.suspendProcess(userContext, processType, asgName, task)
        }
        awsAutoScalingService.setExpirationTime(userContext, asgName, Time.now().plusDays(2), task)
        List<String> instanceIds = group.instances.collect { it.instanceId }
        if (instanceIds.size()) {
            if (group.loadBalancerNames.size()) {
                group.loadBalancerNames.eachWithIndex { String loadBalName, int i ->
                    if (i >= 1) { Time.sleepCancellably(250) } // Avoid rate limits when there are dozens of ELBs
                    awsLoadBalancerService.removeInstances(userContext, loadBalName, instanceIds, task)
                }
            }
            if (configService.doesRegionalDiscoveryExist(userContext.region)) {
                discoveryService.disableAppInstances(userContext, appName, instanceIds, task)
            }
        }
    }

    @Override
    void deleteAsg(UserContext userContext, String asgName) {
        Task task = new Task()
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, asgName)
        List<String> oldLaunchConfigNames = awsAutoScalingService.getLaunchConfigurationNamesForAutoScalingGroup(
                userContext, asgName).findAll { it != group.launchConfigurationName }
        awsAutoScalingService.deleteAutoScalingGroup(userContext, asgName, AsgDeletionMode.FORCE, task)
        oldLaunchConfigNames.each {
            awsAutoScalingService.deleteLaunchConfiguration(userContext, it, task)
        }
    }

    @ManualActivityCompletion
    @Override
    Boolean askIfDeploymentShouldProceed(String notificationDestination, String asgName, String operationDescription,
            String reasonAsgIsUnhealthy) {
        WorkflowExecution workflowExecution = activity.workflowExecution
        String message = """
        Auto Scaling Group '${asgName}' is being deployed.
        ${operationDescription}
        ${getStatusText(asgName, reasonAsgIsUnhealthy)}
        Please determine if the deployment should proceed.

        ${grailsLinkGenerator.link(base: configService.linkCanonicalServerUrl, controller: 'task', action: 'show',
                params: [workflowId: workflowExecution.workflowId, runId: workflowExecution.runId,
                        taskToken: activity.taskToken])}
        """.stripIndent()
        String subject = "Asgard deployment response requested for '${asgName}'."
        emailerService.sendUserEmail(notificationDestination, subject, message)
        true
    }

    @Override
    void sendNotification(String notificationDestination, String asgName, String subject, String reasonAsgIsUnhealthy) {
        String clusterName = Relationships.clusterFromGroupName(asgName)
        String message = """
        ${getStatusText(asgName, reasonAsgIsUnhealthy)}

        ${grailsLinkGenerator.link(base: configService.linkCanonicalServerUrl, controller: 'cluster', action: 'show',
                id: clusterName)}
        """.stripIndent()
        emailerService.sendUserEmail(notificationDestination, subject, message)
    }

    private String getStatusText(String asgName, String reasonAsgIsUnhealthy) {
        String status = reasonAsgIsUnhealthy ? "unhealthy. ${reasonAsgIsUnhealthy}" : 'healthy.'
        "Auto Scaling Group '${asgName}' is ${status}"
    }
}
