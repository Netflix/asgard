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

class AutoScalingGroupHealthCheckTypeTests extends GroovyTestCase {

    void testEnsureValid() {
        assert 'EC2' == AutoScalingGroupHealthCheckType.ensureValidType(null)
        assert 'EC2' == AutoScalingGroupHealthCheckType.ensureValidType('')
        assert 'EC2' == AutoScalingGroupHealthCheckType.ensureValidType(' ')
        assert 'EC2' == AutoScalingGroupHealthCheckType.ensureValidType('EC2')
        assert 'ELB' == AutoScalingGroupHealthCheckType.ensureValidType('ELB')
        assert 'EC2' == AutoScalingGroupHealthCheckType.ensureValidType('ELB ')
        assert 'EC2' == AutoScalingGroupHealthCheckType.ensureValidType('elb')
        assert 'EC2' == AutoScalingGroupHealthCheckType.ensureValidType('nonsense')
    }

    void testBy() {
        assert AutoScalingGroupHealthCheckType.ELB == AutoScalingGroupHealthCheckType.by('ELB')
        assert AutoScalingGroupHealthCheckType.EC2 == AutoScalingGroupHealthCheckType.by('EC2')
        assertNull AutoScalingGroupHealthCheckType.by('blah')
        assertNull AutoScalingGroupHealthCheckType.by('')
        assertNull AutoScalingGroupHealthCheckType.by(null)
    }
}
