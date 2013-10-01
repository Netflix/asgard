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

import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.IpPermission
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.amazonaws.services.elasticloadbalancing.model.SourceSecurityGroup
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class SecurityController {

    def applicationService
    def awsAutoScalingService
    def awsEc2Service
    def awsLoadBalancerService
    def configService

    static allowedMethods = [save: 'POST', update: 'POST', delete: 'POST']

    def index = { redirect(action: 'list', params: params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        Set<String> appNames = Requests.ensureList(params.id).collect { it.split(',') }.flatten() as Set<String>
        Collection<SecurityGroup> securityGroups = awsEc2Service.getSecurityGroups(userContext)
        if (appNames) {
            securityGroups = securityGroups.findAll {
                appNames.contains(Relationships.appNameFromSecurityGroupName(it.groupName))
            }
        }
        securityGroups = securityGroups.sort { it.groupName.toLowerCase() }
        Collection<SourceSecurityGroup> sourceSecGroups = awsLoadBalancerService.getSourceSecurityGroups(userContext)

        Map details = [securityGroups: securityGroups, sourceSecurityGroups: sourceSecGroups, appNames: appNames]
        withFormat {
            html { details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    def show = {
        UserContext userContext = UserContext.of(request)
        def name = params.name ?: params.id
        SecurityGroup group = awsEc2Service.getSecurityGroup(userContext, name)
        if (!group) {
            Requests.renderNotFound('Security Group', name, this)
            return
        }
        group.ipPermissions.sort { it.userIdGroupPairs ? it.userIdGroupPairs[0].groupName : it.fromPort }
        group.ipPermissions.each { it.userIdGroupPairs.sort { it.groupName } }

        List<LaunchConfiguration> launchConfigs = awsAutoScalingService.getLaunchConfigurationsForSecurityGroup(
                userContext, group)
        Collection<Instance> instances = awsEc2Service.getInstancesWithSecurityGroup(userContext, group)
        List<LoadBalancerDescription> lbs = awsLoadBalancerService.getLoadBalancersWithSecurityGroup(userContext, group)

        def details = [
                group: group,
                app: applicationService.getRegisteredApplication(userContext, group.groupName),
                accountNames: configService.awsAccountNames,
                editable: awsEc2Service.isSecurityGroupEditable(group.groupName),
                launchConfigs: launchConfigs,
                instances: instances,
                elbs: lbs
        ]
        withFormat {
            html { return details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    def create = {
        UserContext userContext = UserContext.of(request)
        String name = params.id ?: params.name
        String description = ''
        List<AppRegistration> applications = []
        if (name) {
            AppRegistration app = applicationService.getRegisteredApplication(userContext, name)
            description = app?.description
        } else {
            applications = applicationService.getRegisteredApplications(userContext)
        }
        [
            applications: applications,
            vpcIds: awsEc2Service.getVpcs(userContext)*.vpcId,
            selectedVpcIds: params.selectedVpcIds,
            enableVpc: params.enableVpc,
            name: name,
            description: params.description ?: description,
        ]
    }

    def save = { SecurityCreateCommand cmd ->
        if (cmd.hasErrors()) {
            chain(action: 'create', model: [cmd: cmd], params: params) // Use chain to pass both the errors and the params
        } else {
            UserContext userContext = UserContext.of(request)
            String name = Relationships.buildAppDetailName(params.appName, params.detail)
            try {
                SecurityGroup securityGroup = awsEc2Service.getSecurityGroup(userContext, name)
                if (!securityGroup) {
                    securityGroup = awsEc2Service.createSecurityGroup(userContext, name, params.description, params.vpcId)
                    flash.message = "Security Group '${name}' has been created."
                } else {
                    flash.message = "Security Group '${name}' already exists."
                }
                redirect(action: 'show', params: [id: securityGroup.groupId])
            } catch (Exception e) {
                flash.message = "Could not create Security Group: ${e}"
                chain(action: 'create', model: [cmd: cmd], params: params)
            }
        }
    }

    def edit = {
        UserContext userContext = UserContext.of(request)
        String id = params.id
        SecurityGroup group = awsEc2Service.getSecurityGroup(userContext, id)
        if (!group) {
            Requests.renderNotFound('Security Group', id, this)
            return
        }
        [
                group: group,
                groups: awsEc2Service.getSecurityGroupOptionsForTarget(userContext, group),
                editable: awsEc2Service.isSecurityGroupEditable(group.groupName)
        ]
    }

    def update = {
        String name = params.name ?: params.id
        List<String> selectedGroups = Requests.ensureList(params.selectedGroups)
        UserContext userContext = UserContext.of(request)
        SecurityGroup securityGroup = awsEc2Service.getSecurityGroup(userContext, name)
        if (securityGroup) {
            if (awsEc2Service.isSecurityGroupEditable(securityGroup.groupName)) {
                try {
                    updateSecurityIngress(userContext, securityGroup, selectedGroups, params)
                    flash.message = "Security Group '${securityGroup.groupName}' has been updated."
                    redirect(action: 'show', params: [id: securityGroup.groupId])
                } catch (Exception e) {
                    flash.message = "Could not update Security Group: ${e}"
                    redirect(action: 'edit', params: [id: securityGroup.groupId])
                }
            } else {
                flash.message = "Security group '${securityGroup.groupName}' should not be modified with this tool."
                redirect(action: 'list')
            }
        } else {
            flash.message = "Security Group '${name}' does not exist."
            redirect(action: 'result')
        }
    }

    private void updateSecurityIngress(UserContext userContext, SecurityGroup targetGroup, List<String> selectedGroups, Map portMap) {
        awsEc2Service.getSecurityGroups(userContext).each { srcGroup ->
            boolean wantAccess = selectedGroups.any { it == srcGroup.groupName } && portMap[srcGroup.groupName] != ''
            String wantPorts = wantAccess ? portMap[srcGroup.groupName] : null
            List<IpPermission> wantPerms = awsEc2Service.permissionsFromString(wantPorts)
            awsEc2Service.updateSecurityGroupPermissions(userContext, targetGroup, srcGroup, wantPerms)
        }
    }

    def delete = {
        UserContext userContext = UserContext.of(request)
        String name = params.name ?: params.id
        String msg
        try {
            SecurityGroup securityGroup = awsEc2Service.getSecurityGroup(userContext, name)
            if (securityGroup) {
                awsEc2Service.removeSecurityGroup(userContext, name, securityGroup.groupId)
                msg = "Security Group '${securityGroup.groupName}' has been deleted."
            } else {
                msg = "Security Group '${name}' does not exist."
            }
            flash.message = msg
            redirect(action: 'result')
        } catch (Exception e) {
            flash.message = "Could not delete Security Group: ${e}"
            redirect(action: 'show', params: [id: name])
        }
    }

    def result = { render view: '/common/result' }
}

class SecurityCreateCommand {

    def applicationService

    String appName
    String detail

    static constraints = {

        appName(nullable: false, blank: false, validator: { value, command ->
            UserContext userContext = UserContext.of(Requests.request)
            if (!Relationships.checkName(value)) {
                return 'application.name.illegalChar'
            }
            if (Relationships.usesReservedFormat(value)) {
                return 'name.usesReservedFormat'
            }
            if (!command.applicationService.getRegisteredApplication(userContext, value)) {
                return 'application.name.nonexistent'
            }
            if ("${value}-${command.detail}".length() > Relationships.GROUP_NAME_MAX_LENGTH) {
                return "The complete name cannot exceed ${Relationships.GROUP_NAME_MAX_LENGTH} characters"
            }
        })

        detail(nullable: true, validator: { value, command ->
            if (value && !Relationships.checkDetail(value)) {
                return 'The detail must be empty or consist of alphanumeric characters and hyphens'
            }
            if (Relationships.usesReservedFormat(value)) {
                return 'name.usesReservedFormat'
            }
        })

    }
}
