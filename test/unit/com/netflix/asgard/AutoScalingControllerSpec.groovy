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

import com.amazonaws.services.autoscaling.model.Alarm
import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.Instance
import com.amazonaws.services.autoscaling.model.InstanceMonitoring
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.autoscaling.model.ScalingPolicy
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.amazonaws.services.ec2.model.GroupIdentifier
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Multiset
import com.google.common.collect.TreeMultiset
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.AutoScalingGroupMixin
import com.netflix.asgard.model.AutoScalingProcessType
import com.netflix.asgard.model.Subnets
import com.netflix.frigga.Names
import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings("GroovyAssignabilityCheck")
@TestFor(AutoScalingController)
class AutoScalingControllerSpec extends Specification {

    ApplicationService applicationService = Mock(ApplicationService)
    AwsAutoScalingService awsAutoScalingService = Mock(AwsAutoScalingService)
    AwsEc2Service awsEc2Service = Mock(AwsEc2Service)
    AwsCloudWatchService awsCloudWatchService = Mock(AwsCloudWatchService)
    AwsLoadBalancerService awsLoadBalancerService = Mock(AwsLoadBalancerService)
    ConfigService configService = Mock(ConfigService)

    void setup() {
        TestUtils.setUpMockRequest()
        controller.applicationService = applicationService
        controller.awsAutoScalingService = awsAutoScalingService
        controller.awsEc2Service = awsEc2Service
        controller.awsCloudWatchService = awsCloudWatchService
        controller.awsLoadBalancerService = awsLoadBalancerService
        controller.configService = configService

        configService.getEnableInstanceMonitoring() >> false
    }

    void setupHeavyWeightMocks() {
        Mocks.createDynamicMethods()
        controller.grailsApplication = Mocks.grailsApplication()
        controller.applicationService = Mocks.applicationService()
        controller.awsAutoScalingService = Mocks.awsAutoScalingService()
        controller.awsEc2Service = Mocks.awsEc2Service()
        controller.awsCloudWatchService = Mocks.awsCloudWatchService()
        controller.awsLoadBalancerService = Mocks.awsLoadBalancerService()
        controller.configService = Mocks.configService()
        controller.instanceTypeService = Mocks.instanceTypeService()
        controller.stackService = Mocks.stackService()
    }

    def 'show should return ASG info'() {
        AutoScalingGroup.mixin AutoScalingGroupMixin
        String name = 'helloworld-example-v015'
        String lcName = 'helloworld-example-v015-123456'
        String imageId = 'amy-deadbeef'
        String appName = 'helloworld'
        String sgName = 'helloworld'
        String chaosLink = 'http://chaoseditor'
        String buildLink = 'http://builds'
        String instanceId = 'i-abcdefgh'
        String zoneA = 'us-east-1a'
        String zoneB = 'us-east-1b'
        Instance instance = new Instance(instanceId: instanceId, availabilityZone: zoneA)
        String lbName1 = 'helloworld--frontend'
        String lbName2 = 'helloworld--frontend2'

        given: "one of the ELBs has a zone list that doesn't match the ASG's zone list"
        LoadBalancerDescription lb1 = new LoadBalancerDescription(loadBalancerName: lbName1, availabilityZones: [zoneB])
        LoadBalancerDescription lb2 = new LoadBalancerDescription(loadBalancerName: lbName2, availabilityZones: [zoneA])
        List<String> asgZones = [zoneA]

        and: 'the rest of the ASG is set up pretty conventionally, but without scaling policies or scheduled actions'
        AutoScalingGroup asg = new AutoScalingGroup(autoScalingGroupName: name, launchConfigurationName: lcName,
                instances: [instance], loadBalancerNames: [lbName1, lbName2], availabilityZones: asgZones)
        AutoScalingGroupData asgData = AutoScalingGroupData.from(asg, [:], [], [:], [])
        LaunchConfiguration launchConfig = new LaunchConfiguration(launchConfigurationName: lcName, imageId: imageId,
                securityGroups: [sgName])
        Image image = new Image(imageId: imageId)
        AppRegistration app = new AppRegistration(name: appName, email: 'test@examle.com')
        Multiset<String> zonesWithInstanceCounts = TreeMultiset.create([zoneA])
        List<GroupIdentifier> securityGroupIdObjects = [new GroupIdentifier(groupId: 'sg-123', groupName: sgName)]

        params.id = name

        when:
        Map attrs = controller.show()

        then:
        1 * awsAutoScalingService.getAutoScalingGroup(_, name) >> asg
        1 * awsAutoScalingService.buildAutoScalingGroupData(_, asg) >> asgData
        1 * awsLoadBalancerService.getLoadBalancer(_, 'helloworld--frontend', From.CACHE) >> lb1
        1 * awsLoadBalancerService.getLoadBalancer(_, 'helloworld--frontend2', From.CACHE) >> lb2
        1 * awsAutoScalingService.getAutoScalingGroupActivities(_, 'helloworld-example-v015', 20) >> []
        1 * awsAutoScalingService.getScalingPoliciesForGroup(_, 'helloworld-example-v015') >> []
        1 * awsAutoScalingService.getScheduledActionsForGroup(_, 'helloworld-example-v015') >> []
        1 * awsEc2Service.getSubnets(_) >> Subnets.from([])
        1 * awsAutoScalingService.getLaunchConfiguration(_, lcName) >> launchConfig
        1 * awsEc2Service.getImage(_, imageId, From.CACHE) >> image
        1 * awsEc2Service.getSecurityGroupNameIdPairsByNamesOrIds(_, [sgName]) >> securityGroupIdObjects
        1 * awsCloudWatchService.getAlarms(_, []) >> []
        1 * applicationService.getRegisteredApplication(_, 'helloworld') >> app
        1 * configService.buildServerUrl >> buildLink
        1 * configService.getMonkeyCommanderEditLink('helloworld') >> chaosLink
        0 * _
        response.status == 200

        and: "one of the ELBs has a zone list that doesn't match the ASG's zone list, so a zone mismatch gets reported"
        Map<String, ArrayList<String>> mismatchedElbsToZones = [(lbName1): [zoneB]]

        attrs == [activities: [],
                addToLoadBalancerStatus: 'Enabled',
                alarmsByName: [:],
                app: app,
                azRebalanceStatus: 'Enabled',
                buildServer: buildLink,
                chaosMonkeyEditLink: chaosLink,
                clusterName: 'helloworld-example',
                group: asgData,
                image: image,
                instanceCount: 1,
                launchConfiguration: launchConfig,
                launchStatus: 'Enabled',
                mismatchedElbNamesToZoneLists: mismatchedElbsToZones,
                runHealthChecks: true,
                scalingPolicies: [],
                scheduledActions: [],
                securityGroups: securityGroupIdObjects,
                showPostponeButton: false,
                subnetPurpose: null,
                terminateStatus: 'Enabled',
                variables: Names.parseName(name),
                vpcZoneIdentifier: null,
                zonesWithInstanceCounts: zonesWithInstanceCounts
        ]
    }

    def 'show should return Alarm info'() {
        setupHeavyWeightMocks()
        controller.params.name = 'helloworld-example-v015'
        AwsAutoScalingService mockAwsAutoScalingService = Mock(AwsAutoScalingService)
        controller.awsAutoScalingService = mockAwsAutoScalingService
        mockAwsAutoScalingService.getAutoScalingGroup(_, _) >> {
            new AutoScalingGroup(autoScalingGroupName: it[1])
        }
        AwsCloudWatchService mockAwsCloudWatchService = Mock(AwsCloudWatchService)
        controller.awsCloudWatchService = mockAwsCloudWatchService

        when:
        final attrs = controller.show()

        then:
        1 * mockAwsAutoScalingService.getScalingPoliciesForGroup(_, 'helloworld-example-v015') >> { [
            new ScalingPolicy(alarms: [new Alarm(alarmName: 'alarm1')]),
            new ScalingPolicy(alarms: [new Alarm(alarmName: 'alarm2'), new Alarm(alarmName: 'alarm3')]),
        ] }
        1 * mockAwsCloudWatchService.getAlarms(_, ['alarm1', 'alarm2', 'alarm3']) >> { [
            new MetricAlarm(alarmName: 'alarm1', metricName: 'metric1'),
            new MetricAlarm(alarmName: 'alarm2', metricName: 'metric2'),
            new MetricAlarm(alarmName: 'alarm3', metricName: 'metric3'),
        ] }
        attrs['alarmsByName'] == [
                alarm1: new MetricAlarm(alarmName: 'alarm1', metricName: 'metric1'),
                alarm2: new MetricAlarm(alarmName: 'alarm2', metricName: 'metric2'),
                alarm3: new MetricAlarm(alarmName: 'alarm3', metricName: 'metric3'),
        ]
    }

    def 'show should indicate nonexistent ASG'() {
        setupHeavyWeightMocks()
        controller.params.name = 'doesntexist'

        when:
        controller.show()

        then:
        '/error/missing' == view
        "Auto Scaling Group 'doesntexist' not found in us-east-1 test" == controller.flash.message
    }

    def 'show should indicate nonexistent ASG if invalid characters are used'() {
        setupHeavyWeightMocks()
        controller.params.name = 'nccp-moviecontrol%27'

        when:
        controller.show()

        then:
        '/error/missing' == view
        "Auto Scaling Group 'nccp-moviecontrol%27' not found in us-east-1 test" == controller.flash.message
    }

    def 'update should send aws request'() {
        final mockAutoScalingService = Mock(AwsAutoScalingService)
        controller.awsAutoScalingService = mockAutoScalingService

        controller.params.with {
            [
                name = 'hiyaworld-example-v042',
                launchConfiguration = 'newlaunchConfiguration',
                min = '31',
                desiredCapacity = '42',
                max = '153',
                defaultCooldown = '256',
                healthCheckType = 'newHealthCheckType',
                healthCheckGracePeriod = '17',
                selectedZones = 'us-feast',
                launch = 'disabled',
                terminate = 'enabled'
            ]
        }

        when:
        request.method = 'POST'
        controller.update()

        then:
        // There should be one update with appropriate ASG data
        1 * mockAutoScalingService.getAutoScalingGroup(_, 'hiyaworld-example-v042') >> {
            new AutoScalingGroup(autoScalingGroupName: 'hiyaworld-example-v042')
        }
        1 * mockAutoScalingService.updateAutoScalingGroup(_, new AutoScalingGroupData(
                'hiyaworld-example-v042', null, 31, 153, [],
                "EC2", 17, [], [] as Set, 42, null, ['us-feast'], 256, [],
                'newlaunchConfiguration', [], [:], [], [:], []), ImmutableSet.of(AutoScalingProcessType.Launch),
                ImmutableSet.of(AutoScalingProcessType.Terminate))
        0 * _._
        '/autoScaling/show/hiyaworld-example-v042' == response.redirectUrl
        "AutoScaling Group 'hiyaworld-example-v042' has been updated." == controller.flash.message
    }

    def 'update should fail for invalid process values'() {
        final mockAutoScalingService = Mock(AwsAutoScalingService)
        controller.awsAutoScalingService = mockAutoScalingService

        controller.params.with {
            [
                    name = 'hiyaworld-example-v042',
                    launchConfiguration = 'newlaunchConfiguration',
                    min = '31',
                    desiredCapacity = '42',
                    max = '153',
                    defaultCooldown = '256',
                    healthCheckType = 'newHealthCheckType',
                    healthCheckGracePeriod = '17',
                    selectedZones = 'us-feast',
                    launch = 'suspend',
                    terminate = 'resume'
            ]
        }

        when:
        request.method = 'POST'
        controller.update()

        then:
        IllegalArgumentException ex = thrown()
        ex.message == "launch must have a value in [enabled, disabled] not 'suspend'."
    }

    def 'update should display exception'() {
        final awsAutoScalingService = Mock(AwsAutoScalingService)
        awsAutoScalingService.updateAutoScalingGroup(_, _, _, _) >> {
            throw new IllegalStateException("Uh Oh!")
        }
        awsAutoScalingService.getAutoScalingGroup(_, _) >> { new AutoScalingGroup() }
        controller.awsAutoScalingService = awsAutoScalingService

        controller.params.with {
            [
                name = 'hiyaworld-example-v042',
            ]
        }

        when:
        request.method = 'POST'
        controller.update()

        then:
        '/autoScaling/edit/hiyaworld-example-v042' == response.redirectUrl
        "Could not update AutoScaling Group: java.lang.IllegalStateException: Uh Oh!" == controller.flash.message
    }

    void "create should populate model"() {
        setupHeavyWeightMocks()

        when:
        def attrs = controller.create()

        then:
        [
                'abcache', 'api', 'aws_stats', 'cryptex', 'helloworld', 'ntsuiboot', 'videometadata'
        ] == attrs['applications']*.name
        ['', 'example'] == attrs['stacks']*.name
        ['us-east-1a', 'us-east-1c', 'us-east-1d'] == attrs['zonesGroupedByPurpose'][null]
        ['helloworld--frontend', 'ntsuiboot--frontend'] == attrs.loadBalancersGroupedByVpcId[null]*.loadBalancerName
        'ami-83bd7fea' in attrs['images']*.imageId
        'nf-test-keypair-a' == attrs['defKey']
        ['amzn-linux', 'hadoop', 'nf-support', 'nf-test-keypair-a'] == attrs['keys']*.keyName
        List<String> securityGroupNames = attrs.securityGroupsGroupedByVpcId[null]*.groupName
        assert securityGroupNames.containsAll(
                ['akms', 'helloworld', 'helloworld-frontend', 'helloworld-asgardtest', 'helloworld-tmp', 'ntsuiboot'])

        //note: attrs['instanceTypes'] is sorted by price - cheapest to most expensive
        attrs['instanceTypes'][0].getMonthlyLinuxOnDemandPrice() == '$50.40'
        ['m3.medium', 'c3.large', 'm3.large', 'r3.large', 'c3.xlarge', 'm3.xlarge', 'r3.xlarge',
         'c3.2xlarge', 'm3.2xlarge', 'r3.2xlarge', 'c3.4xlarge', 'i2.xlarge', 'r3.4xlarge', 'c3.8xlarge', 'i2.2xlarge',
         'r3.8xlarge', 'i2.4xlarge', 'hs1.8xlarge', 'i2.8xlarge', 'huge.mainframe', 'c1.medium', 'c1.xlarge',
         'cc1.4xlarge', 'cc2.8xlarge', 'cg1.4xlarge', 'cr1.8xlarge', 'g2.2xlarge', 'hi1.4xlarge', 'm1.large',
         'm1.medium', 'm1.small', 'm1.xlarge', 'm2.2xlarge', 'm2.4xlarge', 'm2.xlarge', 't1.micro'
        ] == attrs['instanceTypes']*.name
    }

    def "create should handle invalid inputs"() {
        setupHeavyWeightMocks()

        controller.params.with {
            min = ''
            desiredCapacity = ''
            max = ''
            defaultCooldown = ''
            healthCheckGracePeriod = ''
        }

        when:
        def attrs = controller.create()

        then:
        attrs.group.minSize == null
        attrs.group.desiredCapacity == null
        attrs.group.maxSize == null
        attrs.group.defaultCooldown == null
        attrs.group.healthCheckGracePeriod == null
    }

    @Unroll("save should create ebs optimized #ebsOptimizedValue launch config for param #ebsOptimizedParam")
    def 'save should create ebs optimized launch config'() {
        controller.configService = Mock(ConfigService)
        controller.awsAutoScalingService = Mock(AwsAutoScalingService)
        controller.awsEc2Service = Mock(AwsEc2Service) {
            getSubnets(_) >> new Subnets([])
        }
        GroupCreateCommand cmd = new GroupCreateCommand()
        controller.params.with {
            appName = 'helloworld'
            ebsOptimized = ebsOptimizedParam
        }
        LaunchConfiguration expectedLaunchConfiguration = new LaunchConfiguration().
                withEbsOptimized(ebsOptimizedValue).withInstanceMonitoring(new InstanceMonitoring().withEnabled(false))

        when:
        request.method = 'POST'
        controller.save(cmd)

        then:
        1 * controller.awsAutoScalingService.createLaunchConfigAndAutoScalingGroup(_, _, expectedLaunchConfiguration,
                _) >> new CreateAutoScalingGroupResult()
        0 * controller.awsAutoScalingService.createLaunchConfigAndAutoScalingGroup(_, _, _, _)

        where:
        ebsOptimizedParam   | ebsOptimizedValue
        null                | false
        ''                  | false
        'true'              | true
        'false'             | false
    }

    void 'should generate group name and environment variables from ASG form inputs'() {
        request.format = 'json'
        params.appName = 'hello-c0latam'
        configService.userDataVarPrefix >> 'CLOUD_'

        when:
        controller.generateName()

        then:
        new ObjectMapper().readValue(response.contentAsString, Map) ==
                [groupName: 'hello-c0latam', envVars: ['CLOUD_COUNTRIES=latam']]
    }
}
