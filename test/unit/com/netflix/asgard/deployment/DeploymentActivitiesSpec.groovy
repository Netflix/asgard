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
package com.netflix.asgard.deployment

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.Instance
import com.amazonaws.services.autoscaling.model.ScheduledUpdateGroupAction
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.netflix.asgard.AwsAutoScalingService
import com.netflix.asgard.AwsEc2Service
import com.netflix.asgard.AwsLoadBalancerService
import com.netflix.asgard.AwsSimpleWorkflowService
import com.netflix.asgard.ConfigService
import com.netflix.asgard.DiscoveryService
import com.netflix.asgard.EmailerService
import com.netflix.asgard.LaunchTemplateService
import com.netflix.asgard.Region
import com.netflix.asgard.UserContext
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.AutoScalingProcessType
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.ScalingPolicyData
import com.netflix.asgard.model.SwfWorkflowTags
import com.netflix.asgard.model.WorkflowExecutionBeanOptions
import com.netflix.asgard.push.Cluster
import com.netflix.asgard.push.PushException
import com.netflix.glisten.ActivityOperations
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
class DeploymentActivitiesSpec extends Specification {

    UserContext userContext = UserContext.auto(Region.US_WEST_1)
    AwsAutoScalingService mockAwsAutoScalingService = Mock(AwsAutoScalingService)
    AwsEc2Service mockAwsEc2Service = Mock(AwsEc2Service)
    LaunchTemplateService mockLaunchTemplateService = Mock(LaunchTemplateService)
    ConfigService mockConfigService = Mock(ConfigService)
    DiscoveryService mockDiscoveryService = Mock(DiscoveryService)
    AwsLoadBalancerService mockAwsLoadBalancerService = Mock(AwsLoadBalancerService)
    EmailerService mockEmailerService = Mock(EmailerService)
    ActivityOperations mockActivityOperations = Mock(ActivityOperations)
    LinkGenerator mockLinkGenerator = Mock(LinkGenerator)
    AwsSimpleWorkflowService mockAwsSimpleWorkflowService = Mock(AwsSimpleWorkflowService)
    DeploymentActivities deploymentActivities = new DeploymentActivitiesImpl(
            awsAutoScalingService: mockAwsAutoScalingService, awsEc2Service: mockAwsEc2Service,
            launchTemplateService: mockLaunchTemplateService, configService: mockConfigService,
            discoveryService: mockDiscoveryService, awsLoadBalancerService: mockAwsLoadBalancerService,
            emailerService: mockEmailerService, activity: mockActivityOperations,
            grailsLinkGenerator: mockLinkGenerator, awsSimpleWorkflowService: mockAwsSimpleWorkflowService)

    AsgDeploymentNames asgDeploymentNames = new AsgDeploymentNames(
            previousAsgName: 'rearden_metal_pourer-v001',
            previousLaunchConfigName: 'rearden_metal_pourer-20130718090003',
            nextAsgName: 'rearden_metal_pourer-v002',
            nextLaunchConfigName: 'rearden_metal_pourer-20130718090004'
    )

    def 'should get ASG deployment names'() {
        when:
        AsgDeploymentNames asgDeploymentNames = deploymentActivities.getAsgDeploymentNames(userContext,
                'rearden_metal_pourer')

        then:
        asgDeploymentNames.previousAsgName == 'rearden_metal_pourer-v001'
        asgDeploymentNames.previousLaunchConfigName == 'rearden_metal_pourer-20130718090003'
        asgDeploymentNames.nextAsgName == 'rearden_metal_pourer-v002'
        asgDeploymentNames.nextLaunchConfigName.startsWith('rearden_metal_pourer-')

        1 * mockAwsAutoScalingService.getCluster(_, 'rearden_metal_pourer') >> new Cluster([
                new AutoScalingGroupData('rearden_metal_pourer-v001', null, null, null, [], null, null, [], [],
                        null, null, [], null, [], 'rearden_metal_pourer-20130718090003', [], [:], [], [:],
                        [], 'oldVpcZoneIdentifier')
        ])
        0 * _
    }

    def 'should construct launch config for next ASG'() {
        LaunchConfigurationBeanOptions inputs = new LaunchConfigurationBeanOptions(instanceType: 'instanceType2')

        when:
        def actualLaunchConfig = deploymentActivities.constructLaunchConfigForNextAsg(userContext,
                new AutoScalingGroupBeanOptions(launchConfigurationName: 'rearden_metal_pourer-20130718090004'),
                inputs)

        then:
        actualLaunchConfig == new LaunchConfigurationBeanOptions(instanceType: 'instanceType2',
                securityGroups: ['defaultSecurityGroup'], iamInstanceProfile: 'defaultIamRole',
                launchConfigurationName: 'rearden_metal_pourer-20130718090004')
        1 * mockConfigService.getDefaultIamRole() >> 'defaultIamRole'
        1 * mockLaunchTemplateService.includeDefaultSecurityGroups(_, false, _) >> ['defaultSecurityGroup']
    }

    def 'should create launch config for next ASG'() {
        when:
        deploymentActivities.createLaunchConfigForNextAsg(userContext, null,
                new LaunchConfigurationBeanOptions(launchConfigurationName: 'rearden_metal_pourer-20130718090004'))

        then:
        with(mockAwsAutoScalingService) {
            1 * createLaunchConfiguration(_, new LaunchConfigurationBeanOptions(
                    launchConfigurationName: 'rearden_metal_pourer-20130718090004'
            ), _)
        }
    }

    def 'should construct next ASG'() {
        AutoScalingGroupBeanOptions expectedAsg = new AutoScalingGroupBeanOptions(minSize: 0, maxSize: 6,
                desiredCapacity: 0, defaultCooldown: 200, healthCheckGracePeriod: 60,
                autoScalingGroupName: 'rearden_metal_pourer-v002',
                launchConfigurationName: 'rearden_metal_pourer-20130718090004',
                suspendedProcesses: [AutoScalingProcessType.AlarmNotifications] as Set)

        expect:
        deploymentActivities.constructNextAsgForCluster(userContext, asgDeploymentNames,
                new AutoScalingGroupBeanOptions(minSize: 4, maxSize: 6, defaultCooldown: 200,
                        healthCheckGracePeriod: 60, suspendedProcesses: [AutoScalingProcessType.AlarmNotifications])
        ) == expectedAsg
    }

    def 'should create next ASG'() {
        when:
        deploymentActivities.createNextAsgForCluster(userContext, new AutoScalingGroupBeanOptions(
                autoScalingGroupName: 'rearden_metal_pourer-v002',
                launchConfigurationName: 'rearden_metal_pourer-20130718090004',
                minSize: 4, desiredCapacity: 5, maxSize: 6, defaultCooldown: 200, healthCheckGracePeriod: 60,
                suspendedProcesses: [AutoScalingProcessType.Launch]))

        then:
        with(mockAwsAutoScalingService) {
            1 * createAutoScalingGroup(_, new AutoScalingGroupBeanOptions(
                    autoScalingGroupName: 'rearden_metal_pourer-v002',
                    launchConfigurationName: 'rearden_metal_pourer-20130718090004',
                    minSize: 4, desiredCapacity: 5, maxSize: 6, defaultCooldown: 200, healthCheckGracePeriod: 60,
                    suspendedProcesses: [AutoScalingProcessType.Launch]
            ), _) >> new AutoScalingGroup(autoScalingGroupName: 'rearden_metal_pourer-v002')
        }
    }

    def 'should copy scaling policies'() {
        when:
        deploymentActivities.copyScalingPolicies(userContext, asgDeploymentNames)

        then:
        with(mockAwsAutoScalingService) {
            1 * getScalingPolicyDatas(_, 'rearden_metal_pourer-v001') >> [new ScalingPolicyData(adjustment: 42,
                    adjustmentType: ScalingPolicyData.AdjustmentType.PercentChangeInCapacity,
                    autoScalingGroupName: 'rearden_metal_pourer-v001')]
            1 * createScalingPolicies(_, [new ScalingPolicyData(adjustment: 42,
                    adjustmentType: ScalingPolicyData.AdjustmentType.PercentChangeInCapacity,
                    autoScalingGroupName: 'rearden_metal_pourer-v002', alarms: [])], _)
        }
    }

    def 'should copy scheduled actions'() {
        when:
        deploymentActivities.copyScheduledActions(userContext, asgDeploymentNames)

        then:
        with(mockAwsAutoScalingService) {
            1 * getScheduledActionsForGroup(_, 'rearden_metal_pourer-v001') >> [new ScheduledUpdateGroupAction(
                    autoScalingGroupName: 'rearden_metal_pourer-v001')]
            1 * copyScheduledActionsForNewAsg(_, 'rearden_metal_pourer-v002', [new ScheduledUpdateGroupAction(
                    autoScalingGroupName: 'rearden_metal_pourer-v001')]) >> [new ScheduledUpdateGroupAction(
                    autoScalingGroupName: 'rearden_metal_pourer-v002')]
            1 * createScheduledActions(_, [new ScheduledUpdateGroupAction(
                    autoScalingGroupName: 'rearden_metal_pourer-v002')], _)
        }
    }

    def 'should resize ASG'() {
        when:
        deploymentActivities.resizeAsg(userContext, 'rearden_metal_pourer-v001', 1, 2, 3)

        then:
        with(mockAwsAutoScalingService) {
            1 * resizeAutoScalingGroup(_, 'rearden_metal_pourer-v001', 1, 2, 3)
        }
    }

    def 'should throw a reason if ASG is unhealthy'() {
        when:
        deploymentActivities.reasonAsgIsUnhealthy(userContext, 'rearden_metal_pourer-v001', 2)

        then:
        with(mockAwsAutoScalingService) {
            1 * reasonAsgIsUnhealthy(_, 'rearden_metal_pourer-v001', 2) >> 'Who is John Galt?'
        }
        PushException exception = thrown()
        exception.message == 'Who is John Galt?'
    }

    def 'should enable an ASG'() {
        when:
        deploymentActivities.enableAsg(userContext, 'rearden_metal_pourer-v001')

        then:
        with(mockAwsAutoScalingService) {
            1 * getAutoScalingGroup(_, 'rearden_metal_pourer-v001') >> new AutoScalingGroup(
                    launchConfigurationName: 'rearden_metal_pourer-20130718090003',
                    minSize: 3, desiredCapacity: 5, maxSize: 6, defaultCooldown: 100,
                    instances: [new Instance(instanceId: 'i-deadc0de'), new Instance(instanceId: 'i-baadc0de')],
                    loadBalancerNames: ['elb1']
            )
            1 * removeExpirationTime(_, 'rearden_metal_pourer-v001', _)
            1 * resumeProcess(_, AutoScalingProcessType.Launch, 'rearden_metal_pourer-v001', _)
            1 * resumeProcess(_, AutoScalingProcessType.Terminate, 'rearden_metal_pourer-v001', _)
            1 * resumeProcess(_, AutoScalingProcessType.AddToLoadBalancer, 'rearden_metal_pourer-v001', _)
        }
        with(mockConfigService) {
            1 * doesRegionalDiscoveryExist(_) >> true
        }
        with(mockAwsLoadBalancerService) {
            1 * addInstances(_, 'elb1', ['i-deadc0de', 'i-baadc0de'], _)
        }
        with(mockDiscoveryService) {
            1 * enableAppInstances(_, 'rearden_metal_pourer', ['i-deadc0de', 'i-baadc0de'], _)
        }
        0 * _
    }

    def 'should disable an ASG'() {
        when:
        deploymentActivities.disableAsg(userContext, 'rearden_metal_pourer-v001')

        then:
        with(mockAwsAutoScalingService) {
            1 * getAutoScalingGroup(_, 'rearden_metal_pourer-v001') >> new AutoScalingGroup(
                    launchConfigurationName: 'rearden_metal_pourer-20130718090003',
                    minSize: 3, desiredCapacity: 5, maxSize: 6, defaultCooldown: 100,
                    instances: [new Instance(instanceId: 'i-deadc0de'), new Instance(instanceId: 'i-baadc0de')],
                    loadBalancerNames: ['elb1']
            )
            1 * setExpirationTime(_, 'rearden_metal_pourer-v001', _, _)
            1 * suspendProcess(_, AutoScalingProcessType.Launch, 'rearden_metal_pourer-v001', _)
            1 * suspendProcess(_, AutoScalingProcessType.Terminate, 'rearden_metal_pourer-v001', _)
            1 * suspendProcess(_, AutoScalingProcessType.AddToLoadBalancer, 'rearden_metal_pourer-v001', _)
        }
        with(mockConfigService) {
            1 * doesRegionalDiscoveryExist(_) >> true
        }
        with(mockAwsLoadBalancerService) {
            1 * removeInstances(_, 'elb1', ['i-deadc0de', 'i-baadc0de'], _)
        }
        with(mockDiscoveryService) {
            1 * disableAppInstances(_, 'rearden_metal_pourer', ['i-deadc0de', 'i-baadc0de'], _)
        }
        0 * _
    }

    def 'should delete an ASG'() {
        when:
        deploymentActivities.deleteAsg(userContext, 'rearden_metal_pourer-v001')

        then:
        with(mockAwsAutoScalingService) {
            1 * getAutoScalingGroup(_, 'rearden_metal_pourer-v001') >> new AutoScalingGroup(
                    launchConfigurationName: 'rearden_metal_pourer-20130718090003',
                    minSize: 3, desiredCapacity: 5, maxSize: 6, defaultCooldown: 100,
                    instances: [new Instance(instanceId: 'i-deadc0de'), new Instance(instanceId: 'i-baadc0de')],
                    loadBalancerNames: ['elb1']
            )
            1 * getLaunchConfigurationNamesForAutoScalingGroup(_, 'rearden_metal_pourer-v001') >> ['oldLC']
            1 * deleteAutoScalingGroup(_, 'rearden_metal_pourer-v001', _, _)
            1 * deleteLaunchConfiguration(_, 'oldLC', _)
        }
        0 * _
    }

    def 'should ask if deployment should proceed'() {
        when:
        deploymentActivities.askIfDeploymentShouldProceed('hrearden@reardenmetal.com', 'rearden_metal_pourer-v001',
                'It has finished pouring.', null)

        then:
        with(mockActivityOperations) {
            getWorkflowExecution() >> new WorkflowExecution(runId: '123', workflowId: 'abc')
            getTaskToken() >> '8badf00d'
        }
        with(mockLinkGenerator) {
            1 * link(_) >> '<link>'
        }
        with(mockEmailerService) {
            1 * sendUserEmail('hrearden@reardenmetal.com',
                    'Asgard deployment response requested for \'rearden_metal_pourer-v001\'.',
                    '''
                    Auto Scaling Group \'rearden_metal_pourer-v001\' is being deployed.
                    It has finished pouring.
                    Auto Scaling Group \'rearden_metal_pourer-v001\' is healthy.
                    Please determine if the deployment should proceed.

                    <link>
                    '''.stripIndent())
        }
        with(mockConfigService) {
            1 * getLinkCanonicalServerUrl() >> 'http://asgard'
        }
        with(mockAwsSimpleWorkflowService) {
            1 * getWorkflowExecutionInfoByWorkflowExecution(new WorkflowExecution(runId: '123', workflowId: 'abc')) >>
                    Mock(WorkflowExecutionBeanOptions) {
                getTags() >> new SwfWorkflowTags(id: 1)
            }
        }
        0 * _
    }

    def 'should send notification'() {
        when:
        deploymentActivities.sendNotification('hrearden@reardenmetal.com', 'rearden_metal_pourer-v001',
                'Read this Hank!', 'Production has halted.')

        then:
        with(mockActivityOperations) {
            getWorkflowExecution() >> new WorkflowExecution(runId: '123', workflowId: 'abc')
            getTaskToken() >> '8badf00d'
        }
        with(mockLinkGenerator) {
            1 * link(_) >> '<link>'
        }
        with(mockEmailerService) {
            1 * sendUserEmail('hrearden@reardenmetal.com',
                    'Read this Hank!',
                    '''
                    Auto Scaling Group \'rearden_metal_pourer-v001\' is unhealthy. Production has halted.

                    <link>
                    '''.stripIndent())
        }
        with(mockConfigService) {
            1 * getLinkCanonicalServerUrl() >> 'http://asgard'
        }
        0 * _
    }

}
