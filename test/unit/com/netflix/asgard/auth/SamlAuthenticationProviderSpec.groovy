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
package com.netflix.asgard.auth

import com.netflix.asgard.auth.SamlAuthenticationProvider.SamlToken
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.AuthenticationInfo
import org.apache.shiro.authc.SimpleAuthenticationInfo
import spock.lang.Specification

class SamlAuthenticationProviderSpec extends Specification {

    SamlAuthenticationProvider provider = new SamlAuthenticationProvider()
    SamlToken samlToken = Mock(SamlToken)

    def 'should throw AuthenticationException when token is null'() {
        when:
        provider.authenticate(null)

        then:
        AuthenticationException e = thrown()
        e.message == 'SAML token cannot be null'
    }

    def 'should throw AuthenticationException for invalid token'() {
        samlToken.valid >> false

        when:
        provider.authenticate(samlToken)

        then:
        AuthenticationException e = thrown()
        e.message == 'Invalid SAML token'
    }

    def 'should authenticate if valid user'() {
        samlToken.valid >> true
        samlToken.principal >> 'username'
        samlToken.credentials >> 'creds'

        when:
        AuthenticationInfo info = provider.authenticate(samlToken)

        then:
        info == new SimpleAuthenticationInfo('username', 'creds', 'AsgardRealm')
    }

}
