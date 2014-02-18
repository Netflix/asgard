/*
 * Copyright 2014 Netflix, Inc.
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

import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.push.Cluster
import com.netflix.frigga.Names
import spock.lang.Specification

class ClusterSpec extends Specification {

    void "asgs are sorted by sequence"() {
        setup:
            final clusterName = "foo"
            final asgGroupName = "group"
            def asg1 = getMockAsgDat(asgGroupName, clusterName, 999)
            def asg2 = getMockAsgDat(asgGroupName, clusterName, 001)
            def asg3 = getMockAsgDat(asgGroupName, clusterName, 500)

        when:
            def cluster = new Cluster([asg2, asg1, asg3], 3, 999)

        then:
            cluster[0] == asg3
            cluster[1] == asg1
            cluster[2] == asg2
    }

    @SuppressWarnings("UnusedObject")
    void "cluster wont compose when no asgs are provided"() {
        when:
            new Cluster([], 3, 999)

        then:
            thrown(IllegalArgumentException)
    }

    @SuppressWarnings("UnusedObject")
    void "cluster wont compose when asgs are in different clusters"() {
        setup:
            def asg1 = getMockAsgDat(null, "foo1", 999)
            def asg2 = getMockAsgDat(null, "foo2", 001)

        when:
            new Cluster([asg1, asg2], 0, 0)

        then:
            thrown(IllegalArgumentException)
    }

    AutoScalingGroupData getMockAsgDat(String groupName, String cluster, int seq) {
        def mock = Mock(AutoScalingGroupData)
        mock.getAutoScalingGroupName() >> groupName
        def names = Mock(Names)
        names.getCluster() >> cluster
        names.getSequence() >> seq
        mock.getNames() >> names
        mock
    }
}
