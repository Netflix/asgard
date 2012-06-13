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
import com.amazonaws.services.ec2.model.InstanceState
import spock.lang.Specification
import com.amazonaws.services.ec2.model.ReservedInstances
import com.netflix.asgard.model.ZoneAvailability
import com.amazonaws.services.ec2.model.Placement

@SuppressWarnings("GroovyPointlessArithmetic")
class AwsEc2ServiceSpec extends Specification {

    def 'active instances should only include pending and running states'() {
        given:
        AwsEc2Service awsEc2Service = new AwsEc2Service()
        awsEc2Service.metaClass.getInstances = { UserContext context ->
            [
                    new Instance(instanceId: 'i-papa', state: new InstanceState(name: 'pending')),
                    new Instance(instanceId: 'i-smurfette', state: new InstanceState(name: 'running')),
                    new Instance(instanceId: 'i-brainy', state: new InstanceState(name: 'shutting-down')),
                    new Instance(instanceId: 'i-jokey', state: new InstanceState(name: 'terminated')),
                    new Instance(instanceId: 'i-hefty', state: new InstanceState(name: 'stopping')),
                    new Instance(instanceId: 'i-barber', state: new InstanceState(name: 'stopped')),
                    new Instance(instanceId: 'i-grouchy', state: new InstanceState(name: 'running'))
            ]
        }

        when:
        Collection<Instance> instances = awsEc2Service.getActiveInstances(null)

        then:
        instances*.instanceId.sort() == ['i-grouchy', 'i-papa', 'i-smurfette']
    }

    def 'zone availabilities should sum, group, and filter reservation counts and instance counts'() {
        given:
        AwsEc2Service awsEc2Service = new AwsEc2Service()
        awsEc2Service.metaClass.getReservedInstances = { UserContext userContext ->
            [
                    [instanceCount: 1, availabilityZone: 'us-east-1a', instanceType: 'm2.xlarge', state: 'active'],
                    [instanceCount: 10, availabilityZone: 'us-east-1a', instanceType: 'm2.xlarge', state: 'active'],
                    [instanceCount: 100, availabilityZone: 'us-east-1a', instanceType: 'm1.small', state: 'active'],
                    [instanceCount: 1000, availabilityZone: 'us-east-1b', instanceType: 'm2.xlarge', state: 'active'],
                    [instanceCount: 10000, availabilityZone: 'us-east-1a', instanceType: 'm2.xlarge', state: 'retired'],
                    [instanceCount: 100000, availabilityZone: 'us-east-1a', instanceType: 'm2.xlarge', state: 'active']
            ].collect { new ReservedInstances(it) }
        }
        awsEc2Service.metaClass.getActiveInstances = {  UserContext userContext ->
            [
                    [instanceType: 'm2.xlarge', placement: new Placement(availabilityZone: 'us-east-1a')],
                    [instanceType: 'm2.xlarge', placement: new Placement(availabilityZone: 'us-east-1a')],
                    [instanceType: 'm2.xlarge', placement: new Placement(availabilityZone: 'us-east-1a')],
                    [instanceType: 'm1.small', placement: new Placement(availabilityZone: 'us-east-1a')],
                    [instanceType: 'm2.xlarge', placement: new Placement(availabilityZone: 'us-east-1b')],
                    [instanceType: 'm2.xlarge', placement: new Placement(availabilityZone: 'us-east-1b')],
                    [instanceType: 'm2.xlarge', placement: new Placement(availabilityZone: 'us-east-1b')],
                    [instanceType: 'm2.xlarge', placement: new Placement(availabilityZone: 'us-east-1b')],
                    [instanceType: 'm2.xlarge', placement: new Placement(availabilityZone: 'us-east-1c')]
            ].collect { new Instance(it) }
        }

        when:
        UserContext userContext = UserContext.auto(Region.US_EAST_1)
        List<ZoneAvailability> zoneAvailabilities = awsEc2Service.getZoneAvailabilities(userContext, 'm2.xlarge')

        then:
        zoneAvailabilities == [
                new ZoneAvailability(zoneName: 'us-east-1a', totalReservations: 100011, usedReservations: 3),
                new ZoneAvailability(zoneName: 'us-east-1b', totalReservations: 1000, usedReservations: 4),
                new ZoneAvailability(zoneName: 'us-east-1c', totalReservations: 0, usedReservations: 1),
        ]
    }
}
