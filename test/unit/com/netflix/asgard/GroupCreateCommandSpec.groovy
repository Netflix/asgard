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

        cmd.appName = 'abcache'
        cmd.region = 'us-east-1'

        mockApplicationService.getRegisteredApplication(_, 'abcache') >> new AppRegistration()
        mockAwsAutoScalingService.getAutoScalingGroup(_, 'abcache')
    }

    def 'testEmptyAppNameIsNotValid'() {
        cmd.appName = ''

        when: cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'blank' == cmd.errors.appName
    }

    def 'testBlankAppNameIsNotValid'() {
        cmd.appName = '   '

        when: cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'blank' == cmd.errors.appName
    }

    def 'testNullAppNameIsNotValid'() {
        cmd.appName = null

        when: cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'nullable' == cmd.errors.appName
    }

    def 'testAppNameWithReservedFormatIsInvalid'() {
        cmd.appName = 'abcachev339'

        when: cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'name.usesReservedFormat' == cmd.errors.appName
    }

    def 'testAppNameWithBadCharacterIsNotValid'() {
        cmd.appName = 'uses-hyphen'

        when: cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'application.name.illegalChar' == cmd.errors.appName
    }

    def 'testUnknownAppNameIsValid'() {
        cmd.appName = 'never_heard_of_this_app'

        when: cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'application.name.nonexistent' == cmd.errors.appName
        1 * mockApplicationService.getRegisteredApplication(_, 'never_heard_of_this_app')
    }

    def 'testKnownAppNameIsValid'() {
        when: cmd.validate()

        then:
        !cmd.hasErrors()
        0 == cmd.errors.errorCount
    }

    def 'testEmptyStackIsValid'() {
        cmd.stack = ''

        when: cmd.validate()

        then:
        !cmd.hasErrors()
        0 == cmd.errors.errorCount
    }

    def 'testNullStackIsValid'() {
        cmd.stack = null

        when: cmd.validate()

        then:
        !cmd.hasErrors()
        0 == cmd.errors.errorCount
    }

    def 'testStackIsValid'() {
        cmd.stack = 'iphone'

        when: cmd.validate()

        then:
        !cmd.hasErrors()
        0 == cmd.errors.errorCount
    }

    def 'testStackIsInvalid'() {
        cmd.stack = 'iphone-and-ipad'

        when: cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'The stack must be empty or consist of alphanumeric characters' == cmd.errors.stack
    }

    def 'testStackWithReservedFormatIsInvalid'() {
        cmd.stack = 'v305'

        when: cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'name.usesReservedFormat' == cmd.errors.stack
    }

    def 'testEmptyNewStackIsValid'() {
        cmd.newStack = ''

        when: cmd.validate()

        then:
        !cmd.hasErrors()
        0 == cmd.errors.errorCount
    }

    def 'testNullNewStackIsValid'() {
        cmd.newStack = null

        when: cmd.validate()

        then:
        !cmd.hasErrors()
        0 == cmd.errors.errorCount
    }

    def 'testNewStackIsValid'() {
        cmd.newStack = 'iphone'

        when: cmd.validate()

        then:
        !cmd.hasErrors()
        0 == cmd.errors.errorCount
    }

    def 'testNewStackIsInvalid'() {
        cmd.newStack = 'iphone-and-ipad'

        when: cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'stack.illegalChar' == cmd.errors.newStack
    }

    def 'testNewStackWithReservedFormatIsInvalid'() {
        cmd.newStack = 'v305'

        when: cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'name.usesReservedFormat' == cmd.errors.newStack
    }

    def 'testStackAndNewStackIsInvalid'() {
        cmd.stack = 'iphone'
        cmd.newStack = 'iphone'

        when: cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'stack.matchesNewStack' == cmd.errors.newStack
    }

    def 'testDetailWithReservedFormatIsInvalid'() {
        cmd.stack = 'iphone'
        cmd.detail = 'v305'

        when: cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'name.usesReservedFormat' == cmd.errors.detail
    }

    def 'testHyphenatedDetailWithReservedFormatIsInvalid'() {
        cmd.stack = 'iphone'
        cmd.detail = 'blah-v305'

        when: cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'name.usesReservedFormat' == cmd.errors.detail
    }

    def 'testTotalNameIsTooLong'() {
        cmd.appName = 'videometadata'
        cmd.stack = 'navigator'
        cmd.detail = 'integration-240-usa-iphone-ipad-ios5-even-numbered-days-except-weekends-and-excluding-' +
                'when-the-moon-is-full'

        when: cmd.validate()

        then:
        cmd.hasErrors()
        1 == cmd.errors.errorCount
        'compoundName.invalid.max.size' == cmd.errors.appName
        1 * mockApplicationService.getRegisteredApplication(_, 'videometadata') >> new AppRegistration()
    }

    @Unroll("""should validate chaosMonkey input #chaosMonkey with error code #chaosMonkeyError when requestedFromGui \
is #requestedFromGui and isChaosMonkeyActive is #isChaosMonkeyActive and optLevel is #optLevel""")
    def 'chaosMonkey constraints'() {
        cmd.chaosMonkey = chaosMonkey
        cmd.region = 'us-east-1'
        cmd.requestedFromGui = requestedFromGui
        cmd.appWithClusterOptLevel = optLevel
        mockCloudReadyService.isChaosMonkeyActive(Region.US_EAST_1) >> isChaosMonkeyActive

        when:
        cmd.validate()

        then:
        cmd.errors.chaosMonkey == chaosMonkeyError

        where:
        chaosMonkey | requestedFromGui  | isChaosMonkeyActive   | optLevel  | chaosMonkeyError
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
