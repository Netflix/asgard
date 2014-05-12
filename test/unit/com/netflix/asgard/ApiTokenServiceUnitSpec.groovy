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
import org.apache.shiro.SecurityUtils
import org.apache.shiro.subject.Subject
import spock.lang.Specification

class ApiTokenServiceUnitSpec extends Specification {

    ApiToken apiToken
    def configService = Mock(ConfigService)
    def emailerService = Mock(EmailerService)
    ApiTokenService apiTokenService = new ApiTokenService(configService: configService, emailerService: emailerService)

    def setup() {
        Subject subject = Mock(Subject)
        subject.principal >> 'test@netflix.com'
        SecurityUtils.metaClass.static.getSubject = { subject }
        apiToken = new ApiToken('ThisPurpose', 'testDL@netflix.com', 90, 'key')
    }

    def 'should validate a valid token'() {
        given: configService.apiEncryptionKeys >> ['key']
        when: boolean valid = apiTokenService.tokenValid(apiToken)
        then: valid
    }

    def 'should fail on an invalid token'() {
        given: configService.apiEncryptionKeys >> ['otherKey']
        when: boolean valid = apiTokenService.tokenValid(apiToken)
        then: !valid
    }

    def 'should not send emails for token that is not close to expiration'() {
        given: configService.apiTokenExpiryWarningThresholdDays >> 7
        when: apiTokenService.checkExpiration(apiToken)
        then: 0 * emailerService._
    }

    def 'should send email when token is near expiration'() {
        configService.apiTokenExpiryWarningThresholdDays >> 100
        configService.canonicalServerName >> 'asgardtest'
        configService.apiEncryptionKeys >> ['key']

        // initialize cache
        configService.apiTokenExpiryWarningIntervalMinutes >> 360
        apiTokenService.afterPropertiesSet()

        when: 'multiple authentication calls'
        2.times { apiTokenService.checkExpiration(apiToken) }

        then: 'only one email is sent'
        1 * emailerService.sendUserEmail('testDL@netflix.com', 'asgardtest API key is about to expire',
                { it.startsWith('The following asgardtest API key is about to expire:') })
    }

}
