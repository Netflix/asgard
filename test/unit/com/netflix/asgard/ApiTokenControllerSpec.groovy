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

import com.netflix.asgard.auth.ApiToken
import grails.test.MockUtils
import grails.test.mixin.TestFor
import org.apache.shiro.subject.Subject
import spock.lang.Specification
import spock.lang.Unroll

@TestFor(ApiTokenController)
class ApiTokenControllerSpec extends Specification {

    def configService = Mock(ConfigService)
    def secretService = Mock(SecretService)

    Subject subject = Mock(Subject)

    def setup() {
        controller.configService = configService
        controller.secretService = secretService
        MockUtils.prepareForConstraintsTests(GenerateApiTokenCommand)

        subject.principal >> 'testUser@netflix.com'
        ShiroTestUtil.setSubject(subject)
    }

    def cleanup() {
        ShiroTestUtil.tearDownShiro()
    }

    def 'should return 401 if api tokens disabled'() {
        configService.apiTokenEnabled >> null

        when:
        controller.beforeInterceptor()

        then:
        response.status == 401
        response.contentAsString == 'This feature is disabled.'
    }

    def 'should return 401 if user is not authenticated'() {
        configService.apiTokenEnabled >> true
        subject.authenticated >> false

        when:
        controller.beforeInterceptor()

        then:
        response.status == 401
        response.contentAsString == 'You must be logged in to use this feature.'
    }

    def 'should return api token for valid request'() {
        secretService.currentApiEncryptionKey >> 'key'
        configService.apiTokenExpirationDays >> 90
        subject.principal >> 'test@netflix.com'
        GenerateApiTokenCommand command = new GenerateApiTokenCommand(purpose: 'ThisPurpose',
                email: 'testDL@netflix.com')
        command.validate()

        when:
        controller.generate(command)

        then:
        response.redirectUrl == '/apiToken/show'
        flash.apiToken == new ApiToken('ThisPurpose', 'testDL@netflix.com', 90, 'key')
    }

    def 'should return error for invalid generate request'() {
        configService
        GenerateApiTokenCommand command = new GenerateApiTokenCommand()
        command.validate()

        when:
        controller.generate(command)

        then:
        response.redirectedUrl == '/apiToken/create'
    }

    @Unroll("hasErrors should return #valid when purpose is #purpose")
    def 'purpose constraints'() {
        when:
        GenerateApiTokenCommand command = new GenerateApiTokenCommand(purpose: purpose, email: 'test@netflix.com')
        command.validate()

        then:
        command.hasErrors() != valid

        where:

        purpose | valid
        null    | false
        ''      | false
        'ab12'  | true
        'ab_12.'| true
        'ab 12' | false
        'ab:12' | false
    }

    @Unroll("hasErrors should return #valid when email is #email")
    def 'email contraints'() {
        when:
        GenerateApiTokenCommand command = new GenerateApiTokenCommand(purpose: 'ThisPurpose', email: email)
        command.validate()

        then:
        command.hasErrors() != valid

        where:

        email              | valid
        null               | false
        ''                 | false
        'test'             | false
        'test@netflix.com' | true
    }
}
