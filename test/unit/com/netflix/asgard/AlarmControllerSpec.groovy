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

import com.amazonaws.services.autoscaling.model.ScalingPolicy
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult
import com.amazonaws.services.cloudwatch.model.Dimension
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.AlarmData
import com.netflix.asgard.model.TopicData
import grails.test.MockUtils
import grails.test.mixin.TestFor
import spock.lang.Specification

@SuppressWarnings("GroovyPointlessArithmetic")
@TestFor(AlarmController)
class AlarmControllerSpec extends Specification {

    void setup() {
        TestUtils.setUpMockRequest()
        MockUtils.prepareForConstraintsTests(AlarmValidationCommand)
        controller.awsCloudWatchService = Mocks.awsCloudWatchService()
    }

    def 'show should display alarm'() {
        final mockAwsCloudWatchService = Mock(AwsCloudWatchService)
        controller.awsCloudWatchService = mockAwsCloudWatchService
        mockAwsCloudWatchService.getAlarm(_, 'alarm-1') >> {
            new MetricAlarm(comparisonOperator: 'GreaterThanThreshold', statistic: 'Average', alarmName: 'alarm-1')
        }

        controller.params.id = 'alarm-1'

        when:
        final attrs = controller.show()

        then:
        'alarm-1' == attrs.alarm.alarmName
    }

    def 'show should not display nonexistent alarm'() {
        controller.params.id = 'doesntexist'

        when:
        controller.show()

        then:
        '/error/missing' == view
        "Alarm 'doesntexist' not found in us-east-1 test" == controller.flash.message
    }

    def 'save should create alarm'() {
        final awsCloudWatchService = Mocks.newAwsCloudWatchService()
        final mockAmazonCloudWatchClient = Mock(AmazonCloudWatch)
        mockAmazonCloudWatchClient.describeAlarms(_) >> { new DescribeAlarmsResult() }
        awsCloudWatchService.awsClient = new MultiRegionAwsClient({ mockAmazonCloudWatchClient })
        controller.awsCloudWatchService = awsCloudWatchService

        final mockAwsSimpleDbService  = Mock(AwsSimpleDbService)
        awsCloudWatchService.awsSimpleDbService = mockAwsSimpleDbService
        mockAwsSimpleDbService.incrementAndGetSequenceNumber(_, _) >> { 1 }

        final awsSnsService = Mock(AwsSnsService)
        controller.awsSnsService = awsSnsService

        final awsAutoScalingService = Mock(AwsAutoScalingService)
        controller.awsAutoScalingService = awsAutoScalingService

        final createCommand = new AlarmValidationCommand()
        createCommand.with {
            comparisonOperator = 'GreaterThanThreshold'
            metric = 'flux_capacitor'
            namespace = 'star_trek/back_to_the_future'
            statistic = 'Average'
            period = 700
            evaluationPeriods = 5
            threshold = 80
            topic = 'api-prodConformity-Report'
            policy = 'nflx_newton_client-v003-17'
        }
        createCommand.validate()

        when:
        controller.save(createCommand)

        then:
        '/alarm/show/nflx_newton_client-v003-1' == response.redirectedUrl

        1 * awsSnsService.getTopic(_, 'api-prodConformity-Report') >> {
            new TopicData('arn:aws:sns:blah')
        }
        0 * awsSnsService.getTopic(_, _)

        1 * awsAutoScalingService.getScalingPolicy(_, 'nflx_newton_client-v003-17') >> {
            new ScalingPolicy(policyARN: 'arn:aws:autoscaling:policyArn', autoScalingGroupName: 'nflx_newton_client-v003')
        }
        0 * awsAutoScalingService.getScalingPolicy(_, _)

        1 * mockAmazonCloudWatchClient.putMetricAlarm(_) >> {
            final PutMetricAlarmRequest actual = it[0]
            final PutMetricAlarmRequest expected = new PutMetricAlarmRequest(
                alarmName: 'nflx_newton_client-v003-1',
                alarmDescription: '',
                actionsEnabled: true,
                alarmActions: ['arn:aws:autoscaling:policyArn', 'arn:aws:sns:blah'],
                metricName: 'flux_capacitor',
                namespace: 'star_trek/back_to_the_future',
                statistic: 'Average',
                dimensions: [new Dimension(name: AlarmData.DIMENSION_NAME_FOR_ASG, value: 'nflx_newton_client-v003')],
                period: 700,
                evaluationPeriods: 5,
                threshold: 80.0,
                comparisonOperator: 'GreaterThanThreshold'
            )

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
        final createCommand = new AlarmValidationCommand()
        createCommand.validate()

        when:
        controller.save(createCommand)

        then:
        '/alarm/create' == response.redirectedUrl
    }

    def 'delete should remove alarm'() {
        final awsCloudWatchService = Mocks.newAwsCloudWatchService()
        final mockAmazonCloudWatchClient = Mock(AmazonCloudWatch)
        mockAmazonCloudWatchClient.describeAlarms(_) >> { new DescribeAlarmsResult() }
        awsCloudWatchService.awsClient = new MultiRegionAwsClient({ mockAmazonCloudWatchClient })
        controller.awsCloudWatchService = awsCloudWatchService

        controller.params.with {
            id = 'scale-up-alarm-helloworld--scalingtest-v000-CPUUtilization-87'
        }

        when:
        controller.delete()

        then:
        1 * mockAmazonCloudWatchClient.describeAlarms(new DescribeAlarmsRequest(
                alarmNames: ['scale-up-alarm-helloworld--scalingtest-v000-CPUUtilization-87']
        )) >> {
            new DescribeAlarmsResult(
                metricAlarms: [new MetricAlarm(
                        alarmName: 'scale-up-alarm-helloworld--scalingtest-v000-CPUUtilization-87',
                        comparisonOperator: 'GreaterThanThreshold', evaluationPeriods: 1, metricName: 'CPUUtilization',
                        namespace: 'AWS/EC2', period: 300, statistic: 'Average', threshold: 87, alarmActions: [],
                        dimensions: [new Dimension(name: AlarmData.DIMENSION_NAME_FOR_ASG, value: 'helloworld--scalingtest-v000')]
                )]
        )}

        1 * mockAmazonCloudWatchClient.deleteAlarms(new DeleteAlarmsRequest(
                alarmNames: ['scale-up-alarm-helloworld--scalingtest-v000-CPUUtilization-87']
        ))

        0 * _._
    }

}
