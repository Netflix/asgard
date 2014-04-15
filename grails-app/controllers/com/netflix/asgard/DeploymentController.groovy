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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.ec2.model.AvailabilityZone
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.asgard.deployment.DeploymentWorkflowOptions
import com.netflix.asgard.deployment.ProceedPreference
import com.netflix.asgard.deployment.StartDeploymentRequest
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.AutoScalingGroupHealthCheckType
import com.netflix.asgard.model.Deployment
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.SubnetTarget
import com.netflix.asgard.model.Subnets
import com.netflix.asgard.push.Cluster
import grails.converters.JSON
import grails.converters.XML

/**
 * Allows management of workflows that automate the deployment of a new ASG to an existing controller.
 */
class DeploymentController {

    static allowedMethods = [proceed: 'POST', rollback: 'POST', startDeployment: 'POST']
    static defaultAction = "list"

    def applicationService
    def awsAutoScalingService
    def awsEc2Service
    def awsLoadBalancerService
    def configService
    def deploymentService
    def flowService
    def instanceTypeService
    ObjectMapper objectMapper

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
            response.status = 404
        }
        deploymentService.cancelDeployment(userContext, deployment)
    }

    /**
     * Proceed with deployment.
     *
     * @param id of the deployment
     * @param token needed to complete SWF activity during deployment
     */
    def proceed() {
        String id = request.JSON.id
        String token = request.JSON.token
        completeDeploymentActivity(token, id, true)
    }

    /**
     * Rollback the deployment.
     *
     * @param id of the deployment
     * @param token needed to complete SWF activity during deployment
     */
    def rollback() {
        String id = request.JSON.id
        String token = request.JSON.token
        completeDeploymentActivity(token, id, false)
    }

    private void completeDeploymentActivity(String token, String id, boolean shouldProceed) {
        ManualActivityCompletionClient manualActivityCompletionClient = flowService.
                getManualActivityCompletionClient(token)
        manualActivityCompletionClient.complete(shouldProceed)
        deploymentService.removeManualTokenForDeployment(id)
    }

    def prepareDeployment(String id) {
        UserContext userContext = UserContext.of(request)
        Cluster cluster = awsAutoScalingService.getCluster(userContext, id)
        AutoScalingGroup lastGroup = awsAutoScalingService.
                getAutoScalingGroup(userContext, cluster.last().autoScalingGroupName, From.AWS)
        Subnets subnets = awsEc2Service.getSubnets(userContext)
        AutoScalingGroupBeanOptions asgOverrides = AutoScalingGroupBeanOptions.from(lastGroup, subnets)
        asgOverrides.with {
            autoScalingGroupName = null
            launchConfigurationName = null
        }

        LaunchConfiguration lc = awsAutoScalingService.getLaunchConfiguration(userContext,
                lastGroup.launchConfigurationName)
        LaunchConfigurationBeanOptions lcOverrides = LaunchConfigurationBeanOptions.from(lc)
        lcOverrides.with {
            launchConfigurationName = null
            userData = null
            iamInstanceProfile = iamInstanceProfile ?: configService.defaultIamRole
            instanceMonitoring = null
        }

        String groupName = lastGroup.autoScalingGroupName
        String appName = Relationships.appNameFromGroupName(groupName)
        String email = applicationService.getEmailFromApp(userContext, appName)
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(
                clusterName: cluster.name,
                notificationDestination: email,
                delayDurationMinutes: 0,
                doCanary: false,
                canaryCapacity: 1,
                canaryStartUpTimeoutMinutes: 30,
                canaryJudgmentPeriodMinutes: 60,
                scaleUp: ProceedPreference.Ask,
                desiredCapacityStartUpTimeoutMinutes: 40,
                desiredCapacityJudgmentPeriodMinutes: 120,
                disablePreviousAsg: ProceedPreference.Ask,
                fullTrafficJudgmentPeriodMinutes: 240,
                deletePreviousAsg: ProceedPreference.Ask
        )

        String nextGroupName = Relationships.buildNextAutoScalingGroupName(lastGroup.autoScalingGroupName)
        Image currentImage = awsEc2Service.getImage(userContext, lc.imageId)
        Collection<Image> images = awsEc2Service.getImagesForPackage(userContext, currentImage.packageName)
        List<SecurityGroup> effectiveSecurityGroups = awsEc2Service.getEffectiveSecurityGroups(userContext)
        Map<String, String> purposeToVpcId = subnets.mapPurposeToVpcId()
        Collection<AvailabilityZone> allAvailabilityZones = awsEc2Service.getAvailabilityZones(userContext)
        List<LoadBalancerDescription> loadBalancers = awsLoadBalancerService.getLoadBalancers(userContext).
                sort { it.loadBalancerName.toLowerCase() }
        List<String> subnetPurposes = subnets.getPurposesForZones(allAvailabilityZones*.zoneName,
                SubnetTarget.EC2).sort()
        List<Map<String, String>> availabilityZonesAndPurpose = []
        subnets.groupZonesByPurpose(allAvailabilityZones*.zoneName, SubnetTarget.EC2).each { purpose, zones ->
            zones.each { zone ->
                availabilityZonesAndPurpose << [
                        zone: zone,
                        purpose: purpose ?: ''
                ]
            }
        }

        Map<String, Object> attributes = [
                deploymentOptions: deploymentOptions,
                lcOptions: lcOverrides,
                asgOptions: asgOverrides,
                environment: [
                        nextGroupName: nextGroupName,
                        terminationPolicies: awsAutoScalingService.terminationPolicyTypes,
                        purposeToVpcId: purposeToVpcId,
                        subnetPurposes: subnetPurposes,
                        availabilityZonesAndPurpose: availabilityZonesAndPurpose,
                        loadBalancers: loadBalancers.collect {
                            [id: it.loadBalancerName, vpcId: it.getVPCId() ?: '']
                        },
                        healthCheckTypes: AutoScalingGroupHealthCheckType.values().collect {
                            [id: it.name(), description: it.description]
                        },
                        instanceTypes: instanceTypeService.getInstanceTypes(userContext).collect {
                            [id: it.name,
                                    price: it.monthlyLinuxOnDemandPrice ? it.monthlyLinuxOnDemandPrice + '/mo' : '']
                        },
                        securityGroups: effectiveSecurityGroups.collect {
                            [id: it.groupId, name: it.groupName, selection: it.vpcId ? it.groupId : it.groupName,
                                    vpcId: it.vpcId ?: '']
                        },
                        images: images.sort { it.imageLocation.toLowerCase() }.collect {
                            [id: it.imageId, imageLocation: it.imageLocation]
                        },
                        keys: awsEc2Service.getKeys(userContext).collect { it.keyName.toLowerCase() }.sort(),
                        spotUrl: configService.spotUrl,
                ]
        ]
        render objectMapper.writer().writeValueAsString(attributes)
    }

    /**
     * Start a deployment.
     */
    def startDeployment() {
        UserContext userContext = UserContext.of(request)
        String json = request.JSON.toString()
        StartDeploymentRequest startDeploymentRequest = objectMapper.reader(StartDeploymentRequest).readValue(json)
        List<String> validationErrors = startDeploymentRequest.validationErrors
        if (validationErrors) {
            response.setStatus(422)
            render ([validationErrors: validationErrors] as JSON)
        } else {
            String deploymentId = deploymentService.startDeployment(userContext,
                    startDeploymentRequest.deploymentOptions, startDeploymentRequest.lcOverrides,
                    startDeploymentRequest.asgOverrides)
            render ([deploymentId: deploymentId] as JSON)
        }
    }
}
