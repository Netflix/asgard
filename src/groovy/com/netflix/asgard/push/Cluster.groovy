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
package com.netflix.asgard.push

import com.netflix.asgard.Check
import com.netflix.asgard.Relationships
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.GroupedInstance

/**
 * Immutable container of auto scaling groups guaranteed to contain an ordered list of groups that share the same
 * cluster name.
 */
class Cluster extends AbstractList<AutoScalingGroupData> {

    final String name
    final List<AutoScalingGroupData> groups

    /**
     * Creates a cluster object for the specified Auto Scaling Groups, sorted based on created time.
     *
     * @param groups
     */
    Cluster(List<AutoScalingGroupData> groups) {

        // Ensure there are some groups and they all belong in the same cluster
        Check.positive(groups?.size(), 'group size')
        name = Relationships.clusterFromGroupName(groups[0].autoScalingGroupName)
        for (AutoScalingGroupData group : groups) {
            Check.equal(name, Relationships.clusterFromGroupName(group.autoScalingGroupName))
        }

        // Sort the groups into push order and store them in this Cluster object
        this.groups = Collections.unmodifiableList(groups.sort { a, b -> a.createdTime.time <=> b.createdTime.time })
    }

    AutoScalingGroupData last() { groups.size() >= 1 ? groups[size() - 1] : null }

    @Override
    AutoScalingGroupData get(int i) { groups[i] }

    @Override
    int size() { groups.size() }

    List<GroupedInstance> getInstances() { groups.collect { it.instances }.flatten() }
}
