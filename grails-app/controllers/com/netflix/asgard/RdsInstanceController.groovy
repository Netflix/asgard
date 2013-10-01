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
import com.netflix.asgard.AwsRdsService.Engine
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class RdsInstanceController {

    static allowedMethods = [delete: 'POST']

    def index = { redirect(action: 'list', params: params) }

    def awsRdsService
    def awsEc2Service

    def list = {
        UserContext userContext = UserContext.of(request)
        def dbInstances = (awsRdsService.getDBInstances(userContext) as List).sort { it.getDBInstanceIdentifier().toLowerCase() }
        withFormat {
            html { [ 'dbInstanceList' : dbInstances] }
            xml { new XML(dbInstances).render(response) }
            json { new JSON(dbInstances).render(response) }
        }
    }

    def edit = {
        UserContext userContext = UserContext.of(request)
        DBInstance dbInstance = awsRdsService.getDBInstance(userContext, params.dBInstanceIdentifier)
        [
            'dbInstance' : dbInstance,
            'allDBSecurityGroups' : awsRdsService.getDBSecurityGroups(userContext),
            'allDBInstanceClasses' : AwsRdsService.getDBInstanceClasses(),
            'instanceDBSecurityGroups': dbInstance.getDBSecurityGroups().collect { it.getDBSecurityGroupName() }
        ]
    }

    def create = {
        UserContext userContext = UserContext.of(request)
        [
            'allDBSecurityGroups' : awsRdsService.getDBSecurityGroups(userContext),
            'allDBInstanceClasses' : AwsRdsService.getDBInstanceClasses(),
            'allDbInstanceEngines' : Engine.values()*.awsValue,
            'allLicenseModels' : AwsRdsService.getLicenseModels(),
            'zoneList':awsEc2Service.getAvailabilityZones(userContext)
        ]
    }

    def save = { DbCreateCommand cmd ->
        log.info "trying to save new db, groups $params.selectedDBSecurityGroups"
        UserContext userContext = UserContext.of(request)
        if (cmd.hasErrors()) {
            chain(action: 'create', model:[cmd:cmd], params:params) // Use chain to pass both the errors and the params
        } else {
            try {
                boolean multiAZ = params.multiAZ == 'on'
                def selectedDBSecurityGroups = (params.selectedDBSecurityGroups instanceof String) ? [params.selectedDBSecurityGroups] : params.selectedDBSecurityGroups as List
                if (!selectedDBSecurityGroups) { selectedDBSecurityGroups = ["default"] }
                //awsRdsService.createDBSecurityGroup(params.name, params.description)

                final DBInstance dbInstance = new DBInstance()
                    .withAllocatedStorage(params.allocatedStorage as Integer)
                    .withAvailabilityZone(params.availabilityZone)
                    .withBackupRetentionPeriod(params.backupRetentionPeriod as Integer)
                    .withDBInstanceClass(params.dBInstanceClass,)
                    .withDBInstanceIdentifier(params.dBInstanceIdentifier)
                    .withDBName(params.dBName)
                    .withEngine(params.engine)
                    .withDBSecurityGroups(selectedDBSecurityGroups)
                    .withMasterUsername(params.masterUsername)
                    .withMultiAZ(multiAZ)
                    .withPreferredBackupWindow(params.preferredBackupWindow)
                    .withPreferredMaintenanceWindow(params.preferredMaintenanceWindow)
                    .withLicenseModel(params.licenseModel)

                awsRdsService.createDBInstance(userContext, dbInstance, params.masterUserPassword, params.port as Integer)
                flash.message = "DB Instance '${params.dBInstanceIdentifier}' has been created."
                redirect(action: 'show', params: [id: params.dBInstanceIdentifier])
            } catch (Exception e) {
                flash.message = "Could not create DB Instance: ${e}"
                chain(action: 'create', params: params) // Use chain instead of redirect, so params are not lost
            }
        }
    }

    def update = { DbUpdateCommand cmd ->
        UserContext userContext = UserContext.of(request)
        if (cmd.hasErrors()) {
            chain(action: 'edit', model:[cmd:cmd], params:params) // Use chain to pass both the errors and the params
        } else {
            try {
                boolean multiAZ = ("on" == params.multiAZ)
                def selectedDBSecurityGroups = (params.selectedDBSecurityGroups instanceof String) ? [params.selectedDBSecurityGroups] : params.selectedDBSecurityGroups
                if (!selectedDBSecurityGroups) { selectedDBSecurityGroups = ["default"] }
                awsRdsService.updateDBInstance(
                    userContext,
                    params.allocatedStorage.toInteger(),
                    params.backupRetentionPeriod.toInteger(),
                    params.dBInstanceClass,
                    params.dBInstanceIdentifier,
                    selectedDBSecurityGroups as Collection<String>,
                    params.masterUserPassword,
                    multiAZ,
                    params.preferredBackupWindow,
                    params.preferredMaintenanceWindow
                )
                flash.message = "DB Instance '${params.dBInstanceIdentifier}' has been updated."
                redirect(action: 'show', params:['dBInstanceIdentifier':params.dBInstanceIdentifier])
            } catch (Exception e) {
                flash.message = "Could not update DB Instance: ${e}"
                chain(action: 'edit', params:params)
            }
        }
    }

    def show = {
        UserContext userContext = UserContext.of(request)
        def dbInstanceId = params.dBInstanceIdentifier ?: params.id

        def dbInstance = null
        try {
            dbInstance = awsRdsService.getDBInstance(userContext, dbInstanceId)
        } catch (Exception ignored) {
            // AWS will throw exception if RDS instance not found. Ignore it.
            // Request will redirect after null check on dbInstance.
        }
        List<DBSnapshot> snapshots = awsRdsService.getDBSnapshotsForInstance(userContext, dbInstanceId)
        if (!dbInstance) {
            Requests.renderNotFound('DB Instance', dbInstanceId, this)
        } else {
            def details = ['dbInstance':dbInstance, 'snapshots' : snapshots]
            withFormat {
                html { return details }
                xml { new XML(details).render(response) }
                json { new JSON(details).render(response) }
            }
        }
    }

    def delete = {
        def id = ""
        UserContext userContext = UserContext.of(request)
        try {
            id = params.dBInstanceIdentifier // instanceId comes from details page
            awsRdsService.deleteDBInstance(userContext, id)
            flash.message = "Deleting DB instance: '${id}'..."
            redirect(action: 'list')
        } catch (Exception e) {
            flash.message = "Could not delete DB instance ${id}: ${e}"
            redirect(action: 'list')
        }

    }
}

class DbCreateCommand {

    Integer allocatedStorage // Must be an integer between 5 and 1024.
    String availabilityZone // The availabilityZone parameter cannot be set if the --multi-az parameter is set to true.
    Integer backupRetentionPeriod // Must be a value from 0 to 8.
    String dBInstanceClass // Valid values: db.m1.small | db.m1.large | db.m1.xlarge | db.m2.2xlarge | db.m2.4xlarge
    String dBInstanceIdentifier // Constraints: Must contain from 1 to 63 alphanumeric characters or hyphens. First character must be a letter. Cannot end with a hyphen or contain two consecutive hyphens.
    String dBName // Cannot be empty. Must contain 1 to 64 alphanumeric characters. Cannot be a word reserved by the specified database engine.
    Collection<String> selectedDBSecurityGroups // At least one
    String masterUsername // Must be an alphanumeric string containing from 1 to 16 characters.
    String masterUserPassword // Must contain 4 to 16 alphanumeric characters.
    Integer port
    String preferredBackupWindow // Constraints: Must be in the format hh24:mi-hh24:mi.
    // Times should be 24-hour Universal Time Coordinated (UTC).
    // Must not conflict with the --preferred-maintenance-window.
    // Must be at least 2 hours.
    // Cannot be set if the --backup-retention-period has not been specified.

    String preferredMaintenanceWindow // Must be in the format ddd:hh24:mi-ddd:hh24:mi.
    // Times should be Universal Time Coordinated (UTC). See example below.
    // Example: --preferred-maintenance-window Tue:00:30-Tue:04:30

    static constraints = {
        allocatedStorage(nullable: false, range: 5..1024)
        availabilityZone(nullable: false, blank: false) // Need more -- custom validator?
        backupRetentionPeriod(blank: false, nullable: false, range: 0..8)
        dBInstanceClass(nullable: false, blank: false)
        dBInstanceIdentifier(nullable: false, blank: false, size: 1..63, matches: '[a-zA-Z]{1}[a-zA-Z0-9-]*[^-]$') // This match does not check the double-hyphen
        dBName(nullable: false, blank: false, size: 1..64, matches: '[a-zA-Z0-9]{1,64}')
        selectedDBSecurityGroups(nullable: false, minSize: 1)
        masterUsername(nullable: false, blank: false, size: 1..16, matches: '[a-zA-Z0-9]{1,16}')
        masterUserPassword(nullable: false, blank: false, size: 4..16, matches: '[a-zA-Z0-9]{4,16}')
        port(nullable: false)
        preferredBackupWindow(blank: true, matches: '(20|21|22|23|[01]\\d|\\d)(([:][0-5]\\d){1,2})-(20|21|22|23|[01]\\d|\\d)(([:][0-5]\\d){1,2})')
        // Did not check for 2 hour min, clash with maintenance, or that backup period specified

        // Should, but did not, validate preferredMaintenanceWindow
    }
}

class DbUpdateCommand {

    Integer allocatedStorage // Must be an integer between 5 and 1024.
    Integer backupRetentionPeriod // Must be a value from 0 to 8.
    String dBInstanceClass // Valid values: db.m1.small | db.m1.large | db.m1.xlarge | db.m2.2xlarge | db.m2.4xlarge
    Collection<String> selectedDBSecurityGroups // At least one
    String masterUserPassword // Must contain 4 to 16 alphanumeric characters.
    String preferredBackupWindow // Constraints: Must be in the format hh24:mi-hh24:mi.
    // Times should be 24-hour Universal Time Coordinated (UTC).
    // Must not conflict with the --preferred-maintenance-window.
    // Must be at least 2 hours.
    // Cannot be set if the --backup-retention-period has not been specified.

    String preferredMaintenanceWindow // Must be in the format ddd:hh24:mi-ddd:hh24:mi.
    // Times should be Universal Time Coordinated (UTC). See example below.
    // Example: --preferred-maintenance-window Tue:00:30-Tue:04:30

    static constraints = {
        allocatedStorage(nullable: false, range: 5..1024)
        backupRetentionPeriod(blank: false, nullable: false, range: 0..8)
        dBInstanceClass(nullable: false, blank: false)
        selectedDBSecurityGroups(nullable: false, minSize: 1)
        masterUserPassword(blank: true, size: 4..16, matches: '[a-zA-Z0-9]{4,16}')
        preferredBackupWindow(blank: true, matches: '(20|21|22|23|[01]\\d|\\d)(([:][0-5]\\d){1,2})-(20|21|22|23|[01]\\d|\\d)(([:][0-5]\\d){1,2})')
        // Did not check for 2 hour min, clash with maintenance, or that backup period specified

        // Should, but did not, validate preferredMaintenanceWindow
    }
}
