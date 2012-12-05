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

import spock.lang.Specification
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.ec2.model.UserIdGroupPair
import com.amazonaws.services.ec2.model.IpPermission

class SecurityGroupDslSpec extends Specification {

    def 'should create Security Groups with ingress permissions'() {
        // wopr can call norad.
        // joshua can call wopr, globalthermonuclearwar, tictactoe.
        // modem and falken can call joshua.
        Closure pairs = { String groupId -> [new UserIdGroupPair(groupId: groupId)] }
        IpPermission woprPerm = new IpPermission(fromPort: 7101, toPort: 7102, userIdGroupPairs: pairs('sg-0'))
        IpPermission joshuaPerm = new IpPermission(fromPort: 7101, toPort: 7102, userIdGroupPairs: pairs('sg-2'))
        IpPermission falkenPerm = new IpPermission(fromPort: 7101, toPort: 7102, userIdGroupPairs: pairs('sg-6'))
        IpPermission modemPerm = new IpPermission(fromPort: 8080, toPort: 8080, userIdGroupPairs: pairs('sg-5'))
        Collection<SecurityGroup> expectedSecurityGroups = [
                new SecurityGroup(groupId: 'sg-1', groupName: 'norad', ipPermissions: [woprPerm]),
                new SecurityGroup(groupId: 'sg-0', groupName: 'wopr', ipPermissions: [joshuaPerm]),
                new SecurityGroup(groupId: 'sg-3', groupName: 'globalthermonuclearwar', ipPermissions: [joshuaPerm]),
                new SecurityGroup(groupId: 'sg-4', groupName: 'tictactoe', ipPermissions: [joshuaPerm]),
                new SecurityGroup(groupId: 'sg-2', groupName: 'joshua', ipPermissions: [modemPerm, falkenPerm]),
                new SecurityGroup(groupId: 'sg-5', groupName: 'modem'),
                new SecurityGroup(groupId: 'sg-6', groupName: 'falken'),
        ] as Set

        expect:
        SecurityGroupDsl.config {
            wopr(7101, 7102) withIngress norad
            joshua(7101, 7102) withIngress([wopr, globalthermonuclearwar, tictactoe])
            modem(8080, 8080) withIngress joshua
            falken(7101, 7102) withIngress joshua
        } == expectedSecurityGroups
    }

    def 'should create Security Groups with egress permissions'() {
        Closure pairs = { String groupId -> [new UserIdGroupPair(groupId: groupId)] }
        IpPermission woprPerm = new IpPermission(fromPort: 7101, toPort: 7102, userIdGroupPairs: pairs('sg-0'))
        Collection<SecurityGroup> expectedSecurityGroups = [
                new SecurityGroup(groupId: 'sg-1', groupName: 'norad', ipPermissionsEgress: [woprPerm]),
                new SecurityGroup(groupId: 'sg-0', groupName: 'wopr'),
        ] as Set

        expect:
        SecurityGroupDsl.config {
            wopr(7101, 7102) withEgress norad
        } == expectedSecurityGroups
    }

    def 'should create Security Groups with ingress and egress permissions'() {
        Closure pairs = { String groupId -> [new UserIdGroupPair(groupId: groupId)] }
        IpPermission woprIngress = new IpPermission(fromPort: 7101, toPort: 7102, userIdGroupPairs: pairs('sg-0'))
        IpPermission woprEgress = new IpPermission(fromPort: 7201, toPort: 7202, userIdGroupPairs: pairs('sg-0'))
        Collection<SecurityGroup> expectedSecurityGroups = [
                new SecurityGroup(groupId: 'sg-1', groupName: 'norad', ipPermissions: [woprIngress],
                        ipPermissionsEgress: [woprEgress]),
                new SecurityGroup(groupId: 'sg-0', groupName: 'wopr'),
        ] as Set

        expect:
        SecurityGroupDsl.config {
            wopr(7101, 7102) withIngress norad
            wopr(7201, 7202) withEgress norad
        } == expectedSecurityGroups
    }

}
