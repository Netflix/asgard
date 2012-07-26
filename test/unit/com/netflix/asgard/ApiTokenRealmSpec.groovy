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
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.AuthenticationInfo
import org.apache.shiro.authc.SimpleAuthenticationInfo
import org.apache.shiro.subject.Subject
import spock.lang.Specification


class ApiTokenRealmSpec extends Specification {

    ApiToken apiToken
    def configService = Mock(ConfigService)
    def emailerService = Mock(EmailerService)
    def secretService = Mock(SecretService)
    ApiTokenRealm realm = new ApiTokenRealm(configService: configService, emailerService: emailerService,
        secretService: secretService)

    def setup() {
        Subject subject = Mock(Subject)
        subject.principal >> 'test@netflix.com'
        ShiroTestUtil.setSubject(subject)
        apiToken = new ApiToken('ThisPurpose', 'testDL@netflix.com', 90, 'key')
    }

    def cleanup() {
        ShiroTestUtil.tearDownShiro()
    }

    def 'should throw AuthenticationException when token is null'() {
        when:
        realm.authenticate(null)

        then:
        AuthenticationException e = thrown()
        e.message == 'API Key cannot be null'
    }

    def 'should authenticate valid token'() {
        configService.apiTokenExpiryWarningThresholdDays >> 7
        secretService.apiEncryptionKeys >> ['key']

        when:
        AuthenticationInfo info = realm.authenticate(apiToken)

        then:
        info == new SimpleAuthenticationInfo('test@netflix.com', apiToken.credentials, 'ApiTokenRealm')
    }

    def 'should throw AuthenticationException when token is invalid'() {
        secretService.apiEncryptionKeys >> ['different key']

        when:
        realm.authenticate(apiToken)

        then:
        AuthenticationException e = thrown()
        e.message == 'API Token is invalid'
    }

    def 'should send email when token is near expiration'() {
        configService.apiTokenExpiryWarningThresholdDays >> 100
        secretService.apiEncryptionKeys >> ['key']

        // initialize cache
        configService.apiTokenExpiryWarningIntervalMinutes >> 360
        realm.afterPropertiesSet()

        when: 'multiple authentication calls'
        2.times { realm.authenticate(apiToken) }

        then: 'only one email is sent'
        1 * emailerService.sendUserEmail('testDL@netflix.com', 'Asgard API key is about to expire', _)
    }

}
