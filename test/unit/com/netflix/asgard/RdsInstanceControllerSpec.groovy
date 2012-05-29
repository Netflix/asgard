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

import grails.plugin.spock.ControllerSpec
import grails.test.MockUtils
import com.netflix.asgard.mock.Mocks

class RdsInstanceControllerSpec extends ControllerSpec {

    final showParams = [
        allocatedStorage: 1,
        availabilityZone: "",
        backupRetentionPeriod: 0,
        dBInstanceClass: "",
        dBInstanceIdentifier: "testDB",
        dBName: "",
        masterUsername: "",
        preferredBackupWindow: "",
        preferredMaintenanceWindow: "",
    ]

    void setup() {
        TestUtils.setUpMockRequest()
        MockUtils.prepareForConstraintsTests(DbCreateCommand)
        MockUtils.prepareForConstraintsTests(DbUpdateCommand)
        controller.awsRdsService = Mocks.awsRdsService()
        controller.awsEc2Service = Mocks.awsEc2Service()
        mockRequest.region = Region.defaultRegion()
    }

    def 'save should show RDS instance after creation'() {
        mockParams.putAll(showParams)
        final cmd = new DbCreateCommand(showParams)

        when:
        controller.save(cmd)

        then:
        redirectArgs.action == controller.show
        redirectArgs.params == [id: "testDB"]
     }

    def 'save should return exception message when necessary'() {
        mockParams.putAll(showParams)
        final cmd = new DbCreateCommand(showParams)
        controller.awsRdsService = { throw new IllegalStateException("Exception Thrown for test.")} as AwsRdsService

        when:
        controller.save(cmd)

        then:
        chainArgs.action == controller.create
        controller.flash.message == "Could not create DB Instance: java.lang.IllegalStateException: Exception Thrown for test."
     }

    def 'save should return error with invalid input'() {
        mockParams.putAll(showParams)
        final cmd = new DbCreateCommand(showParams)
        cmd.validate()

        when:
        controller.save(cmd)

        then:
        chainArgs.action == controller.create
        chainArgs.model.cmd.errors.allocatedStorage == "range"
     }

    def 'create should return possible RDS engines and license model selections'() {
        mockParams.with {
            dBInstanceIdentifier = "0"
        }

        when:
        final model = controller.create()

        then:
        model.allDbInstanceEngines == ['MySQL', 'oracle-se1', 'oracle-se', 'oracle-ee']
        model.allLicenseModels == ['general-public-license', 'license-included', 'bring-your-own-license']

    }
}
