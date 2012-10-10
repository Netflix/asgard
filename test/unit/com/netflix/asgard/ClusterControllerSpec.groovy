package com.netflix.asgard

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.push.Cluster
import grails.test.mixin.TestFor
import spock.lang.Specification
import com.amazonaws.services.autoscaling.model.Instance
import com.netflix.asgard.model.AutoScalingGroupMixin
import com.netflix.asgard.model.Subnets

@SuppressWarnings("GroovyPointlessArithmetic")
@TestFor(ClusterController)
class ClusterControllerSpec extends Specification {

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
            awsEc2Service.getSubnets(_) >> new Subnets([])
            awsEc2Service.getAvailabilityZones(_) >> []
            awsLoadBalancerService = Mock(AwsLoadBalancerService)
            awsLoadBalancerService.getLoadBalancers(_) >> []
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

}
