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

import com.amazonaws.services.ec2.model.Instance
import com.netflix.asgard.push.CommonPushOptions
import com.netflix.asgard.push.RollingPushOperation
import com.netflix.asgard.push.RollingPushOptions
import grails.test.GrailsUnitTestCase
import org.joda.time.DateTime

class PushOperationTests extends GrailsUnitTestCase {

    Date dateAlmond = new DateTime(1280199222000).toDate()
    Date dateBurger = new DateTime(1280133222000).toDate()
    Date dateCarrot = new DateTime(1280166222000).toDate()
    Date dateDanish = new DateTime(1280188222000).toDate()
    Date dateEclair = new DateTime(1280155222000).toDate()
    Date dateFondue = new DateTime(1280133222000).toDate()
    Date dateGelato = new DateTime(1280111222000).toDate()

    def idAlmond = "i-0fde5065"
    def idBurger = "i-a7800acd"
    def idCarrot = "i-9be769f1"
    def idDanish = "i-073cb56d"
    def idEclair = "i-7129a61b"
    def idFondue = "i-a3c24cc9"
    def idGelato = "i-6b3bad00"
    def idNull = "i-deleted"

    List<Instance> mockEc2Instances = [
        new Instance(instanceId: idAlmond, launchTime: dateAlmond),
        new Instance(instanceId: idBurger, launchTime: dateBurger),
        new Instance(instanceId: idCarrot, launchTime: dateCarrot),
        new Instance(instanceId: idDanish, launchTime: dateDanish),
        new Instance(instanceId: idEclair, launchTime: dateEclair),
        new Instance(instanceId: idFondue, launchTime: dateFondue),
        new Instance(instanceId: idGelato, launchTime: dateGelato)
    ]

    void testGetSortedEc2InstancesOldestFirst() {
        RollingPushOperation pushOperation = mockPushOperation(new RollingPushOptions(newestFirst: false,
                common: new CommonPushOptions([:])))
        List asgInstances = mockAutoScalingInstances()
        List ec2Instances = pushOperation.getSortedEc2Instances(asgInstances)

        assert ec2Instances[0].launchTime == dateGelato
        assert ec2Instances[0].instanceId == idGelato

        assert ec2Instances[1].launchTime == dateBurger
        assert ec2Instances[1].instanceId == idBurger

        assert ec2Instances[2].launchTime == dateDanish
        assert ec2Instances[2].instanceId == idDanish
    }

    void testGetSortedEc2InstancesNewestFirst() {
        RollingPushOperation pushOperation = mockPushOperation(new RollingPushOptions(newestFirst: true,
                common: new CommonPushOptions([:])))
        List asgInstances = mockAutoScalingInstances()
        List ec2Instances = pushOperation.getSortedEc2Instances(asgInstances)

        assert ec2Instances[0].launchTime == dateDanish
        assert ec2Instances[0].instanceId == idDanish

        assert ec2Instances[1].launchTime == dateBurger
        assert ec2Instances[1].instanceId == idBurger

        assert ec2Instances[2].launchTime == dateGelato
        assert ec2Instances[2].instanceId == idGelato
    }

    private List mockAutoScalingInstances() {
        List<com.amazonaws.services.autoscaling.model.Instance> asgInstances = [
            new com.amazonaws.services.autoscaling.model.Instance(instanceId: idBurger),
            new com.amazonaws.services.autoscaling.model.Instance(instanceId: idGelato),
            new com.amazonaws.services.autoscaling.model.Instance(instanceId: idDanish),
            new com.amazonaws.services.autoscaling.model.Instance(instanceId: idNull)
        ]
        return asgInstances
    }

    private RollingPushOperation mockPushOperation(RollingPushOptions options) {
        // Mock the AwsEc2Service with necessary methods.
        // Ranges dictate how many times the method is expected to be called.
        def awsEc2Control = mockFor(AwsEc2Service)
        awsEc2Control.demand.getInstances(0..0) { UserContext userContext -> mockEc2Instances }
        awsEc2Control.demand.getInstance(4..4) { UserContext userContext, instanceId ->
            mockEc2Instances.find { it.instanceId == instanceId }
        }
        RollingPushOperation pushOperation = new RollingPushOperation(options)
        pushOperation.awsEc2Service = awsEc2Control.createMock()
        pushOperation.task = new Task()
        return pushOperation
    }
}
