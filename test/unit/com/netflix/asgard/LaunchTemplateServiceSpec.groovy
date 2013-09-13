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

class LaunchTemplateServiceSpec extends Specification {

    ConfigService mockConfigService = Mock(ConfigService)
    CachedMap mockSecurityGroupCache = Mock(CachedMap)
    Caches caches
    LaunchTemplateService launchTemplateService

    def setup() {
        caches = new Caches(new MockCachedMapBuilder([(EntityType.security): mockSecurityGroupCache]))
        launchTemplateService = new LaunchTemplateService(configService: mockConfigService, caches: caches)
    }

    def 'should include default security group names'() {
        when:
        Set<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups([])

        then:
        securityGroups == ['dsg1', 'dsg2'] as Set
        1 * mockConfigService.defaultSecurityGroups >> ['dsg1', 'dsg2']
    }

    def 'should not include duplicate security group names'() {
        when:
        Set<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups(['sg1', 'dsg2'])

        then:
        securityGroups == ['sg1', 'dsg1', 'dsg2'] as Set
        1 * mockConfigService.defaultSecurityGroups >> ['dsg1', 'dsg2']
    }

    def 'should include default VPC security group IDs'() {
        when:
        Set<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups([], true, Region.US_EAST_1)

        then:
        securityGroups == ['sg-101', 'sg-102'] as Set
        1 * mockConfigService.defaultVpcSecurityGroupNames >> ['dsg1', 'dsg2']
        1 * mockSecurityGroupCache.get('dsg1') >> new SecurityGroup(groupId: 'sg-101')
        1 * mockSecurityGroupCache.get('dsg2') >> new SecurityGroup(groupId: 'sg-102')
    }

    def 'should not include duplicate VPC security group IDs'() {
        when:
        Set<String> securityGroups = launchTemplateService.
                includeDefaultSecurityGroups(['sg1', 'sg-102'], true, Region.US_EAST_1)

        then:
        securityGroups == ['sg1', 'sg-101', 'sg-102'] as Set
        1 * mockConfigService.defaultVpcSecurityGroupNames >> ['dsg1', 'dsg2']
        1 * mockSecurityGroupCache.get('dsg1') >> new SecurityGroup(groupId: 'sg-101')
        1 * mockSecurityGroupCache.get('dsg2') >> new SecurityGroup(groupId: 'sg-102')
    }

    def 'should not include default VPC security group IDs not in cache'() {
        when:
        Set<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups(['sg1'], true,
                Region.US_EAST_1)

        then:
        securityGroups == ['sg1', 'sg-101'] as Set
        1 * mockConfigService.defaultVpcSecurityGroupNames >> ['dsg1', 'dsg2']
        1 * mockSecurityGroupCache.get('dsg1') >> new SecurityGroup(groupId: 'sg-101')
        1 * mockSecurityGroupCache.get('dsg2') >> null
    }

    def 'should consistently use id rather than name when ids are specified'() {
        mockConfigService.defaultSecurityGroups >> ['dsg1']
        mockSecurityGroupCache.get('dsg1') >> new SecurityGroup(groupId: 'sg-101')

        when:
        Set<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups(['sg-100'])

        then:
        securityGroups == ['sg-100', 'sg-101'] as Set
    }

    def 'should consistently use name rather than id when names are specified'() {
        mockConfigService.defaultSecurityGroups >> ['dsg1']
        mockSecurityGroupCache.get('dsg1') >> new SecurityGroup(groupId: 'sg-101')

        when:
        Set<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups(['sg1'])

        then:
        securityGroups == ['sg1', 'dsg1'] as Set
    }
}
