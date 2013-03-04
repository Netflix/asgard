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
import com.amazonaws.services.cloudwatch.model.Dimension
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.netflix.asgard.model.AlarmData
import com.netflix.asgard.model.AlarmData.ComparisonOperator
import com.netflix.asgard.model.AlarmData.Statistic
import com.netflix.asgard.model.MetricId
import com.netflix.asgard.model.TopicData
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class AlarmController {

    def awsCloudWatchService
    def awsSnsService
    def awsAutoScalingService

    def allowedMethods = [save: 'POST', update: 'POST', delete: 'POST']

    def index = { redirect(action: 'list', params: params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        List<MetricAlarm> alarms =
                (awsCloudWatchService.getAllAlarms(userContext) as List).sort { it.alarmName?.toLowerCase() }
        Map details = ['alarms': alarms]
        withFormat {
            html { details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    def create = {
        String policyName = params.id ?: params.policy
        UserContext userContext = UserContext.of(request)
        ScalingPolicy policy = awsAutoScalingService.getScalingPolicy(userContext, policyName)
        if (!policy) {
            flash.message = "Policy '${policyName}' does not exist."
            redirect(action: 'result')
            return
        }
        awsCloudWatchService.prepareForAlarmCreation(userContext, params) <<
                [ policy: policyName ]
    }

    def show = {
        UserContext userContext = UserContext.of(request)
        String alarmName = params.id
        MetricAlarm alarm = awsCloudWatchService.getAlarm(userContext, alarmName)
        if (!alarm) {
            Requests.renderNotFound('Alarm', alarmName, this)
        } else {
            alarm.alarmActions.sort()
            alarm.getOKActions().sort()
            alarm.insufficientDataActions.sort()
            List<String> policies = AlarmData.fromMetricAlarm(alarm).policyNames
            Map result = [alarm: alarm, policies: policies]
            withFormat {
                html { return result }
                xml { new XML(result).render(response) }
                json { new JSON(result).render(response) }
            }
        }
    }

    def edit = {
        UserContext userContext = UserContext.of(request)
        String alarmName = params.id ?: params.alarmName
        MetricAlarm alarm = awsCloudWatchService.getAlarm(userContext, alarmName)
        if (!alarm) {
            flash.message = "Alarm '${alarmName}' does not exist."
            redirect(action: 'result')
            return
        }
        AlarmData alarmData = AlarmData.fromMetricAlarm(alarm)
        awsCloudWatchService.prepareForAlarmCreation(userContext, params, alarmData) <<
                [ policy: params.policy, alarmName: alarmName ]
    }

    def delete = {
        final UserContext userContext = UserContext.of(request)
        final String alarmName = params.id
        final MetricAlarm alarm = awsCloudWatchService.getAlarm(userContext, alarmName)
        if (alarm) {
            awsCloudWatchService.deleteAlarms(userContext, [alarmName])
            List<String> policies = AlarmData.fromMetricAlarm(alarm).policyNames
            flash.message = "Alarm ${alarmName} has been deleted."
            chooseRedirect(policies)
        } else {
            Requests.renderNotFound('Alarm', alarmName, this)
        }
    }

    private void chooseRedirect(List<String> policies) {
        Map destination = [action: 'result']
        if (policies?.size() == 1) {
            destination = [controller: 'scalingPolicy', action: 'show', id: policies[0]]
        }
        redirect destination
    }

    def result = { render view: '/common/result' }

    def save = { AlarmValidationCommand cmd ->
        if (cmd.hasErrors()) {
            chain(action: 'create', model: [cmd: cmd], params: params)
        } else {
            final ComparisonOperator comparisonOperator = cmd.comparisonOperator ?
                Enum.valueOf(ComparisonOperator, cmd.comparisonOperator) : null
            final Statistic statistic = cmd.statistic ? Enum.valueOf(Statistic, cmd.statistic) : Statistic.Average
            final UserContext userContext = UserContext.of(request)
            final Collection<String> snsArns = []
            MetricId metricId = cmd.assembleMetric()

            // The topic and policy are optional, but if they are specified then they should exist.
            TopicData topic = awsSnsService.getTopic(userContext, cmd.topic)
            ScalingPolicy policy = awsAutoScalingService.getScalingPolicy(userContext, cmd.policy)
            if (cmd.topic && !topic) {
                throw new IllegalStateException("Topic '${cmd.topic}' does not exist.")
            }
            if (topic?.arn) { snsArns << topic.arn }
            if (cmd.policy && !policy) {
                    throw new IllegalStateException("Scaling Policy '${cmd.policy}' does not exist.")
            }

            Map<String, String> dimensions = AlarmData.dimensionsForAsgName(policy?.autoScalingGroupName,
                    awsCloudWatchService.getDimensionsForNamespace(metricId.namespace))
            final alarm = new AlarmData(
                    description: cmd.description,
                    comparisonOperator: comparisonOperator,
                    metricName: metricId.metricName,
                    namespace: metricId.namespace,
                    statistic: statistic,
                    period: cmd.period,
                    evaluationPeriods: cmd.evaluationPeriods,
                    threshold: cmd.threshold,
                    actionArns: snsArns,
                    dimensions: dimensions
                )
            try {
                String alarmName = awsCloudWatchService.createAlarm(userContext, alarm, policy.policyARN)
                flash.message = "Alarm '${alarmName}' has been created."
                redirect(action: 'show', params: [id: alarmName])
            } catch (Exception e) {
                flash.message = "Could not create Alarm for Scaling Policy '${cmd.policy}': ${e}"
                chain(action: 'create', model: [cmd: cmd], params: params)
            }
        }
    }

    def update = { AlarmValidationCommand cmd ->
        if (cmd.hasErrors()) {
            chain(action: 'edit', model: [cmd: cmd], params: params)
        } else {
            final UserContext userContext = UserContext.of(request)
            MetricId metricId = cmd.assembleMetric()
            MetricAlarm alarm = awsCloudWatchService.getAlarm(userContext, cmd.alarmName)
            alarm.with {
                alarmName = cmd.alarmName
                alarmDescription = cmd.description
                comparisonOperator = cmd.comparisonOperator
                metricName = metricId.metricName
                namespace = metricId.namespace
                statistic = cmd.statistic
                period = cmd.period
                evaluationPeriods = cmd.evaluationPeriods
                threshold = cmd.threshold
            }
            alarm.dimensions = []
            awsCloudWatchService.getDimensionsForNamespace(metricId.namespace).each {
                String value = params[it]
                if (value) {
                    alarm.dimensions << new Dimension(name: it, value: params[it])
                }
            }
            // The topic is optional, but if it is specified then it should exist.
            TopicData topic = awsSnsService.getTopic(userContext, cmd.topic)
            if (cmd.topic && !topic) {
                throw new IllegalStateException("Topic '${cmd.topic}' does not exist.")
            }
            String topicArn = topic?.arn ?: ''
            try {
                awsCloudWatchService.updateAlarm(userContext, AlarmData.fromMetricAlarm(alarm, topicArn))
                flash.message = "Alarm '${alarm.alarmName}' has been updated."
                redirect(action: 'show', params: [id: alarm.alarmName])
            } catch (Exception e) {
                flash.message = "Could not update Alarm '${alarm.alarmName}': ${e}"
                chain(action: 'edit', model: [cmd: cmd], params: params)
            }
        }
    }

    def setState = {
        String alarm = params.alarm
        String state = params.state
        UserContext userContext = UserContext.of(request)
        awsCloudWatchService.setAlarmState(userContext, alarm, state)
        redirect(action: 'show', params: [id: alarm])

    }

}

class AlarmValidationCommand {
    String alarmName
    String description

    String comparisonOperator
    String metric
    String namespace
    String existingMetric
    String statistic
    Integer period
    Integer evaluationPeriods
    Double threshold

    String topic
    String policy

    static constraints = {
        comparisonOperator(nullable: false, blank: false)
        threshold(nullable: false)
        metric(nullable: true, validator: { Object value, AlarmValidationCommand cmd ->
            (value && cmd.namespace) || cmd.existingMetric
        })
    }

    MetricId assembleMetric() {
        existingMetric ? MetricId.fromJson(existingMetric) : MetricId.from(namespace, metric)
    }
}
