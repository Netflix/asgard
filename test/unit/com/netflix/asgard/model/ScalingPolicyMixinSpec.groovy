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

import com.amazonaws.services.autoscaling.model.ScalingPolicy
import spock.lang.Specification

class ScalingPolicyMixinSpec extends Specification {

    def setup() {
        ScalingPolicy.mixin ScalingPolicyMixin
    }

    def 'should display percentage change in capacity'() {
        ScalingPolicy policy = new ScalingPolicy(adjustmentType: 'PercentChangeInCapacity', scalingAdjustment: 10)

        expect:
        policy.toDisplayValue() == '10%'
    }

    def 'should display percentage change in capacity with minimum adjustment'() {
        ScalingPolicy policy = new ScalingPolicy(adjustmentType: 'PercentChangeInCapacity', scalingAdjustment: 10)

        expect:
        policy.toDisplayValue() == '10%'
    }

    def 'should display exact capacity'() {
        ScalingPolicy policy = new ScalingPolicy(adjustmentType: 'ExactCapacity', scalingAdjustment: 10)

        expect:
        policy.toDisplayValue() == '10'
    }

    def 'should display positive change capacity'() {
        ScalingPolicy policy = new ScalingPolicy(adjustmentType: 'ChangeInCapacity', scalingAdjustment: 10)

        expect:
        policy.toDisplayValue() == '+10'
    }

    def 'should display negative change capacity'() {
        ScalingPolicy policy = new ScalingPolicy(adjustmentType: 'ChangeInCapacity', scalingAdjustment: -10)

        expect:
        policy.toDisplayValue() == '-10'
    }
}
