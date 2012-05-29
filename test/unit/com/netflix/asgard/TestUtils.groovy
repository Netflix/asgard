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

import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder

class TestUtils {

    static void setUpMockRequest(Map params = [:]) {
        MockHttpServletRequest mockRequest = getOrCreateMockRequest()
        params.each { String name, String value ->
            mockRequest.setParameter(name, value as String)
        }
        mockRequest.region = Region.defaultRegion()
        mockRequest.env = 'test'
    }

    private static MockHttpServletRequest getOrCreateMockRequest() {
        GrailsWebRequest webRequest = RequestContextHolder.getRequestAttributes() as GrailsWebRequest
        MockHttpServletRequest mockRequest = webRequest?.getRequest() as MockHttpServletRequest
        if (!mockRequest) {
            mockRequest = new GrailsMockHttpServletRequest()
            RequestContextHolder.setRequestAttributes(new GrailsWebRequest(mockRequest,
                    new GrailsMockHttpServletResponse(), new MockServletContext()))
        }
        mockRequest
    }
}
