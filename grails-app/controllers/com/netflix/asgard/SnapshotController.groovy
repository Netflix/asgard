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

import com.amazonaws.services.ec2.model.Snapshot
import com.amazonaws.services.ec2.model.Volume
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class SnapshotController {

    def awsEc2Service

    def index() {
        redirect(action: 'list', params: params)
    }

    def list() {
        UserContext userContext = UserContext.of(request)
        List<Snapshot> snapshots = awsEc2Service.getSnapshots(userContext).sort { it.snapshotId.toLowerCase() }
        Set<String> appNames = Requests.ensureList(params.id).collect { it.split(',') }.flatten() as Set<String>
        if (appNames) {
            snapshots = snapshots.findAll { Snapshot snapshot ->
                appNames.any { snapshot.description.contains(it) }
            }
        }
        withFormat {
            html { [ 'snapshots' : snapshots] }
            xml { new XML(snapshots).render(response) }
            json { new JSON(snapshots).render(response) }
        }
    }

    def show() {
        UserContext userContext = UserContext.of(request)
        String snapshotId = EntityType.snapshot.ensurePrefix(params.snapshotId ?: params.id)
        def snapshot = awsEc2Service.getSnapshot(userContext, snapshotId)
        if (!snapshot) {
            Requests.renderNotFound('EBS Snapshot', snapshotId, this)
        } else {
            String ownerId = snapshot.ownerId
            String accountName = grailsApplication.config.grails.awsAccountNames[ownerId] ?: "${ownerId}"
            def details = [
                    'snapshot': snapshot,
                    'zoneList': awsEc2Service.getAvailabilityZones(userContext),
                    'accountName': accountName
            ]
            withFormat {
                html { return details }
                xml { new XML(details).render(response) }
                json { new JSON(details).render(response) }
            }
        }
    }

    def create() {
        UserContext userContext = UserContext.of(request)
        def snapshot = awsEc2Service.createSnapshot(userContext, params.volumeId, params.description)
        redirect(action: 'show', params: [id: snapshot?.snapshotId])
    }

    def delete() {
        UserContext userContext = UserContext.of(request)

        List<String> snapshotIds = Requests.ensureList(params.snapshotId ?: params.selectedSnapshots)
        List<String> deletedSnapshotIds = []
        List<String> nonexistentSnapshotIds = []

        String message = ''
        try {
            snapshotIds.each {
                if (awsEc2Service.getSnapshot(userContext, it)) {
                    awsEc2Service.deleteSnapshot(userContext, it)
                    deletedSnapshotIds << it
                } else {
                    nonexistentSnapshotIds << it
                }
            }
            if (deletedSnapshotIds) {
                message += "Snapshot${deletedSnapshotIds.size() == 1 ? '' : 's'} deleted: ${deletedSnapshotIds}. "
            }
            if (nonexistentSnapshotIds) {
                int count = nonexistentSnapshotIds.size()
                message += "Snapshot${count == 1 ? '' : 's'} not found: ${nonexistentSnapshotIds}. "
            }
        } catch (Exception e) {
            message = "Error deleting snapshot${snapshotIds.size() == 1 ? '' : 's'} ${snapshotIds}: ${e}"
        }
        flash.message = message
        redirect(action: 'result')
    }

    def result() {
        render view: '/common/result'
    }

    def restore(SnapshotCommand cmd) {
        UserContext userContext = UserContext.of(request)
        if (cmd.hasErrors()) {
            chain(action: 'show', model: [snapshotCommand: cmd])
        } else {
            String snapshotId = EntityType.snapshot.ensurePrefix(params.snapshotId ?: params.id)
            try {
                Volume volume = awsEc2Service.createVolumeFromSnapshot(
                    userContext,
                    params.volumeSize as Integer,
                    params.zone,
                    snapshotId
                )
                redirect(controller: "volume", action: 'show', params: [id: volume.volumeId])
            } catch (Exception e) {
                flash.message = "Could not restore from EBS Snapshot: ${e}"
                redirect(action: 'show', params:[id:snapshotId])
            }
        }
    }
}

class SnapshotCommand {
    String zone
    static constraints = {
        zone(blank: false, nullable: false)
    }
}
