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

import com.netflix.asgard.auth.AsgardToken
import com.netflix.asgard.plugin.AuthenticationProvider
import grails.test.mixin.TestFor
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.subject.Subject
import spock.lang.Specification

@TestFor(AuthController)
class AuthControllerSpec extends Specification {

    Subject subject = Mock(Subject)
    AsgardToken asgardToken = Mock(AsgardToken)
    AuthenticationProvider authenticationProvider = Mock(AuthenticationProvider)
    def pluginService = Mock(PluginService)

    def setup() {
        ShiroTestUtil.setSubject(subject)
        controller.pluginService = pluginService
    }

    def cleanup() {
        ShiroTestUtil.tearDownShiro()
    }

    def 'should throw 403 if authentication is not configured'() {
        when:
        controller.beforeInterceptor()

        then:
        response.status == 403
        response.contentAsString == 'Authentication is not configured.'
    }

    def 'sign in should redirect to target url'() {
        prepareAuthentication()
        controller.session[AuthController.AUTH_TARGET_URL] = '/test'
        authenticationProvider.tokenFromRequest(_) >> asgardToken

        when:
        controller.signIn()

        then:
        1 * subject.login(asgardToken)
        controller.session[AuthController.AUTH_TARGET_URL] == null
        response.redirectUrl == '/test'
    }

    def 'sign in should redirect to index page if no target url'() {
        prepareAuthentication()

        when:
        controller.signIn()

        then:
        1 * subject.login(asgardToken)
        response.redirectUrl == '/'
    }

    def 'should return 401 if AuthenticationException thrown'() {
        prepareAuthentication()
        subject.login(asgardToken) >> { throw new AuthenticationException('Chaos!!')}

        when:
        controller.signIn()

        then:
        response.status == 401
        response.contentAsString == 'Authentication failed with message Chaos!!'
    }

    private prepareAuthentication() {
        pluginService.authenticationProvider >> authenticationProvider
        authenticationProvider.tokenFromRequest(_) >> asgardToken
    }
}
