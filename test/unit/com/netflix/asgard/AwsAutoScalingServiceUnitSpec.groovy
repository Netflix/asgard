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

import com.amazonaws.services.autoscaling.AmazonAutoScaling
import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.DescribePoliciesResult
import com.amazonaws.services.autoscaling.model.DescribeScheduledActionsResult
import com.amazonaws.services.autoscaling.model.Instance
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.autoscaling.model.LifecycleState
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest
import com.amazonaws.services.autoscaling.model.ScalingPolicy
import com.amazonaws.services.autoscaling.model.ScheduledUpdateGroupAction
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest
import com.amazonaws.services.autoscaling.model.SuspendedProcess
import com.amazonaws.services.autoscaling.model.Tag
import com.amazonaws.services.autoscaling.model.TagDescription
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.SecurityGroup
import com.netflix.asgard.model.ApplicationInstance
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.AutoScalingGroupHealthCheckType
import com.netflix.asgard.model.AutoScalingGroupMixin
import com.netflix.asgard.model.AutoScalingProcessType
import com.netflix.asgard.model.EurekaStatus
import com.netflix.asgard.model.InstanceHealth
import com.netflix.asgard.model.StackAsg
import com.netflix.asgard.model.Subnets
import com.netflix.frigga.ami.AppVersion
import java.util.concurrent.Executors
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings(["GroovyAssignabilityCheck"])
class AwsAutoScalingServiceUnitSpec extends Specification {

    AwsAutoScalingService awsAutoScalingService

    @Unroll("""getLaunchConfigurationsForSecurityGroup should return #launchConfigNames when groupId is #groupId \
and groupName is #groupName""")
    def 'should get the launch configurations for a specified security group by name or id'() {
        Closure newLaunchConfig = { name, groups ->
            new LaunchConfiguration(launchConfigurationName: name, securityGroups: groups)
        }
        awsAutoScalingService = Spy(AwsAutoScalingService) {
            getLaunchConfigurations(_) >> {
                [
                        newLaunchConfig('h-1', ['hello', 'elb', 'infrastructure']),
                        newLaunchConfig('h-2', ['elb', 'infrastructure']),
                        newLaunchConfig('h-3', ['hello', 'elb', 'infrastructure']),
                        newLaunchConfig('h-4', ['elb', 'sg-12345678']),
                        newLaunchConfig('h-5', ['elb']),
                        newLaunchConfig('h-6', ['elb', 'sg-12345678']),
                ]
            }
        }
        SecurityGroup securityGroup = new SecurityGroup(groupName: groupName, groupId: groupId)
        UserContext userContext = UserContext.auto(Region.US_WEST_1)

        when:
        List<LaunchConfiguration> launchConfigurations = awsAutoScalingService.getLaunchConfigurationsForSecurityGroup(
                userContext, securityGroup)

        then:
        launchConfigNames == launchConfigurations*.launchConfigurationName

        where:
        launchConfigNames | groupId       | groupName
        ['h-4', 'h-6']    | 'sg-12345678' | null
        ['h-1', 'h-3']    | null          | 'hello'
    }

    @Unroll("""it is #result that a group should be manually sized if it has scaling policies #policyNames and \
scheduled actions #scheduleNames and suspended processes #processNames""")
    def 'should determine whether or not a group needs to be manually sized'() {
        given:
        AutoScalingGroup.mixin AutoScalingGroupMixin
        AmazonAutoScaling mockAmazonAutoScalingClient = Mock(AmazonAutoScaling)
        awsAutoScalingService = new AwsAutoScalingService(
            awsClient: new MultiRegionAwsClient({ mockAmazonAutoScalingClient })
        )
        with (mockAmazonAutoScalingClient) {
            describePolicies(_) >> {
                new DescribePoliciesResult(scalingPolicies: policyNames.collect { new ScalingPolicy(policyName: it) })
            }
            describeScheduledActions(_) >> {
                new DescribeScheduledActionsResult(scheduledUpdateGroupActions: scheduleNames.collect {
                    new ScheduledUpdateGroupAction(scheduledActionName: it)
                })
            }
        }
        AutoScalingGroup group = new AutoScalingGroup(autoScalingGroupName: 'hi', suspendedProcesses:
                processNames.collect { new SuspendedProcess(processName: it) })

        expect:
        awsAutoScalingService.shouldGroupBeManuallySized(UserContext.auto(Region.US_WEST_2), group) == result

        where:
        result | policyNames      | scheduleNames | processNames
        true   | []               | []            | []
        true   | []               | []            | ['Launch']
        true   | []               | []            | ['Terminate']
        true   | []               | []            | ['AlarmNotifications']
        true   | []               | []            | ['Launch', 'Terminate', 'AlarmNotifications']
        false  | ['hi-1', 'hi-2'] | ['hi-8']      | []
        false  | ['hi-1', 'hi-2'] | []            | []
        false  | []               | ['hi-8']      | []
        false  | ['hi-1', 'hi-2'] | ['hi-8']      | ['Launch']
        false  | ['hi-1', 'hi-2'] | ['hi-8']      | ['Terminate']
        true   | ['hi-1', 'hi-2'] | ['hi-8']      | ['AlarmNotifications']
        true   | ['hi-1', 'hi-2'] | ['hi-8']      | ['Launch', 'Terminate', 'AlarmNotifications']
    }

    def 'should retrieve instance health checks'() {
        CachedMap mockAutoScalingCache = Mock {
            list() >> [
                    'service1-int-v007': ['i-10000001'],
                    'service4-dev-v001': ['i-20000001', 'i-20000002'],
                    'service5-int-v042': ['i-30000001', 'i-30000002', 'i-30000003']
            ].collect { asgName, instanceIds ->
                new AutoScalingGroup(autoScalingGroupName: asgName, instances: instanceIds.
                        collect { new Instance(instanceId: it) })
            }
        }
        CachedMap mockApplicationInstanceCache = Mock {
            list() >> [
                    'i-10000001': 'urlForInstance11',
                    'i-20000001': 'urlForInstance21',
                    'i-20000002': 'urlForInstance22',
                    'i-30000001': 'urlForInstance31',
                    'i-30000002': 'urlForInstance32'
            ].collect { id, url ->
                new ApplicationInstance().with {
                    it.instanceId = id
                    it.healthCheckUrl = url
                    it
                }
            }
        }
        Caches caches = new Caches(new MockCachedMapBuilder([
                (EntityType.autoScaling): mockAutoScalingCache,
                (EntityType.applicationInstance): mockApplicationInstanceCache
        ]))
        ConfigService configService = Mock {
            getSignificantStacks() >> ['int']
        }
        AwsEc2Service awsEc2Service = Mock {
            1 * checkHostHealth('urlForInstance11') >> true
            1 * checkHostHealth('urlForInstance31') >> true
            1 * checkHostHealth('urlForInstance32') >> false
            0 * checkHostHealth(_)
        }
        awsAutoScalingService = new AwsAutoScalingService(caches: caches, configService: configService,
                awsEc2Service: awsEc2Service, threadScheduler: new ThreadScheduler(null))

        //noinspection GroovyAccessibility
        expect:
        awsAutoScalingService.retrieveInstanceHealthChecks(Region.US_EAST_1) == [
                'i-10000001': true,
                'i-30000001': true,
                'i-30000002': false
        ].collect { id, url -> new InstanceHealth(id, url) }
    }

    def 'should construct Stack'() {
        CachedMap mockInstanceHealthCache = Mock {
            get(_) >> new InstanceHealth(null, true)
        }
        CachedMap mockAutoScalingCache = Mock {
            list() >> [
                    'service1-int-v007': ['i-10000001'],
                    'service4-dev-v001': ['i-20000001', 'i-20000002'],
                    'service5-int-v042': ['i-30000001', 'i-30000002', 'i-30000003']
            ].collect { asgName, instanceIds ->
                new AutoScalingGroup(autoScalingGroupName: asgName, instances: instanceIds.
                        collect { new Instance(instanceId: it) }, launchConfigurationName: "lc-${asgName}")
            }
        }
        Caches mockCaches = new Caches(new MockCachedMapBuilder([
                (EntityType.instanceHealth): mockInstanceHealthCache,
                (EntityType.autoScaling): mockAutoScalingCache
        ]))
        awsAutoScalingService = Spy(AwsAutoScalingService) {
            1 * getLaunchConfiguration(_, 'lc-service1-int-v007', From.CACHE) >> new LaunchConfiguration(imageId: 'ami-service1')
            1 * getLaunchConfiguration(_, 'lc-service5-int-v042', From.CACHE) >> new LaunchConfiguration(imageId: 'ami-service5')
            0 * getLaunchConfiguration(_, _, _)
        }
        AwsEc2Service mockAwsEc2Service = Mock {
            1 * getImage(_, 'ami-service1', From.CACHE) >> {
                Image image = new Image()
                image.metaClass.getAppVersion = { 'service1-1.0.0-592112' }
                image
            }
            1 * getImage(_, 'ami-service5', From.CACHE) >> {
                Image image = new Image()
                image.metaClass.getAppVersion = { 'service5-1.0.0-592112' }
                image
            }
            0 * getImage(_, _, _)
        }
        awsAutoScalingService.with {
            caches = mockCaches
            awsEc2Service = mockAwsEc2Service
        }

        expect:
        awsAutoScalingService.getStack(new UserContext(region: Region.US_EAST_1), 'int') == [
                new StackAsg(new AutoScalingGroup(autoScalingGroupName: 'service1-int-v007',
                        instances: [new Instance(instanceId: 'i-10000001')],
                        launchConfigurationName: 'lc-service1-int-v007'),
                        new LaunchConfiguration(imageId: 'ami-service1'),
                        new AppVersion(packageName: 'service1', version: '1.0.0', commit: '592112'), 1),
                new StackAsg(new AutoScalingGroup(autoScalingGroupName: 'service5-int-v042',
                        instances: [new Instance(instanceId: 'i-30000001'), new Instance(instanceId: 'i-30000002'),
                        new Instance(instanceId: 'i-30000003')], launchConfigurationName: 'lc-service5-int-v042'),
                        new LaunchConfiguration(imageId: 'ami-service5'),
                        new AppVersion(packageName: 'service5', version: '1.0.0', commit: '592112'), 3)
        ]
    }

    def 'should copy scheduled actions for new ASG'() {
        awsAutoScalingService = Spy(AwsAutoScalingService) {
            nextPolicyId(_) >> 42
        }

        expect:
        awsAutoScalingService.copyScheduledActionsForNewAsg(UserContext.auto(Region.US_WEST_1), 'service1-int-v008', [
                new ScheduledUpdateGroupAction(autoScalingGroupName: 'service1-int-v008', desiredCapacity: 3)
        ]) == [new ScheduledUpdateGroupAction(autoScalingGroupName: 'service1-int-v008', desiredCapacity: 3,
                scheduledActionName: 'service1-int-v008-42')]

    }

    def 'should resize ASG'() {
        awsAutoScalingService = Spy(AwsAutoScalingService) {
            getAutoScalingGroup(_, _) >> { new AutoScalingGroup(autoScalingGroupName: it[1]) }
            getCluster(_, _) >> null
        }
        AmazonAutoScaling mockAmazonAutoScaling = Mock(AmazonAutoScaling)
        awsAutoScalingService.awsClient = Mock(MultiRegionAwsClient) {
            by(_) >> mockAmazonAutoScaling
        }
        awsAutoScalingService.awsEc2Service = Mock(AwsEc2Service) {
            getSubnets(_) >> Subnets.from([])
        }

        when:
        awsAutoScalingService.resizeAutoScalingGroup(UserContext.auto(Region.US_WEST_1), 'service1-int-v008', 1, 2, 3)

        then:
        1 * mockAmazonAutoScaling.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest(
                autoScalingGroupName: 'service1-int-v008', minSize: 1, desiredCapacity: 2, maxSize: 3))
    }


    def 'should determine healthy ASG due to no instances'() {
        awsAutoScalingService = Spy(AwsAutoScalingService)

        expect:
        null == awsAutoScalingService.reasonAsgIsUnhealthy(UserContext.auto(Region.US_WEST_1), 'service1-int-v008', 0)
    }

    def 'should fail health check for missing ASG'() {
        awsAutoScalingService = Spy(AwsAutoScalingService) {
            getAutoScalingGroup(_, _) >> null
        }

        when:
        awsAutoScalingService.reasonAsgIsUnhealthy(UserContext.auto(Region.US_WEST_1), 'service1-int-v008', 1)

        then:
        IllegalStateException e = thrown()
        e.message == "ASG 'service1-int-v008' does not exist."
    }

    def 'should determine unhealthy ASG due to instance count'() {
        awsAutoScalingService = Spy(AwsAutoScalingService) {
            getAutoScalingGroup(_, _) >> { new AutoScalingGroup(autoScalingGroupName: it[1], instances: []) }
        }

        expect:
        awsAutoScalingService.reasonAsgIsUnhealthy(UserContext.auto(Region.US_WEST_1), 'service1-int-v008', 1) ==
                'Instance count is 0. Waiting for 1.'
    }

    def 'should determine unhealthy ASG due to instances not yet in service'() {
        awsAutoScalingService = Spy(AwsAutoScalingService) {
            getAutoScalingGroup(_, _) >> { new AutoScalingGroup(autoScalingGroupName: it[1], instances: [
                    new Instance(instanceId: 'i-f00dcafe', lifecycleState: LifecycleState.Pending.name()) ])
            }
        }

        expect:
        awsAutoScalingService.reasonAsgIsUnhealthy(UserContext.auto(Region.US_WEST_1), 'service1-int-v008', 1) ==
                'Waiting for instances to be in service.'
    }

    def 'should determine unhealthy ASG due to unavailable Eureka data'() {
        awsAutoScalingService = Spy(AwsAutoScalingService) {
            getAutoScalingGroup(_, _) >> { new AutoScalingGroup(autoScalingGroupName: it[1], instances: [
                    new Instance(instanceId: 'i-f00dcafe', lifecycleState: LifecycleState.InService.name()) ])
            }
        }
        awsAutoScalingService.discoveryService = Mock(DiscoveryService) {
            getAppInstancesByIds(_, _) >> []
        }

        expect:
        awsAutoScalingService.reasonAsgIsUnhealthy(UserContext.auto(Region.US_WEST_1), 'service1-int-v008', 1) ==
                'Waiting for Eureka data about instances.'
    }

    def 'should determine unhealthy ASG due to instances not up in Eureka'() {
        awsAutoScalingService = Spy(AwsAutoScalingService) {
            getAutoScalingGroup(_, _) >> { new AutoScalingGroup(autoScalingGroupName: it[1], instances: [
                    new Instance(instanceId: 'i-f00dcafe', lifecycleState: LifecycleState.InService.name()) ])
            }
        }
        awsAutoScalingService.discoveryService = Mock(DiscoveryService) {
            getAppInstancesByIds(_, _) >> [ new ApplicationInstance().with {
                status = EurekaStatus.DOWN.name()
                it
            }]
        }

        expect:
        awsAutoScalingService.reasonAsgIsUnhealthy(UserContext.auto(Region.US_WEST_1), 'service1-int-v008', 1) ==
                'Waiting for all instances to be available in Eureka.'
    }

    def 'should determine unhealthy ASG due to failed instance health check'() {
        awsAutoScalingService = Spy(AwsAutoScalingService) {
            getAutoScalingGroup(_, _) >> { new AutoScalingGroup(autoScalingGroupName: it[1], instances: [
                    new Instance(instanceId: 'i-f00dcafe', lifecycleState: LifecycleState.InService.name()) ])
            }
        }
        awsAutoScalingService.discoveryService = Mock(DiscoveryService) {
            getAppInstancesByIds(_, _) >> [ new ApplicationInstance().with {
                    status = EurekaStatus.UP.name()
                    it
            }]
        }
        awsAutoScalingService.threadScheduler = Mock(ThreadScheduler) {
            getScheduler() >> {
                Executors.newFixedThreadPool(2)
            }
        }
        awsAutoScalingService.awsEc2Service = Mock(AwsEc2Service) {
            checkHostsHealth(_) >> false
        }

        expect:
        awsAutoScalingService.reasonAsgIsUnhealthy(UserContext.auto(Region.US_WEST_1), 'service1-int-v008', 1) ==
                'Waiting for all instances to pass health checks.'
    }

    def 'should determine healthy ASG'() {
        awsAutoScalingService = Spy(AwsAutoScalingService) {
            getAutoScalingGroup(_, _) >> { new AutoScalingGroup(autoScalingGroupName: it[1], instances: [
                    new Instance(instanceId: 'i-f00dcafe', lifecycleState: LifecycleState.InService.name()) ])
            }
        }
        awsAutoScalingService.discoveryService = Mock(DiscoveryService) {
            getAppInstancesByIds(_, _) >> [ new ApplicationInstance().with {
                status = EurekaStatus.UP.name()
                it
            }]
        }
        awsAutoScalingService.awsEc2Service = Mock(AwsEc2Service) {
            checkHostsHealth(_) >> true
        }

        expect:
        awsAutoScalingService.reasonAsgIsUnhealthy(UserContext.auto(Region.US_WEST_1), 'service1-int-v008', 1) == null
    }

    def 'should update ASG and change everything'() {
        AmazonAutoScaling mockAmazonAutoScalingClient = Mock(AmazonAutoScaling)
        awsAutoScalingService = Spy(AwsAutoScalingService)
        awsAutoScalingService.awsEc2Service = Mock(AwsEc2Service)
        awsAutoScalingService.awsClient = new MultiRegionAwsClient({ mockAmazonAutoScalingClient })

        when:
        awsAutoScalingService.updateAutoScalingGroup(UserContext.auto(Region.US_WEST_1), 'autoScalingGroupName1') {
            AutoScalingGroupBeanOptions options ->
            options.with{
                autoScalingGroupName = 'autoScalingGroupName2'
                launchConfigurationName = 'launchConfigurationName2'
                minSize = 6
                maxSize = 8
                desiredCapacity = 7
                defaultCooldown = 9
                availabilityZones = ['us-west-1b']
                loadBalancerNames = ['lb2']
                healthCheckType = AutoScalingGroupHealthCheckType.ELB
                healthCheckGracePeriod = 6
                placementGroup = 'placementGroup2'
                subnetPurpose = 'subnetPurpose2'
                terminationPolicies = ['tp2']
                tags = [new Tag(key: 'key2', value: 'value2')]
                suspendedProcesses = [AutoScalingProcessType.AZRebalance, AutoScalingProcessType.Launch]
            }
        }

        then:
        with (awsAutoScalingService) {
            1 * getAutoScalingGroup(_, 'autoScalingGroupName1') >> new AutoScalingGroup(
                    autoScalingGroupName: 'autoScalingGroupName1',
                    launchConfigurationName: 'launchConfigurationName1',
                    minSize: 1,
                    maxSize: 3,
                    desiredCapacity: 2,
                    defaultCooldown: 4,
                    availabilityZones: ['us-west-1a'],
                    loadBalancerNames: ['lb1'],
                    healthCheckType: AutoScalingGroupHealthCheckType.EC2,
                    healthCheckGracePeriod: 5,
                    placementGroup: 'placementGroup1',
                    vPCZoneIdentifier: 'vPCZoneIdentifier1',
                    terminationPolicies: ['tp1'],
                    tags: [new TagDescription(key: 'key1', value: 'value1')],
                    suspendedProcesses: ['Terminate', 'Launch'].collect { new SuspendedProcess(processName: it) }
            )
        }
        with (awsAutoScalingService.awsEc2Service) {
            1 * getSubnets(_) >> Mock(Subnets) {
                1 * getPurposeFromVpcZoneIdentifier('vPCZoneIdentifier1') >> 'subnetPurpose1'
            }
        }
        with (mockAmazonAutoScalingClient) {
            1 * suspendProcesses(new SuspendProcessesRequest(autoScalingGroupName: 'autoScalingGroupName1',
                    scalingProcesses: ['AZRebalance']
            ))
            1 * resumeProcesses(new ResumeProcessesRequest(autoScalingGroupName: 'autoScalingGroupName1',
                    scalingProcesses: ['Terminate']
            ))
            1 * updateAutoScalingGroup(new UpdateAutoScalingGroupRequest(autoScalingGroupName: 'autoScalingGroupName1',
                    launchConfigurationName: 'launchConfigurationName2', minSize: 6, maxSize: 8, desiredCapacity: 7,
                    defaultCooldown: 9, availabilityZones: ['us-west-1b'], healthCheckType: 'ELB',
                    healthCheckGracePeriod: 6, placementGroup: 'placementGroup2', terminationPolicies: ['tp2']
            ))
        }
    }

    def 'should update ASG and change nothing'() {
        AmazonAutoScaling mockAmazonAutoScalingClient = Mock(AmazonAutoScaling)
        awsAutoScalingService = Spy(AwsAutoScalingService)
        awsAutoScalingService.awsEc2Service = Mock(AwsEc2Service)
        awsAutoScalingService.awsClient = new MultiRegionAwsClient({ mockAmazonAutoScalingClient })

        when:
        awsAutoScalingService.updateAutoScalingGroup(UserContext.auto(Region.US_WEST_1), 'autoScalingGroupName1') {
            AutoScalingGroupBeanOptions options ->
        }

        then:
        with (awsAutoScalingService) {
            1 * getAutoScalingGroup(_, 'autoScalingGroupName1') >> new AutoScalingGroup(
                    autoScalingGroupName: 'autoScalingGroupName1',
                    launchConfigurationName: 'launchConfigurationName1',
                    minSize: 1,
                    maxSize: 3,
                    desiredCapacity: 2,
                    defaultCooldown: 4,
                    availabilityZones: ['us-west-1a'],
                    loadBalancerNames: ['lb1'],
                    healthCheckType: AutoScalingGroupHealthCheckType.EC2,
                    healthCheckGracePeriod: 5,
                    placementGroup: 'placementGroup1',
                    vPCZoneIdentifier: 'vPCZoneIdentifier1',
                    terminationPolicies: ['tp1'],
                    tags: [new TagDescription(key: 'key1', value: 'value1')],
                    suspendedProcesses: ['Terminate', 'Launch'].collect { new SuspendedProcess(processName: it) }
            )
        }
        with (awsAutoScalingService.awsEc2Service) {
            1 * getSubnets(_) >> Mock(Subnets) {
                1 * getPurposeFromVpcZoneIdentifier('vPCZoneIdentifier1') >> 'subnetPurpose1'
            }
        }
        with (mockAmazonAutoScalingClient) {
            0 * suspendProcesses(_)
            0 * resumeProcesses(_)
            1 * updateAutoScalingGroup(new UpdateAutoScalingGroupRequest(autoScalingGroupName: 'autoScalingGroupName1',
                    launchConfigurationName: 'launchConfigurationName1', minSize: 1, maxSize: 3, desiredCapacity: 2,
                    defaultCooldown: 4, availabilityZones: ['us-west-1a'], healthCheckType: 'EC2',
                    healthCheckGracePeriod: 5, placementGroup: 'placementGroup1', terminationPolicies: ['tp1']
            ))
        }
    }

    def 'should return null if update ASG does not exist'() {
        AmazonAutoScaling mockAmazonAutoScalingClient = Mock(AmazonAutoScaling)
        awsAutoScalingService = Spy(AwsAutoScalingService)
        awsAutoScalingService.awsEc2Service = Mock(AwsEc2Service)
        awsAutoScalingService.awsClient = new MultiRegionAwsClient({ mockAmazonAutoScalingClient })

        when:
        AutoScalingGroupBeanOptions options = awsAutoScalingService.updateAutoScalingGroup(UserContext.auto(Region.US_WEST_1), 'autoScalingGroupName1') {
            AutoScalingGroupBeanOptions options ->
        }

        then:
        options == null
        with (awsAutoScalingService) {
            1 * getAutoScalingGroup(_, 'autoScalingGroupName1') >> null
        }
        with (mockAmazonAutoScalingClient) {
            0 * suspendProcesses(_)
            0 * resumeProcesses(_)
            0 * updateAutoScalingGroup(_)
        }
    }
}
