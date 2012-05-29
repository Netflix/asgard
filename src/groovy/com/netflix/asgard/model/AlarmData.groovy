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
import com.google.common.collect.Lists
import com.netflix.asgard.Check
import com.netflix.asgard.Relationships

@Immutable
final class AlarmData {

    static final DIMENSION_NAME_FOR_ASG = 'AutoScalingGroupName'

    enum ComparisonOperator {
        GreaterThanOrEqualToThreshold('>='),
        GreaterThanThreshold('>'),
        LessThanOrEqualToThreshold('<='),
        LessThanThreshold('<')

        final String displayValue
        ComparisonOperator(String displayValue) {
            this.displayValue = displayValue
        }
    }

    static enum Statistic { Average, Maximum, Minimum, SampleCount, Sum
        static Statistic getDefault() { Average }
    }

    String alarmName
    String description = ''
    ComparisonOperator comparisonOperator
    String metricName = 'CPUUtilization'
    String namespace = 'AWS/EC2'
    Statistic statistic = Statistic.Average
    Integer period = 300
    Integer evaluationPeriods = 1
    Double threshold = 0
    Set<String> actionArns = []
    String autoScalingGroupName
    List<String> policyNames = []
    List<String> topicNames = []

    static AlarmData fromMetricAlarm(MetricAlarm metricAlarm, String newTopicArn = null) {
        Check.notNull(metricAlarm, MetricAlarm)
        ComparisonOperator comparison = Enum.valueOf(AlarmData.ComparisonOperator, metricAlarm.comparisonOperator)
        Statistic statistic = Enum.valueOf(AlarmData.Statistic, metricAlarm.statistic)
        String autoScalingGroupName = metricAlarm.dimensions.find { it.name == DIMENSION_NAME_FOR_ASG }?.value
        String policyIndicator = ':policyName/'
        List<String> policyArns = metricAlarm.alarmActions.findAll { it.indexOf(policyIndicator) > 0 }
        List<String> policyNames = policyArns.collect {
            it.substring(it.indexOf(policyIndicator) + policyIndicator.size())
        }
        Closure topicNameFromArn = { it.substring(it.lastIndexOf(':') + 1) }
        List<String> topicArns
        if (newTopicArn == null) {
            String topicIndicator = 'arn:aws:sns:'
            topicArns = metricAlarm.alarmActions.findAll { it.startsWith(topicIndicator) }
        } else {
            topicArns = newTopicArn ? [newTopicArn] : []
        }
        List<String> actionArns = Lists.newArrayList(policyArns)
        actionArns.addAll(topicArns)
        List<String> topicNames = topicArns.collect(topicNameFromArn)
        new AlarmData(
                alarmName: metricAlarm.alarmName,
                description: metricAlarm.alarmDescription,
                comparisonOperator: comparison,
                metricName: metricAlarm.metricName,
                namespace: metricAlarm.namespace,
                statistic: statistic,
                period: metricAlarm.period,
                evaluationPeriods: metricAlarm.evaluationPeriods,
                threshold: metricAlarm.threshold,
                actionArns: actionArns,
                autoScalingGroupName: autoScalingGroupName,
                policyNames: policyNames,
                topicNames: topicNames
         )
    }

    AlarmData copyForAsg(String newAutoScalingGroupName) {
        new AlarmData(
                description: description,
                comparisonOperator: comparisonOperator,
                metricName: metricName,
                namespace: namespace,
                statistic: statistic,
                period: period,
                evaluationPeriods: evaluationPeriods,
                threshold: threshold,
                actionArns: actionArns,
                autoScalingGroupName: newAutoScalingGroupName
         )
    }

    PutMetricAlarmRequest toPutMetricAlarmRequest(String policyArn = null, String id = null) {
        final List<String> alarmActions = []
        if (policyArn) {
            alarmActions << policyArn
            Collection<String> snsArns = actionArns.findAll { it.startsWith('arn:aws:sns:') }
            alarmActions.addAll(snsArns)
        } else {
            alarmActions.addAll(actionArns)
        }
        final List<Dimension> dimensions = [new Dimension(name: AlarmData.DIMENSION_NAME_FOR_ASG,
                value: autoScalingGroupName)]
        String alarmName = id ? Relationships.buildAlarmName(autoScalingGroupName, id) : this.alarmName
        new PutMetricAlarmRequest(
                alarmName: alarmName,
                alarmDescription: description,
                actionsEnabled: true,
                metricName: metricName,
                namespace: namespace,
                period: period,
                evaluationPeriods: evaluationPeriods,
                threshold: threshold,
                comparisonOperator: comparisonOperator?.name(),
                statistic: statistic.name(),
                alarmActions: alarmActions,
                dimensions: dimensions
        )
    }

}
