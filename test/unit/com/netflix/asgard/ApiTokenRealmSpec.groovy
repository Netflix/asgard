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
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.AuthenticationInfo
import org.apache.shiro.authc.SimpleAuthenticationInfo
import org.apache.shiro.subject.Subject
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class ApiTokenRealmSpec extends Specification {

    ApiToken apiToken
    ApiTokenService apiTokenService = Mock(ApiTokenService)
    ApplicationContext applicationContext = Mock(ApplicationContext)
    ApiTokenRealm realm = new ApiTokenRealm(applicationContext: applicationContext)

    def setup() {
        Subject subject = Mock(Subject)
        subject.principal >> 'test@netflix.com'
        SecurityUtils.metaClass.static.getSubject = { subject }
        apiToken = new ApiToken('ThisPurpose', 'testDL@netflix.com', 90, 'key')

        applicationContext.getBean(ApiTokenService) >> apiTokenService
    }

    def 'should throw AuthenticationException when token is null'() {
        when:
        realm.authenticate(null)

        then:
        AuthenticationException e = thrown()
        e.message == 'API Key cannot be null'
    }

    def 'should authenticate valid token'() {
        apiTokenService.tokenValid(apiToken) >> true

        when:
        AuthenticationInfo info = realm.authenticate(apiToken)

        then:
        1 * apiTokenService.checkExpiration(apiToken)
        info == new SimpleAuthenticationInfo('test@netflix.com', apiToken.credentials, 'ApiTokenRealm')
    }

    def 'should throw AuthenticationException when token is invalid'() {
        apiTokenService.tokenValid(apiToken) >> false

        when:
        realm.authenticate(apiToken)

        then:
        AuthenticationException e = thrown()
        e.message == 'API Token is invalid'
        0 * apiTokenService.checkExpiration(apiToken)
    }

}
