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

import groovy.transform.Immutable
import javax.servlet.http.HttpServletRequest
import org.apache.shiro.SecurityUtils
import org.apache.shiro.UnavailableSecurityManagerException
import org.codehaus.jackson.annotate.JsonCreator
import org.codehaus.jackson.annotate.JsonProperty

@Immutable final class UserContext {

    /** The unique identifier of the issue tracker ticket for external recording of system changes. */
    String ticket

    /** The system-wide unique id of a logged in user. */
    String username

    /** The best guess, human readable, canonical network name of the machine that sent the request. */
    String clientHostName

    /** The IP address of the machine that sent the request. */
    String clientIpAddress

    /** The AWS region that the request is intended to focus on. */
    Region region

    /**
     * Indicates whether this UserContext is for an external agent starting a task or an internal process that
     * runs automatically and therefore should not require a tracking code.
     */
    Boolean internalAutomation

    @JsonCreator
    static UserContext of(@JsonProperty('ticket') String ticket, @JsonProperty('username') String username,
              @JsonProperty('clientHostName') String clientHostName,
              @JsonProperty('clientIpAddress') String clientIpAddress,
              @JsonProperty('region') Region region, @JsonProperty('internalAutomation') Boolean internalAutomation) {
        new UserContext(
                ticket: ticket,
                username: username,
                clientHostName: clientHostName,
                clientIpAddress: clientIpAddress,
                region: region,
                internalAutomation: internalAutomation
        )
    }

    /** Factory method takes an HttpServletRequest or a MockHttpServletRequest */
    static of(HttpServletRequest request) {
        String username = null
        try {
            username = SecurityUtils.subject?.principal?.toString()
        } catch (UnavailableSecurityManagerException ignored) {
            // Keep null username if it cannot be found.
        }
        new UserContext(
                ticket: request?.getParameter('ticket')?.trim() ?: request?.getParameter('cmc')?.trim(),
                username: username,
                clientHostName: Requests.getClientHostName(request),
                clientIpAddress: Requests.getClientIpAddress(request),
                region: request?.getAttribute('region') as Region,
                internalAutomation: false
        )
    }

    /**
     * Factory method for a user context for automatic operations that occur without any external agent call.
     *
     * @param region the AWS region on which the current operation should execute
     * @return UserContext a new context object
     */
    static auto(Region region = Region.defaultRegion()) {
        new UserContext(region: region, internalAutomation: true)
    }

    /**
     * Clone the current user context object with all its fields, but replace the Region.
     *
     * @param region the region to use for the returned UserContext object
     * @return UserContext the cloned object with a replaced Region
     */
    UserContext withRegion(Region region) {
        Map constructorProperties = [region: region]
        this.properties.each { String name, def value ->
            if (this[name] && name != 'metaClass' && name != 'region') { constructorProperties[name] = value }
        }
        new UserContext(constructorProperties)
    }
}
