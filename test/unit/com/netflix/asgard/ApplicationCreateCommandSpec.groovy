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
class ApplicationCreateCommandSpec extends Specification {

    ApplicationCreateCommand cmd
    CloudReadyService mockCloudReadyService = Mock(CloudReadyService)

    void setup() {
        mockForConstraintsTests(ApplicationCreateCommand)
        cmd = new ApplicationCreateCommand()
        cmd.cloudReadyService = mockCloudReadyService
    }

    @Unroll("""should validate chaosMonkey input #chaosMonkey with error code #chaosMonkeyError when requestedFromGui \
is #requestedFromGui and isChaosMonkeyActive is #isChaosMonkeyActive""")
    def 'chaosMonkey constraints'() {
        cmd.name = 'helloworld'
        cmd.email = 'me@netflix.com'
        cmd.type = 'service'
        cmd.description = 'Says hello to the world.'
        cmd.owner = 'me'
        cmd.chaosMonkey = chaosMonkey
        cmd.requestedFromGui = requestedFromGui
        mockCloudReadyService.isChaosMonkeyActive() >> isChaosMonkeyActive

        when:
        cmd.validate()

        then:
        cmd.errors.chaosMonkey == chaosMonkeyError

        where:
        chaosMonkey | requestedFromGui  | isChaosMonkeyActive   | chaosMonkeyError
        'enabled'   | true              | true                  | null
        'enabled'   | true              | false                 | null
        'enabled'   | false             | true                  | null
        'enabled'   | false             | false                 | null
        null        | true              | true                  | 'chaosMonkey.optIn.missing.error'
        null        | true              | false                 | null
        null        | false             | true                  | null
        null        | false             | false                 | null
    }

}
