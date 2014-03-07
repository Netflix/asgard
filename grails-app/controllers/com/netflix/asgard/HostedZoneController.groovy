/*
 * Copyright 2013 Netflix, Inc.
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

import com.amazonaws.services.route53.model.AliasTarget
import com.amazonaws.services.route53.model.ChangeInfo
import com.amazonaws.services.route53.model.HostedZone
import com.amazonaws.services.route53.model.RRType
import com.amazonaws.services.route53.model.ResourceRecord
import com.amazonaws.services.route53.model.ResourceRecordSet
import com.amazonaws.services.route53.model.ResourceRecordSetFailover
import com.amazonaws.services.route53.model.ResourceRecordSetRegion
import grails.converters.JSON
import grails.converters.XML

/**
 * Used to interact with Route53 Hosted Zones for DNS management.
 */
class HostedZoneController {

    def awsRoute53Service

    static editActions = ['prepareResourceRecordSet']

    /**
     * Lists all the Route53 DNS hosted zones in the account.
     */
    def list() {
        Collection<HostedZone> hostedZones = awsRoute53Service.getHostedZones()
        withFormat {
            html { [hostedZones: hostedZones] }
            xml { new XML(hostedZones).render(response) }
            json { new JSON(hostedZones).render(response) }
        }
    }

    /**
     * Shows the details of one Route53 DNS hosted zone, including the related resource record sets.
     */
    def show() {
        String hostedZoneIdOrName = params.id
        UserContext userContext = UserContext.of(request)
        HostedZone hostedZone = awsRoute53Service.getHostedZone(userContext, hostedZoneIdOrName)
        if (!hostedZone) {
            Requests.renderNotFound('Hosted Zone', hostedZoneIdOrName, this)
            return
        }

        List<ResourceRecordSet> resourceRecordSets = awsRoute53Service.getResourceRecordSets(userContext, hostedZone.id)
        resourceRecordSets.sort { it.name }
        String deletionWarning = "Really delete Hosted Zone '${hostedZone.id}' with name '${hostedZone.name}' and " +
                "its ${resourceRecordSets.size()} resource record set${resourceRecordSets.size() == 1 ? '' : 's'}?" +
                (resourceRecordSets.size() ? "\n\nThis cannot be undone and could be dangerous." : '')
        Map result = [hostedZone: hostedZone, resourceRecordSets: resourceRecordSets]
        Map guiVars = result + [deletionWarning: deletionWarning]
        withFormat {
            html { guiVars }
            xml { new XML(result).render(response) }
            json { new JSON(result).render(response) }
        }
    }

    /**
     * Displays a form to create a new hosted zone.
     */
    def create() {

    }

    /**
     * Handles submission of a create form, to make a new Route53 DNS hosted zone.
     *
     * @param cmd the command object containing the user parameters for creating the new hosted zone
     */
    def save(HostedZoneSaveCommand cmd) {
        if (cmd.hasErrors()) {
            chain(action: 'create', model: [cmd: cmd], params: params)
            return
        }
        UserContext userContext = UserContext.of(request)
        try {
            HostedZone hostedZone = awsRoute53Service.createHostedZone(userContext, cmd.name, cmd.comment)
            flash.message = "Hosted Zone '${hostedZone.id}' with name '${hostedZone.name}' has been created."
            redirect(action: 'show', id: hostedZone.id)
        } catch (Exception e) {
            flash.message = e.message ?: e.cause?.message
            chain(action: 'create', model: [cmd: cmd], params: params)
        }
    }

    /**
     * Deletes a Route53 DNS hosted zone, including all of its resource record sets.
     */
    def delete() {
        UserContext userContext = UserContext.of(request)
        String id = params.id
        HostedZone hostedZone = awsRoute53Service.getHostedZone(userContext, id)
        if (hostedZone) {
            ChangeInfo changeInfo = awsRoute53Service.deleteHostedZone(userContext, id)
            flash.message = "Deletion of Hosted Zone '${id}' with name '${hostedZone.name}' has started. " +
                    "ChangeInfo: ${changeInfo}"
            redirect([action: 'result'])
        } else {
            Requests.renderNotFound('Hosted Zone', id, this)
        }
    }

    /**
     * Renders a simple page showing the result of a deletion.
     */
    def result() { render view: '/common/result' }

    /**
     * Displays a form to create a new resource record set for the specified Route53 DNS hosted zone.
     */
    def prepareResourceRecordSet() {
        [
                hostedZoneId: params.id ?: params.hostedZoneId,
                types: RRType.values()*.toString().sort(),
                failoverValues: ResourceRecordSetFailover.values()*.toString().sort(),
                resourceRecordSetRegions: ResourceRecordSetRegion.values()*.toString().sort()
        ]
    }

    /**
     * Creates a new resource record set for an existing Route53 DNS hosted zone.
     *
     * @param cmd the command object containing all the parameters for creating the new resource record set
     */
    def addResourceRecordSet(ResourceRecordSetCommand cmd) {

        if (cmd.hasErrors()) {
            chain(action: 'prepareResourceRecordSet', model: [cmd: cmd], params: params)
        } else {
            UserContext userContext = UserContext.of(request)
            String id = cmd.hostedZoneId
            String comment = cmd.comment
            ResourceRecordSet recordSet = resourceRecordSetFromCommandObject(cmd)
            try {
                ChangeInfo changeInfo = awsRoute53Service.createResourceRecordSet(userContext, id, recordSet, comment)
                flash.message = "DNS CREATE change submitted. ChangeInfo: ${changeInfo}"
                redirect(action: 'show', id: id)
            } catch (Exception e) {
                flash.message = "Could not add resource record set: ${e}"
                chain(action: 'prepareResourceRecordSet', model: [cmd: cmd], params: params)
            }
        }
    }

    /**
     * Deletes a resource record set from a Route53 DNS hosted zone.
     *
     * @param cmd the command object enough parameters to identify and delete a distinct resource record set
     */
    def removeResourceRecordSet(ResourceRecordSetCommand cmd) {
        if (cmd.hasErrors()) {
            chain(action: 'show', id: id)
        } else {
            UserContext userContext = UserContext.of(request)
            String id = cmd.hostedZoneId
            String comment = cmd.comment
            ResourceRecordSet recordSet = resourceRecordSetFromCommandObject(cmd)
            try {
                ChangeInfo changeInfo = awsRoute53Service.deleteResourceRecordSet(userContext, id, recordSet, comment)
                flash.message = "DNS DELETE change submitted. ChangeInfo: ${changeInfo}"
            } catch (Exception e) {
                flash.message = "Could not delete resource record set: ${e}"
            }
            redirect(action: 'show', id: id)
        }
    }

    private resourceRecordSetFromCommandObject(ResourceRecordSetCommand cmd) {
        String hostedZoneId = cmd.hostedZoneId
        List<String> resourceRecordStrings = Requests.ensureList(cmd.resourceRecords?.split('\n')).collect { it.trim() }
        String aliasTarget = cmd.aliasTarget
        new ResourceRecordSet(
                name: cmd.resourceRecordSetName,
                type: cmd.type,
                setIdentifier: cmd.setIdentifier ?: null,
                weight: cmd.weight ?: null,
                region: cmd.resourceRecordSetRegion ?: null,
                failover: cmd.failover ?: null,
                tTL: cmd.ttl ?: null,
                resourceRecords: resourceRecordStrings.collect { new ResourceRecord(it) } ?: null,
                aliasTarget: aliasTarget ? new AliasTarget(hostedZoneId, aliasTarget) : null,
                healthCheckId: cmd.healthCheckId ?: null
        )
    }
}

/**
 * User parameters for creating a new hosted zone.
 */
class HostedZoneSaveCommand {
    String name
    String comment
}

/**
 * The parameters for creating or deleting a resource record set.
 */
class ResourceRecordSetCommand {
    String hostedZoneId
    String resourceRecordSetName
    String type // From enum RRType
    String setIdentifier
    Long weight
    String resourceRecordSetRegion // From enum ResourceRecordSetRegion
    String failover // From ResourceRecordSetFailover
    Long ttl
    String resourceRecords
    String aliasTarget
    String healthCheckId
    String comment
}
