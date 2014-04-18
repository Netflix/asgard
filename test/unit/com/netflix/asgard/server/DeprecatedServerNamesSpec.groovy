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
package com.netflix.asgard.server

import com.netflix.asgard.ConfigService
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests for DeprecatedServerNames.
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class DeprecatedServerNamesSpec extends Specification {

    ConfigService configService = Mock(ConfigService)
    DeprecatedServerNames deprecatedServerNames = new DeprecatedServerNames(configService: configService)

    @Unroll("should create pattern #pattern for map #map and URL #url should have match #match and matches=#ok")
    void 'should create a regular expression pattern for matching deprecated server names in URLs'() {

        when:
        Pattern regexPattern = deprecatedServerNames.createPatternForMatchingDeprecatedUrl()
        Matcher matcher = regexPattern.matcher(url)

        then:
        configService.deprecatedServerNamesToReplacements >> map
        regexPattern.pattern() == pattern
        matcher.matches() == ok
        match == (ok ? matcher.group(1) : null)

        where:
        ok    | pattern                          | map                                   | url                | match
        false | '(?!)'                           | [:]                                   | 'http://nac/'      | null
        false | '.*[:][/][/](nac)[:/?].*'        | ['nac': 'asgard']                     | 'http://asgard/'   | null
        true  | '.*[:][/][/](nac)[:/?].*'        | ['nac': 'asgard']                     | 'http://nac/'      | 'nac'
        true  | '.*[:][/][/](nac.yo|nac)[:/?].*' | ['nac.yo': 'asgard', 'nac': 'asgard'] | 'http://nac/'      | 'nac'
        true  | '.*[:][/][/](nac.yo|nac)[:/?].*' | ['nac.yo': 'asgard', 'nac': 'asgard'] | 'http://nac:8080'  | 'nac'
        true  | '.*[:][/][/](nac.yo|nac)[:/?].*' | ['nac.yo': 'asgard', 'nac': 'asgard'] | 'http://nac/okay'  | 'nac'
        true  | '.*[:][/][/](nac.yo|nac)[:/?].*' | ['nac.yo': 'asgard', 'nac': 'asgard'] | 'http://nac?a=b'   | 'nac'
        true  | '.*[:][/][/](nac.yo|nac)[:/?].*' | ['nac.yo': 'asgard', 'nac': 'asgard'] | 'http://nac/?a=b'  | 'nac'
        true  | '.*[:][/][/](nac.yo|nac)[:/?].*' | ['nac.yo': 'asgard', 'nac': 'asgard'] | 'http://nac.yo/'   | 'nac.yo'
        true  | '.*[:][/][/](nac.yo|nac)[:/?].*' | ['nac.yo': 'asgard', 'nac': 'asgard'] | 'http://nac.yo:80' | 'nac.yo'
        true  | '.*[:][/][/](nac.yo|nac)[:/?].*' | ['nac.yo': 'asgard', 'nac': 'asgard'] | 'http://nac.yo?'   | 'nac.yo'
        true  | '.*[:][/][/](nac.yo|nac)[:/?].*' | ['nac.yo': 'asgard', 'nac': 'asgard'] | 'http://nac.yo/ok' | 'nac.yo'
        true  | '.*[:][/][/](nac.yo|nac)[:/?].*' | ['nac.yo': 'asgard', 'nac': 'asgard'] | 'https://nac/ok'   | 'nac'
    }

    @Unroll("should redirect to #redirectTo if method=#method, agent=#agent, URL=#requested, mapping=#map")
    void 'should replace a deprecated server name with a replacement'() {
        HttpServletRequestWrapper request = Mock(HttpServletRequestWrapper)

        when:
        deprecatedServerNames.afterPropertiesSet()
        String result = deprecatedServerNames.replaceDeprecatedServerName(request)

        then:
        result == redirectTo
        configService.deprecatedServerNamesToReplacements >> map
        request.getHeader('user-agent') >> agent
        request.method >> method
        request.requestURL >> new StringBuffer(requested)

        where:
        method | agent                     | requested              | map               | redirectTo
        'GET'  | 'Mozilla/5.0'             | 'http://nac/health'    | ['nac': 'asgard'] | 'http://asgard/health'
        'GET'  | '(KHTML) Chrome/32.16'    | 'http://nac/health'    | ['nac': 'asgard'] | 'http://asgard/health'
        'GET'  | 'Opera 123'               | 'http://nac/health'    | ['nac': 'asgard'] | 'http://asgard/health'
        'GET'  | 'Safari/537.36'           | 'http://nac/health'    | ['nac': 'asgard'] | 'http://asgard/health'
        'GET'  | '(compatible; MSIE 8.0;)' | 'http://nac/health'    | ['nac': 'asgard'] | 'http://asgard/health'
        'GET'  | 'Firefox'                 | 'http://nac/health'    | ['nac': 'asgard'] | 'http://asgard/health'
        'GET'  | 'Firefox'                 | 'https://nac/health'   | ['nac': 'asgard'] | 'https://asgard/health'
        'GET'  | 'Firefox'                 | 'http://nac/zippy?a=b' | ['nac': 'asgard'] | 'http://asgard/zippy?a=b'
        'GET'  | 'curl/7.21.1 libcurl/7'   | 'http://nac/health'    | ['nac': 'asgard'] | null
        'GET'  | ''                        | 'http://nac/health'    | ['nac': 'asgard'] | null
        'GET'  | null                      | 'http://nac/health'    | ['nac': 'asgard'] | null
        'PUT'  | 'Firefox'                 | 'http://nac/health'    | ['nac': 'asgard'] | null
        'POST' | 'Firefox'                 | 'http://nac/health'    | ['nac': 'asgard'] | null
        'HEAD' | 'Firefox'                 | 'http://nac/health'    | ['nac': 'asgard'] | null
        'GET'  | 'Firefox'                 | 'http://asgard/health' | ['nac': 'asgard'] | null
        'GET'  | 'Firefox'                 | 'http://nac/health'    | [:]               | null
    }

    void 'should get the replacement server name for a deprecated server name'() {

        when:
        String result = deprecatedServerNames.getReplacementServerName(requested)

        then:
        1 * configService.deprecatedServerNamesToReplacements >> map
        result == replacement
        0 * _

        where:
        requested | replacement | map
        'nac'     | 'asgard'    | ['nac': 'asgard']
        'asgard'  | null        | ['nac': 'asgard']
        'asgard'  | null        | [:]
    }

    void 'should get the full URL of a request with a query string'() {
        HttpServletRequestWrapper request = Mock(HttpServletRequestWrapper)

        when:
        String result = deprecatedServerNames.getFullUrl(request)

        then:
        2 * request.requestURL >> new StringBuffer('http://asgard/healthcheck')
        1 * request.queryString >> query
        result == url
        0 * _

        where:
        query     | url
        'a=b&c=d' | 'http://asgard/healthcheck?a=b&c=d'
        ''        | 'http://asgard/healthcheck'
        null      | 'http://asgard/healthcheck'
    }

    void 'should get nested request that is a wrapper but has a browser URL'() {
        HttpServletRequestWrapper wrapper1 = Mock(HttpServletRequestWrapper)
        HttpServletRequestWrapper wrapper2 = Mock(HttpServletRequestWrapper)

        when:
        def result = deprecatedServerNames.getNestedRequestWithActualUrl(wrapper2)

        then:
        result == wrapper1
        1 * wrapper2.requestURL >> new StringBuffer('http://asgard/grails/healthcheck.dispatch')
        1 * wrapper2.request >> wrapper1
        1 * wrapper1.requestURL >> new StringBuffer('http://asgard/healthcheck')
        0 * _
    }

    void 'should get nested request that is not a wrapper, two layers deep'() {
        HttpServletRequest innerRequest = Mock(HttpServletRequest)
        HttpServletRequestWrapper wrapper1 = Mock(HttpServletRequestWrapper)
        HttpServletRequestWrapper wrapper2 = Mock(HttpServletRequestWrapper)

        when:
        def result = deprecatedServerNames.getNestedRequestWithActualUrl(wrapper2)

        then:
        result == innerRequest
        1 * wrapper2.requestURL >> new StringBuffer('http://asgard/grails/healthcheck.dispatch')
        1 * wrapper2.request >> wrapper1
        1 * wrapper1.requestURL >> new StringBuffer('http://asgard/grails/healthcheck.dispatch')
        1 * wrapper1.request >> innerRequest
        0 * _
    }

    void 'should get nested request that is not a wrapper, one layer deep'() {
        HttpServletRequest innerRequest = Mock(HttpServletRequest)
        HttpServletRequestWrapper wrapper = Mock(HttpServletRequestWrapper)

        when:
        def result = deprecatedServerNames.getNestedRequestWithActualUrl(wrapper)

        then:
        result == innerRequest
        1 * wrapper.requestURL >> new StringBuffer('http://asgard/grails/healthcheck.dispatch')
        1 * wrapper.request >> innerRequest
        0 * _
    }

    void 'should get request that is not a wrapper, at the top layer'() {
        HttpServletRequest request = Mock(HttpServletRequest)

        when:
        def result = deprecatedServerNames.getNestedRequestWithActualUrl(request)

        then:
        result == request
        0 * _
    }
}
