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

import com.amazonaws.services.rds.model.DBSecurityGroup
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class DbSecurityController {

    def awsRdsService
    def awsEc2Service

    static allowedMethods = [save: 'POST', update: 'POST', delete: 'POST']

    def index = { redirect(action: 'list', params: params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        def dbSecurityGroups = awsRdsService.getDBSecurityGroups(userContext).sort{
                it.getDBSecurityGroupName().toLowerCase() }
        withFormat {
            html {
                [
                        dbSecurityGroups: dbSecurityGroups,
                        accountNames: grailsApplication.config.grails.awsAccountNames
                ]
            }
            xml { new XML(dbSecurityGroups).render(response) }
            json { new JSON(dbSecurityGroups).render(response) }
        }
    }

    def show = {
        UserContext userContext = UserContext.of(request)
        def name = params.name ?: params.id
        def group = awsRdsService.getDBSecurityGroup(userContext, name)
        if (!group) {
            Requests.renderNotFound('DB Security Group', name, this)
        } else {
            Map details = [
                    group: group,
                    accountNames: grailsApplication.config.grails.awsAccountNames
            ]
            // TODO referenced-from lists would be nice too
            withFormat {
                html { return details }
                xml { new XML(details).render(response) }
                json { new JSON(details).render(response) }
            }
        }
    }

    def create = {
        [ ]
    }

    def save = {
        UserContext userContext = UserContext.of(request)
        try {
            awsRdsService.createDBSecurityGroup(userContext, params.name, params.description)
            flash.message = "DB Security Group '${params.name}' has been created."
        } catch (Exception e) {
            flash.message = "Could not create DB Security Group: ${e}"
        }
        redirect(action: 'list')
    }

    def edit = {
        UserContext userContext = UserContext.of(request)
        def name = params.name
        DBSecurityGroup group = awsRdsService.getDBSecurityGroup(userContext, name)
        if (!group) {
            Requests.renderNotFound('DB Security Group', name, this)
        } else {
            return [
                'group' : group,
                'allEC2Groups' : awsEc2Service.getEffectiveSecurityGroups(userContext).collect { it.groupName },
                'selectedEC2Groups' : group.getEC2SecurityGroups().collect { it.getEC2SecurityGroupName() }
            ]
        }
    }

    def update = {
        String name = params.name
        String newName = params.newName
        String description = params.description
        UserContext userContext = UserContext.of(request)

        List<String> selectedGroups = Requests.ensureList(params.selectedGroups)

        List<String> ipRanges = []
        if (params.ipRanges) {
            params.ipRanges.splitEachLine(/[\s,]/, { ipRanges.addAll(it) })
        }
        DBSecurityGroup group = awsRdsService.getDBSecurityGroup(userContext, name)
        try {
            if (description != group.getDBSecurityGroupDescription() || newName != name) {
                group = awsRdsService.renameDBSecurityGroup(userContext, group, newName, description)
                updateDBSecurityIngress(userContext, group, selectedGroups, ipRanges)
                flash.message = "DB Security Group '${name}' has been renamed '${newName}' and updated."
                name = newName
            } else {
                updateDBSecurityIngress(userContext, group, selectedGroups, ipRanges)
                flash.message = "DB Security Group '${name}' has been updated."
            }
        } catch (Exception e) {
            flash.message = "Could not update DB Security Group: ${e}"
        }
        redirect(action: 'show', params: [id: name])
    }

    private void updateDBSecurityIngress(UserContext userContext, DBSecurityGroup targetGroup,
                                         List<String> selectedGroups, List<String> ipRanges) {
        List<String> originalGroups = targetGroup.getEC2SecurityGroups().collect { it.getEC2SecurityGroupName() }
        List<String> originalIPRanges = targetGroup.getIPRanges().collect { it.getCIDRIP() }
        List<String> newGroups = selectedGroups - originalGroups
        List<String> deletedGroups = originalGroups - selectedGroups
        List<String> newIPRanges = ipRanges - originalIPRanges
        List<String> deletedIPRanges = originalIPRanges - ipRanges

        newGroups.each { awsRdsService.authorizeDBSecurityGroupIngressForGroup(userContext,
                targetGroup.getDBSecurityGroupName(), it) }
        deletedGroups.each { awsRdsService.revokeDBSecurityGroupIngressForGroup(userContext,
                targetGroup.getDBSecurityGroupName(), it) }
        newIPRanges.each { awsRdsService.authorizeDBSecurityGroupIngressForIP(userContext,
                targetGroup.getDBSecurityGroupName(), it) }
        deletedIPRanges.each { awsRdsService.revokeDBSecurityGroupIngressForIP(userContext,
                targetGroup.getDBSecurityGroupName(), it) }
    }

    def delete = {
        UserContext userContext = UserContext.of(request)
        try {
            awsRdsService.removeDBSecurityGroup(userContext, params.name)
            flash.message = "DB Security Group '${params.name}' has been deleted."
        } catch (Exception e) {
            flash.message = "Could not delete DB Security Group: ${e}"
        }
        redirect(action: 'list')
    }

}
