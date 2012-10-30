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

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.autoscaling.model.Activity
import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.autoscaling.model.ScalingPolicy
import com.amazonaws.services.autoscaling.model.ScheduledUpdateGroupAction
import com.amazonaws.services.autoscaling.model.SuspendedProcess
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.amazonaws.services.ec2.model.AvailabilityZone
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.google.common.collect.Multiset
import com.google.common.collect.Sets
import com.google.common.collect.TreeMultiset
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.AutoScalingGroupHealthCheckType
import com.netflix.asgard.model.AutoScalingProcessType
import com.netflix.asgard.model.GroupedInstance
import com.netflix.asgard.model.InstancePriceType
import com.netflix.asgard.model.SubnetTarget
import com.netflix.asgard.model.Subnets
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML
import org.joda.time.DateTime
import org.joda.time.Duration

@ContextParam('region')
class AutoScalingController {

    def grailsApplication

    def applicationService
    def awsAutoScalingService
    def awsCloudWatchService
    def awsEc2Service
    def awsLoadBalancerService
    def configService
    def instanceTypeService
    def mergedInstanceService
    def spotInstanceRequestService
    def stackService

    def static allowedMethods = [save: 'POST', update: 'POST', delete: 'POST', postpone: 'POST', pushStart: 'POST']

    def index = { redirect(action: 'list', params: params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        Collection<AutoScalingGroup> groups = awsAutoScalingService.getAutoScalingGroups(userContext)

        // Optimize app name calculation by parsing each ASG only once and storing results in a map.
        Map<String, String> groupNamesToAppNames = [:]
        groups*.autoScalingGroupName.each { String groupName ->
            groupNamesToAppNames.put(groupName, Relationships.appNameFromGroupName(groupName))
        }

        Set<String> appNames = Requests.ensureList(params.id).collect { it.split(',') }.flatten() as Set<String>
        if (appNames) {
            groups = groups.findAll { groupNamesToAppNames[it.autoScalingGroupName] in appNames }
        }
        groups = groups.sort { it.autoScalingGroupName.toLowerCase() }

        // Determine which app names are valid based on ASG names
        List<String> registeredAppNamesList = applicationService.getRegisteredApplications(userContext)*.name
        Set<String> registeredAppNames = new HashSet<String>(registeredAppNamesList)
        Set<String> groupsWithValidAppNames = new HashSet<String>()
        groups*.autoScalingGroupName.each { String asgName ->
            if (groupNamesToAppNames[asgName] in registeredAppNames) {
                groupsWithValidAppNames << asgName
            }
        }

        Map<Object, List<ScalingPolicy>> groupNamesToScalingPolicies = awsAutoScalingService.
                getAllScalingPolicies(userContext).groupBy { it.autoScalingGroupName }

        withFormat {
            html {
                [
                    autoScalingGroups: groups,
                    appNames: appNames,
                    groupsWithValidAppNames: groupsWithValidAppNames,
                    groupNamesToAppNames: groupNamesToAppNames,
                    groupNamesToScalingPolicies: groupNamesToScalingPolicies,
                ]
            }
            xml { new XML(groups).render(response) }
            json { new JSON(groups).render(response) }
        }
    }

    static final Integer DEFAULT_ACTIVITIES = 20

    def show = {
        UserContext userContext = UserContext.of(request)
        String name = params.name ?: params.id
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, name)
        if (!group) {
            Requests.renderNotFound('Auto Scaling Group', name, this)
            return
        } else {
            AutoScalingGroupData groupData = awsAutoScalingService.buildAutoScalingGroupData(userContext, group)
            Multiset<String> zonesWithInstanceCounts = TreeMultiset.create()
            for (GroupedInstance instance in groupData?.instances) {
                zonesWithInstanceCounts.add(instance.availabilityZone)
            }
            String appName = Relationships.appNameFromGroupName(name)
            List<Activity> activities = []
            List<ScalingPolicy> scalingPolicies = []
            List<ScheduledUpdateGroupAction> scheduledActions = []
            try {
                activities = awsAutoScalingService.getAutoScalingGroupActivities(userContext, name, DEFAULT_ACTIVITIES)
                scalingPolicies = awsAutoScalingService.getScalingPoliciesForGroup(userContext, name)
                scheduledActions = awsAutoScalingService.getScheduledActionsForGroup(userContext, name)
            } catch (AmazonServiceException ignored) {
                // The ASG stopped existing. This race condition happens during automated polling in a unit test.
            }
            Integer instanceCount = group.instances.size()
            Boolean runHealthChecks = params.runHealthChecks || instanceCount < 20
            List<String> subnetIds = Relationships.subnetIdsFromVpcZoneIdentifier(group.VPCZoneIdentifier)
            String subnetPurpose = awsEc2Service.getSubnets(userContext).coerceLoneOrNoneFromIds(subnetIds)?.purpose

            final Map<AutoScalingProcessType, String> processTypeToProcessStatusMessage = [:]
            AutoScalingProcessType.with { [Launch, AZRebalance, Terminate, AddToLoadBalancer] }.each {
                final SuspendedProcess suspendedProcess = group.getSuspendedProcess(it)
                processTypeToProcessStatusMessage[it] = suspendedProcess ? "Disabled: '${suspendedProcess.suspensionReason}'" : 'Enabled'
            }
            DateTime dayAfterExpire = groupData?.expirationTimeAsDateTime()?.plusDays(1)
            Duration maxExpirationDuration = AutoScalingGroupData.MAX_EXPIRATION_DURATION
            Boolean showPostponeButton = dayAfterExpire &&
                    new Duration(Time.now(), dayAfterExpire).isShorterThan(maxExpirationDuration)
            String lcName = groupData?.launchConfigurationName
            LaunchConfiguration launchConfig = awsAutoScalingService.getLaunchConfiguration(userContext, lcName)
            Image image = awsEc2Service.getImage(userContext, launchConfig?.imageId, From.CACHE)

            Collection<String> alarmNames = scalingPolicies.collect { it.alarms*.alarmName }.flatten()
            List<MetricAlarm> alarms = awsCloudWatchService.getAlarms(userContext, alarmNames)
            Map<String, MetricAlarm> alarmsByName = alarms.inject([:]) { Map map, alarm ->
                map.put(alarm.alarmName, alarm)
                map
            } as Map

            def details = [
                    instanceCount: instanceCount,
                    showPostponeButton: showPostponeButton,
                    runHealthChecks: runHealthChecks,
                    group: groupData,
                    zonesWithInstanceCounts: zonesWithInstanceCounts,
                    launchConfiguration: launchConfig,
                    image: image,
                    clusterName: Relationships.clusterFromGroupName(name),
                    variables: Relationships.dissectCompoundName(name),
                    launchStatus: processTypeToProcessStatusMessage[AutoScalingProcessType.Launch],
                    azRebalanceStatus: processTypeToProcessStatusMessage[AutoScalingProcessType.AZRebalance],
                    terminateStatus: processTypeToProcessStatusMessage[AutoScalingProcessType.Terminate],
                    addToLoadBalancerStatus: processTypeToProcessStatusMessage[AutoScalingProcessType.AddToLoadBalancer],
                    scalingPolicies: scalingPolicies,
                    scheduledActions: scheduledActions,
                    activities: activities,
                    app: applicationService.getRegisteredApplication(userContext, appName),
                    buildServer: grailsApplication.config.cloud.buildServer,
                    alarmsByName: alarmsByName,
                    subnetPurpose: subnetPurpose ?: null,
                    vpcZoneIdentifier: group.VPCZoneIdentifier
            ]
            withFormat {
                html { return details }
                xml { new XML(details).render(response) }
                json { new JSON(details).render(response) }
            }
        }
    }

    def activities = {
        UserContext userContext = UserContext.of(request)
        String groupName = params.id
        Integer count = params.activityCount as Integer
        List<Activity> activities = awsAutoScalingService.getAutoScalingGroupActivities(userContext, groupName, count)
        Map details = [name: groupName, count: activities.size(), activities: activities]
        withFormat {
            html { return details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    def create = {
        UserContext userContext = UserContext.of(request)
        List<LoadBalancerDescription> loadBalancers = awsLoadBalancerService.getLoadBalancers(userContext).
                sort { it.loadBalancerName.toLowerCase() }

        List<SuspendedProcess> processes = []
        if (params.azRebalance == 'disabled') {
            processes << new SuspendedProcess().withProcessName(AutoScalingProcessType.AZRebalance.name())
        }
        Collection<AvailabilityZone> recommendedZones = awsEc2Service.getRecommendedAvailabilityZones(userContext)
        Collection<String> selectedZones = awsEc2Service.preselectedZoneNames(recommendedZones,
                Requests.ensureList(params.selectedZones))
        AutoScalingGroup group = new AutoScalingGroup(
                minSize: tryParse(params.min),
                desiredCapacity: tryParse(params.desiredCapacity),
                maxSize: tryParse(params.max),
                defaultCooldown: tryParse(params.defaultCooldown),
                healthCheckType: params.healthCheckType,
                healthCheckGracePeriod: tryParse(params.healthCheckGracePeriod),
                availabilityZones: selectedZones,
                suspendedProcesses: processes
        )
        List<SecurityGroup> effectiveGroups = awsEc2Service.getEffectiveSecurityGroups(userContext).sort {
            it.groupName?.toLowerCase()
        }
        Subnets subnets = awsEc2Service.getSubnets(userContext)
        Map<String, String> purposeToVpcId = subnets.mapPurposeToVpcId()
        [
                applications: applicationService.getRegisteredApplications(userContext),
                group: group,
                stacks: stackService.getStacks(userContext),
                allTerminationPolicies: awsAutoScalingService.terminationPolicyTypes,
                terminationPolicy: configService.defaultTerminationPolicy,
                images: awsEc2Service.getAccountImages(userContext).sort { it.imageLocation.toLowerCase() },
                defKey: awsEc2Service.defaultKeyName,
                keys: awsEc2Service.getKeys(userContext).sort { it.keyName.toLowerCase() },
                subnetPurpose: params.subnetPurpose ?: null,
                subnetPurposes: subnets.getPurposesForZones(recommendedZones*.zoneName, SubnetTarget.EC2).sort(),
                zonesGroupedByPurpose: subnets.groupZonesByPurpose(recommendedZones*.zoneName, SubnetTarget.EC2),
                selectedZones: selectedZones,
                purposeToVpcId: purposeToVpcId,
                vpcId: purposeToVpcId[params.subnetPurpose],
                loadBalancersGroupedByVpcId: loadBalancers.groupBy { it.VPCId },
                selectedLoadBalancers: Requests.ensureList(params.selectedLoadBalancers),
                securityGroupsGroupedByVpcId: effectiveGroups.groupBy { it.vpcId },
                selectedSecurityGroups: Requests.ensureList(params.selectedSecurityGroups),
                instanceTypes: instanceTypeService.getInstanceTypes(userContext)
        ]
    }

    private Integer tryParse(String s) {
        s?.isInteger() ? s.toInteger() : null
    }

    def save = { GroupCreateCommand cmd ->

        if (cmd.hasErrors()) {
            chain(action: 'create', model: [cmd:cmd], params: params) // Use chain to pass both the errors and the params
        } else {

            // Auto Scaling Group name
            String groupName = Relationships.buildGroupName(params)
            UserContext userContext = UserContext.of(request)

            // Auto Scaling Group
            def minSize = params.min ?: 0
            def desiredCapacity = params.desiredCapacity ?: 0
            def maxSize = params.max ?: 0
            def defaultCooldown = params.defaultCooldown ?: 10
            String healthCheckType = AutoScalingGroupHealthCheckType.ensureValidType(params.healthCheckType)
            Integer healthCheckGracePeriod = params.healthCheckGracePeriod as Integer
            List<String> terminationPolicies = Requests.ensureList(params.terminationPolicy)
            List<String> availabilityZones = Requests.ensureList(params.selectedZones)
            List<String> loadBalancerNames = Requests.ensureList(params.selectedLoadBalancers)
            AutoScalingGroup groupTemplate = new AutoScalingGroup().withAutoScalingGroupName(groupName).
                    withAvailabilityZones(availabilityZones).withLoadBalancerNames(loadBalancerNames).
                    withMinSize(minSize.toInteger()).withDesiredCapacity(desiredCapacity.toInteger()).
                    withMaxSize(maxSize.toInteger()).withDefaultCooldown(defaultCooldown.toInteger()).
                    withHealthCheckType(healthCheckType).withHealthCheckGracePeriod(healthCheckGracePeriod).
                    withTerminationPolicies(terminationPolicies)

            // If this ASG lauches VPC instances, we must find the proper subnets and add them.
            String subnetPurpose = params.subnetPurpose ?: null
            if (subnetPurpose) {
                List<String> subnetIds = awsEc2Service.getSubnets(userContext).
                        getSubnetIdsForZones(availabilityZones, subnetPurpose, SubnetTarget.EC2)
                groupTemplate.withVPCZoneIdentifier(Relationships.vpcZoneIdentifierFromSubnetIds(subnetIds))
            }

            final Collection<AutoScalingProcessType> suspendedProcesses = Sets.newHashSet()
            if (params.azRebalance == 'disabled') {
                suspendedProcesses << AutoScalingProcessType.AZRebalance
            }

            //Launch Configuration
            String imageId = params.imageId
            String keyName = params.keyName
            List<String> securityGroups = Requests.ensureList(params.selectedSecurityGroups)
            String instanceType = params.instanceType
            String kernelId = params.kernelId ?: null
            String ramdiskId = params.ramdiskId ?: null
            String iamInstanceProfile = params.iamInstanceProfile ?: null
            LaunchConfiguration launchConfigTemplate = new LaunchConfiguration().withImageId(imageId).
                    withKernelId(kernelId).withInstanceType(instanceType).withKeyName(keyName).withRamdiskId(ramdiskId).
                    withSecurityGroups(securityGroups).withIamInstanceProfile(iamInstanceProfile)
            if (params.pricing == InstancePriceType.SPOT.name()) {
                launchConfigTemplate.spotPrice = spotInstanceRequestService.recommendSpotPrice(userContext, instanceType)
            }

            CreateAutoScalingGroupResult result = awsAutoScalingService.createLaunchConfigAndAutoScalingGroup(
                    userContext, groupTemplate, launchConfigTemplate, suspendedProcesses)
            flash.message = result.toString()
            if (result.succeeded()) {
                redirect(action: 'show', params: [id: groupName])
            }
            else {
                chain(action: 'create', model: [cmd: cmd], params: params)
            }
        }
    }

    def edit = {
        UserContext userContext = UserContext.of(request)
        String name = params.name ?: params.id
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, name)
        if (!group) {
            Requests.renderNotFound('Auto Scaling Group', name, this)
            return
        }
        Subnets subnets = awsEc2Service.getSubnets(userContext)
        List<String> subnetIds = Relationships.subnetIdsFromVpcZoneIdentifier(group.VPCZoneIdentifier)
        String subnetPurpose = awsEc2Service.getSubnets(userContext).coerceLoneOrNoneFromIds(subnetIds)?.purpose
        Collection<AvailabilityZone> availabilityZones = awsEc2Service.getAvailabilityZones(userContext)
        return [
                group: group,
                loadBalancers: awsLoadBalancerService.getLoadBalancers(userContext),
                launchConfigurations: awsAutoScalingService.getLaunchConfigurationNamesForAutoScalingGroup(
                     userContext, name).sort { it.toLowerCase() },
                allTerminationPolicies: awsAutoScalingService.terminationPolicyTypes,
                terminationPolicy: group.terminationPolicies[0],
                subnetPurpose: params.subnetPurpose ?: subnetPurpose,
                zonesGroupedByPurpose: subnets.groupZonesByPurpose(availabilityZones*.zoneName, SubnetTarget.EC2),
                selectedZones: Requests.ensureList(params.selectedZones) ?: group?.availabilityZones,
                launchSuspended: group?.isProcessSuspended(AutoScalingProcessType.Launch),
                terminateSuspended: group?.isProcessSuspended(AutoScalingProcessType.Terminate),
                addToLoadBalancerSuspended: group?.isProcessSuspended(AutoScalingProcessType.AddToLoadBalancer),
                vpcZoneIdentifier: group.VPCZoneIdentifier,
        ]
    }

    def update = {
        Map<String, AutoScalingProcessType> processes = AutoScalingProcessType.with { [
                azRebalance: AZRebalance,
                launch: Launch,
                terminate: Terminate,
                addToLoadBalancer: AddToLoadBalancer,
        ] } as Map
        List<String> validProcessValues = ['enabled', 'disabled']
        processes.each { name, processType ->
            String processValue = params[name]
            if (processValue && !(processValue in validProcessValues)) {
                String msg = "${name} must have a value in ${validProcessValues} not '${processValue}'."
                throw new IllegalArgumentException(msg)
            }
        }
        String name = params.name
        UserContext userContext = UserContext.of(request)
        Integer minSize = (params.min ?: 0) as Integer
        Integer desiredCapacity = (params.desiredCapacity ?: 0) as Integer
        Integer maxSize = (params.max ?: 0) as Integer
        def nextAction = 'show'

        if (minSize > desiredCapacity) {
            flash.message = "Error: Minimum size ${minSize} is lower than desired capacity ${desiredCapacity}"
            nextAction = 'edit'
        } else if (desiredCapacity > maxSize) {
            flash.message = "Error: Desired capacity ${desiredCapacity} is lower than max size ${maxSize}"
            nextAction = 'edit'
        } else {
            AutoScalingGroup asg = awsAutoScalingService.getAutoScalingGroup(userContext, name)
            String lcName = params.launchConfiguration
            Integer defaultCooldown = (params.defaultCooldown ?: 10) as Integer
            String healthCheckType = AutoScalingGroupHealthCheckType.ensureValidType(params.healthCheckType)
            Integer healthCheckGracePeriod = params.healthCheckGracePeriod as Integer
            List<String> terminationPolicies = Requests.ensureList(params.terminationPolicy) ?: asg.terminationPolicies
            List<String> availabilityZones = Requests.ensureList(params.selectedZones)
            Collection<AutoScalingProcessType> suspendProcesses = Sets.newHashSet()
            Collection<AutoScalingProcessType> resumeProcesses = Sets.newHashSet()
            processes.each { String paramName, AutoScalingProcessType processType ->
                if (params[paramName] in ['disabled']) {
                    suspendProcesses << processType
                }
                if (params[paramName] in ['enabled']) {
                    resumeProcesses << processType
                }
            }
            final AutoScalingGroupData autoScalingGroupData = AutoScalingGroupData.forUpdate(
                    name, lcName, minSize, desiredCapacity, maxSize, defaultCooldown, healthCheckType,
                    healthCheckGracePeriod, terminationPolicies, availabilityZones
            )
            try {
                awsAutoScalingService.updateAutoScalingGroup(userContext, autoScalingGroupData, suspendProcesses,
                        resumeProcesses)
                flash.message = "AutoScaling Group '${name}' has been updated."
            } catch (Exception e) {
                flash.message = "Could not update AutoScaling Group: ${e}"
                nextAction = 'edit'
            }
        }
        redirect(action: nextAction, params: [id: name])
    }

    def delete = {
        UserContext userContext = UserContext.of(request)
        String name = params.name
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, name)
        Boolean showGroupNext = false
        if (!group) {
            flash.message = "Auto Scaling Group '${name}' not found."
        } else {
            if (group?.instances?.size() <= 0) {
                try {
                    awsAutoScalingService.deleteAutoScalingGroup(userContext, name)
                    flash.message = "AutoScaling Group '${name}' has been deleted."
                } catch (Exception e) {
                    flash.message = "Could not delete Auto Scaling Group: ${e}"
                    showGroupNext = true
                }
            } else {
                flash.message = "You cannot delete an auto scaling group that still has instances. " +
                        "Set the min and max to 0, wait for the instances to disappear, then try deleting again."
                showGroupNext = true
            }
        }
        showGroupNext ? redirect(action: 'show', params: [id: name]) : redirect(action: 'list')
    }

    def postpone = {
        UserContext userContext = UserContext.of(request)
        String name = params.name
        awsAutoScalingService.postponeExpirationTime(userContext, name, Duration.standardDays(1))
        redirect(action: 'show', id: name)
    }

    def generateName = {
        withFormat {
            json {
                if (params.appName) {
                    try {
                        String groupName = Relationships.buildGroupName(params, true)
                        Names names = Relationships.dissectCompoundName(groupName)
                        List<String> envVars = names.labeledEnvironmentVariables(configService.userDataVarPrefix)
                        Map result = [groupName: groupName, envVars: envVars]
                        render(result as JSON)
                    } catch (Exception e) {
                        response.status = 503
                        render e.message
                    }
                } else {
                    response.status = 503
                    render '(App is required)'
                }
            }
        }
    }

    def anyInstance = {
        UserContext userContext = UserContext.of(request)
        String name = params.name ?: params.id
        String field = params.field
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, name)
        List instances = group?.instances
        String instanceId = instances?.size() >= 1 ? instances[0].instanceId : null
        MergedInstance mergedInstance = instanceId ?
                mergedInstanceService.getMergedInstancesByIds(userContext, [instanceId])[0] : null
        String result = mergedInstance?.getFieldValue(field)
        if (!result) {
            response.status = 400
            if (!name) { result = 'name is a required parameter'}
            else if (!field) { result = 'field is a required parameter'}
            else if (!group) { result = "No auto scaling group found with name '$name'"}
            else if (!mergedInstance) { result = "No instances found for auto scaling group '$name'"}
            else { result = "'$field' not found. Valid fields: ${mergedInstance.listFieldNames()}" }
        }
        render result
    }

    /**
     * TODO: This endpoint can be removed when ASG tagging is stable and more trusted
     */
    def removeExpirationTime = {
        UserContext userContext = UserContext.of(request)
        String name = params.id ?: params.name
        awsAutoScalingService.removeExpirationTime(userContext, name)
        flash.message = "Removed expiration time from auto scaling group '${name}'"
        redirect(action: 'show', id: name)
    }

    def imageless = {
        UserContext userContext = UserContext.of(request)
        List<AppRegistration> allApps = applicationService.getRegisteredApplications(userContext)
        Collection<AutoScalingGroup> allGroups = awsAutoScalingService.getAutoScalingGroups(userContext)
        Collection<AutoScalingGroup> groupsWithMissingImages = allGroups.findAll { AutoScalingGroup group ->
            String lcName = group.launchConfigurationName
            LaunchConfiguration lc = awsAutoScalingService.getLaunchConfiguration(userContext, lcName, From.CACHE)
            awsEc2Service.getImage(userContext, lc?.imageId, From.CACHE) == null
        }
        groupsWithMissingImages.sort { it.autoScalingGroupName }

        Map<String, AppRegistration> groupNamesToApps = [:]
        groupsWithMissingImages*.autoScalingGroupName.each { String groupName ->
            groupNamesToApps.put(groupName, allApps.find { it.name == Relationships.appNameFromGroupName(groupName) })
        }

        withFormat {
            html {
                [
                        autoScalingGroups: groupsWithMissingImages,
                        groupNamesToApps: groupNamesToApps
                ]
            }
            xml { new XML(groupsWithMissingImages).render(response) }
            json { new JSON(groupsWithMissingImages).render(response) }
        }
    }
}
