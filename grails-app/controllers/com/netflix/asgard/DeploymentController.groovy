/*
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.asgard

import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient
import com.google.common.collect.Sets
import com.netflix.asgard.deployment.DeploymentWorkflowOptions
import com.netflix.asgard.deployment.ProceedPreference
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.AutoScalingProcessType
import com.netflix.asgard.model.Deployment
import com.netflix.asgard.model.InstancePriceType
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.Subnets
import com.netflix.asgard.push.Cluster
import grails.converters.JSON
import grails.converters.XML

/**
 * Allows management of workflows that automate the deployment of a new ASG to an existing controller.
 */
class DeploymentController {

    static allowedMethods = [deploy: 'POST', proceed: 'POST', rollback: 'POST']

    def applicationService
    def awsAutoScalingService
    def awsEc2Service
    def deploymentService
    def flowService

    def index = { redirect(action: 'list', params: params) }

    /**
     * Lists all running deployments and the last 100 completed deployments.
     */
    def list() {
        List<Deployment> deployments = deploymentService.runningDeployments + deploymentService.finishedDeployments
        withFormat {
            html { [ deployments : deployments ] }
            xml { new XML(deployments).render(response) }
            json { new JSON(deployments).render(response) }
        }
    }

    /**
     * Shows the details of a specific deployment.
     *
     * @param id of the deployment
     */
    def show(String id) {
        Deployment deployment = deploymentService.getDeploymentById(id)
        if (!deployment) {
            Requests.renderNotFound('Deployment', id, this)
        } else {
            withFormat {
                html { return [ deployment : deployment ] }
                xml { new XML(deployment).render(response) }
                json { new JSON(deployment).render(response) }
            }
        }
    }

    /**
     * Cancels a deployment.
     *
     * @param id of the deployment
     */
    def cancel(String id) {
        UserContext userContext = UserContext.of(request)
        Deployment deployment = deploymentService.getDeploymentById(id)
        if (!deployment) {
            Requests.renderNotFound('Deployment', id, this)
        } else {
            deploymentService.cancelDeployment(userContext, deployment)
            flash.message = "Deployment '${id}' canceled ('${deployment.description}')."
            redirect(action: 'show', id: id)
        }
    }

    /**
     * Proceed with deployment.
     *
     * @param id of the deployment
     * @param token needed to complete SWF activity during deployment
     */
    def proceed(String id, String token) {
        completeDeploymentActivity(token, id, true)
    }

    /**
     * Rollback the deployment.
     *
     * @param id of the deployment
     * @param token needed to complete SWF activity during deployment
     */
    def rollback(String id, String token) {
        completeDeploymentActivity(token, id, false)
    }

    private void completeDeploymentActivity(String token, String id, boolean shouldProceed) {
        ManualActivityCompletionClient manualActivityCompletionClient = flowService.
                getManualActivityCompletionClient(token)
        try {
            manualActivityCompletionClient.complete(shouldProceed)
            flash.message = "Automated deployment will ${shouldProceed ? '' : 'not '}proceed."
        } catch (Exception e) {
            flash.message = "Deployment failed: ${e.toString()}"
        }
        redirect([controller: 'deployment', action: 'show', id: id])
    }

    /**
     * Start a deployment.
     *
     * @param cmd holds deployment attributes
     */
    def deploy(DeployCommand cmd) {
        UserContext userContext = UserContext.of(request)
        String clusterName = cmd.clusterName
        if (cmd.hasErrors()) {
            flash.message = "Cluster '${clusterName}' is invalid."
            chain(controller: 'cluster', action: 'prepareDeployment', model: [cmd: cmd], params: params)
            return
        }
        Cluster cluster = awsAutoScalingService.getCluster(userContext, clusterName)
        if (cluster.size() != 1) {
            flash.message = "Cluster '${clusterName}' should only have one ASG to enable automatic deployment."
            chain(controller: 'cluster', action: 'prepareDeployment', model: [cmd: cmd], params: params,
                    id: clusterName)
            return
        }
        AutoScalingGroupData group = cluster.last()
        if ( group.isLaunchingSuspended() ||
                group.isTerminatingSuspended() ||
                group.isAddingToLoadBalancerSuspended()
        ) {
            flash.message = "ASG in cluster '${clusterName}' should be receiving traffic to enable automatic deployment."
            chain(controller: 'cluster', action: 'prepareDeployment', model: [cmd: cmd], params: params,
                    id: clusterName)
            return
        }
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions()
        bindData(deploymentOptions, params)
        deploymentOptions.clusterName = clusterName

        if (params.createAsgOnly) {
            String appName = Relationships.appNameFromGroupName(clusterName)
            String email = applicationService.getEmailFromApp(userContext, appName)
            deploymentOptions.with {
                notificationDestination = email
                delayDurationMinutes = 0
                doCanary = false
                desiredCapacityStartUpTimeoutMinutes = 30
                desiredCapacityJudgmentPeriodMinutes = 0
                disablePreviousAsg = ProceedPreference.No
            }
        }
        String subnetPurpose = params.subnetPurpose
        Subnets subnets = awsEc2Service.getSubnets(userContext)
        String vpcId = subnets.getVpcIdForSubnetPurpose(subnetPurpose) ?: ''
        List<String> loadBalancerNames = Requests.ensureList(params["selectedLoadBalancersForVpcId${vpcId}"] ?:
                params["selectedLoadBalancers"])

        Collection<AutoScalingProcessType> newSuspendedProcesses = Sets.newHashSet()
        if (params.azRebalance == 'disabled') {
            newSuspendedProcesses << AutoScalingProcessType.AZRebalance
        }
        if (Boolean.parseBoolean(params.trafficAllowed)) {
            newSuspendedProcesses << AutoScalingProcessType.AddToLoadBalancer
        }

        AutoScalingGroupBeanOptions asgOptions = new AutoScalingGroupBeanOptions(
                availabilityZones: Requests.ensureList(params.selectedZones),
                loadBalancerNames: loadBalancerNames,
                minSize: params.min as Integer,
                desiredCapacity: params.desiredCapacity as Integer,
                maxSize: params.max as Integer,
                defaultCooldown: params.defaultCooldown as Integer,
                healthCheckType: params.healthCheckType,
                healthCheckGracePeriod: params.healthCheckGracePeriod as Integer,
                terminationPolicies: Requests.ensureList(params.terminationPolicy),
                subnetPurpose: subnetPurpose,
                suspendedProcesses: newSuspendedProcesses
        )
        LaunchConfigurationBeanOptions lcOptions = new LaunchConfigurationBeanOptions(
                imageId: params.imageId,
                instanceType: params.instanceType,
                keyName: params.keyName,
                securityGroups: Requests.ensureList(params.selectedSecurityGroups),
                iamInstanceProfile: params.iamInstanceProfile,
                instancePriceType: InstancePriceType.parse(params.pricing),
                ebsOptimized: params.ebsOptimized?.toBoolean()
        )

        String taskId = deploymentService.startDeployment(userContext, cmd.clusterName, deploymentOptions, lcOptions,
                asgOptions)
        redirect(controller: 'deployment', action: 'show', id: taskId)
    }
}

class DeployCommand {
    String clusterName

    static constraints = {
        clusterName(nullable: false, blank: false)
    }
}
