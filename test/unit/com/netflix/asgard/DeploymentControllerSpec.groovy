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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.Instance
import com.amazonaws.services.autoscaling.model.InstanceMonitoring
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.ec2.model.AvailabilityZone
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.KeyPairInfo
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.asgard.deployment.DeploymentWorkflowOptions
import com.netflix.asgard.deployment.steps.CreateAsgStep
import com.netflix.asgard.deployment.steps.DeleteAsgStep
import com.netflix.asgard.deployment.steps.DisableAsgStep
import com.netflix.asgard.deployment.steps.JudgmentStep
import com.netflix.asgard.deployment.steps.ResizeStep
import com.netflix.asgard.deployment.steps.WaitStep
import com.netflix.asgard.model.AsgRoleInCluster
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.AutoScalingGroupHealthCheckType
import com.netflix.asgard.model.AutoScalingGroupMixin
import com.netflix.asgard.model.Deployment
import com.netflix.asgard.model.HardwareProfile
import com.netflix.asgard.model.InstancePriceType
import com.netflix.asgard.model.InstanceTypeData
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.SubnetData
import com.netflix.asgard.model.SubnetTarget
import com.netflix.asgard.model.Subnets
import com.netflix.asgard.push.Cluster
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(DeploymentController)
class DeploymentControllerSpec extends Specification {

    ObjectMapper objectMapper = new ObjectMapper()

    String activityCompletionRequest = '{"id":"123","token":"abc"}'

    Closure<String> createStartDeploymentRequest = { int maxSize = 5 ->
        """{"asgOptions":
        {"suspendedProcesses":[],"tags":[],"autoScalingGroupName":null,
        "subnetPurpose":"internal","healthCheckType":"EC2",
        "placementGroup":null,"launchConfigurationName":null,"desiredCapacity":"3",
        "availabilityZones":["us-west-1c","us-west-1a"],"loadBalancerNames":["helloclay--frontend"],
        "minSize":0,"healthCheckGracePeriod":600,"defaultCooldown":10,"maxSize":${maxSize},
        "terminationPolicies":["OldestLaunchConfiguration"]},
        "lcOptions":
        {"securityGroups":["sg-12345678"],"kernelId":"","launchConfigurationName":null,
        "userData":null,"instancePriceType":"ON_DEMAND","instanceType":"m1.large","blockDeviceMappings":[],
        "imageId":"ami-12345678","keyName":"keypair","ramdiskId":"","instanceMonitoringIsEnabled":false,
        "iamInstanceProfile":"BaseIAMRole","ebsOptimized":false},
        "deploymentOptions":
        {"clusterName":"helloclay--test","notificationDestination":"cmccoy@netflix.com", "steps": [
                {"type": "Wait", "durationMinutes": 5, "description": "delay"},
                {"type": "CreateAsg"},
                {"type": "Resize", "capacity": 1, "startUpTimeoutMinutes": 31},
                {"type": "Judgment", "durationMinutes": 61},
                {"type": "Resize", "capacity": 3, "startUpTimeoutMinutes": 41},
                {"type": "Judgment", "durationMinutes": 121},
                {"type": "DisableAsg", "targetAsg": "Previous"},
                {"type": "Judgment", "durationMinutes": 241},
                {"type": "DeleteAsg", "targetAsg": "Previous"}
        ]}}""" as String
    }

    void setup() {
        AutoScalingGroup.mixin AutoScalingGroupMixin
        controller.with() {
            applicationService = Mock(ApplicationService)
            awsAutoScalingService = Mock(AwsAutoScalingService)
            awsEc2Service = Mock(AwsEc2Service) {
                getSubnets(_) >> new Subnets([])
            }
            awsLoadBalancerService = Mock(AwsLoadBalancerService)
            configService = Mock(ConfigService)
            deploymentService = Mock(DeploymentService)
            flowService = Mock(FlowService)
            instanceTypeService = Mock(InstanceTypeService)
            objectMapper = new ObjectMapper()
        }
    }

    def 'should cancel deployment'() {
        Deployment deployment = new Deployment('123', 'hiworld', Region.US_EAST_1, new WorkflowExecution(), 'deployin')

        when:
        controller.cancel('123')

        then:
        response.status == 200

        and:
        with(controller.deploymentService) {
            1 * getDeploymentById('123') >> deployment
            1 * cancelDeployment(_, deployment)
        }
    }

    def 'should not cancel missing deployment'() {
        when:
        controller.cancel('123')

        then:
        response.status == 404

        and:
        1 * controller.deploymentService.getDeploymentById('123')
    }

    def 'should deploy'() {
        request.JSON = createStartDeploymentRequest()

        when:
        request.method = 'POST'
        controller.start()

        then:
        response.status == 200
        objectMapper.readValue(response.contentAsString, Map) == [deploymentId: "123"]

        and:
        controller.awsAutoScalingService.getCluster(_, 'helloworld') >> {
            new Cluster([
                    AutoScalingGroupData.from(new AutoScalingGroup(autoScalingGroupName: 'helloworld-example-v014',
                            instances: [new Instance(instanceId: 'i-8ee4eeee')]), [:], [], [:], [])
            ])
        }
        1 * controller.deploymentService.startDeployment(_,
                new DeploymentWorkflowOptions(
                        clusterName: "helloclay--test",
                        notificationDestination: "cmccoy@netflix.com",
                        steps: [
                                new WaitStep(durationMinutes: 5, description: "delay"),
                                new CreateAsgStep(),
                                new ResizeStep(capacity: 1, startUpTimeoutMinutes: 31),
                                new JudgmentStep(durationMinutes: 61),
                                new ResizeStep(capacity: 3, startUpTimeoutMinutes: 41),
                                new JudgmentStep(durationMinutes: 121),
                                new DisableAsgStep(targetAsg: AsgRoleInCluster.Previous),
                                new JudgmentStep(durationMinutes: 241),
                                new DeleteAsgStep(targetAsg: AsgRoleInCluster.Previous)
                        ]
                ),
                new LaunchConfigurationBeanOptions(
                        launchConfigurationName: null,
                        imageId: "ami-12345678",
                        keyName: "keypair",
                        securityGroups: ["sg-12345678"],
                        userData: null,
                        instanceType: "m1.large",
                        kernelId: "",
                        ramdiskId: "",
                        blockDeviceMappings: [],
                        instanceMonitoringIsEnabled: false,
                        instancePriceType: InstancePriceType.ON_DEMAND,
                        iamInstanceProfile: "BaseIAMRole",
                        ebsOptimized: false
                ),
                new AutoScalingGroupBeanOptions(
                        autoScalingGroupName: null,
                        launchConfigurationName: null,
                        minSize: 0,
                        maxSize: 5,
                        desiredCapacity: 3,
                        defaultCooldown: 10,
                        availabilityZones: ["us-west-1a", "us-west-1c"],
                        loadBalancerNames: ["helloclay--frontend"],
                        healthCheckType: AutoScalingGroupHealthCheckType.EC2,
                        healthCheckGracePeriod: 600,
                        placementGroup: null,
                        subnetPurpose: "internal",
                        terminationPolicies: ["OldestLaunchConfiguration"],
                        tags: [],
                        suspendedProcesses: []
                )
        ) >> '123'
    }

    def 'deploy should fail on invalid request'() {
        request.JSON = createStartDeploymentRequest(0)

        when:
        request.method = 'POST'
        controller.start()

        then:
        response.status == 422
        objectMapper.readValue(response.contentAsString, Map) == [validationErrors: [
                "Resize ASG capacity '1' is greater than the ASG's maximum instance bound '0'.",
                "Resize ASG capacity '3' is greater than the ASG's maximum instance bound '0'."
        ]]
    }

    def 'should proceed with deployment'() {
        request.JSON = activityCompletionRequest

        when:
        request.method = 'POST'
        controller.proceed()

        then:
        1 * controller.flowService.getManualActivityCompletionClient('abc') >> Mock(ManualActivityCompletionClient) {
            1 * complete(true)
        }
        1 * controller.deploymentService.removeManualTokenForDeployment('123')
        0 * _

        and:
        response.status == 200
    }

    def 'should rollback deployment'() {
        request.JSON = activityCompletionRequest

        when:
        request.method = 'POST'
        controller.rollback()

        then:
        1 * controller.flowService.getManualActivityCompletionClient('abc') >> Mock(ManualActivityCompletionClient) {
            1 * complete(false)
        }
        1 * controller.deploymentService.removeManualTokenForDeployment('123')
        0 * _

        and:
        response.status == 200
    }

    def 'should show deployment'() {
        when:
        controller.show('123')

        then:
        response.status == 200
        objectMapper.readValue(response.text, Map) == objectMapper.readValue("""{"id":"123","clusterName":"cluster",
            "region":"us-west-1","workflowExecution":null,"description":null,"owner":null,"startTime":null,
            "updateTime":null,"status":null,"log":null,"steps":null,"token":null,"durationString":"0s","done":true,
            "regionCode":"us-west-1","logForSteps":[[]]}""", Map)

        and:
        1 * controller.deploymentService.getDeploymentById('123') >> new Deployment('123', 'cluster', Region.US_WEST_1)
    }

    def 'should not show missing deployment'() {
        when:
        controller.show('123')

        then:
        response.status == 404

        and:
        1 * controller.deploymentService.getDeploymentById('123')
    }

    def 'should list deployments'() {
        when:
        def result = controller.list()

        then:
        result.deployments == [ new Deployment('2'), new Deployment('1') ]

        and:
        with(controller.deploymentService) {
            1 * getFinishedDeployments() >> [ new Deployment('1') ]
            1 * getRunningDeployments() >> [ new Deployment('2') ]
        }
    }

    def 'should prepare deployment'() {
        Image.metaClass['getPackageName'] = { -> "package123" }

        AutoScalingGroup asg = new AutoScalingGroup(
                autoScalingGroupName: "helloclay--test-v456",
                launchConfigurationName: "lc123",
                minSize: 0,
                maxSize: 5,
                desiredCapacity: 3,
                defaultCooldown: 10,
                availabilityZones: ["us-west-1a", "us-west-1c"],
                loadBalancerNames: ["helloclay--frontend"],
                healthCheckType: AutoScalingGroupHealthCheckType.EC2,
                healthCheckGracePeriod: 600,
                placementGroup: null,
                terminationPolicies: ["OldestLaunchConfiguration"],
                tags: [],
                suspendedProcesses: [],
                vPCZoneIdentifier: "1"
        )
        LaunchConfiguration lc = new LaunchConfiguration(
                launchConfigurationName: "lc123",
                imageId: "ami-12345678",
                keyName: "keypair",
                securityGroups: ["sg-12345678"],
                userData: null,
                instanceType: "m1.large",
                kernelId: "",
                ramdiskId: "",
                blockDeviceMappings: [],
                instanceMonitoring: new InstanceMonitoring(enabled: false),
                iamInstanceProfile: "BaseIAMRole",
                ebsOptimized: false,
                associatePublicIpAddress: false
        )
        Subnets subnets = new Subnets([
                new SubnetData("1", "", "vpc1", "", 1, "us-east-1", "internal", SubnetTarget.EC2)
        ])

        when:
        controller.prepare("helloclay--test", false, null)

        then:
        response.status == 200
        def results = objectMapper.readValue(response.contentAsString, Map)

        results.lcOptions == [
                        launchConfigurationName: null,
                        imageId: "ami-12345678",
                        keyName: "keypair",
                        securityGroups: ["sg-12345678"],
                        userData: null,
                        instanceType: "m1.large",
                        kernelId: "",
                        ramdiskId: "",
                        blockDeviceMappings: null,
                        instanceMonitoringIsEnabled: false,
                        instancePriceType: "ON_DEMAND",
                        iamInstanceProfile: "BaseIAMRole",
                        ebsOptimized: false,
                        associatePublicIpAddress: false
                ]

        results.asgOptions == [
                        autoScalingGroupName: null,
                        launchConfigurationName: null,
                        minSize: 0,
                        maxSize: 5,
                        desiredCapacity: 3,
                        defaultCooldown: 10,
                        availabilityZones: ["us-west-1a", "us-west-1c"],
                        loadBalancerNames: ["helloclay--frontend"],
                        healthCheckType: "EC2",
                        healthCheckGracePeriod: 600,
                        placementGroup: null,
                        subnetPurpose: "internal",
                        terminationPolicies: ["OldestLaunchConfiguration"],
                        tags: [],
                        suspendedProcesses: []
                ]

        and:
        with(controller.awsAutoScalingService) {
            1 * getCluster(_, "helloclay--test") >> new Cluster([AutoScalingGroupData.from(asg, [:], [], [:], [])])
            1 * getAutoScalingGroup(_, "helloclay--test-v456", From.AWS) >> asg
            1 * getLaunchConfiguration(_, "lc123") >> lc
        }
        with(controller.awsEc2Service) {
            1 * getSubnets(_) >> subnets
            1 * getSecurityGroup(_, 'sg-12345678') >> new SecurityGroup(groupId: 'sg-12345678')
        }
        0 * _
    }

    def 'should prepare deployment with environment and template'() {
        Image.metaClass['getPackageName'] = { -> "package123" }

        AutoScalingGroup asg = new AutoScalingGroup(
                autoScalingGroupName: "helloclay--test-v456",
                launchConfigurationName: "lc123",
                minSize: 0,
                maxSize: 5,
                desiredCapacity: 3,
                defaultCooldown: 10,
                availabilityZones: ["us-west-1c", "us-west-1a"],
                loadBalancerNames: ["helloclay--frontend"],
                healthCheckType: AutoScalingGroupHealthCheckType.EC2,
                healthCheckGracePeriod: 600,
                placementGroup: null,
                terminationPolicies: ["OldestLaunchConfiguration"],
                tags: [],
                suspendedProcesses: [],
                vPCZoneIdentifier: "1"
        )
        LaunchConfiguration lc = new LaunchConfiguration(
                launchConfigurationName: "lc123",
                imageId: "ami-12345678",
                keyName: "keypair",
                securityGroups: ["sg-12345678"],
                userData: null,
                instanceType: "m1.large",
                kernelId: "",
                ramdiskId: "",
                blockDeviceMappings: [],
                instanceMonitoring: null,
                iamInstanceProfile: "BaseIAMRole",
                ebsOptimized: false,
                associatePublicIpAddress: false
        )
        Subnets subnets = new Subnets([
                new SubnetData("1", "", "vpc1", "", 1, "us-east-1", "internal", SubnetTarget.EC2)
        ])

        when:
        controller.prepare("helloclay--test", true, "CreateAndCleanUpPreviousAsg")

        then:
        response.status == 200
        def results = objectMapper.readValue(response.contentAsString, Map)
                results.deploymentOptions == [
                        clusterName: "helloclay--test",
                        notificationDestination: "jdoe@netflix.com",
                        steps:[
                                [type: "CreateAsg"],
                                [type: "Resize", targetAsg: "Next", capacity: 3, startUpTimeoutMinutes: 40],
                                [type: "DisableAsg", targetAsg: "Previous"],
                                [type: "DeleteAsg", targetAsg: "Previous"]
                        ]
                ]
                results.lcOptions == [
                        launchConfigurationName: null,
                        imageId: "ami-12345678",
                        keyName: "keypair",
                        securityGroups: ["sg-12345678"],
                        userData: null,
                        instanceType: "m1.large",
                        kernelId: "",
                        ramdiskId: "",
                        blockDeviceMappings: null,
                        instanceMonitoringIsEnabled: false,
                        instancePriceType: "ON_DEMAND",
                        iamInstanceProfile: "BaseIAMRole",
                        ebsOptimized: false,
                        associatePublicIpAddress: false
                ]
                results.asgOptions == [
                        autoScalingGroupName: null,
                        launchConfigurationName: null,
                        minSize: 0,
                        maxSize: 5,
                        desiredCapacity: 3,
                        defaultCooldown: 10,
                        availabilityZones: ["us-west-1a", "us-west-1c"],
                        loadBalancerNames: ["helloclay--frontend"],
                        healthCheckType: "EC2",
                        healthCheckGracePeriod: 600,
                        placementGroup: null,
                        subnetPurpose: "internal",
                        terminationPolicies: ["OldestLaunchConfiguration"],
                        tags: [],
                        suspendedProcesses: []
                ]
                results.environment == [
                        nextGroupName: "helloclay--test-v457",
                        terminationPolicies: ["tp1"],
                        purposeToVpcId: [internal: "vpc1"],
                        subnetPurposes: ["internal"],
                        availabilityZonesAndPurpose: [
                                [zone: "us-east-1", purpose:""],
                                [zone: "us-east-1", purpose:"internal"]
                        ],
                        loadBalancers: [[id:"lb1", vpcId:"vpc1"]],
                        healthCheckTypes: [
                                [id: "EC2", description: "Replace terminated instances"],
                                [id: "ELB", description: "Replace instances that fail ELB health check"]
                        ],
                        instanceTypes: [[id: null, price:""]],
                        securityGroups: [[id: "sg-1", name: "hcsg", vpcId: "vpc1"]],
                        images: [[id: "img123", imageLocation: "imgloc"]],
                        keys: ["key1"],
                        spotUrl: "spotUrl"
                ]

        and:
        with(controller.awsAutoScalingService) {
            1 * getCluster(_, "helloclay--test") >> new Cluster([AutoScalingGroupData.from(asg, [:], [], [:], [])])
            1 * getAutoScalingGroup(_, "helloclay--test-v456", From.AWS) >> asg
            1 * getLaunchConfiguration(_, "lc123") >> lc
            1 * getTerminationPolicyTypes() >> ["tp1"]
        }
        with(controller.awsEc2Service) {
            1 * getSubnets(_) >> subnets
            1 * getImage(_, "ami-12345678") >> new Image()
            1 * getImagesForPackage(_, "package123") >> [new Image(imageId: "img123", imageLocation: "imgloc")]
            1 * getEffectiveSecurityGroups(_) >> [new SecurityGroup(groupId: "sg-1", groupName: "hcsg", vpcId: "vpc1")]
            1 * getAvailabilityZones(_) >> [new AvailabilityZone(zoneName: "us-east-1")]
            1 * getKeys(_) >> [new KeyPairInfo(keyName: "key1")]
            1 * getSecurityGroup(_, 'sg-12345678') >> new SecurityGroup(groupId: 'sg-12345678')
        }
        with(controller.awsLoadBalancerService) {
            1 * getLoadBalancers(_) >> [new LoadBalancerDescription(loadBalancerName: "lb1", vPCId: "vpc1")]
        }
        with(controller.applicationService) {
            1 * getEmailFromApp(_, "helloclay") >> "jdoe@netflix.com"
        }
        with(controller.instanceTypeService) {
            1 * getInstanceTypes(_) >> [new InstanceTypeData(hardwareProfile: new HardwareProfile(name: "it1"))]
        }
        with(controller.configService) {
            1 * getSpotUrl() >> "spotUrl"
            1 * getEnableInstanceMonitoring()
        }
        0 * _
    }
}
