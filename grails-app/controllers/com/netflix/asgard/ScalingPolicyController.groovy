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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.ScalingPolicy
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.netflix.asgard.model.AlarmData
import com.netflix.asgard.model.MetricId
import com.netflix.asgard.model.ScalingPolicyData
import com.netflix.asgard.model.TopicData
import com.netflix.asgard.model.AlarmData.ComparisonOperator
import com.netflix.asgard.model.AlarmData.Statistic
import com.netflix.asgard.model.ScalingPolicyData.AdjustmentType
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class ScalingPolicyController {

    def awsAutoScalingService
    def awsCloudWatchService
    def awsSnsService
    Caches caches

    def allowedMethods = [save: 'POST', update: 'POST', delete: 'POST']

    def index = { redirect(action: 'list', params: params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        List<ScalingPolicy> policies = awsAutoScalingService.getAllScalingPolicies(userContext).sort { it.policyName }
        Map<String, MetricAlarm> alarmsByName = caches.allAlarms.by(userContext.region).unmodifiable()
        withFormat {
            html {
                [
                        scalingPolicies: policies,
                        alarmsByName: alarmsByName,
                ]
            }
            xml { new XML(policies).render(response) }
            json { new JSON(policies).render(response) }
        }
    }

    def create = {
        String groupName = params.id ?: params.group
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(UserContext.of(request), groupName)
        String adjustmentType = params.adjustmentType ?: ScalingPolicyData.AdjustmentType.default.name()
        String adjustment = params.adjustment
        String minAdjustmentStep = params.minAdjustmentStep
        String cooldown = params.cooldown ?: '600'
        if (group) {
            [
                    adjustmentTypes: AdjustmentType.values(), group: groupName, adjustmentType: adjustmentType,
                    adjustment: adjustment, minAdjustmentStep: minAdjustmentStep, cooldown: cooldown
            ] << awsCloudWatchService.prepareForAlarmCreation(UserContext.of(request), params)
        } else {
            flash.message = "Group '${groupName}' does not exist."
            redirect(action: 'result')
        }
    }

    def show = {
        UserContext userContext = UserContext.of(request)
        String scalingPolicyName = params.id
        ScalingPolicy scalingPolicy = awsAutoScalingService.getScalingPolicy(userContext, scalingPolicyName)
        if (scalingPolicy) {
            List<MetricAlarm> alarms = awsCloudWatchService.getAlarms(userContext, scalingPolicy.alarms*.alarmName)
            Map result = [scalingPolicy: scalingPolicy, alarms: alarms]
            withFormat {
                html { result }
                xml { new XML(result).render(response) }
                json { new JSON(result).render(response) }
            }
        } else {
            Requests.renderNotFound('Scaling Policy', scalingPolicyName, this)
        }
    }

    def edit = {
        UserContext userContext = UserContext.of(request)
        String policyName = params.id ?: params.policyName
        ScalingPolicy policy = awsAutoScalingService.getScalingPolicy(userContext, policyName)
        String adjustmentType = params.adjustmentType ?: policy?.adjustmentType
        String adjustment = params.adjustment ?: policy?.scalingAdjustment
        String minAdjustmentStep = params.minAdjustmentStep ?: policy?.minAdjustmentStep
        String cooldown = params.cooldown ?: policy?.cooldown
        if (policy) {
            [
                    scalingPolicy: policy, adjustmentTypes: AdjustmentType.values(), group: policy.autoScalingGroupName,
                    adjustmentType: adjustmentType, adjustment: adjustment,
                    minAdjustmentStep: minAdjustmentStep, cooldown: cooldown
            ]
        } else {
            flash.message = "Policy '${policyName}' does not exist."
            redirect(action: 'result')
        }
    }

    def delete = {
        UserContext userContext = UserContext.of(request)
        String scalingPolicyName = params.id
        ScalingPolicy scalingPolicy = awsAutoScalingService.getScalingPolicy(userContext, scalingPolicyName)
        if (scalingPolicy) {
            awsAutoScalingService.deleteScalingPolicy(userContext, scalingPolicy)
            flash.message = "Scaling Policy '${scalingPolicyName}' has been deleted."
            redirect(controller: 'autoScaling', action: 'show', params: [id: scalingPolicy.autoScalingGroupName])
        } else {
            Requests.renderNotFound('Scaling Policy', scalingPolicyName, this)
        }
    }

    def save = { ScalingPolicyCreateCommand cmd ->
        if (cmd.hasErrors()) {
            chain(action: 'create', model: [cmd: cmd], params: params)
        } else {
            final AdjustmentType adjustmentType = cmd.adjustmentType ?
                Enum.valueOf(AdjustmentType, cmd.adjustmentType) : AdjustmentType.PercentChangeInCapacity
            final ComparisonOperator comparisonOperator = cmd.comparisonOperator ?
                Enum.valueOf(ComparisonOperator, cmd.comparisonOperator) : null
            final Statistic statistic = cmd.statistic ? Enum.valueOf(Statistic, cmd.statistic) : null
            final UserContext userContext = UserContext.of(request)
            final List<AlarmData> alarms = []
            final List<String> snsArns = []
            MetricId metricId = cmd.assembleMetric()

            // The topic is optional, but if it is specified then it should exist.
            TopicData topic = awsSnsService.getTopic(userContext, cmd.topic)
            if (cmd.topic && !topic) {
                throw new IllegalStateException("Topic '${cmd.topic}' does not exist.")
            }
            if (topic?.arn) { snsArns << topic.arn }

            Map<String, String> dimensions = AlarmData.dimensionsForAsgName(cmd.group, awsCloudWatchService.
                    getDimensionsForNamespace(metricId.namespace))
            alarms << new AlarmData(
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
            final ScalingPolicyData scalingPolicyData = new ScalingPolicyData(
                autoScalingGroupName: cmd.group,
                adjustmentType: adjustmentType,
                adjustment: cmd.adjustment,
                cooldown: cmd.cooldown,
                minAdjustmentStep: cmd.minAdjustmentStep,
                alarms: alarms,
            )
            try {
                List<String> policyNames = awsAutoScalingService.createScalingPolicies(userContext, [scalingPolicyData])
                String policyName = Check.lone(policyNames, String)
                flash.message = "Scaling Policy '${policyName}' has been created."
                redirect(action: 'show', params: [id: policyName])
            } catch (Exception e) {
                flash.message = "Could not create Scaling Policy for Auto Scaling Group '${scalingPolicyData.autoScalingGroupName}': ${e}"
                chain(action: 'create', model: [cmd: cmd], params: params)
            }
        }
    }

    def update = { ScalingPolicyUpdateCommand cmd ->
        if (cmd.hasErrors()) {
            chain(action: 'edit', model: [cmd: cmd], params: params)
        } else {
            AdjustmentType inputAdjustmentType = cmd.adjustmentType ?
                Enum.valueOf(AdjustmentType, cmd.adjustmentType) : AdjustmentType.PercentChangeInCapacity
            UserContext userContext = UserContext.of(request)
            ScalingPolicy policy = awsAutoScalingService.getScalingPolicy(userContext, cmd.policyName)
            try {
                policy.with {
                    adjustmentType = inputAdjustmentType
                    scalingAdjustment = cmd.adjustment
                    cooldown = cmd.cooldown
                    minAdjustmentStep = cmd.minAdjustmentStep
                }
                awsAutoScalingService.updateScalingPolicy(userContext, ScalingPolicyData.fromPolicyAndAlarms(policy))
                flash.message = "Scaling Policy '${policy.policyName}' has been updated."
                redirect(action: 'show', params: [id: policy.policyName])
            } catch (Exception e) {
                flash.message = "Could not update Scaling Policy '${policy.policyName}': ${e}"
                chain(action: 'edit', model: [cmd: cmd], params: params)
            }
        }
    }

    def result = { render view: '/common/result' }

}

class ScalingPolicyCreateCommand {
    String policyName
    String group

    String adjustmentType
    Integer adjustment
    Integer cooldown
    Integer minAdjustmentStep

    String comparisonOperator
    String metric
    String namespace
    String existingMetric
    String statistic
    Integer period
    Integer evaluationPeriods
    Double threshold

    String topic

    static constraints = {
        group(nullable: false, blank: false)
        adjustmentType(nullable: false, blank: false)
        adjustment(nullable: false)
        cooldown(nullable: false)
        threshold(nullable: false)
        comparisonOperator(nullable: false, blank: false)
        metric(nullable: true, validator: { Object value, ScalingPolicyCreateCommand cmd ->
            (value && cmd.namespace) || cmd.existingMetric
        })
    }

    MetricId assembleMetric() {
        existingMetric ? MetricId.fromJson(existingMetric) : MetricId.from(namespace, metric)
    }
}

class ScalingPolicyUpdateCommand {
    String policyName
    String adjustmentType
    Integer adjustment
    Integer cooldown
    Integer minAdjustmentStep

    static constraints = {
        policyName(nullable: false, blank: false)
        adjustmentType(nullable: false, blank: false)
        adjustment(nullable: false)
        cooldown(nullable: false)
    }

}
