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

import com.fasterxml.jackson.databind.ObjectMapper
import javax.servlet.http.HttpServletRequest
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification

class UserContextSpec extends Specification {

    void 'should create a UserContext from a servlet request'() {
        HttpServletRequest request = new MockHttpServletRequest()
        request.setAttribute('region', Region.US_EAST_1)
        request.setParameter('ticket', 'CMC-123')

        when:
        UserContext userContext = UserContext.of(request)

        then:
        userContext.region == Region.US_EAST_1
        userContext.ticket == 'CMC-123'
        userContext.clientHostName == 'localhost'
        userContext.clientIpAddress == 'localhost'
    }

    void 'should create a new UserContext object when specifying a new region for an old UserContext'() {

        when:
        UserContext userContext = UserContext.auto(Region.US_EAST_1)

        then:
        userContext.region == Region.US_EAST_1

        when:
        UserContext singaporeContext = userContext.withRegion(Region.AP_SOUTHEAST_1)
        singaporeContext.region == Region.AP_SOUTHEAST_1

        then: 'original is unchanged'
        userContext.region == Region.US_EAST_1
    }

    void 'Jackson should be able to make a UserContext from a JSON string'() {
        String json = '\
                {"ticket":"CMC-123","username":"hsimpson","clientHostName":"laptop-hsimpson",\
                "clientIpAddress":"1.2.3.4","region":"' + region + '"}'.stripIndent()

        when:
        ObjectMapper mapper = new ObjectMapper()
        UserContext userContext = mapper.readValue(json, UserContext)

        then:
        userContext == new UserContext(ticket: 'CMC-123', region: Region.US_WEST_2, username: 'hsimpson',
                clientHostName: 'laptop-hsimpson', clientIpAddress: '1.2.3.4')

        where:
        region      | explanation
        'US_WEST_2' | 'legacy json blobs with Region enum names are still supported'
        'us-west-2' | 'newer json blobs with Region code values are preferred'
    }
}
