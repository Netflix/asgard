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
package com.netflix.asgard.model

import com.amazonaws.services.cloudwatch.model.Dimension
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest
import com.netflix.asgard.model.AlarmData.ComparisonOperator
import com.netflix.asgard.model.AlarmData.Statistic
import spock.lang.Specification

class AlarmDataSpec extends Specification {

    final AlarmData alarmData = new AlarmData(
            alarmName: 'scale-up-alarm-fantasticService-v003-AvailabilityService_completedTasks-73',
            autoScalingGroupName: 'fantasticService-v003',
            comparisonOperator: ComparisonOperator.GreaterThanThreshold,
            metricName: 'AvailabilityService_completedTasks',
            namespace: 'NFLX/Epic',
            statistic: Statistic.Minimum,
            period: 23,
            evaluationPeriods: 7,
            threshold: 73,
            actionArns: ['arn:aws:sns:us-east-1:149000000000:nccp-wii-auto-scale-alert-topic'],
            dimensions: [AutoScalingGroupName: 'fantasticService-v003']
    )

    def 'should build AlarmData'() {
        expect:
        'scale-up-alarm-fantasticService-v003-AvailabilityService_completedTasks-73' == alarmData.alarmName
        'fantasticService-v003' == alarmData.autoScalingGroupName
        ComparisonOperator.GreaterThanThreshold == alarmData.comparisonOperator
        'AvailabilityService_completedTasks' == alarmData.metricName
        'NFLX/Epic' == alarmData.namespace
        Statistic.Minimum == alarmData.statistic
        23 == alarmData.period
        7 == alarmData.evaluationPeriods
        73 == alarmData.threshold
    }

    def 'should build AlarmData with defaults'() {
        final AlarmData expectedDefaultsAlarmData = new AlarmData(
            alarmName: 'scale-up-alarm-fantasticService-v003-AvailabilityService_completedTasks-73'
        )

        expect:
        'scale-up-alarm-fantasticService-v003-AvailabilityService_completedTasks-73' == expectedDefaultsAlarmData.alarmName
        null == expectedDefaultsAlarmData.autoScalingGroupName
        null == expectedDefaultsAlarmData.comparisonOperator
        'CPUUtilization' == expectedDefaultsAlarmData.metricName
        'AWS/EC2' == expectedDefaultsAlarmData.namespace
        Statistic.Average == expectedDefaultsAlarmData.statistic
        300 == expectedDefaultsAlarmData.period
        1 == expectedDefaultsAlarmData.evaluationPeriods
        0 == expectedDefaultsAlarmData.threshold
    }

    def 'should build AlarmData from AWS metric alarm'() {
        final MetricAlarm alarm = new MetricAlarm(
                alarmName: 'scale-down-alarm-spectacular-Encoder-v031-AmountOfStuff-32',
                comparisonOperator: 'LessThanOrEqualToThreshold',
                metricName: 'AmountOfStuff',
                namespace: 'NFLX/ASGARD',
                statistic: 'Sum',
                period: 222,
                evaluationPeriods: 3,
                threshold: 32,
                alarmActions: ['arn:aws:sns:us-east-1:149000000000:sE-auto-scale-alert-topic',
                        'arn:aws:autoscaling:us-east-1:149000000000:scalingPolicy:cf25d568-7d55-4fa7-80c8-c6ee6b088a81:autoScalingGroupName/realtimerouter:policyName/scale-down-realtimerouter-10-600'],
                dimensions: [new Dimension(name: AlarmData.DIMENSION_NAME_FOR_ASG, value: 'spectacular-Encoder-v031')],
        )

        when:
        final AlarmData alarmDataFromMetricAlarm = AlarmData.fromMetricAlarm(alarm)

        then:
        new AlarmData(
            alarmName: 'scale-down-alarm-spectacular-Encoder-v031-AmountOfStuff-32',
            autoScalingGroupName: 'spectacular-Encoder-v031',
            comparisonOperator: ComparisonOperator.LessThanOrEqualToThreshold,
            metricName: 'AmountOfStuff',
            namespace: 'NFLX/ASGARD',
            statistic: Statistic.Sum,
            period: 222,
            evaluationPeriods: 3,
            threshold: 32,
            actionArns: ['arn:aws:sns:us-east-1:149000000000:sE-auto-scale-alert-topic',
                        'arn:aws:autoscaling:us-east-1:149000000000:scalingPolicy:cf25d568-7d55-4fa7-80c8-c6ee6b088a81:autoScalingGroupName/realtimerouter:policyName/scale-down-realtimerouter-10-600'],
            policyNames: ['scale-down-realtimerouter-10-600'],
            topicNames: ['sE-auto-scale-alert-topic'],
            dimensions: [AutoScalingGroupName: 'spectacular-Encoder-v031']
        ) == alarmDataFromMetricAlarm
    }

    def 'should build AlarmData from AWS metric alarm with new topic'() {
        final MetricAlarm alarm = new MetricAlarm(
                alarmName: 'scale-down-alarm-spectacular-Encoder-v031-AmountOfStuff-32',
                comparisonOperator: 'LessThanOrEqualToThreshold',
                metricName: 'AmountOfStuff',
                namespace: 'NFLX/ASGARD',
                statistic: 'Sum',
                period: 222,
                evaluationPeriods: 3,
                threshold: 32,
                alarmActions: ['arn:aws:sns:us-east-1:149000000000:sE-auto-scale-alert-topic',
                        'arn:aws:autoscaling:us-east-1:149000000000:scalingPolicy:cf25d568-7d55-4fa7-80c8-c6ee6b088a81:autoScalingGroupName/realtimerouter:policyName/scale-down-realtimerouter-10-600'],
                dimensions: [new Dimension(name: AlarmData.DIMENSION_NAME_FOR_ASG, value: 'spectacular-Encoder-v031')],
        )

        when:
        final AlarmData alarmDataFromMetricAlarm = AlarmData.fromMetricAlarm(alarm,
                'arn:aws:sns:us-east-1:149000000000:sE-auto-scale-alert-topic2')

        then:
        new AlarmData(
                alarmName: 'scale-down-alarm-spectacular-Encoder-v031-AmountOfStuff-32',
                autoScalingGroupName: 'spectacular-Encoder-v031',
                comparisonOperator: ComparisonOperator.LessThanOrEqualToThreshold,
                metricName: 'AmountOfStuff',
                namespace: 'NFLX/ASGARD',
                statistic: Statistic.Sum,
                period: 222,
                evaluationPeriods: 3,
                threshold: 32,
                actionArns: ['arn:aws:sns:us-east-1:149000000000:sE-auto-scale-alert-topic2',
                        'arn:aws:autoscaling:us-east-1:149000000000:scalingPolicy:cf25d568-7d55-4fa7-80c8-c6ee6b088a81:autoScalingGroupName/realtimerouter:policyName/scale-down-realtimerouter-10-600'],
                policyNames: ['scale-down-realtimerouter-10-600'],
                topicNames: ['sE-auto-scale-alert-topic2'],
                dimensions: [AutoScalingGroupName: 'spectacular-Encoder-v031']
        ) == alarmDataFromMetricAlarm
    }

    def 'should build AlarmData from AWS metric alarm and clear topic'() {
        final MetricAlarm alarm = new MetricAlarm(
                alarmName: 'scale-down-alarm-spectacular-Encoder-v031-AmountOfStuff-32',
                comparisonOperator: 'LessThanOrEqualToThreshold',
                metricName: 'AmountOfStuff',
                namespace: 'NFLX/ASGARD',
                statistic: 'Sum',
                period: 222,
                evaluationPeriods: 3,
                threshold: 32,
                alarmActions: ['arn:aws:sns:us-east-1:149000000000:sE-auto-scale-alert-topic',
                        'arn:aws:autoscaling:us-east-1:149000000000:scalingPolicy:cf25d568-7d55-4fa7-80c8-c6ee6b088a81:autoScalingGroupName/realtimerouter:policyName/scale-down-realtimerouter-10-600'],
                dimensions: [new Dimension(name: AlarmData.DIMENSION_NAME_FOR_ASG, value: 'spectacular-Encoder-v031')],
        )

        when:
        final AlarmData alarmDataFromMetricAlarm = AlarmData.fromMetricAlarm(alarm, '')

        then:
        new AlarmData(
                alarmName: 'scale-down-alarm-spectacular-Encoder-v031-AmountOfStuff-32',
                autoScalingGroupName: 'spectacular-Encoder-v031',
                comparisonOperator: ComparisonOperator.LessThanOrEqualToThreshold,
                metricName: 'AmountOfStuff',
                namespace: 'NFLX/ASGARD',
                statistic: Statistic.Sum,
                period: 222,
                evaluationPeriods: 3,
                threshold: 32,
                actionArns: ['arn:aws:autoscaling:us-east-1:149000000000:scalingPolicy:cf25d568-7d55-4fa7-80c8-c6ee6b088a81:autoScalingGroupName/realtimerouter:policyName/scale-down-realtimerouter-10-600'],
                policyNames: ['scale-down-realtimerouter-10-600'],
                topicNames: [],
                dimensions: [AutoScalingGroupName: 'spectacular-Encoder-v031']
        ) == alarmDataFromMetricAlarm
    }

    def 'should copy AlarmData'() {
        final AlarmData expectedCopy = new AlarmData(
            autoScalingGroupName: 'fantasticService-v004',
            comparisonOperator: ComparisonOperator.GreaterThanThreshold,
            metricName: 'AvailabilityService_completedTasks',
            namespace: 'NFLX/Epic',
            statistic: Statistic.Minimum,
            period: 23,
            evaluationPeriods: 7,
            threshold: 73,
            actionArns: ['arn:aws:sns:us-east-1:149000000000:nccp-wii-auto-scale-alert-topic'],
            dimensions: [AutoScalingGroupName: 'fantasticService-v004']
         )

        expect:
        alarmData.copyForAsg('fantasticService-v004') == expectedCopy

    }

    def 'should create put alarm request'() {
        when:
        final PutMetricAlarmRequest alarm = alarmData.
                toPutMetricAlarmRequest('arn:aws:autoscaling:us-east-1:149000000000:scalingPolicy:cf25d568-7d55-4fa7-80c8-c6ee6b088a81:autoScalingGroupName/realtimerouter:policyName/scale-down-realtimerouter-10-600')

        then:
        'scale-up-alarm-fantasticService-v003-AvailabilityService_completedTasks-73' == alarm.alarmName
        '' == alarm.alarmDescription
        alarm.actionsEnabled
        ['arn:aws:sns:us-east-1:149000000000:nccp-wii-auto-scale-alert-topic'] == alarm.OKActions
        ['arn:aws:sns:us-east-1:149000000000:nccp-wii-auto-scale-alert-topic',
                'arn:aws:autoscaling:us-east-1:149000000000:scalingPolicy:cf25d568-7d55-4fa7-80c8-c6ee6b088a81:autoScalingGroupName/realtimerouter:policyName/scale-down-realtimerouter-10-600'] as Set == alarm.alarmActions as Set
        ['arn:aws:sns:us-east-1:149000000000:nccp-wii-auto-scale-alert-topic'] == alarm.insufficientDataActions
        'AvailabilityService_completedTasks' == alarm.metricName
        'NFLX/Epic' == alarm.namespace
        'Minimum' == alarm.statistic
                23 == alarm.period
        null == alarm.unit
        7 == alarm.evaluationPeriods
        73 == alarm.threshold
        'GreaterThanThreshold' == alarm.comparisonOperator
        [new Dimension(name: AlarmData.DIMENSION_NAME_FOR_ASG, value: 'fantasticService-v003')] == alarm.dimensions
    }

}
