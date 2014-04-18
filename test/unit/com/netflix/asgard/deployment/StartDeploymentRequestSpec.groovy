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
package com.netflix.asgard.deployment

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.asgard.deployment.steps.CreateAsgStep
import com.netflix.asgard.deployment.steps.DeleteAsgStep
import com.netflix.asgard.deployment.steps.DisableAsgStep
import com.netflix.asgard.deployment.steps.JudgmentStep
import com.netflix.asgard.deployment.steps.ResizeStep
import com.netflix.asgard.deployment.steps.WaitStep
import com.netflix.asgard.model.AsgRoleInCluster
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.AutoScalingProcessType
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import spock.lang.Specification

class StartDeploymentRequestSpec extends Specification {
    ObjectMapper mapper = new ObjectMapper()

    StartDeploymentRequest startDeploymentRequest = new StartDeploymentRequest(
            new DeploymentWorkflowOptions(
                    clusterName: "helloworld",
                    notificationDestination:"jdoe@netflix.com",
                    steps: [
                            new WaitStep(durationMinutes: 5, description:  "delay"),
                            new CreateAsgStep(),
                            new ResizeStep(capacity: 1, targetAsg: AsgRoleInCluster.Next, startUpTimeoutMinutes: 30),
                            new ResizeStep(capacity: 3, targetAsg: AsgRoleInCluster.Next, startUpTimeoutMinutes: 40),
                            new JudgmentStep(durationMinutes: 120),
                            new DisableAsgStep(targetAsg: AsgRoleInCluster.Previous),
                            new DeleteAsgStep(targetAsg: AsgRoleInCluster.Previous)
                    ]
            ),
            new LaunchConfigurationBeanOptions(
                    imageId: "ami-12345678",
                    keyName: "nf-test-keypair-a",
                    securityGroups: ["sg-12345678"],
                    userData: "#!/bin/bash",
                    instanceType: "m1.large",
                    kernelId: "123",
                    ramdiskId: "abc",
                    instanceMonitoringIsEnabled: false,
                    instancePriceType: "ON_DEMAND",
                    iamInstanceProfile: "BaseIAMRole",
                    associatePublicIpAddress: false,
                    ebsOptimized: true),
            new AutoScalingGroupBeanOptions(
                    minSize: 1,
                    maxSize: 3,
                    desiredCapacity: 2,
                    defaultCooldown: 10,
                    availabilityZones: ["us-west-1a"],
                    loadBalancerNames: ["helloworld--frontend"],
                    healthCheckType: "EC2",
                    healthCheckGracePeriod: 600,
                    subnetPurpose: "internal",
                    terminationPolicies: ["OldestLaunchConfiguration"],
                    suspendedProcesses: [AutoScalingProcessType.AddToLoadBalancer])
    )

    String json = '{"deploymentOptions":{"clusterName":"helloworld","notificationDestination":"jdoe@netflix.com",' +
            '"steps":[' +
            '{"type":"Wait","durationMinutes":5,"description":"delay"},' +
            '{"type":"CreateAsg"},' +
            '{"type":"Resize","targetAsg":"Next","capacity":1,"startUpTimeoutMinutes":30},' +
            '{"type":"Resize","targetAsg":"Next","capacity":3,"startUpTimeoutMinutes":40},' +
            '{"type":"Judgment","durationMinutes":120},' +
            '{"type":"DisableAsg","targetAsg":"Previous"},' +
            '{"type":"DeleteAsg","targetAsg":"Previous"}' +
            ']},' +

            '"lcOptions":{"launchConfigurationName":null,"imageId":"ami-12345678",' +
            '"keyName":"nf-test-keypair-a","securityGroups":["sg-12345678"],' +
            '"userData":"#!/bin/bash","instanceType":"m1.large","kernelId":"123","ramdiskId":"abc",' +
            '"blockDeviceMappings":null,"instanceMonitoringIsEnabled":false,"instancePriceType":"ON_DEMAND",' +
            '"iamInstanceProfile":"BaseIAMRole","ebsOptimized":true,"associatePublicIpAddress":false},' +

            '"asgOptions":{"autoScalingGroupName":null,"launchConfigurationName":null,"minSize":1,"maxSize":3,' +
            '"desiredCapacity":2,"defaultCooldown":10,"availabilityZones":["us-west-1a"],' +
            '"loadBalancerNames":["helloworld--frontend"],"healthCheckType":"EC2","healthCheckGracePeriod":600,' +
            '"placementGroup":null,"subnetPurpose":"internal",' +
            '"terminationPolicies":["OldestLaunchConfiguration"],"tags":null,' +
            '"suspendedProcesses":["AddToLoadBalancer"]}}'

    void 'should convert a StartDeploymentRequest to JSON'() {
        expect:
        mapper.writeValueAsString(startDeploymentRequest) == json
    }

    void 'should convert JSON to a StartDeploymentRequest'() {
        expect:
        startDeploymentRequest == mapper.reader(StartDeploymentRequest).readValue(json)
    }

    void 'should return no errors for valid request'() {
        expect:
        startDeploymentRequest.validationErrors == []
    }

    void 'should return errors for invalid capacity bounds'() {
        startDeploymentRequest.asgOptions.maxSize = 1
        expect:
        startDeploymentRequest.validationErrors == [
                "Resize ASG capacity '3' is greater than the ASG's maximum instance bound '1'."
        ]
    }
}
