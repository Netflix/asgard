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

    final createCommandParams = [
        allocatedStorage: 5,
        backupRetentionPeriod: 0,
        dBInstanceClass: 'dbClass',
        dBInstanceIdentifier: 'testDB',
        dBName: 'DBname',
        masterUsername: 'testname',
        masterUserPassword: 'testpassword',
        port: 3306,
        preferredBackupWindow: '',
        preferredMaintenanceWindow: ''
    ]

    void setup() {
        mockForConstraintsTests(DbCreateCommand)
        cmd = new DbCreateCommand()
    }

    @Unroll("""should validate input to create an RDS database
               with error code #errorAvailabilityZone
               when availability zone is #availabilityZone and multi availability zone is #multiAZ""")
    def 'RDS database constraints for availability zones'() {
        DbCreateCommand cmd = new DbCreateCommand(createCommandParams).with {
            it.availabilityZone = availabilityZone
            it.multiAZ = multiAZ
        }

        when:
        cmd.validate()

        then:
        cmd.errors.availabilityZone == errorAvailabilityZone

        where:
        availabilityZone | multiAZ | errorAvailabilityZone
        ''               | ''      | 'dbCreateCommand.multiaz.availabilityzones.error'
        ''               | 'on'    | null
        'us-east-1a'     | ''      | null
        'us-east-1a'     | 'on'    | 'dbCreateCommand.multiaz.availabilityzones.error'
    }

    @Unroll("""should validate input to create RDS databases
               with error code #errorSelectedSecurityGroups
               when the selected security groups are #selectedSecurityGroups and
                    the selected DB security groups #selectedDBSecurityGroups and
                    the subnet purpose is #subnetPurpose""")
    def 'RDS database constraints for selected security groups'() {
        DbCreateCommand cmd = new DbCreateCommand(createCommandParams).with {
            it.subnetPurpose = subnetPurpose
            it.selectedSecurityGroups = selectedSecurityGroups
            it.selectedDBSecurityGroups = selectedDBSecurityGroups
        }

        when:
        cmd.validate()

        then:
        cmd.errors.selectedSecurityGroups == errorSelectedSecurityGroups

        where:
        subnetPurpose | selectedSecurityGroups | selectedDBSecurityGroups | errorSelectedSecurityGroups
        ''            | null                   | null                     | null
        ''            | null                   | ['dbsecgroup']           | null
        ''            | ['secgroup']           | null                     | 'dbCreateCommand.selectedSecurityGroups.vpc.error'
        ''            | ['secgroup']           | ['dbsecgroup']           | 'dbCreateCommand.selectedSecurityGroups.vpc.error'
        'internal'    | null                   | null                     | 'dbCreateCommand.selectedSecurityGroups.minSize.error'
        'internal'    | null                   | ['dbsecgroup']           | null
        'internal'    | ['secgroup']           | null                     | null
        'internal'    | ['secgroup']           | ['dbsecgroup']           | null
    }

    @Unroll("""should validate input to create RDS databases
               with error code #errorSelectedDBSecurityGroups
               when the selected security groups are #selectedSecurityGroups and
                    the selected DB security groups #selectedDBSecurityGroups and
                    the subnet purpose is #subnetPurpose""")
    def 'RDS database constraints for selected DB security groups'() {
        DbCreateCommand cmd = new DbCreateCommand(createCommandParams).with {
            it.subnetPurpose = subnetPurpose
            it.selectedSecurityGroups = selectedSecurityGroups
            it.selectedDBSecurityGroups = selectedDBSecurityGroups
        }

        when:
        cmd.validate()

        then:
        cmd.errors.selectedDBSecurityGroups == errorSelectedDBSecurityGroups

        where:
        subnetPurpose | selectedSecurityGroups | selectedDBSecurityGroups | errorSelectedDBSecurityGroups
        ''            | null                   | null                     | null
        ''            | null                   | ['dbsecgroup']           | null
        ''            | ['secgroup']           | null                     | null
        ''            | ['secgroup']           | ['dbsecgroup']           | null
        'internal'    | null                   | null                     | 'dbCreateCommand.selectedSecurityGroups.minSize.error'
        'internal'    | null                   | ['dbsecgroup']           | null
        'internal'    | ['secgroup']           | null                     | null
        'internal'    | ['secgroup']           | ['dbsecgroup']           | 'dbCreateCommand.selectedSecurityGroups.vpc.error'
    }
}
