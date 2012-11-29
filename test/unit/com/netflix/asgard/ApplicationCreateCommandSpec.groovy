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

@TestMixin(ControllerUnitTestMixin)
class ApplicationCreateCommandSpec extends Specification {

    ApplicationCreateCommand cmd
    CloudReadyService mockCloudReadyService = Mock(CloudReadyService)

    void setup() {
        mockForConstraintsTests(ApplicationCreateCommand)
        cmd = new ApplicationCreateCommand()
        cmd.cloudReadyService = mockCloudReadyService

        cmd.name = 'helloworld'
        cmd.email = 'me@netflix.com'
        cmd.type = 'service'
        cmd.description = 'Says hello to the world.'
        cmd.owner = 'me'
    }

    def 'should validate when Chaos Monkey choice is made'() {
        cmd.chaosMonkey = 'enabled'

        when: cmd.validate()

        then:
        !cmd.hasErrors()
    }

    def 'should validate with no Chaos Monkey choice when Chaos Monkey is not active'() {
        cmd.requestedFromGui = true

        when: cmd.validate()

        then:
        !cmd.hasErrors()
        1 * mockCloudReadyService.isChaosMonkeyActive() >> false
    }

    def 'should validate with no Chaos Monkey choice when request does not come from GUI'() {
        cmd.requestedFromGui = false

        when: cmd.validate()

        then:
        !cmd.hasErrors()
        1 * mockCloudReadyService.isChaosMonkeyActive() >> true
    }

    def 'should not validate with no Chaos Monkey choice when it is expected'() {
        cmd.requestedFromGui = true

        when: cmd.validate()

        then:
        cmd.hasErrors()
        cmd.errors.errorCount == 1
        cmd.errors.chaosMonkey == 'chaosMonkey.optIn.missing.error'
        1 * mockCloudReadyService.isChaosMonkeyActive() >> true
    }

}
