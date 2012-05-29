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

class GroupedInstanceStateTests extends GroovyTestCase {

    private GroupedInstanceState makeOne() {
        new GroupedInstanceState([
                discoveryStatus: 'UP',
                imageId: 'ami-deadbeef',
                buildJobName: '/some/job',
                buildNumber: '4',
                loadBalancers: ['hello', 'goodbye']])
    }

    void testEqualsAndHashCode() {
        GroupedInstanceState instanceState1 = makeOne()
        GroupedInstanceState instanceState2 = makeOne()
        assert instanceState1 == instanceState2
        assert instanceState2 == instanceState1
    }

    void testHashCode() {
        GroupedInstanceState instanceState1 = makeOne()
        GroupedInstanceState instanceState2 = makeOne()
        Map<GroupedInstanceState, Integer> statesToCounts = [:]
        statesToCounts.put(instanceState1, 9)
        assert statesToCounts.containsKey(instanceState2)
        assert 9 == statesToCounts.get(instanceState2)
    }
}
