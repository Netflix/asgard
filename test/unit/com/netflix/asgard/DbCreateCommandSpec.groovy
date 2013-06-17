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

import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import spock.lang.Specification
import spock.lang.Unroll

@TestMixin(ControllerUnitTestMixin)
class DbCreateCommandSpec extends Specification {

    DbCreateCommand cmd

    void setup() {
        mockForConstraintsTests(DbCreateCommand)
        cmd = new DbCreateCommand()
    }

    @Unroll("""should validate input to create RDS databases""")
    def 'RDS database constraints'() {
        cmd.allocatedStorage = 5
        cmd.backupRetentionPeriod = 0
        cmd.dBInstanceClass = "dbClass"
        cmd.dBInstanceIdentifier = "testDB"
        cmd.dBName = "DBname"
        cmd.masterUsername = "testname"
        cmd.masterUserPassword = "testpassword"
        cmd.port = 3306
        cmd.preferredBackupWindow = ""
        cmd.preferredMaintenanceWindow = ""
        cmd.availabilityZone = availabilityZone
        cmd.multiAZ = multiAZ
        cmd.selectedSecurityGroups = selectedSecurityGroups
        cmd.selectedDBSecurityGroups = selectedDBSecurityGroups
        cmd.subnetPurpose = subnetPurpose

        when:
        cmd.validate()

        then:
        cmd.errors.availabilityZone == errorAvailabilityZone
        cmd.errors.selectedSecurityGroups == errorSelectedSecurityGroups
        cmd.errors.selectedDBSecurityGroups == errorSelectedDBSecurityGroups

        where:
        availabilityZone | multiAZ | subnetPurpose | selectedSecurityGroups | selectedDBSecurityGroups | errorAvailabilityZone                             | errorSelectedSecurityGroups                            | errorSelectedDBSecurityGroups
        "us-east-1a"     | ""      | ""            | null                   | null                     | null                                              | null                                                   | null
        ""               | "on"    | ""            | null                   | null                     | null                                              | null                                                   | null
        "us-east-1a"     | "on"    | ""            | null                   | null                     | 'dbCreateCommand.multiaz.availabilityzones.error' | null                                                   | null
        ""               | ""      | ""            | null                   | null                     | 'dbCreateCommand.multiaz.availabilityzones.error' | null                                                   | null
        "us-east-1a"     | ""      | "internal"    | null                   | null                     | null                                              | 'dbCreateCommand.selectedSecurityGroups.minSize.error' | null
        "us-east-1a"     | ""      | ""            | ["secgroup"]           | null                     | null                                              | 'dbCreateCommand.selectedSecurityGroups.vpc.error'     | null
        "us-east-1a"     | "on"    | ""            | ["secgroup"]           | null                     | 'dbCreateCommand.multiaz.availabilityzones.error' | 'dbCreateCommand.selectedSecurityGroups.vpc.error'     | null
        "us-east-1a"     | ""      | "internal"    | ["secgroup"]           | ["dbsecgroup"]           | null                                              | null                                                   | 'dbCreateCommand.selectedDBSecurityGroups.vpc.error'
        "us-east-1a"     | "on"    | "internal"    | ["secgroup"]           | ["dbsecgroup"]           | 'dbCreateCommand.multiaz.availabilityzones.error' | null                                                   | 'dbCreateCommand.selectedDBSecurityGroups.vpc.error'
    }
}
