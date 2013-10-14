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

import grails.validation.Validateable

@Validateable @SuppressWarnings("GroovyAssignabilityCheck") class GroupCreateCommand {

    def applicationService
    def awsAutoScalingService
    def cloudReadyService

    String appName
    String stack
    String newStack
    String detail
    String chaosMonkey
    String region
    String imageId
    boolean requestedFromGui
    boolean appWithClusterOptLevel

    static constraints = {
        appName(nullable: false, blank: false, validator: validateAppName)
        stack(nullable: true, validator: validateStack)
        newStack(nullable: true, validator: validateNewStack)
        detail(nullable: true, validator: validateDetail)
        chaosMonkey(nullable: true, validator: validateChaosMonkey)
        imageId(nullable: false, blank: false)
    }

    private static Closure validateAppName = { value, command ->
        if (!Relationships.checkName(value)) {
            return "application.name.illegalChar"
        }
        if (Relationships.usesReservedFormat(value)) {
            return "name.usesReservedFormat"
        }
        if (!command.applicationService.getRegisteredApplication(null, value)) {
            return "application.name.nonexistent"
        }

        String groupName = Relationships.buildGroupName(command.properties)
        if (groupName.length() > Relationships.GROUP_NAME_MAX_LENGTH) {
            return ['compoundName.invalid.max.size', groupName, Relationships.GROUP_NAME_MAX_LENGTH]
        }
        Region region = Region.withCode(command.region)
        if (command.awsAutoScalingService.getAutoScalingGroup(new UserContext(region: region), groupName)) {
            return ['groupName.exists', groupName]
        }
    }

    private static Closure validateStack = { value, command ->
        if (value && !Relationships.checkName(value)) {
            return "The stack must be empty or consist of alphanumeric characters"
        }
        if (Relationships.usesReservedFormat(value)) {
            return "name.usesReservedFormat"
        }
    }

    private static Closure validateNewStack = { value, command ->
        if (value && !Relationships.checkName(value)) {
            return "stack.illegalChar"
        }
        if (value && command.stack) {
            return "stack.matchesNewStack"
        }
        if (Relationships.usesReservedFormat(value)) {
            return "name.usesReservedFormat"
        }
    }

    private static Closure validateDetail = { value, command ->
        if (value && !Relationships.checkDetail(value)) {
            return "The detail must be empty or consist of alphanumeric characters and hyphens"
        }
        if (Relationships.usesReservedFormat(value)) {
            return "name.usesReservedFormat"
        }
    }

    private static Closure validateChaosMonkey = { value, command ->
        if (!command.chaosMonkey) {
            Region region = Region.withCode(command.region)
            boolean isChaosMonkeyChoiceNeglected = command.cloudReadyService.isChaosMonkeyActive(region) &&
                    command.requestedFromGui && command.appWithClusterOptLevel
            if (isChaosMonkeyChoiceNeglected) {
                return "chaosMonkey.optIn.missing.error"
            }
        }
    }
}
