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

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.InstanceState
import com.amazonaws.services.ec2.model.IpPermission
import com.amazonaws.services.ec2.model.Placement
import com.amazonaws.services.ec2.model.ReservedInstances
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.ec2.model.UserIdGroupPair
import com.netflix.asgard.model.SecurityGroupOption
import com.netflix.asgard.model.SimpleDbSequenceLocator
import com.netflix.asgard.model.ZoneAvailability
import spock.lang.Specification

@SuppressWarnings("GroovyPointlessArithmetic")
class AwsEc2ServiceSpec extends Specification {

    UserContext userContext
    AmazonEC2 mockAmazonEC2
    CachedMap mockSecurityGroupCache
    CachedMap mockInstanceCache
    CachedMap mockReservationCache
    AwsEc2Service awsEc2Service
    ConfigService configService
    TaskService taskService

    def setup() {
        userContext = UserContext.auto(Region.US_EAST_1)
        mockAmazonEC2 = Mock(AmazonEC2)
        mockSecurityGroupCache = Mock(CachedMap)
        mockInstanceCache = Mock(CachedMap)
        mockReservationCache = Mock(CachedMap)
        configService = Mock(ConfigService)
        taskService = new TaskService(awsSimpleDbService: new AwsSimpleDbService() {
            String incrementAndGetSequenceNumber(UserContext userContext, SimpleDbSequenceLocator locator) { '1' }
        }, grailsApplication: [config: [cloud: [:]]])

        Caches caches = new Caches(new MockCachedMapBuilder([
                (EntityType.security): mockSecurityGroupCache,
                (EntityType.instance): mockInstanceCache,
                (EntityType.reservation): mockReservationCache,
        ]))
        awsEc2Service = new AwsEc2Service(awsClient: new MultiRegionAwsClient({ mockAmazonEC2 }), caches: caches,
                configService: configService, taskService: taskService)
    }

    def 'active instances should only include pending and running states'() {
        mockInstanceCache.list() >> [
                new Instance(instanceId: 'i-papa', state: new InstanceState(name: 'pending')),
                new Instance(instanceId: 'i-smurfette', state: new InstanceState(name: 'running')),
                new Instance(instanceId: 'i-brainy', state: new InstanceState(name: 'shutting-down')),
                new Instance(instanceId: 'i-jokey', state: new InstanceState(name: 'terminated')),
                new Instance(instanceId: 'i-hefty', state: new InstanceState(name: 'stopping')),
                new Instance(instanceId: 'i-barber', state: new InstanceState(name: 'stopped')),
                new Instance(instanceId: 'i-grouchy', state: new InstanceState(name: 'running'))
        ]

        when:
        Collection<Instance> instances = awsEc2Service.getActiveInstances(userContext)

        then:
        instances*.instanceId.sort() == ['i-grouchy', 'i-papa', 'i-smurfette']
    }

    def 'zone availabilities should sum, group, and filter reservation counts and instance counts'() {
        mockReservationCache.list() >> [
                [instanceCount: 1, availabilityZone: 'us-east-1a', instanceType: 'm2.xlarge', state: 'active'],
                [instanceCount: 10, availabilityZone: 'us-east-1a', instanceType: 'm2.xlarge', state: 'active'],
                [instanceCount: 100, availabilityZone: 'us-east-1a', instanceType: 'm1.small', state: 'active'],
                [instanceCount: 1000, availabilityZone: 'us-east-1b', instanceType: 'm2.xlarge', state: 'active'],
                [instanceCount: 10000, availabilityZone: 'us-east-1a', instanceType: 'm2.xlarge', state: 'retired'],
                [instanceCount: 100000, availabilityZone: 'us-east-1a', instanceType: 'm2.xlarge', state: 'active']
        ].collect { new ReservedInstances(it) }
        Placement zoneA = new Placement(availabilityZone: 'us-east-1a')
        Placement zoneB = new Placement(availabilityZone: 'us-east-1b')
        Placement zoneC = new Placement(availabilityZone: 'us-east-1c')
        InstanceState running = new InstanceState(name: 'running')
        mockInstanceCache.list() >> [
                new Instance(instanceType: 'm2.xlarge', placement: zoneA, state: running),
                new Instance(instanceType: 'm2.xlarge', placement: zoneA, state: running),
                new Instance(instanceType: 'm2.xlarge', placement: zoneA, state: running),
                new Instance(instanceType: 'm1.small', placement: zoneA, state: running),
                new Instance(instanceType: 'm2.xlarge', placement: zoneB, state: running),
                new Instance(instanceType: 'm2.xlarge', placement: zoneB, state: running),
                new Instance(instanceType: 'm2.xlarge', placement: zoneB, state: running),
                new Instance(instanceType: 'm2.xlarge', placement: zoneB, state: running),
                new Instance(instanceType: 'm2.xlarge', placement: zoneC, state: running),
        ]

        when:
        List<ZoneAvailability> zoneAvailabilities = awsEc2Service.getZoneAvailabilities(userContext, 'm2.xlarge')

        then:
        zoneAvailabilities == [
                new ZoneAvailability(zoneName: 'us-east-1a', totalReservations: 100011, usedReservations: 3),
                new ZoneAvailability(zoneName: 'us-east-1b', totalReservations: 1000, usedReservations: 4),
                new ZoneAvailability(zoneName: 'us-east-1c', totalReservations: 0, usedReservations: 1),
        ]
    }

    def 'zone availability should be empty if there are no reservations'() {
        mockReservationCache.list() >> []
        Placement zoneA = new Placement(availabilityZone: 'us-east-1a')
        InstanceState running = new InstanceState(name: 'running')
        mockInstanceCache.list() >> [new Instance(instanceType: 'm2.xlarge', placement: zoneA, state: running)]

        when:
        List<ZoneAvailability> zoneAvailabilities = awsEc2Service.getZoneAvailabilities(userContext, 'm2.xlarge')

        then:
        zoneAvailabilities == []
    }

    def 'should get security group by name'() {
        SecurityGroup expectedSecurityGroup = new SecurityGroup(groupId: 'sg-123', groupName: 'super_secure')

        when:
        SecurityGroup actualSecurityGroup = awsEc2Service.getSecurityGroup(userContext, 'super_secure')

        then:
        actualSecurityGroup == expectedSecurityGroup
        1 * mockAmazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupNames: ['super_secure'])) >>
                new DescribeSecurityGroupsResult(securityGroups: [expectedSecurityGroup])
        1 * mockSecurityGroupCache.put('super_secure', expectedSecurityGroup) >> expectedSecurityGroup
    }

    def 'should get security group by ID'() {
        SecurityGroup expectedSecurityGroup = new SecurityGroup(groupId: 'sg-123', groupName: 'super_secure')

        when:
        SecurityGroup actualSecurityGroup = awsEc2Service.getSecurityGroup(userContext, 'sg-123')

        then:
        actualSecurityGroup == expectedSecurityGroup
        1 * mockAmazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupIds: ['sg-123'])) >>
                new DescribeSecurityGroupsResult(securityGroups: [expectedSecurityGroup])
        1 * mockSecurityGroupCache.put('super_secure', expectedSecurityGroup) >> expectedSecurityGroup
    }

    def 'should get security group by name from cache'() {
        SecurityGroup expectedSecurityGroup = new SecurityGroup(groupId: 'sg-123', groupName: 'super_secure')

        when:
        SecurityGroup actualSecurityGroup = awsEc2Service.getSecurityGroup(userContext, 'super_secure', From.CACHE)

        then:
        actualSecurityGroup == expectedSecurityGroup
        1 * mockSecurityGroupCache.get('super_secure') >> expectedSecurityGroup
        0 * _
    }

    def 'should get security group by ID from cache'() {
        SecurityGroup expectedSecurityGroup = new SecurityGroup(groupId: 'sg-123', groupName: 'super_secure')

        when:
        SecurityGroup actualSecurityGroup = awsEc2Service.getSecurityGroup(userContext, 'sg-123', From.CACHE)

        then:
        actualSecurityGroup == expectedSecurityGroup
        1 * mockSecurityGroupCache.list() >> [expectedSecurityGroup]
        1 * mockSecurityGroupCache.get('super_secure') >> expectedSecurityGroup
        0 * _
    }

    def 'should return null when getting security group by ID from empty cache'() {
        when:
        SecurityGroup actualSecurityGroup = awsEc2Service.getSecurityGroup(userContext, 'sg-123', From.CACHE)

        then:
        actualSecurityGroup == null
        1 * mockSecurityGroupCache.list() >> []
        1 * mockSecurityGroupCache.get(null)
        0 * _
    }

    def 'should put null in cache by name when security group is not found by name'() {
        when:
        SecurityGroup actualSecurityGroup = awsEc2Service.getSecurityGroup(userContext, 'super_secure')

        then:
        null == actualSecurityGroup
        1 * mockAmazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupNames: ['super_secure'])) >> {
            throw new AmazonServiceException('there is no security group like that')
        }
        1 * mockSecurityGroupCache.put('super_secure', null)
    }

    def 'should put null in cache by name when security group is not found by id'() {
        when:
        SecurityGroup actualSecurityGroup = awsEc2Service.getSecurityGroup(userContext, 'sg-123')

        then:
        null == actualSecurityGroup
        1 * mockAmazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupIds: ['sg-123'])) >> {
            throw new AmazonServiceException('there is no security group like that')
        }
        1 * mockSecurityGroupCache.list() >> [new SecurityGroup(groupId: 'sg-123', groupName: 'super_secure')]
        1 * mockSecurityGroupCache.put('super_secure', null)
    }

    def 'should put nothing in cache when security group is not found by id or in cache'() {
        when:
        SecurityGroup actualSecurityGroup =  awsEc2Service.getSecurityGroup(userContext, 'sg-123')

        then:
        null == actualSecurityGroup
        1 * mockAmazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupIds: ['sg-123'])) >> {
            throw new AmazonServiceException('there is no security group like that')
        }
        1 * mockSecurityGroupCache.list() >> [new SecurityGroup(groupId: 'sg-000', groupName: 'super_secure')]
        0 * mockSecurityGroupCache.put(_, _)
    }

    private List<SecurityGroup> simulateWarGames() {
        // wopr can call norad.
        // joshua can call wopr, globalthermonuclearwar, tictactoe.
        // modem and falken can call joshua.
        Closure pairs = { String groupName -> [new UserIdGroupPair(groupName: groupName)] }
        IpPermission woprPerm = new IpPermission(fromPort: 7101, toPort: 7102, userIdGroupPairs: pairs('wopr'))
        IpPermission joshuaPerm = new IpPermission(fromPort: 7101, toPort: 7102, userIdGroupPairs: pairs('joshua'))
        IpPermission falkenPerm = new IpPermission(fromPort: 7101, toPort: 7102, userIdGroupPairs: pairs('falken'))
        IpPermission modemPerm = new IpPermission(fromPort: 8080, toPort: 8080, userIdGroupPairs: pairs('modem'))
        [
                new SecurityGroup(groupName: 'norad', ipPermissions: [woprPerm]),
                new SecurityGroup(groupName: 'wopr', ipPermissions: [joshuaPerm]),
                new SecurityGroup(groupName: 'globalthermonuclearwar', ipPermissions: [joshuaPerm]),
                new SecurityGroup(groupName: 'tictactoe', ipPermissions: [joshuaPerm]),
                new SecurityGroup(groupName: 'joshua', ipPermissions: [modemPerm, falkenPerm]),
                new SecurityGroup(groupName: 'modem'),
                new SecurityGroup(groupName: 'falken'),
        ]
    }

    def 'options for target security group should include all groups sorted, but only some allowed to call target'() {

        List<SecurityGroup> warGamesSecurityGroups = simulateWarGames()
        SecurityGroup joshua = warGamesSecurityGroups.find { it.groupName == 'joshua' }

        when:
        List<SecurityGroupOption> joshuaCallers = awsEc2Service.getSecurityGroupOptionsForTarget(userContext, joshua)

        then:
        joshuaCallers == [
                new SecurityGroupOption('falken', 'joshua', true, '7101-7102'),
                new SecurityGroupOption('globalthermonuclearwar', 'joshua', false, '7001'),
                new SecurityGroupOption('joshua', 'joshua', false, '7001'),
                new SecurityGroupOption('modem', 'joshua', true, '8080'),
                new SecurityGroupOption('norad', 'joshua', false, '7001'),
                new SecurityGroupOption('tictactoe', 'joshua', false, '7001'),
                new SecurityGroupOption('wopr', 'joshua', false, '7001'),
        ]
        1 * mockSecurityGroupCache.list() >> { warGamesSecurityGroups }
    }

    def 'options for source group should include all groups sorted, but only some allowed to be called by source'() {

        when:
        List<SecurityGroupOption> gamesForJoshua = awsEc2Service.getSecurityGroupOptionsForSource(userContext, 'joshua')

        then:
        gamesForJoshua == [
                new SecurityGroupOption('joshua', 'falken', false, '7001'),
                new SecurityGroupOption('joshua', 'globalthermonuclearwar', true, '7101-7102'),
                new SecurityGroupOption('joshua', 'joshua', false, '7001'),
                new SecurityGroupOption('joshua', 'modem', false, '7001'),
                new SecurityGroupOption('joshua', 'norad', false, '7001'),
                new SecurityGroupOption('joshua', 'tictactoe', true, '7101-7102'),
                new SecurityGroupOption('joshua', 'wopr', true, '7101-7102'),
        ]
        1 * mockSecurityGroupCache.list() >> { simulateWarGames() }
    }

    def 'updating a security group relationship should result in one task, one auth, and one revoke'() {

        List<UserIdGroupPair> pairs = [new UserIdGroupPair(groupName: 'modem')]
        List<IpPermission> existingPerms = [
                new IpPermission(ipProtocol: 'tcp', fromPort: 5000, toPort: 5001, userIdGroupPairs: pairs),
                new IpPermission(ipProtocol: 'tcp', fromPort: 6000, toPort: 6001, userIdGroupPairs: pairs)
        ]
        SecurityGroup targetGroup = new SecurityGroup(groupName: 'joshua', ipPermissions: existingPerms)
        int tasksDoneBefore = taskService.getCompleted().size()

        when:
        awsEc2Service.updateSecurityGroupPermissions(userContext, targetGroup, 'modem', '7003,8080')

        then:
        taskService.getCompleted().size() == tasksDoneBefore + 1 // Cannot use a spock mock because closure needs to run
        1 * mockAmazonEC2.authorizeSecurityGroupIngress(new AuthorizeSecurityGroupIngressRequest(
                groupName: targetGroup.groupName, ipPermissions: [
                        new IpPermission(ipProtocol: 'tcp', fromPort: 7003, toPort: 7003, userIdGroupPairs: pairs),
                        new IpPermission(ipProtocol: 'tcp', fromPort: 8080, toPort: 8080, userIdGroupPairs: pairs),
                ]
        ))
        1 * mockAmazonEC2.revokeSecurityGroupIngress(new RevokeSecurityGroupIngressRequest(
                groupName: targetGroup.groupName, ipPermissions: existingPerms
        ))
        1 * mockAmazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupNames: ['joshua'])) >> {
            new DescribeSecurityGroupsResult(securityGroups: [targetGroup])
        }
        1 * configService.getAwsAccountNumber()
        1 * mockSecurityGroupCache.put('joshua', targetGroup)
        0 * _._
    }
}
