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

import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest
import com.amazonaws.services.autoscaling.model.ScalingPolicy
import com.amazonaws.services.cloudwatch.model.Dimension
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.netflix.asgard.model.ScalingPolicyData.AdjustmentType
import spock.lang.Specification
import com.netflix.asgard.model.AlarmData.Statistic
import com.netflix.asgard.model.AlarmData.ComparisonOperator

class ScalingPolicyDataSpec extends Specification {

    final AlarmData alarmData1 = new AlarmData(
            alarmName: 'scale-up-alarm-fantasticService-v003-AvailabilityService_completedTasks-73',
            autoScalingGroupName: 'fantasticService-v003',
            comparisonOperator: ComparisonOperator.GreaterThanThreshold,
            metricName: 'AvailabilityService_completedTasks',
            namespace: 'NFLX/Epic',
            statistic: Statistic.Minimum,
            period: 23,
            evaluationPeriods: 7,
            threshold: 73,
            actionArns: ['arn:aws:sns:us-east-1:149000000000:nccp-wii-auto-scale-alert-topic']
    )

    final AlarmData alarmData2 = new AlarmData(
            alarmName: 'scale-up-alarm-fantasticService-v003-AvailabilityService_completedTasks-88',
            autoScalingGroupName: 'fantasticService-v003',
            comparisonOperator: ComparisonOperator.GreaterThanThreshold,
            metricName: 'AvailabilityService_completedTasks',
            namespace: 'NFLX/Epic',
            statistic: Statistic.Minimum,
            period: 23,
            evaluationPeriods: 7,
            threshold: 88,
            actionArns: ['arn:aws:sns:us-east-1:149000000000:nccp-wii-auto-scale-alert-topic']
     )

    final ScalingPolicyData scalingPolicyData = new ScalingPolicyData(
            policyName: 'scale-up-fantasticService-v003-22-555',
            autoScalingGroupName: 'fantasticService-v003',
            adjustmentType: AdjustmentType.ChangeInCapacity,
            adjustment: 22,
            minAdjustmentStep: 3,
            cooldown: 555,
            alarms: [alarmData1, alarmData2]
     )

    def 'should build ScalingPolicyData'() {
        expect:
        'scale-up-fantasticService-v003-22-555' == scalingPolicyData.policyName
        'fantasticService-v003' == scalingPolicyData.autoScalingGroupName
        AdjustmentType.ChangeInCapacity == scalingPolicyData.adjustmentType
        22 == scalingPolicyData.adjustment
        555 == scalingPolicyData.cooldown
        [alarmData1, alarmData2] == scalingPolicyData.alarms
    }

    def 'should build ScalingPolicyData from AWS policy and alarms'() {

        final ScalingPolicy policy = new ScalingPolicy(
                policyName: 'scale-down-spectacular-Encoder-v031-27-444',
                autoScalingGroupName: 'spectacular-Encoder-v031',
                adjustmentType: 'PercentChangeInCapacity',
                scalingAdjustment: 27,
                cooldown: 444,
        )
        final MetricAlarm alarm1 = new MetricAlarm(
                alarmName: 'super-scale-downin-alarm-spectacular-Encoder-v031-AmountOfStuff-32',
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
        final MetricAlarm alarm2 = new MetricAlarm(
                alarmName: 'super-scale-downin-alarm-spectacular-Encoder-v031-AmountOfStuff-67',
                comparisonOperator: 'LessThanOrEqualToThreshold',
                metricName: 'AmountOfStuff',
                namespace: 'NFLX/ASGARD',
                statistic: 'Sum',
                period: 222,
                evaluationPeriods: 3,
                threshold: 67,
                alarmActions: ['arn:aws:sns:us-east-1:149000000000:sE-auto-scale-alert-topic',
                        'arn:aws:autoscaling:us-east-1:149000000000:scalingPolicy:cf25d568-7d55-4fa7-80c8-c6ee6b088a81:autoScalingGroupName/realtimerouter:policyName/scale-down-realtimerouter-10-600'],
                dimensions: [new Dimension(name: AlarmData.DIMENSION_NAME_FOR_ASG, value: 'spectacular-Encoder-v031')],
        )

        when:
        final ScalingPolicyData scalingPolicyForAlarmAndPolicy = ScalingPolicyData.fromPolicyAndAlarms(policy, [alarm1, alarm2])

        then:
        new ScalingPolicyData(
            policyName: 'scale-down-spectacular-Encoder-v031-27-444',
            autoScalingGroupName: 'spectacular-Encoder-v031',
            adjustmentType: AdjustmentType.PercentChangeInCapacity,
            adjustment: 27,
            cooldown: 444,
            alarms: [
                    new AlarmData(
                            alarmName: 'super-scale-downin-alarm-spectacular-Encoder-v031-AmountOfStuff-32',
                            comparisonOperator: ComparisonOperator.LessThanOrEqualToThreshold,
                            metricName: 'AmountOfStuff',
                            namespace: 'NFLX/ASGARD',
                            statistic: Statistic.Sum,
                            period: 222,
                            evaluationPeriods: 3,
                            threshold: 32,
                            actionArns: ['arn:aws:sns:us-east-1:149000000000:sE-auto-scale-alert-topic',
                                    'arn:aws:autoscaling:us-east-1:149000000000:scalingPolicy:cf25d568-7d55-4fa7-80c8-c6ee6b088a81:autoScalingGroupName/realtimerouter:policyName/scale-down-realtimerouter-10-600'],
                            autoScalingGroupName: 'spectacular-Encoder-v031',
                            policyNames: ['scale-down-realtimerouter-10-600'],
                            topicNames: ['sE-auto-scale-alert-topic'],
                            dimensions: [AutoScalingGroupName: 'spectacular-Encoder-v031']
                    ),
                    new AlarmData(
                            alarmName: 'super-scale-downin-alarm-spectacular-Encoder-v031-AmountOfStuff-67',
                            comparisonOperator: ComparisonOperator.LessThanOrEqualToThreshold,
                            metricName: 'AmountOfStuff',
                            namespace: 'NFLX/ASGARD',
                            statistic: Statistic.Sum,
                            period: 222,
                            evaluationPeriods: 3,
                            threshold: 67,
                            actionArns: ['arn:aws:sns:us-east-1:149000000000:sE-auto-scale-alert-topic',
                                    'arn:aws:autoscaling:us-east-1:149000000000:scalingPolicy:cf25d568-7d55-4fa7-80c8-c6ee6b088a81:autoScalingGroupName/realtimerouter:policyName/scale-down-realtimerouter-10-600'],
                            autoScalingGroupName: 'spectacular-Encoder-v031',
                            policyNames: ['scale-down-realtimerouter-10-600'],
                            topicNames: ['sE-auto-scale-alert-topic'],
                            dimensions: [AutoScalingGroupName: 'spectacular-Encoder-v031']
                    )
            ]
        ) == scalingPolicyForAlarmAndPolicy

    }

    def 'should copy ScalingPolicyData'() {
        final ScalingPolicyData expectedCopy = new ScalingPolicyData(
            autoScalingGroupName: 'fantasticService-v004',
            adjustmentType: AdjustmentType.ChangeInCapacity,
            adjustment: 22,
            minAdjustmentStep: 3,
            cooldown: 555,
            alarms: [
                new AlarmData(
                    autoScalingGroupName: 'fantasticService-v004',
                    comparisonOperator: ComparisonOperator.GreaterThanThreshold,
                    metricName: 'AvailabilityService_completedTasks',
                    namespace: 'NFLX/Epic',
                    statistic: Statistic.Minimum,
                    period: 23,
                    evaluationPeriods: 7,
                    threshold: 73,
                    actionArns: ['arn:aws:sns:us-east-1:149000000000:nccp-wii-auto-scale-alert-topic']),
                new AlarmData(
                    autoScalingGroupName: 'fantasticService-v004',
                    comparisonOperator: ComparisonOperator.GreaterThanThreshold,
                    metricName: 'AvailabilityService_completedTasks',
                    namespace: 'NFLX/Epic',
                    statistic: Statistic.Minimum,
                    period: 23,
                    evaluationPeriods: 7,
                    threshold: 88,
                    actionArns: ['arn:aws:sns:us-east-1:149000000000:nccp-wii-auto-scale-alert-topic'])
            ]
        )

        expect:
        scalingPolicyData.copyForAsg('fantasticService-v004') == expectedCopy
    }

    def 'should create put policy request'() {
        when:
        final PutScalingPolicyRequest request = scalingPolicyData.toPutScalingPolicyRequest()

        then:
        'fantasticService-v003' == request.autoScalingGroupName
        'scale-up-fantasticService-v003-22-555' == request.policyName
        22 == request.scalingAdjustment
        'ChangeInCapacity' == request.adjustmentType
        555 == request.cooldown
    }

}
