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
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.AutoScalingProcessType
import com.netflix.asgard.model.GroupedInstance
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

@ContextParam('region')
class ClusterController {

    def static allowedMethods = [createNextGroup: 'POST', resize: 'POST', delete: 'POST', activate: 'POST',
            deactivate: 'POST']

    def grailsApplication
    def awsAutoScalingService
    def awsEc2Service
    def awsLoadBalancerService
    def configService
    def mergedInstanceService
    def pushService
    def spotInstanceRequestService
    def taskService

    def index = { redirect(action: 'list', params: params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        Collection<Cluster> clusterObjects = awsAutoScalingService.getClusters(userContext)
        Set<String> appNames = Requests.ensureList(params.id).collect { it.split(',') }.flatten() as Set<String>
        if (appNames) {
            clusterObjects = clusterObjects.findAll { Cluster cluster ->
                appNames.contains(Relationships.appNameFromGroupName(cluster.name))
            }
        }
        clusterObjects = clusterObjects.sort{ it.name.toLowerCase() }
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

    def show = {
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
                    Boolean okayToCreateGroup = cluster.size() < Relationships.CLUSTER_MAX_GROUPS
                    String recommendedNextStep = cluster.size() >= Relationships.CLUSTER_MAX_GROUPS ?
                        'Delete an old group before pushing to a new group.' :
                        cluster.size() <= 1 ? 'Create a new group and switch traffic to it' :
                        'Switch traffic to the preferred group, then delete legacy group'
                    Collection<Task> runningTasks = taskService.getRunningTasksByObject(Link.to(EntityType.cluster,
                            cluster.name), userContext.region)

                    boolean showAllImages = params.allImages ? true : false
                    Map attributes = pushService.prepareEdit(userContext, lastGroup.autoScalingGroupName, showAllImages,
                            actionName, Requests.ensureList(params.selectedSecurityGroups))
                    Collection<AvailabilityZone> availabilityZones = awsEc2Service.getAvailabilityZones(userContext)
                    Collection<String> selectedZones = awsEc2Service.preselectedZoneNames(availabilityZones,
                            Requests.ensureList(params.selectedZones), lastGroup)
                    Subnets subnets = awsEc2Service.getSubnets(userContext)
                    List<LoadBalancerDescription> loadBalancers = awsLoadBalancerService.getLoadBalancers(userContext).
                            sort { it.loadBalancerName.toLowerCase() }
                    List<String> selectedLoadBalancers = Requests.ensureList(params.selectedLoadBalancers) ?: lastGroup
                            .loadBalancerNames
                    List<String> subnetIds = Relationships.subnetIdsFromVpcZoneIdentifier(lastGroup.vpcZoneIdentifier)
                    String subnetPurpose = subnets.coerceLoneOrNoneFromIds(subnetIds)?.purpose
                    List<String> subnetPurposes = subnets.getPurposesForZones(availabilityZones*.zoneName,
                            SubnetTarget.EC2).sort()
                    attributes.putAll([
                            cluster: cluster,
                            runningTasks: runningTasks,
                            group: lastGroup,
                            nextGroupName: nextGroupName,
                            okayToCreateGroup: okayToCreateGroup,
                            recommendedNextStep: recommendedNextStep,
                            buildServer: grailsApplication.config.cloud.buildServer,
                            vpcZoneIdentifier: lastGroup.vpcZoneIdentifier,
                            zonesGroupedByPurpose: subnets.groupZonesByPurpose(availabilityZones*.zoneName, SubnetTarget.EC2),
                            selectedZones: selectedZones,
                            subnetPurposes: subnetPurposes,
                            subnetPurpose: subnetPurpose ?: null,
                            loadBalancersGroupedByVpcId: loadBalancers.groupBy { it.VPCId },
                            selectedLoadBalancers: selectedLoadBalancers,
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

    def showLastGroup = {
        UserContext userContext = UserContext.of(request)
        String name = params.id ?: params.name
        Cluster cluster = awsAutoScalingService.getCluster(userContext, name)
        if (!cluster) {
            Requests.renderNotFound('Cluster', name, this)
        } else {
            redirect([controller: 'autoScaling', action: 'show', params: [id: cluster.last().autoScalingGroupName]])
        }
    }

    def result = { render view: '/common/result' }

    def createNextGroup = {
        UserContext userContext = UserContext.of(request)
        String name = params.name
        Cluster cluster = awsAutoScalingService.getCluster(userContext, name)

        if (!cluster) {
            flash.message = "No auto scaling groups exist with cluster name ${name}"
            redirect(action: 'result')
            return
        }

        Boolean okayToCreateGroup = cluster.size() < Relationships.CLUSTER_MAX_GROUPS
        if (okayToCreateGroup) {
            AutoScalingGroupData lastGroup = cluster.last()
            String lcName = lastGroup.launchConfigurationName
            LaunchConfiguration lastLaunchConfig = awsAutoScalingService.getLaunchConfiguration(userContext, lcName)
            String appName = Relationships.appNameFromGroupName(name)
            List<String> securityGroups = Requests.ensureList(params.selectedSecurityGroups)
            List<String> termPolicies = Requests.ensureList(params.terminationPolicy)
            List<String> loadBalancerNames = Requests.ensureList(params.selectedLoadBalancers)
            // Availability zones default to the last group's value since this field is required.
            List<String> selectedZones = Requests.ensureList(params.selectedZones) ?: lastGroup.availabilityZones
            String azRebalance = params.azRebalance
            boolean lastRebalanceSuspended = lastGroup.isProcessSuspended(AutoScalingProcessType.AZRebalance)
            boolean azRebalanceSuspended = (azRebalance == null) ? lastRebalanceSuspended : (azRebalance == 'disabled')
            String minSizeParam = params.min
            Integer minSize = minSizeParam ? minSizeParam as Integer : lastGroup.minSize
            String desiredCapacityParam = params.desiredCapacity
            Integer desiredCapacity = desiredCapacityParam ? desiredCapacityParam as Integer : lastGroup.desiredCapacity
            String maxSizeParam = params.max
            Integer maxSize = maxSizeParam ? maxSizeParam as Integer : lastGroup.maxSize
            InitialTraffic initialTraffic = params.trafficAllowed ? InitialTraffic.ALLOWED : InitialTraffic.PREVENTED
            boolean checkHealth = params.containsKey('checkHealth')
            boolean discoveryExists = configService.doesRegionalDiscoveryExist(userContext.region)
            if (discoveryExists && initialTraffic == InitialTraffic.PREVENTED && !checkHealth) {
                flash.message = "Due to a Eureka limitation, you must enable traffic and/or wait for health checks"
                redirect(action: 'show', params: [id: name])
                return
            }
            String instanceType = params.instanceType ?: lastLaunchConfig.instanceType
            String spotPrice = null
            if (!params.pricing) {
                spotPrice = lastLaunchConfig.spotPrice
            } else if (params.pricing == InstancePriceType.SPOT.name()) {
                spotPrice = spotInstanceRequestService.recommendSpotPrice(userContext, instanceType)
            }

            final String nextGroupName = Relationships.buildNextAutoScalingGroupName(lastGroup.autoScalingGroupName)
            List<ScalingPolicyData> lastScalingPolicies = awsAutoScalingService.getScalingPolicyDatas(userContext,
                    lastGroup.autoScalingGroupName)
            List<ScalingPolicyData> newScalingPolicies = lastScalingPolicies.collect { ScalingPolicyData policy ->
                policy.copyForAsg(nextGroupName)
            }

            List<ScheduledUpdateGroupAction> lastScheduledActions = awsAutoScalingService.getScheduledActionsForGroup(
                    userContext, lastGroup.autoScalingGroupName)
            List<String> ignoredScheduledActionProperties = ['startTime', 'time', 'autoScalingGroupName',
                    'scheduledActionName', 'scheduledActionARN']
            List<ScheduledUpdateGroupAction> newScheduledActions = lastScheduledActions.collect {
                ScheduledUpdateGroupAction newScheduledAction = BeanState.ofSourceBean(it).
                        ignoreProperties(ignoredScheduledActionProperties).injectState(new ScheduledUpdateGroupAction())
                String id = awsAutoScalingService.nextPolicyId(userContext)
                newScheduledAction.with {
                    autoScalingGroupName = nextGroupName
                    scheduledActionName = Relationships.buildScalingPolicyName(nextGroupName, id)
                }
                newScheduledAction
            }

            Integer lastGracePeriod = lastGroup.healthCheckGracePeriod
            Subnets subnets = awsEc2Service.getSubnets(userContext)
            String subnetPurpose = params.subnetPurpose
            String vpcZoneIdentifier = subnets.constructNewVpcZoneIdentifierForPurposeAndZones(subnetPurpose,
                    selectedZones)
            String iamInstanceProfile = params.iamInstanceProfile ?: null
            if (params.noOptionalDefaults != 'true') {
                securityGroups = securityGroups ?: lastLaunchConfig.securityGroups
                termPolicies = termPolicies ?: lastGroup.terminationPolicies
                loadBalancerNames = loadBalancerNames ?: lastGroup.loadBalancerNames
                vpcZoneIdentifier = vpcZoneIdentifier ?: subnets.constructNewVpcZoneIdentifierForZones(lastGroup.vpcZoneIdentifier,
                        selectedZones)
                iamInstanceProfile = iamInstanceProfile ?: lastLaunchConfig.iamInstanceProfile
            }
            GroupCreateOptions options = new GroupCreateOptions(
                    common: new CommonPushOptions(
                            userContext: userContext,
                            checkHealth: checkHealth,
                            afterBootWait: params.afterBootWait?.toInteger() ?: 30,
                            appName: appName,
                            env: grailsApplication.config.cloud.accountName,
                            imageId: params.imageId ?: lastLaunchConfig.imageId,
                            instanceType: instanceType,
                            groupName: nextGroupName,
                            securityGroups: securityGroups,
                            maxStartupRetries: params.maxStartupRetries?.toInteger() ?: 5
                    ),
                    initialTraffic: initialTraffic,
                    minSize: minSize,
                    desiredCapacity: desiredCapacity,
                    maxSize: maxSize,
                    defaultCooldown: params.defaultCooldown as Integer ?: lastGroup.defaultCooldown,
                    healthCheckType: params.healthCheckType ?: lastGroup.healthCheckType.name(),
                    healthCheckGracePeriod: params.healthCheckGracePeriod as Integer ?: lastGracePeriod,
                    terminationPolicies: termPolicies,
                    batchSize: params.batchSize as Integer ?: GroupResizeOperation.DEFAULT_BATCH_SIZE,
                    loadBalancerNames: loadBalancerNames,
                    iamInstanceProfile: iamInstanceProfile,
                    keyName: params.keyName ?: lastLaunchConfig.keyName,
                    availabilityZones: selectedZones,
                    zoneRebalancingSuspended: azRebalanceSuspended,
                    scalingPolicies: newScalingPolicies,
                    scheduledActions: newScheduledActions,
                    vpcZoneIdentifier: vpcZoneIdentifier,
                    spotPrice: spotPrice
            )
            def operation = pushService.startGroupCreate(options)
            flash.message = "${operation.task.name} has been started."
            redirectToTask(operation.taskId)
        }
    }

    def resize = {
        UserContext userContext = UserContext.of(request)
        Integer newMin = (params.minSize ?: params.minAndMaxSize) as Integer
        Integer newMax = (params.maxSize ?: params.minAndMaxSize) as Integer
        Integer batchSize = params.batchSize as Integer
        GroupResizeOperation operation =
                pushService.startGroupResize(userContext, params.name, newMin, newMax, batchSize)
        redirectToTask(operation.taskId)
    }

    def delete = {
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

    def activate = {
        UserContext userContext = UserContext.of(request)
        GroupActivateOperation operation = pushService.startGroupActivate(userContext, params.name)
        redirectToTask(operation.taskId)
    }

    def deactivate = {
        UserContext userContext = UserContext.of(request)
        GroupDeactivateOperation operation = pushService.startGroupDeactivate(userContext, params.name)
        redirectToTask(operation.taskId)
    }

    def anyInstance = {
        UserContext userContext = UserContext.of(request)
        String name = params.id
        String field = params.field
        Cluster cluster = awsAutoScalingService.getCluster(userContext, name)
        List<GroupedInstance> instances = cluster?.instances
        String instanceId = instances?.size() >= 1 ? instances[0].instanceId : null
        MergedInstance mergedInstance = instanceId ?
                mergedInstanceService.getMergedInstancesByIds(userContext, [instanceId])[0] : null
        String result = mergedInstance?.getFieldValue(field)
        if (!result) {
            response.status = 400
            if (!name) { result = 'name is a required parameter'}
            else if (!field) { result = 'field is a required parameter'}
            else if (!cluster) { result = "No cluster found with name '$name'"}
            else if (!mergedInstance) { result = "No instances found for cluster '$name'"}
            else { result = "'$field' not found. Valid fields: ${mergedInstance.listFieldNames()}" }
        }
        render result
    }

    private void redirectToTask(String taskId) {
        redirect(controller: 'task', action: 'show', params: [id: taskId])
    }
}
