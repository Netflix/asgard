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
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.netflix.asgard.Check
import com.netflix.asgard.Relationships
import groovy.transform.Immutable

@Immutable
final class ScalingPolicyData {

    static enum AdjustmentType { ChangeInCapacity, ExactCapacity, PercentChangeInCapacity
        static AdjustmentType getDefault() { PercentChangeInCapacity }
    }

    String arn
    String policyName
    String autoScalingGroupName
    AdjustmentType adjustmentType = AdjustmentType.PercentChangeInCapacity
    Integer adjustment
    Integer cooldown
    Integer minAdjustmentStep
    Collection<AlarmData> alarms

    static ScalingPolicyData fromPolicyAndAlarms(ScalingPolicy scalingPolicy, Collection<MetricAlarm> alarms = []) {
        Check.notNull(scalingPolicy, ScalingPolicy)
        AdjustmentType adjustmentType = Enum.valueOf(ScalingPolicyData.AdjustmentType, scalingPolicy.adjustmentType)
        final List<AlarmData> alarmDatas = alarms.collect { AlarmData.fromMetricAlarm(it) }
        new ScalingPolicyData(
                arn: scalingPolicy.policyARN,
                policyName: scalingPolicy.policyName,
                autoScalingGroupName: scalingPolicy.autoScalingGroupName,
                adjustment: scalingPolicy.scalingAdjustment,
                cooldown: scalingPolicy.cooldown,
                adjustmentType: adjustmentType,
                minAdjustmentStep: scalingPolicy.minAdjustmentStep,
                alarms: alarmDatas
         )
    }

    ScalingPolicyData copyForAsg(String newAutoScalingGroupName) {
        final List<AlarmData> alarmCopies = alarms.collect { it.copyForAsg(newAutoScalingGroupName) }
        new ScalingPolicyData(
                autoScalingGroupName: newAutoScalingGroupName,
                adjustment: adjustment,
                minAdjustmentStep: minAdjustmentStep,
                adjustmentType: adjustmentType,
                cooldown: cooldown,
                alarms: alarmCopies
         )
    }

    PutScalingPolicyRequest toPutScalingPolicyRequest(String id = null) {
        String policyName = id ? Relationships.buildScalingPolicyName(autoScalingGroupName, id) : this.policyName
        new PutScalingPolicyRequest(
                policyName: policyName,
                autoScalingGroupName: autoScalingGroupName,
                scalingAdjustment: adjustment,
                minAdjustmentStep: minAdjustmentStep,
                adjustmentType: adjustmentType.name(),
                cooldown: cooldown
        )
    }

}
