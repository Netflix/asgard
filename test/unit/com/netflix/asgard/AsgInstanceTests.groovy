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

import com.amazonaws.services.autoscaling.model.Instance
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription

class AsgInstanceTests extends GroovyTestCase {

    void testCopy() {

        new MonkeyPatcherService().createDynamicMethods()

        Instance original = new Instance().withInstanceId("i-test").withAvailabilityZone("us-east-1d").withLifecycleState("running")

        Instance copy = original.copy()

        assert !copy.is(original)
        assert copy == original

        copy.setAvailabilityZone "us-east-1c"
        assert copy.getAvailabilityZone() == "us-east-1c"
        assert original.getAvailabilityZone() == "us-east-1d"
        copy.metaClass.loadBalancers = [new LoadBalancerDescription()]

        boolean missingPropExceptionThrown = false
        try { original.loadBalancers } catch (MissingPropertyException ignored) { missingPropExceptionThrown = true }
        assert missingPropExceptionThrown

        assert 1 == copy.loadBalancers.size()

        assert original.getInstanceId() == copy.getInstanceId()
        assert original.getLifecycleState() == copy.getLifecycleState()
    }
}
