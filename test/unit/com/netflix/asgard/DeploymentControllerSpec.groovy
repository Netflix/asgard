package com.netflix.asgard

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.Instance
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.AutoScalingGroupMixin
import com.netflix.asgard.model.Deployment
import com.netflix.asgard.model.Subnets
import com.netflix.asgard.push.Cluster
import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(DeploymentController)
class DeploymentControllerSpec extends Specification {

    void setup() {
        AutoScalingGroup.mixin AutoScalingGroupMixin
        controller.with() {
            awsAutoScalingService = Mock(AwsAutoScalingService)
            awsEc2Service = Mock(AwsEc2Service) {
                getSubnets(_) >> new Subnets([])
            }
            deploymentService = Mock(DeploymentService)
            flowService = Mock(FlowService)
        }
    }

    def 'deploy should fail on an invalid cluster name'() {
        DeployCommand cmd = Mock(DeployCommand)
        1 * cmd.hasErrors() >> true

        when:
        controller.deploy(cmd)

        then:
        flash.chainModel.cmd == cmd
        response.redirectedUrl == '/cluster/prepareDeployment'
    }

    def 'deploy should fail when the cluster has two autoscaling group'() {
        DeployCommand cmd = new DeployCommand(clusterName: 'helloworld')
        controller.awsAutoScalingService.getCluster(_, 'helloworld') >> {
            new Cluster([
                    AutoScalingGroupData.from(new AutoScalingGroup(autoScalingGroupName: 'helloworld-example-v014',
                            instances: [new Instance(instanceId: 'i-8ee4eeee')]), [:], [], [:], []),
                    AutoScalingGroupData.from(new AutoScalingGroup(autoScalingGroupName: 'helloworld-example-v015',
                            instances: [new Instance(instanceId: 'i-6ef9f30e'), new Instance(instanceId: 'i-95fe1df6')]),
                            [:], [], [:], [])
            ])
        }

        when:
        controller.deploy(cmd)

        then:
        flash.message == "Cluster 'helloworld' should only have one ASG to enable automatic deployment."
        flash.chainModel.cmd == cmd
        response.redirectedUrl == '/cluster/prepareDeployment/helloworld'
    }

    @Unroll
    def 'deploy should fail when the current cluster is not accepting traffic'() {
        DeployCommand cmd = new DeployCommand(clusterName: 'helloworld')
        Cluster cluster = Mock(Cluster)
        AutoScalingGroupData asg = Mock(AutoScalingGroupData)
        controller.awsAutoScalingService.getCluster(_, 'helloworld') >> cluster
        cluster.size() >> 1
        cluster.last() >> asg
        asg.isLaunchingSuspended() >> launchSuspended
        asg.isTerminatingSuspended() >> terminateSuspended
        asg.isAddingToLoadBalancerSuspended() >> addToLoadBalancerSuspended

        when:
        controller.deploy(cmd)

        then:
        flash.message == "ASG in cluster 'helloworld' should be receiving traffic to enable automatic deployment."
        flash.chainModel.cmd == cmd
        response.redirectedUrl == '/cluster/prepareDeployment/helloworld'

        where:
        launchSuspended | terminateSuspended | addToLoadBalancerSuspended
        true            | true               | true
        true            | false              | false
        false           | true               | false
        false           | false              | true
    }

    def 'should deploy'() {
        DeployCommand cmd = new DeployCommand(clusterName: 'helloworld')
        controller.awsAutoScalingService.getCluster(_, 'helloworld') >> {
            new Cluster([
                    AutoScalingGroupData.from(new AutoScalingGroup(autoScalingGroupName: 'helloworld-example-v014',
                            instances: [new Instance(instanceId: 'i-8ee4eeee')]), [:], [], [:], [])
            ])
        }

        when:
        controller.deploy(cmd)

        then:
        1 * controller.deploymentService.startDeployment(_, 'helloworld', _, _, _) >> '123'
        flash.message == null
        response.redirectedUrl == '/deployment/show/123'
    }

    def 'should proceed with deployment'() {
        when:
        controller.proceed('123', 'abc')

        then:
        1 * controller.flowService.getManualActivityCompletionClient('abc') >> Mock(ManualActivityCompletionClient) {
            1 * complete(true)
        }
        0 * _

        and:
        flash.message == 'Automated deployment will proceed.'
        response.redirectedUrl == '/deployment/show/123'
    }

    def 'should rollback deployment'() {
        when:
        controller.rollback('123', 'abc')

        then:
        1 * controller.flowService.getManualActivityCompletionClient('abc') >> Mock(ManualActivityCompletionClient) {
            1 * complete(false)
        }
        0 * _

        and:
        flash.message == 'Automated deployment will not proceed.'
        response.redirectedUrl == '/deployment/show/123'
    }

    def 'should fail deployment decision on error'() {
        when:
        controller.proceed('123', 'abc')

        then:
        1 * controller.flowService.getManualActivityCompletionClient('abc') >> Mock(ManualActivityCompletionClient) {
            1 * complete(true) >> { throw new IllegalAccessException('Oh noes!') }
        }
        0 * _

        and:
        flash.message == 'Deployment failed: java.lang.IllegalAccessException: Oh noes!'
        response.redirectedUrl == '/deployment/show/123'
    }

    def 'should cancel deployment'() {
        Deployment deployment = new Deployment('123', 'hiworld', Region.US_EAST_1, new WorkflowExecution(), 'deployin')

        when:
        controller.cancel('123')

        then:
        flash.message == "Deployment '123' canceled ('deployin')."
        response.redirectedUrl == '/deployment/show/123'

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

    def 'should show deployment'() {
        when:
        def result = controller.show('123')

        then:
        result.deployment == new Deployment('123')

        and:
        1 * controller.deploymentService.getDeploymentById('123') >> new Deployment('123')
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
}
