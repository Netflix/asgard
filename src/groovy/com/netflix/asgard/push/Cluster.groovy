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

import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.GroupedInstance

/**
 * Immutable container of auto scaling groups guaranteed to contain an ordered list of groups that share the same
 * cluster name.
 */
class Cluster extends AbstractList<AutoScalingGroupData> {

    final List<AutoScalingGroupData> groups

    Cluster(List<AutoScalingGroupData> groups, int clusterMax, int sequenceMax) {
        if (!groups.size()) {
            throw new IllegalArgumentException("No AutoScalingGroupData was provided!")
        }
        def clusterNames = groups*.getNames().cluster.unique()
        if (clusterNames.size() != 1) {
            throw new IllegalArgumentException("AutoScalingGroupData belongs to different clusters!")
        }
        def sorted = new PushSequenceComparator(clusterNames.first(), clusterMax, sequenceMax).sort(groups)
        this.groups = Collections.unmodifiableList(sorted)
    }

    AutoScalingGroupData last() {
        groups.size() >= 1 ? groups[size() - 1] : null
    }

    @Override
    AutoScalingGroupData get(int i) {
        groups[i]
    }

    @Override
    int size() {
        groups.size()
    }

    /**
     * @return Returns the Asgard representation of the AWS instances in this cluster
     */
    List<GroupedInstance> getInstances() {
        groups*.instances.flatten()
    }

    static class PushSequenceComparator implements Comparator<AutoScalingGroupData> {

        private final String name
        private final Integer clusterMax
        private final Integer sequenceMax
        private final Integer sequenceDistanceMax

        PushSequenceComparator(String name, Integer clusterMax, Integer sequenceMax) {
            this.name = name
            this.clusterMax = clusterMax
            this.sequenceMax = sequenceMax
            this.sequenceDistanceMax = sequenceMax - clusterMax * 2
        }

        @Override
        int compare(AutoScalingGroupData a, AutoScalingGroupData b) {
            final aNames = a.names
            final bNames = b.names
            if (aNames.cluster != bNames.cluster) {
                clusterNotTheSame a, b
            }
            final aSeq = aNames.sequence
            final bSeq = bNames.sequence
            if (!aSeq || !bSeq) {
                aSeq ? 1 : -1
            } else {
                // If a and b are very far apart then greater number goes first, to achieve 997, 998, 999, 000, 001, 002
                (aSeq - bSeq).abs() > sequenceDistanceMax ? bSeq - aSeq : aSeq - bSeq
            }
        }

        static void clusterNotTheSame(AutoScalingGroupData a, AutoScalingGroupData b) {
            throw new IllegalArgumentException(
                    """\
                        |AutoScalingGroup ${a.autoScalingGroupName} is not in the same cluster as\
                        |${b.autoScalingGroupName}
                        |""".stripMargin())
        }

        List<AutoScalingGroupData> sort(List<AutoScalingGroupData> groups) {
            groups.sort this
        }
    }
}
