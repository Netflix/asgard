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
import com.netflix.asgard.model.MetricId
import com.netflix.asgard.model.ScalingPolicyData
import com.netflix.asgard.model.ScalingPolicyData.AdjustmentType

class MetaTests extends GroovyTestCase {

    void testToMap() {
        MetricId metricId = new MetricId('AWS/EC2', 'RotationsPerMinute')
        Map<String, ?> expected = [
                displayText: 'AWS/EC2 - RotationsPerMinute',
                namespace: 'AWS/EC2',
                metricName: 'RotationsPerMinute'
        ]
        assert expected == Meta.toMap(metricId)

        ScalingPolicyData scalingPolicyData = new ScalingPolicyData(
                adjustmentType: AdjustmentType.PercentChangeInCapacity, cooldown: 60, minAdjustmentStep: 3,
                autoScalingGroupName: 'helloworld-example-v122'
        )
        Map<String, ?> expectedScalingPolicyMap = [
                adjustment: null, adjustmentType: AdjustmentType.PercentChangeInCapacity, alarms: null, arn: null,
                autoScalingGroupName: 'helloworld-example-v122', cooldown: 60, minAdjustmentStep: 3,
                policyName: null
        ]
        assert expectedScalingPolicyMap == Meta.toMap(scalingPolicyData)
    }

    void testPretty() {
        assert 'Auto Scaling Group' == Meta.pretty(AutoScalingGroup)
        assert "{MaxSize: 5, AvailabilityZones: [], LoadBalancerNames: [], Instances: [], SuspendedProcesses: [], \
EnabledMetrics: [], Tags: [], TerminationPolicies: [], }".stripIndent() == Meta.
                pretty(new AutoScalingGroup().withMaxSize(5))
        assert 'null' == Meta.pretty(null)
    }

    void testSplitCamelCase() {
        assert 'lowercase' == Meta.splitCamelCase("lowercase")
        assert 'Class' == Meta.splitCamelCase("Class")
        assert 'My Class' == Meta.splitCamelCase("MyClass")
        assert 'HTML' == Meta.splitCamelCase("HTML")
        assert 'PDF Loader' == Meta.splitCamelCase("PDFLoader")
        assert 'A String' == Meta.splitCamelCase("AString")
        assert 'Simple XML Parser' == Meta.splitCamelCase("SimpleXMLParser")
        assert 'GL 11 Version' == Meta.splitCamelCase("GL11Version")

        assert 'dev Phase' == Meta.splitCamelCase("devPhase")
        assert 'hardware' == Meta.splitCamelCase("hardware")
        assert 'partners' == Meta.splitCamelCase("partners")
        assert 'revision' == Meta.splitCamelCase("revision")
        assert 'used By' == Meta.splitCamelCase("usedBy")
        assert 'red Black Swap' == Meta.splitCamelCase("redBlackSwap")
    }

    void testCopy() {
        AutoScalingGroup original = new AutoScalingGroup().withAutoScalingGroupName('hello')
        AutoScalingGroup copy = Meta.copy(original)

        assert original.autoScalingGroupName == copy.autoScalingGroupName
        assert !original.is(copy)
    }
}
