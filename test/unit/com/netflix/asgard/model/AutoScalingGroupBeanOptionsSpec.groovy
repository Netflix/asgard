/*
 * Copyright 2013 Netflix, Inc.
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
import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest
import com.amazonaws.services.autoscaling.model.SuspendedProcess
import com.amazonaws.services.autoscaling.model.Tag
import com.amazonaws.services.autoscaling.model.TagDescription
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest
import com.google.common.collect.ImmutableSet
import spock.lang.Specification

class AutoScalingGroupBeanOptionsSpec extends Specification {

    Subnets subnets = Mock(Subnets)

    AutoScalingGroupBeanOptions asgOptions = new AutoScalingGroupBeanOptions(
            autoScalingGroupName: 'autoScalingGroupName1',
            launchConfigurationName: 'launchConfigurationName1',
            minSize: 1,
            maxSize: 3,
            desiredCapacity: 2,
            defaultCooldown: 4,
            availabilityZones: ['us-west-1a'],
            loadBalancerNames: ['lb1'],
            healthCheckType: AutoScalingGroupHealthCheckType.EC2,
            healthCheckGracePeriod: 5,
            placementGroup: 'placementGroup1',
            subnetPurpose: 'subnetPurpose1',
            terminationPolicies: ['tp1'],
            tags: [new Tag(key: 'key1', value: 'value1')],
            suspendedProcesses: [AutoScalingProcessType.Terminate, AutoScalingProcessType.Launch]
    )

    UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest = new UpdateAutoScalingGroupRequest(
            autoScalingGroupName: 'autoScalingGroupName1',
            launchConfigurationName: 'launchConfigurationName1',
            minSize: 1,
            maxSize: 3,
            desiredCapacity: 2,
            defaultCooldown: 4,
            availabilityZones: ['us-west-1a'],
            healthCheckType: AutoScalingGroupHealthCheckType.EC2,
            healthCheckGracePeriod: 5,
            placementGroup: 'placementGroup1',
            terminationPolicies: ['tp1']
    )

    AutoScalingGroup awsAutoScalingGroup = new AutoScalingGroup(
            autoScalingGroupName: 'autoScalingGroupName1',
            launchConfigurationName: 'launchConfigurationName1',
            minSize: 1,
            maxSize: 3,
            desiredCapacity: 2,
            defaultCooldown: 4,
            availabilityZones: ['us-west-1a'],
            loadBalancerNames: ['lb1'],
            healthCheckType: AutoScalingGroupHealthCheckType.EC2,
            healthCheckGracePeriod: 5,
            placementGroup: 'placementGroup1',
            vPCZoneIdentifier: 'vPCZoneIdentifier1',
            terminationPolicies: ['tp1'],
            tags: [new TagDescription(key: 'key1', value: 'value1')],
            suspendedProcesses: ['Terminate', 'Launch'].collect { new SuspendedProcess(processName: it) }
    )

    def 'should deep copy'() {
        when:
        AutoScalingGroupBeanOptions actualAsg = AutoScalingGroupBeanOptions.from(asgOptions)

        then:
        asgOptions == actualAsg

        when:
        actualAsg.tags.iterator().next().value = 'value2'

        then:
        asgOptions != actualAsg
    }

    def 'should create from AWS AutoScalingGroup'() {
        asgOptions.subnetPurpose = null
        awsAutoScalingGroup.setVPCZoneIdentifier(null)

        expect:
        AutoScalingGroupBeanOptions.from(awsAutoScalingGroup, null) == asgOptions
    }

    def 'should create from AWS AutoScalingGroup in VPC'() {
        when:
        AutoScalingGroupBeanOptions actualOptions = AutoScalingGroupBeanOptions.from(awsAutoScalingGroup, subnets)

        then:
        actualOptions == asgOptions
        1 * subnets.getPurposeFromVpcZoneIdentifier('vPCZoneIdentifier1') >> 'subnetPurpose1'
    }

    def 'should create UpdateAutoScalingGroupRequest'() {
        asgOptions.subnetPurpose = null

        expect:
        asgOptions.getUpdateAutoScalingGroupRequest(null) == updateAutoScalingGroupRequest
    }

    def 'should create UpdateAutoScalingGroupRequest in VPC'() {
        when:
        UpdateAutoScalingGroupRequest request = asgOptions.getUpdateAutoScalingGroupRequest(subnets)

        then:
        request == updateAutoScalingGroupRequest.withVPCZoneIdentifier('vpc1')
        1 * subnets.constructNewVpcZoneIdentifierForPurposeAndZones('subnetPurpose1',
                ImmutableSet.of('us-west-1a')) >> 'vpc1'
    }

    def 'should create SuspendProcessesRequestForUpdate'() {
        expect:
        asgOptions.getSuspendProcessesRequestForUpdate(AutoScalingProcessType.with { [AZRebalance, Launch] }) ==
                new SuspendProcessesRequest(autoScalingGroupName: 'autoScalingGroupName1',
                scalingProcesses: ['AZRebalance'])
    }

    def 'should create ResumeProcessesRequestForUpdate'() {
        expect:
        asgOptions.getResumeProcessesRequestForUpdate(AutoScalingProcessType.with { [AZRebalance, Launch] }) ==
                new ResumeProcessesRequest(autoScalingGroupName: 'autoScalingGroupName1',
                scalingProcesses: ['Terminate'])
    }
}