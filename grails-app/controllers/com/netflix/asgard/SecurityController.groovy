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

import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.elasticloadbalancing.model.SourceSecurityGroup
import grails.converters.JSON
import grails.converters.XML

class SecurityController {

    def applicationService
    def awsEc2Service
    def awsLoadBalancerService

    def static allowedMethods = [save:'POST', update:'POST', delete:'POST']

    def index = { redirect(action: 'list', params:params) }

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
        group?.ipPermissions?.sort { it.userIdGroupPairs ? it.userIdGroupPairs[0].groupName : it.fromPort }
        group?.ipPermissions?.each { it.userIdGroupPairs.sort { it.groupName } }
        if (!group) {
            Requests.renderNotFound('Security Group', name, this)
            return
        } else {
            def details = [
                    'group' : group,
                    'app' : applicationService.getRegisteredApplication(userContext, group.groupName),
                    'accountNames' : grailsApplication.config.grails.awsAccountNames,
                    'editable' : awsEc2Service.isSecurityGroupEditable(group.groupName) ]
            // TODO referenced-from lists would be nice too
            withFormat {
                html { return details }
                xml { new XML(details).render(response) }
                json { new JSON(details).render(response) }
            }
        }
    }

    def create = {
        UserContext userContext = UserContext.of(request)
        [
            'applications' : applicationService.getRegisteredApplications(userContext)
        ]
    }

    def save = { SecurityCreateCommand cmd ->

        if (cmd.hasErrors()) {
            chain(action: 'create', model: [cmd:cmd], params: params) // Use chain to pass both the errors and the params
        } else {
            UserContext userContext = UserContext.of(request)
            String name = Relationships.buildAppDetailName(params.appName, params.detail)
            try {
                if (!awsEc2Service.getSecurityGroup(userContext, name)) {
                    awsEc2Service.createSecurityGroup(userContext, name, params.description)
                    flash.message = "Security Group '${name}' has been created."
                } else {
                    flash.message = "Security Group '${name}' already exists."
                }
                redirect(action: 'show', params: [id: name])
            } catch (Exception e) {
                flash.message = "Could not create Security Group: ${e}"
                chain(action: 'create', model: [cmd:cmd], params: params)
            }
        }
    }

    def edit = {
        UserContext userContext = UserContext.of(request)
        String name = params.id
        SecurityGroup group = awsEc2Service.getSecurityGroup(userContext, name)
        if (!group) {
            Requests.renderNotFound('Security Group', name, this)
            return
        } else {
            return [
                'group' : group,
                'groups' : getSecurityAccessibility(userContext, group),
                'editable' : awsEc2Service.isSecurityGroupEditable(group.groupName)
            ]
        }
    }

    private Map<String, List> getSecurityAccessibility(UserContext userContext, SecurityGroup targetGroup) {
        Collection<SecurityGroup> allGroups = awsEc2Service.getEffectiveSecurityGroups(userContext)
        String defaultPorts = awsEc2Service.bestIngressPortsFor(targetGroup)
        Map<String, List> groupNamesToAccessibilityData = [:]
        allGroups.each { SecurityGroup srcGroup ->
            String ports = awsEc2Service.getIngressFrom(targetGroup, srcGroup.groupName)
            groupNamesToAccessibilityData[srcGroup.groupName] = ports ? [ true, ports ] : [ false, defaultPorts ]
        }
        groupNamesToAccessibilityData
    }

    def update = {
        String name = params.name
        UserContext userContext = UserContext.of(request)
        if (awsEc2Service.isSecurityGroupEditable(name)) {
            List<String> selectedGroups = Requests.ensureList(params.selectedGroups)
            SecurityGroup group = awsEc2Service.getSecurityGroup(userContext, name)
            try {
                updateSecurityIngress(userContext, group, selectedGroups, params)
                flash.message = "Security Group '${name}' has been updated."
            } catch (Exception e) {
                flash.message = "Could not update Security Group: ${e}"
            }
            redirect(action: 'show', params:[id: name])
        } else {
            flash.message = "Security group ${name} should not be modified with this tool."
            redirect(action: 'list')
        }
    }

    private void updateSecurityIngress(UserContext userContext, SecurityGroup targetGroup, List<String> selectedGroups, Map portMap) {
        awsEc2Service.getSecurityGroups(userContext).each {srcGroup ->
            boolean wantAccess = selectedGroups.any {it == srcGroup.groupName} && portMap[srcGroup.groupName] != ''
            String wantPorts = wantAccess ? portMap[srcGroup.groupName] : null
            awsEc2Service.updateSecurityGroupPermissions(userContext, targetGroup, srcGroup.groupName, wantPorts)
        }
    }

    def delete = {
        UserContext userContext = UserContext.of(request)
        String msg
        try {
            String name = params.name
            if (awsEc2Service.getSecurityGroup(userContext, name)) {
                awsEc2Service.removeSecurityGroup(userContext, name)
                msg = "Security Group '${name}' has been deleted."
            } else {
                msg = "Security Group '${name}' does not exist."
            }
        } catch (Exception e) {
            msg = "Could not delete Security Group: ${e}"
        }
        flash.message = msg
        redirect(action: 'result')
    }

    def result = { render view: '/common/result' }
}

class SecurityCreateCommand {

    def applicationService

    String appName
    String detail

    static constraints = {

        appName(nullable: false, blank: false, validator: { value, command->
            UserContext userContext = UserContext.of(Requests.request)
            if (!Relationships.checkName(value)) {
                return "application.name.illegalChar"
            }
            if (Relationships.usesReservedFormat(value)) {
                return "name.usesReservedFormat"
            }
            if (!command.applicationService.getRegisteredApplication(userContext, value)) {
                return "application.name.nonexistent"
            }
            if ("${value}-${command.detail}".length() > Relationships.GROUP_NAME_MAX_LENGTH) {
                return "The complete name cannot exceed ${Relationships.GROUP_NAME_MAX_LENGTH} characters"
            }
        })

        detail(nullable: true, validator: { value, command->
            if (value && !Relationships.checkDetail(value)) {
                return "The detail must be empty or consist of alphanumeric characters and hyphens"
            }
            if (Relationships.usesReservedFormat(value)) {
                return "name.usesReservedFormat"
            }
        })

    }
}
