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

import com.amazonaws.services.rds.model.DBInstance
import com.amazonaws.services.rds.model.DBSnapshot
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class DbSnapshotController {

    def awsRdsService

    def index = { redirect(action: 'list', params:params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        List<DBSnapshot> dbSnapshots = awsRdsService.getDBSnapshots(userContext).sort { it.getDBSnapshotIdentifier() }
        withFormat {
            html { [ 'snapshots' : dbSnapshots] }
            xml { new XML(dbSnapshots).render(response) }
            json { new JSON(dbSnapshots).render(response) }
        }
    }

    def create = { CreateDBSnapshotCommand cmd ->
        UserContext userContext = UserContext.of(request)
        if (cmd.hasErrors()) {
            flash.message = "DB Snapshot name may not be blank."
            redirect(controller:"rdsInstance", action:'show', params:[name:params.dBInstanceIdentifier])
        } else {
            awsRdsService.createDBSnapshot(userContext, params.dBInstanceIdentifier, params.snapshotName)
            redirect(action: 'show', params: [name: params.snapshotName])
        }
    }

    def quickRestore = { RestoreDBCommand cmd ->
        UserContext userContext = UserContext.of(request)
        if (cmd.hasErrors()) {
            flash.message = "DB Instance name may not be blank."
            redirect(action: 'list')
        } else {
            try {
                DBInstance instance = awsRdsService.restoreFromSnapshot(userContext, params.name,
                        params.dBInstanceIdentifier)
                String identifier = instance.getDBInstanceIdentifier()
                redirect(controller: "rdsInstance", action: 'show', params: [dBInstanceIdentifier: identifier])
            } catch (Exception e) {
                flash.message = "Could not restore from DB Snapshot: ${e}"
                redirect(action: 'list')
            }
        }
    }

    def show = {
        UserContext userContext = UserContext.of(request)
        def name = params.name ?: params.id
        DBSnapshot snapshot = awsRdsService.getDBSnapshot(userContext, name)
        if (!snapshot) {
            Requests.renderNotFound('DB Snapshot', name, this)
        } else {
            def details = ['snapshot' : snapshot]
            // TODO referenced-from lists would be nice too
            withFormat {
                html { return details }
                xml { new XML(details).render(response) }
                json { new JSON(details).render(response) }
            }
        }
    }

    def delete = {
        UserContext userContext = UserContext.of(request)
        def snapshotIds = []
        if (params.dBSnapshotIdentifier) { snapshotIds << params.dBSnapshotIdentifier }
        def selectedSnapshots = params.selectedSnapshots
        if (selectedSnapshots) {
            snapshotIds.addAll ((selectedSnapshots instanceof String) ? [selectedSnapshots] : selectedSnapshots as List)
        }

        def message = ""
        try {
            def deletedCount = 0
            snapshotIds.each{
                awsRdsService.deleteDBSnapshot(userContext, it)
                message += (deletedCount > 0) ? ", $it" : "Snapshot(s) deleted: $it"
                deletedCount++
            }
            flash.message = message
        } catch (Exception e) {
            flash.message = "Could not delete DB Snapshot: ${e}"
        }
        redirect(action: 'list')
    }

}

class CreateDBSnapshotCommand {
    String snapshotName
    static constraints = {
        snapshotName(blank:false)
    }
}

class RestoreDBCommand {
    String dBInstanceIdentifier
    static constraints = {
        dBInstanceIdentifier(blank:false)
    }
}
