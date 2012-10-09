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
import com.amazonaws.services.autoscaling.model.ScalingPolicy
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.google.common.collect.ImmutableSet
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.AutoScalingProcessType
import grails.test.mixin.TestFor
import spock.lang.Specification

@SuppressWarnings("GroovyPointlessArithmetic")
@TestFor(AutoScalingController)
class AutoScalingControllerSpec extends Specification {

    void setup() {
        Mocks.createDynamicMethods()
        TestUtils.setUpMockRequest()
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
        controller.params.name = 'helloworld-example-v015'

        when:
        final attrs = controller.show()

        then:
        'helloworld' == attrs['app'].name
        0 == attrs['activities'].size()
        3 == attrs['instanceCount']
        attrs['group'].with {
            'helloworld-example-v015' == autoScalingGroupName
            'i-8ee4eeee' == instances[0].instanceId
        }
     }

    def 'show should return Alarm info'() {
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
        1 * mockAwsAutoScalingService.getScalingPoliciesForGroup(_, 'helloworld-example-v015') >> {[
            new ScalingPolicy(alarms: [new Alarm(alarmName: 'alarm1')]),
            new ScalingPolicy(alarms: [new Alarm(alarmName: 'alarm2'), new Alarm(alarmName: 'alarm3')]),
        ]}
        1 * mockAwsCloudWatchService.getAlarms(_, ['alarm1', 'alarm2', 'alarm3']) >> {[
            new MetricAlarm(alarmName: 'alarm1', metricName: 'metric1'),
            new MetricAlarm(alarmName: 'alarm2', metricName: 'metric2'),
            new MetricAlarm(alarmName: 'alarm3', metricName: 'metric3'),
        ]}
        attrs['alarmsByName'] == [
                alarm1: new MetricAlarm(alarmName: 'alarm1', metricName: 'metric1'),
                alarm2: new MetricAlarm(alarmName: 'alarm2', metricName: 'metric2'),
                alarm3: new MetricAlarm(alarmName: 'alarm3', metricName: 'metric3'),
        ]
    }

    def 'show should indicate nonexistent ASG'() {
        controller.params.name = 'doesntexist'

        when:
        controller.show()

        then:
        '/error/missing' == view
        "Auto Scaling Group 'doesntexist' not found in us-east-1 test" == controller.flash.message
    }

    def 'show should indicate nonexistent ASG if invalid characters are used'() {
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
        controller.update()

        then:
        '/autoScaling/edit/hiyaworld-example-v042' == response.redirectUrl
        "Could not update AutoScaling Group: java.lang.IllegalStateException: Uh Oh!" == controller.flash.message
    }

    void "create should populate model"() {
        when:
        def attrs = controller.create()

        then:
        ['abcache', 'api', 'aws_stats', 'cryptex', 'helloworld', 'ntsuiboot',
                'videometadata'] == attrs['applications']*.name
        ['', 'example'] == attrs['stacks']*.name
        ['us-east-1a', 'us-east-1c', 'us-east-1d'] == attrs['zonesGroupedByPurpose'][null]
        ['helloworld--frontend', 'ntsuiboot--frontend'] == attrs.loadBalancersGroupedByVpcId[null]*.loadBalancerName
        'ami-83bd7fea' in attrs['images']*.imageId
        'nf-test-keypair-a' == attrs['defKey']
        ['amzn-linux', 'hadoop', 'nf-support', 'nf-test-keypair-a'] == attrs['keys']*.keyName
        List<String> securityGroupNames = attrs.securityGroupsGroupedByVpcId[null]*.groupName
        assert securityGroupNames.containsAll(['akms', 'helloworld', 'helloworld-frontend', 'helloworld-asgardtest',
                'helloworld-tmp', 'ntsuiboot'])
        [
                't1.micro', 'm1.small', 'c1.medium', 'm1.large', 'm2.xlarge', 'c1.xlarge', 'm1.xlarge', 'm2.2xlarge',
                'cc1.4xlarge', 'm2.4xlarge', 'cg1.4xlarge', 'cc2.8xlarge', 'huge.mainframe'
        ] == attrs['instanceTypes']*.name
    }

    def "create should handle invalid inputs"() {
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
}
