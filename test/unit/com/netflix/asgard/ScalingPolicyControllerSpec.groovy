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
import com.amazonaws.services.autoscaling.model.Alarm
import com.amazonaws.services.autoscaling.model.DeletePolicyRequest
import com.amazonaws.services.autoscaling.model.DescribePoliciesRequest
import com.amazonaws.services.autoscaling.model.DescribePoliciesResult
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest
import com.amazonaws.services.autoscaling.model.PutScalingPolicyResult
import com.amazonaws.services.autoscaling.model.ScalingPolicy
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult
import com.amazonaws.services.cloudwatch.model.Dimension
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.AlarmData
import com.netflix.asgard.model.TopicData
import grails.test.MockUtils
import grails.test.mixin.TestFor
import spock.lang.Specification

@SuppressWarnings("GroovyPointlessArithmetic")
@TestFor(ScalingPolicyController)
class ScalingPolicyControllerSpec extends Specification {

    void setup() {
        TestUtils.setUpMockRequest()
        MockUtils.prepareForConstraintsTests(ScalingPolicyCreateCommand)
        controller.awsAutoScalingService = Mocks.awsAutoScalingService()
    }

    def 'save should create scaling policy and associated alarm'() {
        final awsAutoScalingService = Mocks.newAwsAutoScalingService()
        final mockAmazonAutoScalingClient = Mock(AmazonAutoScaling)
        mockAmazonAutoScalingClient.describePolicies(_) >> { new DescribePoliciesResult() }
        awsAutoScalingService.awsClient = new MultiRegionAwsClient({ mockAmazonAutoScalingClient })

        final awsCloudWatchService = Mocks.newAwsCloudWatchService()
        final mockAmazonCloudWatchClient = Mock(AmazonCloudWatch)
        mockAmazonCloudWatchClient.describeAlarms(_) >> { new DescribeAlarmsResult() }
        awsCloudWatchService.awsClient = new MultiRegionAwsClient({ mockAmazonCloudWatchClient })
        awsAutoScalingService.awsCloudWatchService = awsCloudWatchService

        final mockAwsSimpleDbService  = Mock(AwsSimpleDbService)
        awsAutoScalingService.awsSimpleDbService = mockAwsSimpleDbService
        awsCloudWatchService.awsSimpleDbService = mockAwsSimpleDbService
        mockAwsSimpleDbService.incrementAndGetSequenceNumber(_, _) >> { 1 }

        controller.awsAutoScalingService = awsAutoScalingService

        final awsSnsService = Mock(AwsSnsService)
        controller.awsSnsService = awsSnsService

        final createCommand = new ScalingPolicyCreateCommand()
        createCommand.with {
            group = 'nflx_newton_client-v003'
            adjustmentType = 'PercentChangeInCapacity'
            adjustment = 50
            cooldown = 500
            comparisonOperator = 'GreaterThanThreshold'
            metric = 'flux_capacitor'
            namespace = 'star_trek/back_to_the_future'
            statistic = 'Average'
            period = 700
            evaluationPeriods = 5
            threshold = 80
            topic = 'api-prodConformity-Report'
        }
        createCommand.validate()

        when:
        controller.save(createCommand)

        then:
        '/scalingPolicy/show/nflx_newton_client-v003-1' == response.redirectUrl

        1 * awsSnsService.getTopic(_, 'api-prodConformity-Report') >> {
            new TopicData('arn:aws:sns:blah')
        }
        0 * awsSnsService.getTopic(_, _)

        1 * mockAmazonAutoScalingClient.putScalingPolicy(new PutScalingPolicyRequest(
                policyName: 'nflx_newton_client-v003-1',
                autoScalingGroupName: 'nflx_newton_client-v003',
                scalingAdjustment: 50,
                adjustmentType: 'PercentChangeInCapacity',
                cooldown: 500
            )) >> {
            new PutScalingPolicyResult().withPolicyARN('arn:blah')
        }
        0 * mockAmazonAutoScalingClient.putScalingPolicy(_)

        1 * mockAmazonCloudWatchClient.putMetricAlarm(_) >> {
            final PutMetricAlarmRequest actual = it[0]
            final PutMetricAlarmRequest expected = new PutMetricAlarmRequest(
                alarmName: 'nflx_newton_client-v003-1',
                alarmDescription: '',
                actionsEnabled: true,
                alarmActions: ['arn:blah', 'arn:aws:sns:blah'],
                metricName: 'flux_capacitor',
                namespace: 'star_trek/back_to_the_future',
                statistic: 'Average',
                dimensions: [new Dimension(name: AlarmData.DIMENSION_NAME_FOR_ASG, value: 'nflx_newton_client-v003')],
                period: 700,
                evaluationPeriods: 5,
                threshold: 80.0,
                comparisonOperator: 'GreaterThanThreshold'
            )

            //   assert it[0] == expected
            // This would be a nice way to compare, but it fails. Since Amazon did not provide equality in their API,
            // we use mixins to implement equality using reflection and straight Java (via Apache).
            // In this case, PutMetricAlarmRequest contains Dimension references. Both those classes use Groovy mixins
            // to add Java equals methods. The PutMetricAlarmRequest mixin-based equals method ignores the Dimension
            // mixin-based equals method.
            // Perhaps we need a Groovy implementation of reflection equals rather than Apache's java implementation?
            // Instead we compare field by field so that the mixins will work as intended. We are basically flattening
            // out the comparison because nested mixins don't work in this case.
            [
                'alarmName', 'alarmDescription', 'actionsEnabled', 'alarmActions', 'metricName', 'namespace',
                'statistic', 'dimensions', 'period', 'evaluationPeriods', 'threshold', 'comparisonOperator',
            ].each {
                assert actual[it] == expected[it]
            }
        }
        0 * mockAmazonCloudWatchClient.putMetricAlarm(_)
    }

    def 'save should fail without required values'() {
        final createCommand = new ScalingPolicyCreateCommand()
        createCommand.validate()

        when:
        controller.save(createCommand)

        then:
        createCommand.hasErrors()
        '/scalingPolicy/create' == response.redirectUrl
    }

    def 'delete should remove policy'() {
        final awsAutoScalingService = Mocks.newAwsAutoScalingService()
        final mockAmazonAutoScalingClient = Mock(AmazonAutoScaling)
        awsAutoScalingService.awsClient = new MultiRegionAwsClient({ mockAmazonAutoScalingClient })
        controller.awsAutoScalingService = awsAutoScalingService

        final awsCloudWatchService = Mocks.newAwsCloudWatchService()
        final mockAmazonCloudWatchClient = Mock(AmazonCloudWatch)
        awsCloudWatchService.awsClient = new MultiRegionAwsClient({ mockAmazonCloudWatchClient })
        awsAutoScalingService.awsCloudWatchService = awsCloudWatchService

        controller.params.id = 'scale-up-helloworld--scalingtest-v000-32-301'

        when:
        controller.delete()

        then:
        mockAmazonAutoScalingClient.describePolicies(new DescribePoliciesRequest(
                autoScalingGroupName: null,
                policyNames: ['scale-up-helloworld--scalingtest-v000-32-301']
        )) >> { new DescribePoliciesResult(
                scalingPolicies: [new ScalingPolicy(
                        policyName: 'scale-up-helloworld--scalingtest-v000-32-301',
                        adjustmentType: 'PercentChangeInCapacity', scalingAdjustment: 32,
                        autoScalingGroupName: 'helloworld--scalingtest-v000', cooldown: 301,
                        alarms: []
                )]
        )}

        1 * mockAmazonAutoScalingClient.deletePolicy(new DeletePolicyRequest(
                autoScalingGroupName: 'helloworld--scalingtest-v000',
                policyName: 'scale-up-helloworld--scalingtest-v000-32-301'
        ))

        0 * _._
    }

    def 'delete should remove policy and alarm'() {
        final awsAutoScalingService = Mocks.newAwsAutoScalingService()
        final mockAmazonAutoScalingClient = Mock(AmazonAutoScaling)
        awsAutoScalingService.awsClient = new MultiRegionAwsClient({ mockAmazonAutoScalingClient })
        controller.awsAutoScalingService = awsAutoScalingService

        final awsCloudWatchService = Mocks.newAwsCloudWatchService()
        final mockAmazonCloudWatchClient = Mock(AmazonCloudWatch)
        awsCloudWatchService.awsClient = new MultiRegionAwsClient({ mockAmazonCloudWatchClient })
        awsAutoScalingService.awsCloudWatchService = awsCloudWatchService

        controller.params.id = 'scale-up-helloworld--scalingtest-v000-32-301'

        when:
        controller.delete()

        then:
        mockAmazonAutoScalingClient.describePolicies(new DescribePoliciesRequest(
                autoScalingGroupName: null,
                policyNames: ['scale-up-helloworld--scalingtest-v000-32-301']
        )) >> { new DescribePoliciesResult(
                scalingPolicies: [new ScalingPolicy(
                        policyName: 'scale-up-helloworld--scalingtest-v000-32-301',
                        adjustmentType: 'PercentChangeInCapacity', scalingAdjustment: 32,
                        autoScalingGroupName: 'helloworld--scalingtest-v000', cooldown: 301,
                        alarms: [new Alarm(alarmName: 'scale-up-alarm-helloworld--scalingtest-v000-CPUUtilization-87')]
                )]
        )}

        1 * mockAmazonAutoScalingClient.deletePolicy(new DeletePolicyRequest(
                autoScalingGroupName: 'helloworld--scalingtest-v000',
                policyName: 'scale-up-helloworld--scalingtest-v000-32-301'
        ))

        1 * mockAmazonCloudWatchClient.deleteAlarms(new DeleteAlarmsRequest(
                alarmNames: ['scale-up-alarm-helloworld--scalingtest-v000-CPUUtilization-87']
        ))

        0 * _._
    }

}
