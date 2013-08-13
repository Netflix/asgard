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
import com.amazonaws.services.ec2.model.DescribeSubnetsResult
import com.amazonaws.services.ec2.model.GroupIdentifier
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.InstanceState
import com.amazonaws.services.ec2.model.IpPermission
import com.amazonaws.services.ec2.model.Placement
import com.amazonaws.services.ec2.model.ReservedInstances
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.ec2.model.Subnet
import com.amazonaws.services.ec2.model.Tag
import com.amazonaws.services.ec2.model.UserIdGroupPair
import com.google.common.collect.ImmutableList
import com.netflix.asgard.model.SecurityGroupOption
import com.netflix.asgard.model.SubnetData
import com.netflix.asgard.model.SubnetTarget
import com.netflix.asgard.model.ZoneAvailability
import spock.lang.Specification
import spock.lang.Unroll

class AwsEc2ServiceUnitSpec extends Specification {

    UserContext userContext
    AmazonEC2 mockAmazonEC2
    CachedMap mockSecurityGroupCache
    CachedMap mockInstanceCache
    CachedMap mockReservationCache
    AwsEc2Service awsEc2Service

    def setup() {
        userContext = UserContext.auto(Region.US_EAST_1)
        mockAmazonEC2 = Mock(AmazonEC2)
        mockSecurityGroupCache = Mock(CachedMap)
        mockInstanceCache = Mock(CachedMap)
        mockReservationCache = Mock(CachedMap)
        Caches caches = new Caches(new MockCachedMapBuilder([
                (EntityType.security): mockSecurityGroupCache,
                (EntityType.instance): mockInstanceCache,
                (EntityType.reservation): mockReservationCache,
        ]))
        TaskService taskService = new TaskService() {
            def runTask(UserContext userContext, String name, Closure work, Link link = null) {
                work(new Task())
            }
        }
        awsEc2Service = new AwsEc2Service(awsClient: new MultiRegionAwsClient({ mockAmazonEC2 }), caches: caches,
                taskService: taskService)
    }

    @Unroll("""getInstancesWithSecurityGroup should return #instanceIds when groupId is #groupId \
and groupName is #groupName""")
    def 'should get the instances for a specified security group by name or id'() {
        GroupIdentifier apiGroup = new GroupIdentifier(groupName: 'api')
        GroupIdentifier cassGroup = new GroupIdentifier(groupName: 'cass')
        GroupIdentifier idGroup = new GroupIdentifier(groupId: 'sg-12345678')
        awsEc2Service = Spy(AwsEc2Service) {
            getInstances(_) >> {
                [
                        new Instance(instanceId: 'i-deadbeef', securityGroups: [apiGroup, idGroup]),
                        new Instance(instanceId: 'i-ba5eba11', securityGroups: []),
                        new Instance(instanceId: 'i-cafebabe', securityGroups: [apiGroup, cassGroup]),
                        new Instance(instanceId: 'i-f005ba11', securityGroups: [apiGroup]),
                        new Instance(instanceId: 'i-ca55e77e', securityGroups: [apiGroup, cassGroup]),
                        new Instance(instanceId: 'i-b01dface', securityGroups: [idGroup])
                ]
            }
        }
        SecurityGroup securityGroup = new SecurityGroup(groupName: groupName, groupId: groupId)
        UserContext userContext = UserContext.auto(Region.US_WEST_1)

        expect:
        instanceIds == awsEc2Service.getInstancesWithSecurityGroup(userContext, securityGroup)*.instanceId

        where:
        groupId       | groupName | instanceIds
        'sg-12345678' | null      | ['i-deadbeef', 'i-b01dface']
        null          | 'api'     | ['i-deadbeef', 'i-cafebabe', 'i-f005ba11', 'i-ca55e77e']
        null          | 'cass'    | ['i-cafebabe', 'i-ca55e77e']
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
        0 * _
    }

    def 'should get security group by ID'() {
        SecurityGroup expectedSecurityGroup = new SecurityGroup(groupId: 'sg-123', groupName: 'super_secure')

        when:
        SecurityGroup actualSecurityGroup = awsEc2Service.getSecurityGroup(userContext, 'sg-123')

        then:
        actualSecurityGroup == expectedSecurityGroup
        1 * mockAmazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupIds: ['sg-123'])) >>
                new DescribeSecurityGroupsResult(securityGroups: [expectedSecurityGroup])
        1 * mockSecurityGroupCache.list() >> [expectedSecurityGroup]
        1 * mockSecurityGroupCache.put('super_secure', expectedSecurityGroup) >> expectedSecurityGroup
        0 * _
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
        0 * _
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
        0 * _
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
        0 * _
    }

    def 'should find non cached VPC Security Group by name'() {
        // This will happen when getting a VPC Security Group by name that was created too recently to be in the cache
        SecurityGroup securityGroup = new SecurityGroup(groupId: 'sg-123', groupName: 'super_secure')

        when:
        SecurityGroup actualSecurityGroup =  awsEc2Service.getSecurityGroup(userContext, 'super_secure')

        then:
        null == actualSecurityGroup
        1 * mockAmazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupNames: ['super_secure'])) >> {
            AmazonServiceException e = new AmazonServiceException('you cannot ask for a VPC Security Group by name')
            e.errorCode = 'InvalidParameterValue'
            throw e
        }
        1 * mockSecurityGroupCache.get('super_secure') >> securityGroup
        1 * mockAmazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupIds: ['sg-123'])) >>
                new DescribeSecurityGroupsResult(securityGroups: [securityGroup])
        1 * mockSecurityGroupCache.put('super_secure', securityGroup)
        0 * _
    }

    def 'should not try to find non cached VPC Security Group by name without specific errorCode'() {
        when:
        SecurityGroup actualSecurityGroup =  awsEc2Service.getSecurityGroup(userContext, 'super_secure')

        then:
        null == actualSecurityGroup
        1 * mockAmazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupNames: ['super_secure'])) >> {
            AmazonServiceException e = new AmazonServiceException('you cannot ask for a VPC Security Group by name')
            e.errorCode = 'NotInvalidParameterValue'
            throw e
        }
        1 * mockSecurityGroupCache.put('super_secure', null)
        0 * _
    }

    def 'should not try to find non cached VPC Security Group by name if given id'() {
        when:
        SecurityGroup actualSecurityGroup =  awsEc2Service.getSecurityGroup(userContext, 'sg-123')

        then:
        null == actualSecurityGroup
        1 * mockAmazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupIds: ['sg-123'])) >> {
            AmazonServiceException e = new AmazonServiceException('there is no security group like that')
            e.errorCode = 'InvalidParameterValue'
            throw e
        }
        1 * mockSecurityGroupCache.list() >> []
        0 * _
    }

    def 'should retrieve subnets'() {
        Closure awsSubnet = { String id, String zone, String purpose, String target ->
            new Subnet(subnetId: id, availabilityZone: zone, tags: [new Tag(key: 'immutable_metadata',
                    value: "{ \"purpose\": \"${purpose}\", \"target\": \"${target}\" }")])
        }
        List<Subnet> subnets = ImmutableList.of(
                awsSubnet('subnet-e9b0a3a1', 'us-east-1a', 'internal', 'ec2'),
                awsSubnet('subnet-e9b0a3a4', 'us-east-1a', 'external', 'elb'),
        )
        AmazonEC2 mockAmazonEC2 = Mock(AmazonEC2)
        Caches caches = new Caches(new MockCachedMapBuilder([
                (EntityType.subnet): new CachedMapBuilder(null).of(EntityType.subnet).buildCachedMap(),
        ]))
        AwsEc2Service awsEc2Service = new AwsEc2Service(awsClient: new MultiRegionAwsClient({ mockAmazonEC2 }),
                caches: caches)
        awsEc2Service.initializeCaches()

        when:
        caches.allSubnets.fill()

        then:
        1 * mockAmazonEC2.describeSubnets() >> new DescribeSubnetsResult(subnets: subnets)
        ImmutableList.copyOf(awsEc2Service.getSubnets(userContext).allSubnets) == [
                new SubnetData(subnetId: 'subnet-e9b0a3a1', availabilityZone: 'us-east-1a', purpose: 'internal',
                        target: SubnetTarget.EC2),
                new SubnetData(subnetId: 'subnet-e9b0a3a4', availabilityZone: 'us-east-1a', purpose: 'external',
                        target: SubnetTarget.ELB),
        ]
    }

    private Collection<SecurityGroup> simulateWarGames() {
        SecurityGroupDsl.config {
            wopr(7101, 7102) withIngress norad
            joshua(7101, 7102) withIngress([wopr, globalthermonuclearwar, tictactoe])
            modem(8080, 8080) withIngress joshua
            falken(7101, 7102) withIngress joshua
        }
    }

    def 'options for target security group should include all groups sorted, but only some allowed to call target'() {

        Collection<SecurityGroup> warGamesSecurityGroups = simulateWarGames()
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

        Collection<SecurityGroup> warGamesSecurityGroups = simulateWarGames()
        SecurityGroup joshua = warGamesSecurityGroups.find { it.groupName == 'joshua' }

        when:
        List<SecurityGroupOption> gamesForJoshua = awsEc2Service.getSecurityGroupOptionsForSource(userContext, joshua)

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

    def 'should update security groups'() {
        List<UserIdGroupPair> userIdGroupPairs = [new UserIdGroupPair(groupId: 'sg-s')]
        SecurityGroup source = new SecurityGroup(groupName: 'source', groupId: 'sg-s')
        SecurityGroup target = new SecurityGroup(groupName: 'target', groupId: 'sg-t', ipPermissions: [
                new IpPermission(fromPort: 1, toPort: 1, userIdGroupPairs: userIdGroupPairs),
                new IpPermission(fromPort: 2, toPort: 2, userIdGroupPairs: userIdGroupPairs),
        ])

        when:
        awsEc2Service.updateSecurityGroupPermissions(userContext, target, source, [
                new IpPermission(fromPort: 2, toPort: 2),
                new IpPermission(fromPort: 3, toPort: 3),
        ])

        then:
        1 * mockAmazonEC2.authorizeSecurityGroupIngress(new AuthorizeSecurityGroupIngressRequest(groupId: 'sg-t',
                ipPermissions: [
                        new IpPermission(fromPort: 3, toPort: 3, ipProtocol: 'tcp', userIdGroupPairs: userIdGroupPairs),
                ]))
        1 * mockAmazonEC2.revokeSecurityGroupIngress(new RevokeSecurityGroupIngressRequest(groupId: 'sg-t',
                ipPermissions: [
                        new IpPermission(fromPort: 1, toPort: 1, ipProtocol: 'tcp', userIdGroupPairs: userIdGroupPairs),
                ]))
        1 * mockAmazonEC2.describeSecurityGroups(_) >> new DescribeSecurityGroupsResult(
                securityGroups: [new SecurityGroup()])
        0 * _
    }
}
