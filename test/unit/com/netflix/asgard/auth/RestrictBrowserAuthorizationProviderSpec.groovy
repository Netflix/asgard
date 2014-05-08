/*
 * Copyright 2014 Netflix, Inc.
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

import javax.servlet.http.HttpServletRequest
import org.apache.shiro.SecurityUtils
import org.apache.shiro.subject.Subject
import spock.lang.Specification

/**
 * Tests for RestrictBrowserAuthorizationProvider.
 */
class RestrictBrowserAuthorizationProviderSpec extends Specification {

    @SuppressWarnings("GroovyAssignabilityCheck")
    def "should determine if a request is authorized based on its current session status and whether it's a browser"() {

        HttpServletRequest request = Mock(HttpServletRequest)
        Subject subject = Mock(Subject)
        RestrictBrowserAuthorizationProvider provider = new RestrictBrowserAuthorizationProvider()
        SecurityUtils.metaClass.static.getSubject = { subject }

        when:
        authorized = provider.isAuthorized(request, 'instance', 'list')

        then:
        subject.authenticated >> authenticated
        request.getHeader('user-agent') >> userAgent

        where:
        authorized | authenticated | userAgent
        true       | true          | 'curl'
        true       | true          | 'Mozilla'
        true       | false         | 'Mozilla'
        false      | false         | 'curl'
    }
}
