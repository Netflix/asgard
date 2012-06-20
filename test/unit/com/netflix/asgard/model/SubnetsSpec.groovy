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

import com.amazonaws.services.ec2.model.Subnet
import com.amazonaws.services.ec2.model.Tag
import spock.lang.Specification

import static com.netflix.asgard.model.SubnetData.Target.ec2
import static com.netflix.asgard.model.SubnetData.Target.elb

class SubnetsSpec extends Specification {

    static SubnetData subnet(String id, String zone, String purpose, SubnetData.Target target) {
        new SubnetData(subnetId: id, availabilityZone: zone, purpose: purpose, target: target)
    }

    Subnets subnets

    void setup() {
        subnets = new Subnets([
                subnet('subnet-e9b0a3a1', 'us-east-1a', 'internal', ec2),
                subnet('subnet-e9b0a3a2', 'us-east-1a', 'external', ec2),
                subnet('subnet-e9b0a3a3', 'us-east-1a', 'internal', elb),
                subnet('subnet-e9b0a3a4', 'us-east-1a', 'external', elb),
                subnet('subnet-c1e8b2b1', 'us-east-1b', 'internal', ec2),
                subnet('subnet-c1e8b2b2', 'us-east-1b', 'external', ec2),
        ])
    }

    def 'should create Subnets from AWS objects'() {
        List<Subnet> awsSubnets = [
                new Subnet(subnetId: 'subnet-e9b0a3a1', state: 'available', vpcId: 'vpc-11112222',
                        cidrBlock: '10.10.1.0/21', availableIpAddressCount: 42, availabilityZone: 'us-east-1a',
                        tags: [new Tag(key: 'immutable_metadata', value: '{purpose: "internal", "target": "ec2" }')]),
        ]
        List<SubnetData> expectedSubnets = [
                new SubnetData(subnetId: 'subnet-e9b0a3a1', state: 'available', vpcId: 'vpc-11112222',
                        cidrBlock: '10.10.1.0/21', availableIpAddressCount: 42, availabilityZone: 'us-east-1a',
                        purpose: 'internal', target: ec2)
        ]
        expect: expectedSubnets == Subnets.from(awsSubnets).allSubnets

    }

    def 'should fail when creating Subnets from AWS objects with invalid target'() {
        List<Subnet> awsSubnets = [
                new Subnet(subnetId: 'subnet-e9b0a3a1', state: 'available', vpcId: 'vpc-11112222',
                        cidrBlock: '10.10.1.0/21', availableIpAddressCount: 42, availabilityZone: 'us-east-1a',
                        tags: [new Tag(key: 'immutable_metadata', value: '{purpose: "internal", "target": "y2k" }')]),
        ]

        when:
        Subnets.from(awsSubnets)

        then:
        IllegalArgumentException e = thrown(IllegalArgumentException)
        e.message.startsWith 'No enum '
    }

    def 'should find subnet by ID'() {
        SubnetData expectedSubnet = new SubnetData(subnetId: 'subnet-e9b0a3a1', availabilityZone: 'us-east-1a',
                purpose: 'internal', target: ec2)
        expect: expectedSubnet == subnets.findSubnetById('subnet-e9b0a3a1')
    }

    def 'should return null when subnet is not found by ID'() {
        expect: null == subnets.findSubnetById('subnet-acbdabcd')
    }

    def 'should fail when finding subnet by null'() {
        when: subnets.findSubnetById(null)
        then: thrown(NullPointerException)
    }

    def 'should return subnets for zones'() {
        List<String> expectedSubnets = ['subnet-e9b0a3a1', 'subnet-c1e8b2b1']
        expect: expectedSubnets == subnets.getSubnetIdsForZones(['us-east-1a', 'us-east-1b'], 'internal', ec2)
    }

    def 'should return no subnet for missing zone'() {
        List<String> expectedSubnets = ['subnet-e9b0a3a1']
        expect: expectedSubnets == subnets.getSubnetIdsForZones(['us-east-1a', 'us-east-1c'], 'internal', ec2)
    }

    def 'should return no subnets when given a missing zone'() {
        expect: subnets.getSubnetIdsForZones(['us-east-1z'], 'internal', ec2).isEmpty()
    }

    def 'should return no subnets when given no zones'() {
        expect: subnets.getSubnetIdsForZones([], 'internal', ec2).isEmpty()
    }

    def 'should fail to return subnets for null zone'() {
        when: subnets.getSubnetIdsForZones(null, 'internal', ec2)
        then: thrown(NullPointerException)
    }

    def 'should fail to return subnets for null purpose'() {
        when: subnets.getSubnetIdsForZones(['us-east-1a'], null, ec2)
        then: thrown(NullPointerException)
    }

    def 'should return subnet for target'() {
        subnets = new Subnets([
                subnet('subnet-e9b0a3a1', 'us-east-1a', 'internal', ec2),
                subnet('subnet-e9b0a3a2', 'us-east-1a', 'internal', null),
        ])
        expect: ['subnet-e9b0a3a1'] == subnets.getSubnetIdsForZones(['us-east-1a'], 'internal', ec2)
    }

    def 'should return subnet without target if none specified'() {
        subnets = new Subnets([
                subnet('subnet-e9b0a3a1', 'us-east-1a', 'internal', ec2),
                subnet('subnet-e9b0a3a2', 'us-east-1a', 'internal', null),
        ])
        expect: ['subnet-e9b0a3a2'] == subnets.getSubnetIdsForZones(['us-east-1a'], 'internal')
    }

    def 'should fail to return multiple subnets with same description and zone'() {
        subnets = new Subnets([
                subnet('subnet-c1e8b2c1', 'us-east-1c', 'internal', ec2),
                subnet('subnet-c1e8b2c3', 'us-east-1c', 'internal', ec2),
        ])

        when:
        subnets.getSubnetIdsForZones(['us-east-1c'], 'internal', ec2)

        then:
        IllegalArgumentException e = thrown(IllegalArgumentException)
        e.message.startsWith 'duplicate key: '
    }

    def 'should not return subnets without purpose'() {
        subnets = Subnets.from([
                new Subnet(subnetId: 'subnet-e9b0a3a2', availabilityZone: 'us-east-1a'),
        ])
        expect: subnets.getSubnetIdsForZones(['us-east-1a'], '').isEmpty()
    }

    def 'should return purposes for zones and target'() {
        expect: ['internal', 'external'] as Set == subnets.getPurposesForZones(['us-east-1a', 'us-east-1b'], ec2)
    }

    def 'should fail to return purposes for null zones'() {
        when: subnets.getPurposesForZones(null, ec2)
        then: thrown(NullPointerException)
    }

    def 'should return only purposes without target when not specified'() {
        subnets = new Subnets([
                subnet('subnet-e9b0a3a1', 'us-east-1a', 'internal', ec2),
                subnet('subnet-e9b0a3a2', 'us-east-1a', 'external', null),
        ])
        expect: ['external'] as Set == subnets.getPurposesForZones(['us-east-1a'])
    }

    def 'should return only purposes that are in all zones'() {
        subnets = new Subnets([
                subnet('subnet-e9b0a3a1', 'us-east-1a', 'internal', ec2),
                subnet('subnet-e9b0a3a2', 'us-east-1a', 'external', ec2),
                subnet('subnet-e9b0a3a3', 'us-east-1a', 'internal', elb),
                subnet('subnet-e9b0a3a4', 'us-east-1a', 'external', elb),
                subnet('subnet-c1e8b2b1', 'us-east-1b', 'internal', ec2),
                subnet('subnet-c1e8b2b2', 'us-east-1b', 'external', ec2),
                subnet('subnet-c1e8b2b3', 'us-east-1b', 'vulnerable', ec2),
                subnet('subnet-e9b0a3c1', 'us-east-1c', 'internal', ec2),
                subnet('subnet-e9b0a3c2', 'us-east-1c', 'external', ec2),
                subnet('subnet-e9b0a3c3', 'us-east-1c', 'vulnerable', ec2),
                subnet('subnet-e9b0a3c4', 'us-east-1c', 'insecure', ec2),
        ])

        expect:
        ['internal', 'external'] as Set == subnets.getPurposesForZones(['us-east-1a', 'us-east-1b', 'us-east-1c'], ec2)
    }

    def 'should return no purposes when including zone without subnets'() {
        expect: subnets.getPurposesForZones(['us-east-1a', 'us-east-1b', 'us-east-1c'], ec2).isEmpty()
    }
}
