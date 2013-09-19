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
import com.amazonaws.services.autoscaling.model.Instance

class AutoScalingGroupTests extends GroovyTestCase {

    void testCopy() {

        new MonkeyPatcherService().createDynamicMethods()

        Date start = new Date()
        Collection<Instance> instances = [new Instance().withInstanceId("id-blahblah")]
        AutoScalingGroup original = new AutoScalingGroup().withAutoScalingGroupName("name")
                .withAvailabilityZones(["one", "two"]).withDefaultCooldown(10).withDesiredCapacity(2).withCreatedTime(start)
                .withMaxSize(4).withMinSize(4).withLoadBalancerNames([]).withLaunchConfigurationName("lcname")
                .withInstances(instances)
        AutoScalingGroup copy = original.copy()

        assert !copy.is(original)

        copy.defaultCooldown = 30
        assert copy.defaultCooldown == 30
        assert original.defaultCooldown == 10

        copy.metaClass.appNameIsValid = true

        boolean missingPropExceptionThrown = false
        try { original.appNameIsValue } catch (MissingPropertyException ignored) { missingPropExceptionThrown = true }
        assert missingPropExceptionThrown

        assert copy.appNameIsValid

        assert original.getAvailabilityZones() == copy.getAvailabilityZones()
        assert original.getMaxSize() == copy.getMaxSize()
        assert original.getLaunchConfigurationName() == copy.getLaunchConfigurationName()

        assert original.instances[0].instanceId == copy.instances[0].instanceId
        assert !original.instances[0].is(copy.instances[0])
    }
}
