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
import com.amazonaws.services.autoscaling.model.ScheduledUpdateGroupAction
import com.amazonaws.services.simpleworkflow.flow.annotations.ManualActivityCompletion
import com.netflix.asgard.AwsAutoScalingService
import com.netflix.asgard.AwsEc2Service
import com.netflix.asgard.AwsLoadBalancerService
import com.netflix.asgard.AwsSimpleWorkflowService
import com.netflix.asgard.Caches
import com.netflix.asgard.ConfigService
import com.netflix.asgard.DeploymentService
import com.netflix.asgard.DiscoveryService
import com.netflix.asgard.EmailerService
import com.netflix.asgard.LaunchTemplateService
import com.netflix.asgard.PluginService
import com.netflix.asgard.Relationships
import com.netflix.asgard.Task
import com.netflix.asgard.Time
import com.netflix.asgard.UserContext
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.AutoScalingProcessType
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.ScalingPolicyData
import com.netflix.asgard.model.ScheduledAsgAnalysis
import com.netflix.asgard.model.Subnets
import com.netflix.asgard.model.SwfWorkflowTags
import com.netflix.asgard.model.WorkflowExecutionBeanOptions
import com.netflix.asgard.push.AsgDeletionMode
import com.netflix.glisten.ActivityOperations
import com.netflix.glisten.impl.swf.SwfActivityOperations
import org.codehaus.groovy.grails.web.mapping.LinkGenerator

class DeploymentActivitiesImpl implements DeploymentActivities {

    @Delegate ActivityOperations activity = new SwfActivityOperations()

    AwsAutoScalingService awsAutoScalingService
    AwsEc2Service awsEc2Service
    AwsLoadBalancerService awsLoadBalancerService
    AwsSimpleWorkflowService awsSimpleWorkflowService
    Caches caches
    ConfigService configService
    DeploymentService deploymentService
    DiscoveryService discoveryService
    EmailerService emailerService
    LaunchTemplateService launchTemplateService
    PluginService pluginService
    LinkGenerator grailsLinkGenerator

    @Override
    AsgDeploymentNames getAsgDeploymentNames(UserContext userContext, String clusterName) {
        AutoScalingGroupData lastAsg = awsAutoScalingService.getCluster(userContext, clusterName).last()
        String nextAsgName = Relationships.buildNextAutoScalingGroupName(lastAsg.autoScalingGroupName)
        new AsgDeploymentNames(
                previousAsgName: lastAsg.autoScalingGroupName,
                previousLaunchConfigName: lastAsg.launchConfigurationName,
                nextAsgName: nextAsgName,
                nextLaunchConfigName: Relationships.buildLaunchConfigurationName(nextAsgName)
        )
    }

    @Override
    LaunchConfigurationBeanOptions constructLaunchConfigForNextAsg(UserContext userContext,
            AutoScalingGroupBeanOptions nextAutoScalingGroup, LaunchConfigurationBeanOptions inputs) {
        LaunchConfigurationBeanOptions launchConfiguration = LaunchConfigurationBeanOptions.from(inputs)
        launchConfiguration.launchConfigurationName = nextAutoScalingGroup.launchConfigurationName
        Subnets subnets = awsEc2Service.getSubnets(userContext)
        String vpcId = subnets.getVpcIdForSubnetPurpose(nextAutoScalingGroup.subnetPurpose)
        Collection<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups(
                launchConfiguration.securityGroups, vpcId, userContext.region)
        launchConfiguration.securityGroups = securityGroups
        launchConfiguration.iamInstanceProfile = launchConfiguration.iamInstanceProfile ?: configService.defaultIamRole
        launchConfiguration
    }

    @Override
    String createLaunchConfigForNextAsg(UserContext userContext, AutoScalingGroupBeanOptions autoScalingGroup,
        LaunchConfigurationBeanOptions launchConfiguration) {
        launchConfiguration.userData = launchTemplateService.buildUserData(userContext, autoScalingGroup,
                launchConfiguration)
        awsAutoScalingService.createLaunchConfiguration(userContext, launchConfiguration, new Task())
        launchConfiguration.launchConfigurationName
    }

    @Override
    String createNextAsgForClusterWithoutInstances(UserContext userContext, AutoScalingGroupBeanOptions asgOptions) {
        Task task = new Task()
        AutoScalingGroupBeanOptions autoScalingGroupWithNoInstances = AutoScalingGroupBeanOptions.from(asgOptions)
        autoScalingGroupWithNoInstances.with {
            minSize = 0
            desiredCapacity = 0
        }
        AutoScalingGroup resultingAutoScalingGroup = awsAutoScalingService.createAutoScalingGroup(userContext,
                autoScalingGroupWithNoInstances, task)
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
    String reasonAsgIsNotOperational(UserContext userContext, String asgName, int expectedSize) {
        awsAutoScalingService.reasonAsgIsNotOperational(userContext, asgName, expectedSize)
    }

    @Override
    Boolean enableAsg(UserContext userContext, String asgName) {
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
        true
    }

    @Override
    Boolean disableAsg(UserContext userContext, String asgName) {
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
        true
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
    Boolean askIfDeploymentShouldProceed(UserContext userContext, String notificationDestination, String asgName,
            String operationDescription) {
        WorkflowExecutionBeanOptions workflowExecutionBeanOptions = awsSimpleWorkflowService.
                getWorkflowExecutionInfoByWorkflowExecution(activity.workflowExecution)
        SwfWorkflowTags tags = workflowExecutionBeanOptions.tags
        deploymentService.setManualTokenForDeployment(tags.id, activity.taskToken)
        String clusterName = Relationships.clusterFromGroupName(asgName)
        String link = grailsLinkGenerator.link(base: configService.linkCanonicalServerUrl, controller: 'cluster',
                action: 'show', params: [id: clusterName, region: userContext.region.code])
        String message = """
        Auto Scaling Group '${asgName}' is being deployed.
        ${operationDescription}
        Please determine if the deployment should proceed.

        ${link}
        """.stripIndent()

        String subject = "Asgard deployment response requested for '${asgName}'."
        emailerService.sendUserEmail(notificationDestination, subject, message)
        true
    }

    @Override
    void sendNotification(UserContext userContext, String notificationDestination, String clusterName, String subject,
            String message) {
        String messageWithLink = """\
        ${message}
        ${grailsLinkGenerator.link(base: configService.linkCanonicalServerUrl, controller: 'cluster', action: 'show',
                params: [id: clusterName, region: userContext.region.code])}""".stripIndent()
        emailerService.sendUserEmail(notificationDestination, subject, messageWithLink)
    }

    @Override
    ScheduledAsgAnalysis startAsgAnalysis(String clusterName, String notificationDestination) {
        pluginService.asgAnalyzer.startAnalysis(clusterName, notificationDestination)
    }

    @Override
    void stopAsgAnalysis(String name) {
        pluginService.asgAnalyzer.stopAnalysis(name)
    }

}
