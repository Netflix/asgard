package com.netflix.asgard

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.Instance
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.AutoScalingGroupHealthCheckType
import com.netflix.asgard.model.AutoScalingGroupMixin
import com.netflix.asgard.model.InstancePriceType
import com.netflix.asgard.model.SubnetData
import com.netflix.asgard.model.SubnetTarget
import com.netflix.asgard.model.Subnets
import com.netflix.asgard.push.Cluster
import com.netflix.asgard.push.GroupCreateOperation
import com.netflix.asgard.push.GroupCreateOptions
import grails.test.mixin.TestFor
import spock.lang.Specification

@SuppressWarnings("GroovyPointlessArithmetic")
@TestFor(ClusterController)
class ClusterControllerSpec extends Specification {


    final Closure<SubnetData> subnet = { String id, String zone, String purpose ->
        new SubnetData(subnetId: id, availabilityZone: zone, purpose: purpose, target: SubnetTarget.EC2, vpcId: 'vpc-1')
    }

    final Subnets subnets = new Subnets([
            subnet('subnet-1', 'us-east-1c', 'internal'),
            subnet('subnet-2', 'us-east-1c', 'external'),
            subnet('subnet-3', 'us-east-1e', 'internal'),
            subnet('subnet-4', 'us-east-1e', 'external'),
    ])
    final AutoScalingGroup asg = new AutoScalingGroup(autoScalingGroupName: 'helloworld-example-v015',
        minSize: 3, desiredCapacity: 5, maxSize: 7, healthCheckGracePeriod: 42, defaultCooldown: 360,
        launchConfigurationName: 'helloworld-lc', healthCheckType: AutoScalingGroupHealthCheckType.EC2,
        instances: [new Instance(instanceId: 'i-6ef9f30e'), new Instance(instanceId: 'i-95fe1df6')],
        availabilityZones: ['us-east-1c'], loadBalancerNames: ['hello-elb'], terminationPolicies: ['hello-tp'],
        vPCZoneIdentifier: 'subnet-1')
    final LaunchConfiguration launchConfiguration = new LaunchConfiguration(imageId: 'lastImageId',
            instanceType: 'lastInstanceType', keyName: 'lastKeyName', securityGroups: ['sg-123', 'sg-456'],
            iamInstanceProfile: 'lastIamProfile', spotPrice: '1.23')

    final Closure<Cluster> constructClusterFromAsg = { AutoScalingGroup asg ->
        new Cluster([AutoScalingGroupData.from(asg, [:], [], [:], [])])
    }

    void setup() {
        AutoScalingGroup.mixin AutoScalingGroupMixin
        TestUtils.setUpMockRequest()
        controller.with() {
            awsAutoScalingService = Mock(AwsAutoScalingService)
            taskService = Mock(TaskService)
            taskService.getRunningTasksByObject(*_) >> []
            pushService = Mock(PushService)
            pushService.prepareEdit(*_) >> [:]
            awsEc2Service = Mock(AwsEc2Service)
            awsEc2Service.getSubnets(_) >> subnets
            awsEc2Service.getAvailabilityZones(_) >> []
            awsLoadBalancerService = Mock(AwsLoadBalancerService)
            awsLoadBalancerService.getLoadBalancers(_) >> []
            configService = Mock(ConfigService)
        }
    }

    def 'show should display cluster with one ASG'() {
        controller.awsAutoScalingService.getCluster(_, 'helloworld-example') >> {
            new Cluster([
                AutoScalingGroupData.from(new AutoScalingGroup(autoScalingGroupName: 'helloworld-example-v015',
                    instances: [new Instance(instanceId: 'i-6ef9f30e'), new Instance(instanceId: 'i-95fe1df6')]),
                        [:], [], [:], [])
            ])
        }
        controller.params.id = 'helloworld-example'

        when:
        final attrs = controller.show()

        then:
        'helloworld-example-v015' == attrs.group.autoScalingGroupName
        'helloworld-example' == attrs.cluster.name
        'helloworld-example-v015' == attrs.cluster.last().autoScalingGroupName
        ['i-6ef9f30e', 'i-95fe1df6'] as Set == attrs.cluster.instances*.instanceId as Set
        'helloworld-example-v016' == attrs.nextGroupName
        attrs.okayToCreateGroup
        'Create a new group and switch traffic to it' == attrs.recommendedNextStep
    }

    def 'show should display cluster with two ASGs'() {
        controller.awsAutoScalingService.getCluster(_, 'helloworld-example') >> {
            new Cluster([
                    AutoScalingGroupData.from(new AutoScalingGroup(autoScalingGroupName: 'helloworld-example-v014',
                            instances: [new Instance(instanceId: 'i-8ee4eeee')]), [:], [], [:], []),
                    AutoScalingGroupData.from(new AutoScalingGroup(autoScalingGroupName: 'helloworld-example-v015',
                            instances: [new Instance(instanceId: 'i-6ef9f30e'), new Instance(instanceId: 'i-95fe1df6')]),
                            [:], [], [:], [])
            ])
        }
        controller.params.id = 'helloworld-example'

        when:
        final attrs = controller.show()

        then:
        'helloworld-example-v015' == attrs.group.autoScalingGroupName
        'helloworld-example' == attrs.cluster.name
        'helloworld-example-v015' == attrs.cluster.last().autoScalingGroupName
        ['i-8ee4eeee', 'i-6ef9f30e', 'i-95fe1df6'] as Set == attrs.cluster.instances*.instanceId as Set
        'helloworld-example-v016' == attrs.nextGroupName
        attrs.okayToCreateGroup
        'Switch traffic to the preferred group, then delete legacy group' == attrs.recommendedNextStep
    }

    def 'show should not display nonexistent cluster'() {
        controller.params.id = 'doesntexist'

        when:
        controller.show()

        then:
        '/error/missing' == view
        "Cluster 'doesntexist' not found in us-east-1 test" == controller.flash.message
    }

    def 'next group should have defaults'() {
        controller.awsAutoScalingService.getCluster(_, 'helloworld-example') >> constructClusterFromAsg(asg)
        controller.awsAutoScalingService.getLaunchConfiguration(_, 'helloworld-lc') >> launchConfiguration
        controller.params.name = 'helloworld-example'

        when:
        controller.createNextGroup()

        then:
        1 * controller.pushService.startGroupCreate({ GroupCreateOptions options ->
            options.with {
                assert common.appName == 'helloworld'
                assert common.imageId == 'lastImageId'
                assert common.instanceType == 'lastInstanceType'
                assert common.securityGroups == ['sg-123', 'sg-456']
                assert keyName == 'lastKeyName'
                assert availabilityZones == ['us-east-1c']
                assert loadBalancerNames == ['hello-elb']
                assert terminationPolicies == ['hello-tp']
                assert zoneRebalancingSuspended == false
                assert minSize == 3
                assert desiredCapacity == 5
                assert maxSize == 7
                assert healthCheckType == 'EC2'
                assert healthCheckGracePeriod == 42
                assert defaultCooldown == 360
                assert vpcZoneIdentifier == 'subnet-1'
                assert iamInstanceProfile == 'lastIamProfile'
                assert spotPrice == '1.23'
            }
            true
        }) >> { args ->
            new GroupCreateOperation(null).with {
                task = new Task()
                it
            }
        }
    }

    def 'next group should have no defaults for optional fields'() {
        controller.awsAutoScalingService.getCluster(_, 'helloworld-example') >> constructClusterFromAsg(asg)
        controller.awsAutoScalingService.getLaunchConfiguration(_, 'helloworld-lc') >> launchConfiguration
        controller.params.with {
            name = 'helloworld-example'
            noOptionalDefaults = 'true'
        }

        when:
        controller.createNextGroup()

        then:
        1 * controller.pushService.startGroupCreate({ GroupCreateOptions options ->
            options.with {
                assert common.appName == 'helloworld'
                assert common.imageId == 'lastImageId'
                assert common.instanceType == 'lastInstanceType'
                assert common.securityGroups == []
                assert keyName == 'lastKeyName'
                assert availabilityZones == ['us-east-1c']
                assert loadBalancerNames == []
                assert terminationPolicies == []
                assert zoneRebalancingSuspended == false
                assert minSize == 3
                assert desiredCapacity == 5
                assert maxSize == 7
                assert healthCheckType == 'EC2'
                assert healthCheckGracePeriod == 42
                assert defaultCooldown == 360
                assert vpcZoneIdentifier == null
                assert iamInstanceProfile == null
                assert spotPrice == '1.23'
            }
            true
        }) >> { args ->
            new GroupCreateOperation(null).with {
                task = new Task()
                it
            }
        }
    }

    def 'next group should have selections over defaults'() {
        controller.awsAutoScalingService.getCluster(_, 'helloworld-example') >> constructClusterFromAsg(asg)
        controller.awsAutoScalingService.getLaunchConfiguration(_, 'helloworld-lc') >> launchConfiguration
        controller.params.with() {
            name = 'helloworld-example'
            selectedSecurityGroups = 'sg-789'
            selectedZones = 'us-east-1e'
            terminationPolicy = 'hello-tp2'
            selectedLoadBalancers = 'hello-elb2'
            azRebalance = 'disabled'
            min = '13'
            desiredCapacity = '15'
            max = '17'
            imageId = 'newImageId'
            instanceType = 'newInstanceType'
            defaultCooldown = '720'
            healthCheckType = 'ELB'
            healthCheckGracePeriod = '72'
            iamInstanceProfile = 'newIamProfile'
            keyName = 'newKeyName'
            subnetPurpose = 'external'
            pricing = InstancePriceType.ON_DEMAND.name()
        }

        when:
        controller.createNextGroup()

        then:
        1 * controller.pushService.startGroupCreate({ GroupCreateOptions options ->
            options.with {
                assert common.appName == 'helloworld'
                assert common.imageId == 'newImageId'
                assert common.instanceType == 'newInstanceType'
                assert common.securityGroups == ['sg-789']
                assert keyName == 'newKeyName'
                assert availabilityZones == ['us-east-1e']
                assert loadBalancerNames == ['hello-elb2']
                assert terminationPolicies == ['hello-tp2']
                assert zoneRebalancingSuspended == true
                assert minSize == 13
                assert desiredCapacity == 15
                assert maxSize == 17
                assert healthCheckType == 'ELB'
                assert healthCheckGracePeriod == 72
                assert defaultCooldown == 720
                assert vpcZoneIdentifier == 'subnet-4'
                assert iamInstanceProfile == 'newIamProfile'
                assert spotPrice == null
            }
            true
        }) >> { args ->
            new GroupCreateOperation(null).with {
                task = new Task()
                it
            }
        }
    }

}
