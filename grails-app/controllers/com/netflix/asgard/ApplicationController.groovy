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
import com.amazonaws.services.ec2.model.IpPermission
import com.amazonaws.services.ec2.model.SecurityGroup
import com.netflix.asgard.collections.GroupedAppRegistrationSet
import com.netflix.asgard.model.ApplicationInstance
import com.netflix.asgard.model.MonitorBucketType
import com.netflix.asgard.model.Owner
import grails.converters.JSON
import grails.converters.XML
import org.apache.commons.collections.Bag
import org.apache.commons.collections.HashBag

class ApplicationController {

    def applicationService
    def awsEc2Service
    def awsAutoScalingService
    def awsLoadBalancerService
    def configService
    def discoveryService

    static allowedMethods = [save: 'POST', update: 'POST', delete: 'POST', securityUpdate: 'POST']

    static editActions = ['security']

    def index() {
        redirect(action: 'list', params: params)
    }

    def list() {
        UserContext userContext = UserContext.of(request)
        GroupedAppRegistrationSet apps = applicationService.getGroupedRegisteredApplications(userContext)

        Bag groupCountsPerAppName = groupCountsPerAppName(userContext)
        Bag instanceCountsPerAppName = instanceCountsPerAppName(userContext)

        List<String> terms = Requests.ensureList(params.id).collect { it.split(',') }.flatten()
        Set<String> lowercaseTerms = terms*.toLowerCase() as Set
        if (terms) {
            apps = apps.findAll { AppRegistration app ->
                    app.name.toLowerCase() in lowercaseTerms ||
                    app.owner?.toLowerCase() in lowercaseTerms ||
                    app.email?.toLowerCase() in lowercaseTerms ||
                    app.group?.toLowerCase() in lowercaseTerms ||
                        (app.tags && app.tags*.toLowerCase().find { it == lowercaseTerms })
            } as GroupedAppRegistrationSet
        }
        withFormat {
            html {
                [
                        applications: apps,
                        terms: terms,
                        groupCountsPerAppName: groupCountsPerAppName,
                        instanceCountsPerAppName: instanceCountsPerAppName,
                ]
            }
            xml { new XML(apps).render(response) }
            json { new JSON(apps).render(response) }
        }
    }

    def owner() {
        UserContext userContext = UserContext.of(request)
        List<AppRegistration> apps = applicationService.getRegisteredApplications(userContext)
        Bag groupCountsPerAppName = groupCountsPerAppName(userContext)
        Bag instanceCountsPerAppName = instanceCountsPerAppName(userContext)
        Map<String, Owner> ownerNamesToOwners = [:]

        for (AppRegistration app in apps) {
            String ownerName = app.owner
            if (ownerNamesToOwners[ownerName] == null) {
                ownerNamesToOwners[ownerName] = new Owner(name: ownerName)
            }
            Owner owner = ownerNamesToOwners[ownerName]
            owner.emails << app.email
            owner.appNames << app.name
            owner.autoScalingGroupCount += groupCountsPerAppName.getCount(app.name)
            owner.instanceCount += instanceCountsPerAppName.getCount(app.name)
        }

        // Sort by number of instances descending, then by number of auto scaling groups descending
        List<Owner> owners = (ownerNamesToOwners.values() as List).
                sort { -1 * it.autoScalingGroupCount }.sort { -1 * it.instanceCount }

        withFormat {
            html { [owners: owners] }
            xml { new XML(owners).render(response) }
            json { new JSON(owners).render(response) }
        }
    }

    private Bag groupCountsPerAppName(UserContext userContext) {
        Collection<AutoScalingGroup> groups = awsAutoScalingService.getAutoScalingGroups(userContext)
        List<String> groupNames = groups*.autoScalingGroupName
        Bag groupCountsPerAppName = new HashBag()
        groupNames.each { groupCountsPerAppName.add(Relationships.appNameFromGroupName(it)) }
        groupCountsPerAppName
    }

    private Bag instanceCountsPerAppName(UserContext userContext) {
        Collection<ApplicationInstance> discInstances = discoveryService.getAppInstances(userContext)
        List<String> appInstanceNames = discInstances*.appName
        Bag instanceCountsPerAppName = new HashBag()
        appInstanceNames.each { instanceCountsPerAppName.add(it) }
        instanceCountsPerAppName
    }

    def show() {
        String name = params.name ?: params.id
        UserContext userContext = UserContext.of(request)
        AppRegistration app = applicationService.getRegisteredApplication(userContext, name)
        if (!app) {
            Requests.renderNotFound('Application', name, this)
        } else {
            if (log.debugEnabled) {
                log.debug "In show, name=${app.name} type=${app.type} description=${app.description}"
            }
            List<AutoScalingGroup> groups =
                    awsAutoScalingService.getAutoScalingGroupsForApp(userContext, name).sort { it.autoScalingGroupName }
            List<String> clusterNames =
                    groups.collect { Relationships.clusterFromGroupName(it.autoScalingGroupName) }.unique()
            request.alertingServiceConfigUrl = configService.alertingServiceConfigUrl
            SecurityGroup appSecurityGroup = awsEc2Service.getSecurityGroup(userContext, name)
            def details = [
                    app: app,
                    strictName: Relationships.checkStrictName(app.name),
                    clusters: clusterNames,
                    groups: groups,
                    balancers: awsLoadBalancerService.getLoadBalancersForApp(userContext, name),
                    securities: awsEc2Service.getSecurityGroupsForApp(userContext, name),
                    appSecurityGroup: appSecurityGroup,
                    launches: awsAutoScalingService.getLaunchConfigurationsForApp(userContext, name),
                    chaosMonkeyEditLink: configService.getMonkeyCommanderEditLink(app.name)
            ]
            withFormat {
                html { return details }
                xml { new XML(details).render(response) }
                json { new JSON(details).render(response) }
            }
        }
    }

    static String[] typeList = ['Standalone Application', 'Web Application', 'Web Service']

    def create = {
        [
                typeList: typeList
        ]
    }

    def save(ApplicationCreateCommand cmd) {
        if (cmd.hasErrors()) {
            chain(action: 'create', model: [cmd: cmd], params: params)
        } else {
            String name = params.name
            UserContext userContext = UserContext.of(request)
            String group = params.group
            String type = params.type
            String desc = params.description
            String owner = params.owner
            String email = params.email
            String monitorBucketTypeString = params.monitorBucketType
            String tags = normalizeTagDelimiter(params.tags)
            MonitorBucketType bucketType = Enum.valueOf(MonitorBucketType, monitorBucketTypeString)
            def result = applicationService.createRegisteredApplication(
                userContext, name, group, type, desc, owner, email, bucketType, tags
            )
            flash.message = result.toString()
            if (result.succeeded()) {
                redirect(action: 'show', params: [id: name])
            } else {
                chain(action: 'create', model: [cmd: cmd], params: params)
            }
        }
    }

    def edit() {
        UserContext userContext = UserContext.of(request)
        String name = params.name ?: params.id
        log.debug "Edit App: ${name}"
        def app = applicationService.getRegisteredApplication(userContext, name)
        ['app': app, 'typeList': typeList]
    }

    def update() {
        String name = params.name
        UserContext userContext = UserContext.of(request)
        String group = params.group
        String type = params.type
        String desc = params.description
        String owner = params.owner
        String email = params.email
        String tags = normalizeTagDelimiter(params.tags)
        String monitorBucketTypeString = params.monitorBucketType
        try {
            MonitorBucketType bucketType = Enum.valueOf(MonitorBucketType, monitorBucketTypeString)
            def result = applicationService.updateRegisteredApplication(
                userContext, name, group, type, desc, owner, email, bucketType, tags
            )
            flash.message = result.toString()
        } catch (Exception e) {
            flash.message = "Could not update Application: ${e}"
        }
        redirect(action: 'show', params: [id: name])
    }

    def delete() {
        String name = params.name
        UserContext userContext = UserContext.of(request)
        log.info "Delete App: ${name}"
        try {
            applicationService.deleteRegisteredApplication(userContext, name)
            flash.message = "Application '${name}' has been deleted."
        } catch (ValidationException ve) {
            flash.message = "Could not delete Application: ${ve.message}"
            redirect(action: 'show', params: [id: name])
            return
        } catch (Exception e) {
            flash.message = "Could not delete Application: ${e}"
        }
        redirect(action: 'list')
    }

    def security() {
        String name = params.name
        String securityGroupId = params.securityGroupId
        UserContext userContext = UserContext.of(request)
        AppRegistration app = applicationService.getRegisteredApplication(userContext, name)
        if (!app) {
            flash.message = "Application '${name}' not found."
            redirect(action: 'list')
            return
        }
        SecurityGroup group = awsEc2Service.getSecurityGroup(userContext, securityGroupId)
        if (!group) {
            flash.message = "Could not retrieve or create Security Group '${name}'"
            redirect(action: 'list')
            return
        }
        [
                app: app,
                name: name,
                group: group,
                groups: awsEc2Service.getSecurityGroupOptionsForSource(userContext, group)
        ]
    }

    def securityUpdate() {
        String name = params.name
        UserContext userContext = UserContext.of(request)
        List<String> selectedGroups = Requests.ensureList(params.selectedGroups)
        SecurityGroup group = awsEc2Service.getSecurityGroup(userContext, name)
        updateSecurityEgress(userContext, group, selectedGroups, params)
        flash.message = "Successfully updated access for Application '${name}'"
        redirect(action: 'show', params: [id: name])
    }

    // Security Group permission updating logic

    private void updateSecurityEgress(UserContext userContext, SecurityGroup srcGroup, List<String> selectedGroups,
                                      Map portMap) {
        awsEc2Service.getSecurityGroups(userContext).each { SecurityGroup targetGroup ->
            boolean wantAccess = selectedGroups.any { it == targetGroup.groupName } &&
                    portMap[targetGroup.groupName] != ''
            String wantPorts = wantAccess ? portMap[targetGroup.groupName] : null
            List<IpPermission> wantPerms = awsEc2Service.permissionsFromString(wantPorts)
            awsEc2Service.updateSecurityGroupPermissions(userContext, targetGroup, srcGroup, wantPerms)
        }
    }

    private static String normalizeTagDelimiter(String tags) {
        // Fail early & means we can support null parameters
        if (!tags) {
            return
        }

        if (tags.contains(",")) {
            tags.split(',')*.trim().join(',')
        } else {
            tags.split()*.trim().join(',')
        }
    }
}

class ApplicationCreateCommand {

    String name
    String email
    String type
    String description
    String owner
    String tags
    String monitorBucketType
    boolean requestedFromGui

    static constraints = {
        name(nullable: false, blank: false, size: 1..Relationships.APPLICATION_MAX_LENGTH,
                validator: { value, command ->
            if (!Relationships.checkName(value)) {
                return "application.name.illegalChar"
            }
            if (Relationships.usesReservedFormat(value)) {
                return "name.usesReservedFormat"
            }
        })
        email(nullable: false, blank: false, email: true)
        type(nullable: false, blank: false)
        description(nullable: false, blank: false)
        monitorBucketType(nullable: false, blank: false)
        owner(nullable: false, blank: false)
        tags(nullable: true, blank: true)
    }
}
