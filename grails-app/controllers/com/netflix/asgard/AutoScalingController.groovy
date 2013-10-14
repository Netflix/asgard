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
    def cloudReadyService
    def configService
    def instanceTypeService
    def mergedInstanceService
    def spotInstanceRequestService
    def stackService

    static allowedMethods = [save: 'POST', update: 'POST', delete: 'POST', postpone: 'POST', pushStart: 'POST']

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
        Set<String> groupsWithValidAppNames = [] as Set
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

            Collection<LoadBalancerDescription> mismatchedLoadBalancers = group.loadBalancerNames.findResults {
                LoadBalancerDescription elb = awsLoadBalancerService.getLoadBalancer(userContext, it, From.CACHE)
                elb?.availabilityZones?.sort() == groupData.availabilityZones ? null : elb
            }
            Map<String, List<String>> mismatchedElbNamesToZoneLists = mismatchedLoadBalancers.collectEntries {
                [it.loadBalancerName, it.availabilityZones.sort()]
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

            final Map<AutoScalingProcessType, String> processTypeToStatusMessage = [:]
            AutoScalingProcessType.with { [Launch, AZRebalance, Terminate, AddToLoadBalancer] }.each {
                final SuspendedProcess process = group.getSuspendedProcess(it)
                processTypeToStatusMessage[it] = process ? "Disabled: '${process.suspensionReason}'" : 'Enabled'
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
            String clusterName = Relationships.clusterFromGroupName(name)
            boolean isChaosMonkeyActive = cloudReadyService.isChaosMonkeyActive(userContext.region)
            def details = [
                    instanceCount: instanceCount,
                    showPostponeButton: showPostponeButton,
                    runHealthChecks: runHealthChecks,
                    group: groupData,
                    zonesWithInstanceCounts: zonesWithInstanceCounts,
                    mismatchedElbNamesToZoneLists: mismatchedElbNamesToZoneLists,
                    launchConfiguration: launchConfig,
                    image: image,
                    clusterName: clusterName,
                    variables: Relationships.dissectCompoundName(name),
                    launchStatus: processTypeToStatusMessage[AutoScalingProcessType.Launch],
                    azRebalanceStatus: processTypeToStatusMessage[AutoScalingProcessType.AZRebalance],
                    terminateStatus: processTypeToStatusMessage[AutoScalingProcessType.Terminate],
                    addToLoadBalancerStatus: processTypeToStatusMessage[AutoScalingProcessType.AddToLoadBalancer],
                    scalingPolicies: scalingPolicies,
                    scheduledActions: scheduledActions,
                    activities: activities,
                    app: applicationService.getRegisteredApplication(userContext, appName),
                    buildServer: configService.buildServerUrl,
                    alarmsByName: alarmsByName,
                    subnetPurpose: subnetPurpose ?: null,
                    vpcZoneIdentifier: group.VPCZoneIdentifier,
                    isChaosMonkeyActive: isChaosMonkeyActive,
                    chaosMonkeyEditLink: cloudReadyService.constructChaosMonkeyEditLink(userContext.region, appName)
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
        String subnetPurpose = params.subnetPurpose ?: null
        String vpcId = purposeToVpcId[subnetPurpose]
        Set<String> appsWithClusterOptLevel = []
        if (cloudReadyService.isChaosMonkeyActive(userContext.region)) {
            try {
                appsWithClusterOptLevel = cloudReadyService.applicationsWithOptLevel('cluster')
            } catch (ServiceUnavailableException sue) {
                flash.message = "${sue.message} Therefore, you should specify your application's " +
                        'Chaos Monkey settings directly in Cloudready after ASG creation.'
            }
        }
        [
                applications: applicationService.getRegisteredApplications(userContext),
                group: group,
                stacks: stackService.getStacks(userContext),
                allTerminationPolicies: awsAutoScalingService.terminationPolicyTypes,
                terminationPolicy: configService.defaultTerminationPolicy,
                images: awsEc2Service.getAccountImages(userContext).sort { it.imageLocation.toLowerCase() },
                defKey: awsEc2Service.defaultKeyName,
                keys: awsEc2Service.getKeys(userContext).sort { it.keyName.toLowerCase() },
                subnetPurpose: subnetPurpose,
                subnetPurposes: subnets.getPurposesForZones(recommendedZones*.zoneName, SubnetTarget.EC2).sort(),
                zonesGroupedByPurpose: subnets.groupZonesByPurpose(recommendedZones*.zoneName, SubnetTarget.EC2),
                selectedZones: selectedZones,
                purposeToVpcId: purposeToVpcId,
                vpcId: vpcId,
                loadBalancersGroupedByVpcId: loadBalancers.groupBy { it.VPCId },
                selectedLoadBalancers: Requests.ensureList(params["selectedLoadBalancersForVpcId${vpcId ?: ''}"]),
                securityGroupsGroupedByVpcId: effectiveGroups.groupBy { it.vpcId },
                selectedSecurityGroups: Requests.ensureList(params.selectedSecurityGroups),
                instanceTypes: instanceTypeService.getInstanceTypes(userContext),
                iamInstanceProfile: configService.defaultIamRole,
                spotUrl: configService.spotUrl,
                isChaosMonkeyActive: cloudReadyService.isChaosMonkeyActive(userContext.region),
                appsWithClusterOptLevel: appsWithClusterOptLevel ?: []
        ]
    }

    private Integer tryParse(String s) {
        s?.isInteger() ? s.toInteger() : null
    }

    def save = { GroupCreateCommand cmd ->
        if (cmd.hasErrors()) {
            chain(action: 'create', model: [cmd:cmd], params: params) // Use chain to pass both the errors and params
        } else {
            UserContext userContext = UserContext.of(request)
            // Auto Scaling Group name
            String groupName = Relationships.buildGroupName(params)
            Subnets subnets = awsEc2Service.getSubnets(userContext)
            String subnetPurpose = params.subnetPurpose ?: null
            String vpcId = subnets.mapPurposeToVpcId()[subnetPurpose] ?: ''

            // Auto Scaling Group
            Integer minSize = (params.min ?: 0) as Integer
            Integer desiredCapacity = (params.desiredCapacity ?: 0) as Integer
            Integer maxSize = (params.max ?: 0) as Integer
            desiredCapacity = Ensure.bounded(minSize, desiredCapacity, maxSize)
            Integer defaultCooldown = (params.defaultCooldown ?: 10) as Integer
            String healthCheckType = AutoScalingGroupHealthCheckType.ensureValidType(params.healthCheckType)
            Integer healthCheckGracePeriod = params.healthCheckGracePeriod as Integer
            List<String> terminationPolicies = Requests.ensureList(params.terminationPolicy)
            List<String> availabilityZones = Requests.ensureList(params.selectedZones)
            List<String> loadBalancerNames = Requests.ensureList(params["selectedLoadBalancersForVpcId${vpcId}"] ?:
                    params["selectedLoadBalancers"])
            AutoScalingGroup groupTemplate = new AutoScalingGroup().withAutoScalingGroupName(groupName).
                    withAvailabilityZones(availabilityZones).withLoadBalancerNames(loadBalancerNames).
                    withMinSize(minSize).withDesiredCapacity(desiredCapacity).
                    withMaxSize(maxSize).withDefaultCooldown(defaultCooldown).
                    withHealthCheckType(healthCheckType).withHealthCheckGracePeriod(healthCheckGracePeriod).
                    withTerminationPolicies(terminationPolicies)

            // If this ASG lauches VPC instances, we must find the proper subnets and add them.
            if (subnetPurpose) {
                List<String> subnetIds = subnets.getSubnetIdsForZones(availabilityZones, subnetPurpose,
                        SubnetTarget.EC2)
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
            String instType = params.instanceType
            String kernelId = params.kernelId ?: null
            String ramdiskId = params.ramdiskId ?: null
            String iamInstanceProfile = params.iamInstanceProfile ?: configService.defaultIamRole
            boolean ebsOptimized = params.ebsOptimized?.toBoolean()
            LaunchConfiguration launchConfigTemplate = new LaunchConfiguration().withImageId(imageId).
                    withKernelId(kernelId).withInstanceType(instType).withKeyName(keyName).withRamdiskId(ramdiskId).
                    withSecurityGroups(securityGroups).withIamInstanceProfile(iamInstanceProfile).
                    withEbsOptimized(ebsOptimized)
            if (params.pricing == InstancePriceType.SPOT.name()) {
                launchConfigTemplate.spotPrice = spotInstanceRequestService.recommendSpotPrice(userContext, instType)
            }
            boolean enableChaosMonkey = params.chaosMonkey == 'enabled'
            CreateAutoScalingGroupResult result = awsAutoScalingService.createLaunchConfigAndAutoScalingGroup(
                    userContext, groupTemplate, launchConfigTemplate, suspendedProcesses, enableChaosMonkey)
            flash.message = result.toString()
            if (result.succeeded()) {
                redirect(action: 'show', params: [id: groupName])
            } else {
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
        boolean launchSuspended = group?.isProcessSuspended(AutoScalingProcessType.Launch)
        boolean terminateSuspended = group?.isProcessSuspended(AutoScalingProcessType.Terminate)
        boolean alarmNotesSuspended = group?.isProcessSuspended(AutoScalingProcessType.AlarmNotifications)
        boolean manualStaticSizingNeeded = awsAutoScalingService.shouldGroupBeManuallySized(userContext, group)
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
                launchSuspended: launchSuspended,
                terminateSuspended: terminateSuspended,
                alarmNotificationsSuspended: alarmNotesSuspended,
                addToLoadBalancerSuspended: group?.isProcessSuspended(AutoScalingProcessType.AddToLoadBalancer),
                manualStaticSizingNeeded: manualStaticSizingNeeded,
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
        desiredCapacity = Ensure.bounded(minSize, desiredCapacity, maxSize)
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
                        List<String> envVars = Relationships.labeledEnvironmentVariables(groupName,
                                configService.userDataVarPrefix)
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
        if (!name || !field) {
            response.status = 400
            if (!name) { render 'name is a required parameter' }
            if (!field) { render 'field is a required parameter' }
            return
        }
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, name)
        List<String> instanceIds = group?.instances*.instanceId
        MergedInstance mergedInstance = mergedInstanceService.findHealthyInstance(userContext, instanceIds)
        String result = mergedInstance?.getFieldValue(field)
        if (!result) {
            response.status = 404
            if (!group) { result = "No auto scaling group found with name '$name'" }
            else if (!mergedInstance) { result = "No instances found for auto scaling group '$name'" }
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
