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

import javax.servlet.http.HttpServletRequest
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

@SuppressWarnings("GroovyAssignabilityCheck") class GroupCreateCommand {

    def applicationService
    def awsAutoScalingService

    String appName
    String stack
    String newStack
    String detail

    static constraints = {

        appName(nullable: false, blank: false, validator: { value, command ->
            HttpServletRequest request = Requests.getRequest()
            UserContext userContext = UserContext.of(request)
            if (!Relationships.checkName(value)) {
                return "application.name.illegalChar"
            }
            if (Relationships.usesReservedFormat(value)) {
                return "name.usesReservedFormat"
            }
            if (!command.applicationService.getRegisteredApplication(userContext, value)) {
                return "application.name.nonexistent"
            }

            String groupName = Relationships.buildGroupName(new GrailsParameterMap(request))
            if (groupName.length() > Relationships.GROUP_NAME_MAX_LENGTH) {
                return ['compoundName.invalid.max.size', groupName, Relationships.GROUP_NAME_MAX_LENGTH]
            }
            if (command.awsAutoScalingService.getAutoScalingGroup(userContext, groupName)) {
                return ['groupName.exists', groupName]
            }
        })

        stack(nullable: true, validator: { value, command ->
            if (value && !Relationships.checkName(value)) {
                return "The stack must be empty or consist of alphanumeric characters"
            }
            if (Relationships.usesReservedFormat(value)) {
                return "name.usesReservedFormat"
            }
        })

        newStack(nullable: true, validator: { value, command ->
            if (value && !Relationships.checkName(value)) {
                return "stack.illegalChar"
            }
            if (value && command.stack) {
                return "stack.matchesNewStack"
            }
            if (Relationships.usesReservedFormat(value)) {
                return "name.usesReservedFormat"
            }
        })

        detail(nullable: true, validator: { value, command ->
            if (value && !Relationships.checkDetail(value)) {
                return "The detail must be empty or consist of alphanumeric characters and hyphens"
            }
            if (Relationships.usesReservedFormat(value)) {
                return "name.usesReservedFormat"
            }
        })

    }
}
