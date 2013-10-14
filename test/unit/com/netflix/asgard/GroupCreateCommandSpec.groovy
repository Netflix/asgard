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
class GroupCreateCommandSpec extends Specification {

    GroupCreateCommand cmd
    ApplicationService mockApplicationService = Mock(ApplicationService)
    AwsAutoScalingService mockAwsAutoScalingService = Mock(AwsAutoScalingService)
    CloudReadyService mockCloudReadyService = Mock(CloudReadyService)

    void setup() {
        mockForConstraintsTests(GroupCreateCommand)
        cmd = new GroupCreateCommand()
        cmd.applicationService = mockApplicationService
        cmd.awsAutoScalingService = mockAwsAutoScalingService
        cmd.cloudReadyService = mockCloudReadyService
    }

    @Unroll("""should validate appName input '#appName' with error code '#error'""")
    def 'appName constraints'() {
        cmd.appName = appName
        mockApplicationService.getRegisteredApplication(_, 'abcache') >> new AppRegistration()

        when:
        cmd.validate()

        then:
        cmd.errors.appName == error

        where:
        appName                     | error
        ''                          | 'blank'
        '   '                       | 'blank'
        null                        | 'nullable'
        'abcachev339'               | 'name.usesReservedFormat'
        'uses-hyphen'               | 'application.name.illegalChar'
        'never_heard_of_this_app'   | 'application.name.nonexistent'
        'abcache'                   | null
    }

    def 'should validate with an error when total name is too long'() {
        cmd.appName = 'videometadata'
        cmd.stack = 'navigator'
        cmd.imageId = 'imageId'
        cmd.detail = 'integration-240-usa-iphone-ipad-ios5-even-numbered-days-except-weekends-and-excluding-' +
                'when-the-moon-is-full'

        when:
        cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'compoundName.invalid.max.size' == cmd.errors.appName
        //noinspection GroovyAssignabilityCheck
        1 * mockApplicationService.getRegisteredApplication(_, 'videometadata') >> new AppRegistration()
    }

    @Unroll("""should validate stack input '#stack' with error code '#error'""")
    def 'stack constraints'() {
        cmd.stack = stack

        when:
        cmd.validate()

        then:
        cmd.errors.stack == error

        where:
        stack               | error
        ''                  | null
        null                | null
        'iphone'            | null
        'iphone-and-ipad'   | 'The stack must be empty or consist of alphanumeric characters'
        'v305'              | 'name.usesReservedFormat'
    }

    @Unroll("""should validate newStack input '#newStack' with error code '#error' when stack is '#stack'""")
    def 'newStack constraints'() {
        cmd.stack = stack
        cmd.newStack = newStack

        when:
        cmd.validate()

        then:
        cmd.errors.newStack == error

        where:
        newStack            | stack     | error
        ''                  | null      | null
        null                | null      | null
        'iphone'            | null      | null
        'iphone-and-ipad'   | null      | 'stack.illegalChar'
        'v305'              | null      | 'name.usesReservedFormat'
        'iphone'            | 'iphone'  | 'stack.matchesNewStack'
    }

    @Unroll("""should validate detail input '#detail' with error code '#error'""")
    def 'detail constraints'() {
        cmd.stack = 'iphone'
        cmd.detail = detail

        when:
        cmd.validate()

        then:
        cmd.errors.detail == error

        where:
        detail        | error
        'v305'        | 'name.usesReservedFormat'
        'blah-v305'   | 'name.usesReservedFormat'
    }

    @Unroll("""should validate chaosMonkey input '#chaosMonkey' with error code '#error' when requestedFromGui is \
'#requestedFromGui' and isChaosMonkeyActive is '#isChaosMonkeyActive' and optLevel is '#optLevel'""")
    def 'chaosMonkey constraints'() {
        cmd.chaosMonkey = chaosMonkey
        cmd.region = 'us-east-1'
        cmd.requestedFromGui = requestedFromGui
        cmd.appWithClusterOptLevel = optLevel
        mockCloudReadyService.isChaosMonkeyActive(Region.US_EAST_1) >> isChaosMonkeyActive

        when:
        cmd.validate()

        then:
        cmd.errors.chaosMonkey == error

        where:
        chaosMonkey | requestedFromGui  | isChaosMonkeyActive   | optLevel  | error
        'enabled'   | true              | true                  | true      | null
        'enabled'   | false             | true                  | true      | null
        'enabled'   | true              | false                 | true      | null
        'enabled'   | true              | true                  | false     | null
        'enabled'   | true              | false                 | false     | null
        'enabled'   | false             | true                  | false     | null
        'enabled'   | false             | false                 | true      | null
        'enabled'   | false             | false                 | false     | null
        null        | true              | true                  | true      | 'chaosMonkey.optIn.missing.error'
        null        | false             | true                  | true      | null
        null        | true              | false                 | true      | null
        null        | true              | true                  | false     | null
        null        | true              | false                 | false     | null
        null        | false             | true                  | false     | null
        null        | false             | false                 | true      | null
        null        | false             | false                 | false     | null
    }
}
