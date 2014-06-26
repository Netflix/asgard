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
package com.netflix.asgard

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.autoscaling.model.ScheduledUpdateGroupAction
import com.amazonaws.services.ec2.model.AvailabilityZone
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.google.common.annotations.VisibleForTesting
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.AutoScalingProcessType
import com.netflix.asgard.model.Deployment
import com.netflix.asgard.model.InstancePriceType
import com.netflix.asgard.model.ScalingPolicyData
import com.netflix.asgard.model.SubnetTarget
import com.netflix.asgard.model.Subnets
import com.netflix.asgard.push.Cluster
import com.netflix.asgard.push.CommonPushOptions
import com.netflix.asgard.push.GroupActivateOperation
import com.netflix.asgard.push.GroupCreateOptions
import com.netflix.asgard.push.GroupDeactivateOperation
import com.netflix.asgard.push.GroupDeleteOperation
import com.netflix.asgard.push.GroupResizeOperation
import com.netflix.asgard.push.InitialTraffic
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML
import groovy.transform.PackageScope

@ContextParam('region')
class ClusterController {

    static allowedMethods = [createNextGroup: 'POST', resize: 'POST', delete: 'POST', activate: 'POST',
            deactivate: 'POST']

    def grailsApplication
    def applicationService
    def awsAutoScalingService
    def awsEc2Service
    def awsLoadBalancerService
    def configService
    def deploymentService
    def mergedInstanceService
    def pushService
    def spotInstanceRequestService
    def taskService

    def index() {
        redirect(action: 'list', params: params)
    }

    def list() {
        UserContext userContext = UserContext.of(request)
        Collection<Cluster> clusterObjects = awsAutoScalingService.getClusters(userContext)
        Set<String> appNames = Requests.ensureList(params.id).collect { it.split(',') }.flatten() as Set<String>
        if (appNames) {
            clusterObjects = clusterObjects.findAll { Cluster cluster ->
                appNames.contains(Relationships.appNameFromGroupName(cluster.name))
            }
        }
        clusterObjects = clusterObjects.sort { it.name.toLowerCase() }
        withFormat {
            html { [clusters: clusterObjects, appNames: appNames] }
            xml {
                render(contentType: "text/xml") {
                    clusters {
                        for (c in clusterObjects) {
                            cluster() {
                                name(c.name)
                                for (AutoScalingGroupData asg in c.groups) {
                                    autoScalingGroup(asg.autoScalingGroupName)
                                }
                            }

                        }
                    }
                }
            }
            json {
                def clusterLights = []
                clusterObjects.each { Cluster cluster ->
                    clusterLights.add([
                            cluster: cluster.name,
                            autoScalingGroups: cluster.collect { it.autoScalingGroupName }.sort()
                    ])
                }
                render clusterLights as JSON
            }
        }
    }

    def show() {
        UserContext userContext = UserContext.of(request)
        String name = params.id ?: params.name
        Cluster cluster = awsAutoScalingService.getCluster(userContext, name)
        if (!cluster) {
            Requests.renderNotFound('Cluster', name, this)
        } else if (name == cluster.name) {
            withFormat {
                html {
                    AutoScalingGroupData lastGroup = cluster.last()
                    String nextGroupName = Relationships.buildNextAutoScalingGroupName(lastGroup.autoScalingGroupName)
                    int clusterMaxGroups = configService.clusterMaxGroups
                    Boolean okayToCreateGroup = cluster.size() < clusterMaxGroups
                    String recommendedNextStep = cluster.size() >= clusterMaxGroups ?
                        'Delete an old group before pushing to a new group.' :
                        cluster.size() <= 1 ? 'Create a new group and switch traffic to it' :
                        'Switch traffic to the preferred group, then delete legacy group'
                    Collection<Task> runningTasks = taskService.getRunningTasksByObject(Link.to(EntityType.cluster,
                            cluster.name), userContext.region)

                    boolean showAllImages = params.allImages ? true : false
                    Map attributes = pushService.prepareEdit(userContext, lastGroup.autoScalingGroupName, showAllImages,
                            Requests.ensureList(params.selectedSecurityGroups))
                    Collection<AvailabilityZone> availabilityZones = awsEc2Service.getAvailabilityZones(userContext)
                    Collection<String> selectedZones = awsEc2Service.preselectedZoneNames(availabilityZones,
                            Requests.ensureList(params.selectedZones), lastGroup)
                    List<LoadBalancerDescription> loadBalancers = awsLoadBalancerService.getLoadBalancers(userContext).
                            sort { it.loadBalancerName.toLowerCase() }
                    Subnets subnets = awsEc2Service.getSubnets(userContext)
                    List<String> subnetIds = Relationships.subnetIdsFromVpcZoneIdentifier(lastGroup.vpcZoneIdentifier)
                    String subnetPurpose = subnets.coerceLoneOrNoneFromIds(subnetIds)?.purpose
                    String vpcId = subnets.getVpcIdForSubnetPurpose(subnetPurpose) ?: ''
                    List<String> selectedLoadBalancers = Requests.ensureList(
                            params["selectedLoadBalancersForVpcId${vpcId}"]) ?: lastGroup.loadBalancerNames
                    log.debug """ClusterController.show for Cluster '${cluster.name}' Load Balancers from last Group: \
${lastGroup.loadBalancerNames}"""
                    List<String> subnetPurposes = subnets.getPurposesForZones(availabilityZones*.zoneName,
                            SubnetTarget.EC2).sort()
                    Map<String, Collection<String>> zonesByPurpose = subnets.groupZonesByPurpose(
                            availabilityZones*.zoneName, SubnetTarget.EC2)
                    Deployment deployment = deploymentService.getRunningDeploymentForCluster(userContext.region,
                            cluster.name)
                    attributes.putAll([
                            cluster: cluster,
                            runningTasks: runningTasks,
                            group: lastGroup,
                            nextGroupName: nextGroupName,
                            okayToCreateGroup: okayToCreateGroup,
                            recommendedNextStep: recommendedNextStep,
                            buildServer: configService.buildServerUrl,
                            vpcZoneIdentifier: lastGroup.vpcZoneIdentifier,
                            zonesGroupedByPurpose: zonesByPurpose,
                            selectedZones: selectedZones,
                            subnetPurposes: subnetPurposes,
                            subnetPurpose: subnetPurpose ?: null,
                            loadBalancersGroupedByVpcId: loadBalancers.groupBy { it.VPCId },
                            selectedLoadBalancers: selectedLoadBalancers,
                            spotUrl: configService.spotUrl,
                            pricing: params.pricing ?: attributes.pricing,
                            deployment: deployment
                    ])
                    attributes
                }
                xml { new XML(cluster).render(response) }
                json { new JSON(cluster).render(response) }
            }
        } else {
            params['id'] = cluster.name
            redirect(action: 'show', params: params)
        }
    }

    def showLastGroup() {
        UserContext userContext = UserContext.of(request)
        String name = params.id ?: params.name
        Cluster cluster = awsAutoScalingService.getCluster(userContext, name)
        if (!cluster) {
            Requests.renderNotFound('Cluster', name, this)
        } else {
            redirect([controller: 'autoScaling', action: 'show', params: [id: cluster.last().autoScalingGroupName]])
        }
    }

    def result() {
        render view: '/common/result'
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def createNextGroup() {
        UserContext userContext = UserContext.of(request)
        String name = params.name
        Cluster cluster = awsAutoScalingService.getCluster(userContext, name)

        if (!cluster) {
            flash.message = "No auto scaling groups exist with cluster name ${name}"
            redirect(action: 'result')
            return
        }

        Boolean okayToCreateGroup = cluster.size() < configService.clusterMaxGroups
        if (okayToCreateGroup) {
            AutoScalingGroupData lastGroup = cluster.last()
            String lcName = lastGroup.launchConfigurationName
            LaunchConfiguration lastLaunchConfig = awsAutoScalingService.getLaunchConfiguration(userContext, lcName)
            String appName = Relationships.appNameFromGroupName(name)
            List<String> securityGroups = Requests.ensureList(params.selectedSecurityGroups)
            List<String> termPolicies = Requests.ensureList(params.terminationPolicy)
            Subnets subnets = awsEc2Service.getSubnets(userContext)
            String subnetPurpose = params.subnetPurpose
            String vpcId = subnets.getVpcIdForSubnetPurpose(subnetPurpose) ?: ''
            List<String> loadBalancerNames = Requests.ensureList(params["selectedLoadBalancersForVpcId${vpcId}"] ?:
                    params["selectedLoadBalancers"])
            // Availability zones default to the last group's value since this field is required.
            List<String> selectedZones = Requests.ensureList(params.selectedZones) ?: lastGroup.availabilityZones
            String azRebalance = params.azRebalance
            boolean lastRebalanceSuspended = lastGroup.isProcessSuspended(AutoScalingProcessType.AZRebalance)
            boolean azRebalanceSuspended = shouldAzRebalanceBeSuspended(azRebalance, lastRebalanceSuspended)
            Integer minSize = convertToIntOrUseDefault(params.min, lastGroup.minSize)
            Integer desiredCapacity = convertToIntOrUseDefault(params.desiredCapacity, lastGroup.desiredCapacity)
            Integer maxSize = convertToIntOrUseDefault(params.max, lastGroup.maxSize)
            InitialTraffic initialTraffic = params.trafficAllowed ? InitialTraffic.ALLOWED : InitialTraffic.PREVENTED
            boolean checkHealth = params.containsKey('checkHealth')
            String instanceType = params.instanceType ?: lastLaunchConfig.instanceType
            String spotPrice = determineSpotPrice(lastLaunchConfig, userContext, instanceType)

            final String nextGroupName = Relationships.buildNextAutoScalingGroupName(lastGroup.autoScalingGroupName)
            List<ScalingPolicyData> lastScalingPolicies = awsAutoScalingService.getScalingPolicyDatas(userContext,
                    lastGroup.autoScalingGroupName)
            List<ScalingPolicyData> newScalingPolicies = lastScalingPolicies.collect { ScalingPolicyData policy ->
                policy.copyForAsg(nextGroupName)
            }

            List<ScheduledUpdateGroupAction> lastScheduledActions = awsAutoScalingService.getScheduledActionsForGroup(
                    userContext, lastGroup.autoScalingGroupName)
            List<ScheduledUpdateGroupAction> newScheduledActions = awsAutoScalingService.copyScheduledActionsForNewAsg(
                    userContext, nextGroupName, lastScheduledActions)

            Integer lastGracePeriod = lastGroup.healthCheckGracePeriod
            String vpcZoneIdentifier = subnets.constructNewVpcZoneIdentifierForPurposeAndZones(subnetPurpose,
                    selectedZones)
            String iamInstanceProfile = params.iamInstanceProfile ?: lastLaunchConfig.iamInstanceProfile
            iamInstanceProfile = iamInstanceProfile ?: configService.defaultIamRole
            log.debug """ClusterController.createNextGroup for Cluster '${cluster.name}' Selected Load Balancers: \
${loadBalancerNames}"""
            log.debug """ClusterController.createNextGroup for Cluster '${cluster.name}' Load Balancers from last \
Group: ${lastGroup.loadBalancerNames}"""
            boolean ebsOptimized = params.containsKey('ebsOptimized') ? params.ebsOptimized?.toBoolean() :
                lastLaunchConfig.ebsOptimized
            if (params.noOptionalDefaults != 'true') {
                securityGroups = securityGroups ?: lastLaunchConfig.securityGroups
                termPolicies = termPolicies ?: lastGroup.terminationPolicies
                loadBalancerNames = loadBalancerNames ?: lastGroup.loadBalancerNames
                vpcZoneIdentifier = vpcZoneIdentifier ?: subnets.constructNewVpcZoneIdentifierForZones(
                        lastGroup.vpcZoneIdentifier, selectedZones)
            }
            log.debug """ClusterController.createNextGroup for Cluster '${cluster.name}' Load Balancers for next \
Group: ${loadBalancerNames}"""
            GroupCreateOptions options = new GroupCreateOptions(
                    common: new CommonPushOptions(
                            userContext: userContext,
                            checkHealth: checkHealth,
                            afterBootWait: convertToIntOrUseDefault(params.afterBootWait, 30),
                            appName: appName,
                            env: grailsApplication.config.cloud.accountName,
                            imageId: params.imageId ?: lastLaunchConfig.imageId,
                            instanceType: instanceType,
                            groupName: nextGroupName,
                            securityGroups: securityGroups,
                            maxStartupRetries: convertToIntOrUseDefault(params.maxStartupRetries, 5)
                    ),
                    initialTraffic: initialTraffic,
                    minSize: minSize,
                    desiredCapacity: desiredCapacity,
                    maxSize: maxSize,
                    defaultCooldown: convertToIntOrUseDefault(params.defaultCooldown, lastGroup.defaultCooldown),
                    healthCheckType: params.healthCheckType ?: lastGroup.healthCheckType.name(),
                    healthCheckGracePeriod: convertToIntOrUseDefault(params.healthCheckGracePeriod, lastGracePeriod),
                    terminationPolicies: termPolicies,
                    batchSize: convertToIntOrUseDefault(params.batchSize, GroupResizeOperation.DEFAULT_BATCH_SIZE),
                    loadBalancerNames: loadBalancerNames,
                    iamInstanceProfile: iamInstanceProfile,
                    keyName: params.keyName ?: lastLaunchConfig.keyName,
                    availabilityZones: selectedZones,
                    zoneRebalancingSuspended: azRebalanceSuspended,
                    scalingPolicies: newScalingPolicies,
                    scheduledActions: newScheduledActions,
                    vpcZoneIdentifier: vpcZoneIdentifier,
                    spotPrice: spotPrice,
                    ebsOptimized: ebsOptimized
            )
            def operation = pushService.startGroupCreate(options)
            flash.message = "${operation.task.name} has been started."
            redirectToTask(operation.taskId)
        }
    }

    @VisibleForTesting
    @PackageScope
    int convertToIntOrUseDefault(String value, Integer defaultValue) {
        value?.isInteger() ? value.toInteger() : defaultValue
    }

    private boolean shouldAzRebalanceBeSuspended(String azRebalance, boolean lastRebalanceSuspended) {
        (azRebalance == null) ? lastRebalanceSuspended : (azRebalance == 'disabled')
    }

    private String determineSpotPrice(LaunchConfiguration lastLaunchConfig, UserContext userContext,
                                      String instanceType) {
        String spotPrice = null
        if (!params.pricing) {
            spotPrice = lastLaunchConfig.spotPrice
        } else if (params.pricing == InstancePriceType.SPOT.name()) {
            spotPrice = spotInstanceRequestService.recommendSpotPrice(userContext, instanceType)
        }
        spotPrice
    }

    def resize() {
        UserContext userContext = UserContext.of(request)
        Integer newMin = (params.minSize ?: params.minAndMaxSize) as Integer
        Integer newMax = (params.maxSize ?: params.minAndMaxSize) as Integer
        Integer batchSize = params.batchSize as Integer
        GroupResizeOperation operation =
                pushService.startGroupResize(userContext, params.name, newMin, newMax, batchSize)
        redirectToTask(operation.taskId)
    }

    def delete() {
        UserContext userContext = UserContext.of(request)
        def name = params.name
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, name)
        if (group) {
            GroupDeleteOperation operation = pushService.startGroupDelete(userContext, group)
            redirectToTask(operation.taskId)
        } else {
            Requests.renderNotFound('Auto scaling group', name, this)
        }
    }

    def activate() {
        UserContext userContext = UserContext.of(request)
        GroupActivateOperation operation = pushService.startGroupActivate(userContext, params.name)
        redirectToTask(operation.taskId)
    }

    def deactivate() {
        UserContext userContext = UserContext.of(request)
        GroupDeactivateOperation operation = pushService.startGroupDeactivate(userContext, params.name)
        redirectToTask(operation.taskId)
    }

    def anyInstance() {
        UserContext userContext = UserContext.of(request)
        String name = params.id
        String field = params.field
        if (!name || !field) {
            response.status = 400
            if (!name) { render 'name is a required parameter' }
            if (!field) { render 'field is a required parameter' }
            return
        }
        Cluster cluster = awsAutoScalingService.getCluster(userContext, name)
        List<String> instanceIds = cluster?.instances*.instanceId
        MergedInstance mergedInstance = mergedInstanceService.findHealthyInstance(userContext, instanceIds)
        String result = mergedInstance?.getFieldValue(field)
        if (!result) {
            response.status = 404
            if (!cluster) { result = "No cluster found with name '$name'" }
            else if (!mergedInstance) { result = "No instances found for cluster '$name'" }
            else { result = "'$field' not found. Valid fields: ${mergedInstance.listFieldNames()}" }
        }
        render result
    }

    private void redirectToTask(String taskId) {
        redirect(controller: 'task', action: 'show', params: [id: taskId])
    }
}
