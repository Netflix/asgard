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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.Instance
import com.netflix.asgard.MonkeyPatcherService
import com.netflix.asgard.model.AutoScalingGroupData
import spock.lang.Specification

@SuppressWarnings("UnusedObject")
class ClusterSpec extends Specification {

    Instance ideadbeef = new Instance(instanceId: 'i-deadbeef')
    Instance iaaaa4444 = new Instance(instanceId: 'i-aaaa4444')
    Instance ieeee9999 = new Instance(instanceId: 'i-eeee9999')

    AutoScalingGroupData groupOrig
    AutoScalingGroupData groupv000
    AutoScalingGroupData groupv001
    AutoScalingGroupData groupv002

    Cluster cluster

    void setup() {
        new MonkeyPatcherService().createDynamicMethods()
        groupOrig = makeGroupData('helloworld-example', 1398890600000, [ideadbeef, iaaaa4444])
        groupv000 = makeGroupData('helloworld-example-v000', 1398890700000)
        groupv001 = makeGroupData('helloworld-example-v001', 1398890800000, [ieeee9999])
        groupv002 = makeGroupData('helloworld-example-v002', 1398890900000)
        cluster = new Cluster([groupv000, groupv002, groupOrig, groupv001])
    }

    private AutoScalingGroupData makeGroupData(String name, long createdTimeMillis, List<Instance> instances = []) {
        AutoScalingGroupData.from(new AutoScalingGroup(autoScalingGroupName: name,
                createdTime: new Date(createdTimeMillis), instances: instances), null, null, null, [])
    }

    void "should create a new cluster"() {

        expect:
        groupOrig.autoScalingGroupName == cluster[0].autoScalingGroupName
        groupv000.autoScalingGroupName == cluster[1].autoScalingGroupName
        groupv001.autoScalingGroupName == cluster[2].autoScalingGroupName
        groupv002.autoScalingGroupName == cluster[3].autoScalingGroupName
    }

    void "last should be the most recent"() {

        expect:
        groupv002.autoScalingGroupName == cluster.last().autoScalingGroupName
    }

    void "should get instances"() {

        expect:
        [ideadbeef, iaaaa4444, ieeee9999].collect { it.instanceId } == cluster.instances.collect { it.instanceId }
    }

    void "should get instance ids"() {

        expect:
        ['i-deadbeef', 'i-aaaa4444', 'i-eeee9999'] == cluster.instances.collect { it.instanceId }
    }

    void "should fail when zero ASGs are provided"() {

        when:
        new Cluster([])

        then:
        thrown(IllegalArgumentException)
    }

    void "should fail if ASGs are in different clusters"() {

        AutoScalingGroupData asg1 = makeGroupData("helloworld", 1398890600000)
        AutoScalingGroupData asg2 = makeGroupData("goodbyeworld", 1398890700000)

        when:
        new Cluster([asg1, asg2])

        then:
        thrown(IllegalStateException)
    }
}
