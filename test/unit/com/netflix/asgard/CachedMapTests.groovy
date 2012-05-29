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

class CachedMapTests extends GroovyTestCase {

    void testPutAllAndRemoveMissing() {

        CachedMap<AutoScalingGroup> cachedMap = new CachedMapBuilder(null).of(EntityType.autoScaling).
                buildCachedMap()
        cachedMap.ensureSetUp( { it.autoScalingGroupName }, { [] } )
        assert 0 == cachedMap.list().size()

        cachedMap.put('groucho', new AutoScalingGroup(autoScalingGroupName: 'groucho'))
        cachedMap.put('harpo', new AutoScalingGroup(autoScalingGroupName: 'harpo'))
        assert 2 == cachedMap.list().size()
        assert 'groucho' == cachedMap.get('groucho').autoScalingGroupName

        List<String> namesToChange = ['zeppo', 'groucho', 'chico']
        List<AutoScalingGroup> matchingGroupsFromCloud = [
                new AutoScalingGroup(autoScalingGroupName: 'chico'),
                new AutoScalingGroup(autoScalingGroupName: 'zeppo')
        ]
        cachedMap.putAllAndRemoveMissing(namesToChange, matchingGroupsFromCloud)
        assert 3 == cachedMap.list().size()
        assert ['chico', 'harpo', 'zeppo'] == cachedMap.list()*.autoScalingGroupName.sort()
        assertNull cachedMap.get('groucho')
    }
}
