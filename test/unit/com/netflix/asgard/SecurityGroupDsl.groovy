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

import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.ec2.model.IpPermission
import com.amazonaws.services.ec2.model.UserIdGroupPair

class SecurityGroupDsl {

    Map<String, SecurityGroupAttributes> securityGroupAttributesByName = [:]

    /**
     * Evaluates DSLs describing security groups. Example:
     *  modem can call joshua on port 8080
     *  joshua can call globalthermonuclearwar, and tictactoe from port 7101 to port 7102
     *
     *      SecurityGroupDsl.config {
     *          modem(8080, 8080) >> joshua
     *          joshua(7101, 7102) >> [globalthermonuclearwar, tictactoe]
     *      }
     *
     * @param closure DSL describing security groups and their relationships
     * @return security groups created from the DSL
     */
    def static Collection<SecurityGroup> config(closure) {
        SecurityGroupDsl securityGroupDsl = new SecurityGroupDsl()
        closure.delegate = securityGroupDsl
        closure()
        securityGroupDsl.securityGroupAttributesByName.values()*.securityGroup as Set
    }

    def methodMissing(String name, args) {
        SecurityGroupAttributes securityGroupAttributes = propertyMissing(name)
        List<UserIdGroupPair> pairs = [new UserIdGroupPair(groupId: securityGroupAttributes.securityGroup.groupId)]
        IpPermission ipPermission = new IpPermission(fromPort: args[0], toPort: args[1], userIdGroupPairs: pairs)
        securityGroupAttributes.ipPermission = ipPermission
        securityGroupAttributes
    }

    def propertyMissing(String name) {
        SecurityGroupAttributes securityGroupAttributes = securityGroupAttributesByName[name]
        if (!securityGroupAttributes) {
            int index = securityGroupAttributesByName.size()
            SecurityGroup securityGroup = new SecurityGroup(groupId: "sg-${index}", groupName: name)
            securityGroupAttributes = new SecurityGroupAttributes()
            securityGroupAttributes.securityGroup = securityGroup
            securityGroupAttributesByName[name] = securityGroupAttributes
        }
        securityGroupAttributes
    }

    private static class SecurityGroupAttributes {
        SecurityGroup securityGroup
        IpPermission ipPermission

        def rightShift(callees) {
            callees.each {
                it.securityGroup.withIpPermissions(ipPermission)
            }
        }
    }
}
