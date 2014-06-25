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

import com.amazonaws.AmazonServiceException
import com.netflix.asgard.Region
import com.netflix.asgard.UserContext
import com.netflix.asgard.deployment.steps.CreateAsgStep
import com.netflix.asgard.deployment.steps.DeleteAsgStep
import com.netflix.asgard.deployment.steps.DisableAsgStep
import com.netflix.asgard.deployment.steps.JudgmentStep
import com.netflix.asgard.deployment.steps.ResizeStep
import com.netflix.asgard.deployment.steps.WaitStep
import com.netflix.asgard.model.AsgRoleInCluster
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.InstancePriceType
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.ScheduledAsgAnalysis
import com.netflix.glisten.impl.local.LocalWorkflowOperations
import org.joda.time.DateTime
import spock.lang.Specification

class DeploymentWorkflowSpec extends Specification {

    DeploymentActivities mockActivities = Mock(DeploymentActivities)
    LocalWorkflowOperations workflowOperations = LocalWorkflowOperations.of(mockActivities)
    def workflowExecuter = workflowOperations.getExecuter(DeploymentWorkflowImpl).&deploy as DeploymentWorkflow

    UserContext userContext = UserContext.auto(Region.US_WEST_2)

    AsgDeploymentNames asgDeploymentNames = new AsgDeploymentNames(
        previousAsgName: 'the_seaward-v002', previousLaunchConfigName: 'the_seaward-v002-20130626140848',
        nextAsgName: 'the_seaward-v003', nextLaunchConfigName: 'the_seaward-v003-20130626140848'
    )

    LaunchConfigurationBeanOptions lcInputs = new LaunchConfigurationBeanOptions(securityGroups: ['sg-defec8ed'],
            instancePriceType: InstancePriceType.ON_DEMAND)

    LaunchConfigurationBeanOptions lcTemplate = new LaunchConfigurationBeanOptions(
            securityGroups: ['sg-defec8ed', 'sg-default'], iamInstanceProfile: 'defaultIamInstanceProfile',
            instancePriceType: InstancePriceType.ON_DEMAND, launchConfigurationName: 'the_seaward-v003-20130626140848')

    AutoScalingGroupBeanOptions asgInputs = new AutoScalingGroupBeanOptions(
            availabilityZones: ['us-west2a', 'us-west2b'], minSize: 1, desiredCapacity: 3, maxSize: 4,
            subnetPurpose: 'internal')

    AutoScalingGroupBeanOptions asgTemplate = new AutoScalingGroupBeanOptions(
            availabilityZones: ['us-west2a', 'us-west2b'], minSize: 1, desiredCapacity: 3, maxSize: 4,
            subnetPurpose: 'internal', launchConfigurationName: 'the_seaward-v003-20130626140848',
            autoScalingGroupName: 'the_seaward-v003')

    List<String> createAsgLog = [
            "Creating Launch Configuration 'the_seaward-v003-20130626140848'.",
            "Creating Auto Scaling Group 'the_seaward-v003' initially with 0 instances.",
            'Copying Scaling Policies and Scheduled Actions.'
    ]

    String canaryScaleUpLog = "Waiting up to 30 minutes while resizing to 1 instance."

    String canaryJudgeLog = "ASG will now be evaluated for up to 60 minutes during the judgment period."

    String fullCapacityScaleUpLog = "Waiting up to 40 minutes while resizing to 3 instances."

    private createAsgInteractions() {
        with(mockActivities) {
            1 * getAsgDeploymentNames(userContext, 'the_seaward') >> asgDeploymentNames
            1 * constructLaunchConfigForNextAsg(userContext, asgTemplate, lcInputs) >> lcTemplate
            1 * createLaunchConfigForNextAsg(userContext, asgTemplate, lcTemplate) >> 'the_seaward-v003-20130626140848'
            1 * createNextAsgForClusterWithoutInstances(userContext, asgTemplate) >> 'the_seaward-v003'
            1 * copyScalingPolicies(userContext, asgDeploymentNames) >> 0
            1 * copyScheduledActions(userContext, asgDeploymentNames) >> 0
        }
    }

    def 'should execute full deployment'() {
        workflowOperations.addFiredTimerNames(['delay', 'waitAfterEurekaChange'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new WaitStep(durationMinutes: 10, description:  "delay"),
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 1, startUpTimeoutMinutes: 30),
                        new ResizeStep(capacity: 3, startUpTimeoutMinutes: 40),
                        new DisableAsgStep(targetAsg: AsgRoleInCluster.Previous),
                        new DeleteAsgStep(targetAsg: AsgRoleInCluster.Previous)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}', 'Waiting 10 minutes before next step.', '{"step":1}'] +
                createAsgLog + '{"step":2}' + canaryScaleUpLog + '{"step":3}' + fullCapacityScaleUpLog + [
                '{"step":4}',
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                '{"step":5}',
                "Deleting ASG 'the_seaward-v002'.",
                "Deployment was successful."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> ''
        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.deleteAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment succeeded for ASG 'the_seaward-v003'.", "Deployment was successful.")
    }

    def 'should remind judge to decide at the end of judgment period'() {
        workflowOperations.addFiredTimerNames(['delay', 'judgmentTimeout'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new WaitStep(durationMinutes: 10, description:  "delay"),
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 1, startUpTimeoutMinutes: 30),
                        new JudgmentStep(durationMinutes: 60),
                        new ResizeStep(capacity: 3, startUpTimeoutMinutes: 40)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}', 'Waiting 10 minutes before next step.', '{"step":1}'] +
                createAsgLog + '{"step":2}' + canaryScaleUpLog + '{"step":3}' + canaryJudgeLog +
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable."

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> ''
        then: 1 * mockActivities.startAsgAnalysis('the_seaward', 'gob@bluth.com') >> new ScheduledAsgAnalysis(
                "ASG analysis for 'the_seaward' cluster.", new DateTime())
        then: 1 * mockActivities.askIfDeploymentShouldProceed(_, 'gob@bluth.com', 'the_seaward-v003',
                "ASG will now be evaluated for up to 60 minutes during the judgment period.") >> false
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Judgment period for ASG 'the_seaward-v003' has ended.",
                "Please make a decision to proceed or roll back.")
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002') >> true
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003') >> true
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable.")
    }

    def 'should execute deployment without canary or delay'() {
        workflowOperations.addFiredTimerNames(['waitAfterEurekaChange'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 3, startUpTimeoutMinutes: 40),
                        new DisableAsgStep(targetAsg: AsgRoleInCluster.Previous),
                        new DeleteAsgStep(targetAsg: AsgRoleInCluster.Previous)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}'] + createAsgLog + '{"step":1}' + fullCapacityScaleUpLog + [
                '{"step":2}',
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                '{"step":3}',
                "Deleting ASG 'the_seaward-v002'.",
                "Deployment was successful."
        ]
        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.deleteAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment succeeded for ASG 'the_seaward-v003'.", "Deployment was successful.")
    }

    def 'should execute canary without scaling up'() {
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 1, startUpTimeoutMinutes: 30)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}'] + createAsgLog + '{"step":1}' + canaryScaleUpLog + [
                "Deployment was successful."
        ]
        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> ''
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment succeeded for ASG 'the_seaward-v003'.", "Deployment was successful.")
    }

    def 'should display error and rollback deployment if there is an error checking health'() {
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 1, startUpTimeoutMinutes: 30)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}'] + createAsgLog + '{"step":1}' + canaryScaleUpLog +
                "Deployment was rolled back due to error: java.lang.IllegalStateException: Something really went wrong!"
        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> {
            throw new IllegalStateException('Something really went wrong!')
        }
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002') >> true
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003') >> true
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back due to error: java.lang.IllegalStateException: Something really went wrong!"
        )
    }

    def 'should retry health check if not ready yet.'() {
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 3, startUpTimeoutMinutes: 40)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}'] + createAsgLog + '{"step":1}' + fullCapacityScaleUpLog + [
                "Deployment was successful."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> 'Not healthy Yet'
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment succeeded for ASG 'the_seaward-v003'.", "Deployment was successful.")
    }

    def 'should rollback for start up time out'() {
        workflowOperations.addFiredTimerNames(['startupTimeout'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 1, startUpTimeoutMinutes: 30)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}'] + createAsgLog + '{"step":1}' + canaryScaleUpLog +
                "Deployment was rolled back. ASG 'the_seaward-v003' was not at capacity after 30 minutes."

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 4)
        then: mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> 'Not operational yet.'
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002') >> true
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003') >> true
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back. ASG 'the_seaward-v003' was not at capacity after 30 minutes.")
    }

    def 'should rollback for desired capacity start up time out'() {
        workflowOperations.addFiredTimerNames(['startupTimeout'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 3, startUpTimeoutMinutes: 40)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}'] + createAsgLog + '{"step":1}' + fullCapacityScaleUpLog +
                "Deployment was rolled back. ASG 'the_seaward-v003' was not at capacity after 40 minutes."

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 3, 4)
        then: (1.._) * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> 'Not healthy Yet'
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002') >> true
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003') >> true
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back. ASG 'the_seaward-v003' was not at capacity after 40 minutes.")
    }

    def 'should rollback for canary decision to not proceed'() {
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 1, startUpTimeoutMinutes: 30),
                        new JudgmentStep(durationMinutes: 60),
                        new ResizeStep(capacity: 3, startUpTimeoutMinutes: 40)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}'] + createAsgLog + '{"step":1}' + canaryScaleUpLog +
                '{"step":2}' + canaryJudgeLog +
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable."

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> ''
        then: 1 * mockActivities.startAsgAnalysis('the_seaward', 'gob@bluth.com') >> new ScheduledAsgAnalysis(
                "ASG analysis for 'the_seaward' cluster.", new DateTime())
        then: 1 * mockActivities.askIfDeploymentShouldProceed(_, 'gob@bluth.com', 'the_seaward-v003',
                "ASG will now be evaluated for up to 60 minutes during the judgment period.") >> false
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002') >> true
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003') >> true
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable.")
    }

    def 'should continue deployment for canary decision to proceed'() {
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 1, startUpTimeoutMinutes: 30),
                        new JudgmentStep(durationMinutes: 60),
                        new ResizeStep(capacity: 3, startUpTimeoutMinutes: 40)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}'] + createAsgLog + '{"step":1}' + canaryScaleUpLog +
                '{"step":2}' + canaryJudgeLog + '{"step":3}' + fullCapacityScaleUpLog + [
                "Deployment was successful."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> ''
        then: 1 * mockActivities.startAsgAnalysis('the_seaward', 'gob@bluth.com') >> new ScheduledAsgAnalysis(
                "ASG analysis for 'the_seaward' cluster.", new DateTime())
        then: 1 * mockActivities.askIfDeploymentShouldProceed(_, 'gob@bluth.com', 'the_seaward-v003',
                "ASG will now be evaluated for up to 60 minutes during the judgment period.") >> true
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment succeeded for ASG 'the_seaward-v003'.", "Deployment was successful.")
    }

    def 'should rollback deployment for full capacity decision to not proceed'() {
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 3, startUpTimeoutMinutes: 40),
                        new JudgmentStep(durationMinutes: 120),
                        new DisableAsgStep(targetAsg: AsgRoleInCluster.Previous),
                        new DeleteAsgStep(targetAsg: AsgRoleInCluster.Previous)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}'] + createAsgLog + '{"step":1}' + fullCapacityScaleUpLog + [
                '{"step":2}',
                "ASG will now be evaluated for up to 120 minutes during the judgment period.",
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.startAsgAnalysis('the_seaward', 'gob@bluth.com') >> new ScheduledAsgAnalysis(
                "ASG analysis for 'the_seaward' cluster.", new DateTime())
        then: 1 * mockActivities.askIfDeploymentShouldProceed(_, 'gob@bluth.com', 'the_seaward-v003',
                "ASG will now be evaluated for up to 120 minutes during the judgment period.") >> false
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002') >> true
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003') >> true
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable.")
    }

    def 'should continue with full capacity decision to proceed'() {
        workflowOperations.addFiredTimerNames(['waitAfterEurekaChange'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 3, startUpTimeoutMinutes: 40),
                        new JudgmentStep(durationMinutes: 120),
                        new DisableAsgStep(targetAsg: AsgRoleInCluster.Previous),
                        new DeleteAsgStep(targetAsg: AsgRoleInCluster.Previous)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}'] + createAsgLog + '{"step":1}' + fullCapacityScaleUpLog + [
                '{"step":2}',
                "ASG will now be evaluated for up to 120 minutes during the judgment period.",
                '{"step":3}',
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                '{"step":4}',
                "Deleting ASG 'the_seaward-v002'.",
                "Deployment was successful."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.startAsgAnalysis('the_seaward', 'gob@bluth.com') >> new ScheduledAsgAnalysis(
                "ASG analysis for 'the_seaward' cluster.", new DateTime())
        then: 1 * mockActivities.askIfDeploymentShouldProceed(_, 'gob@bluth.com', 'the_seaward-v003',
                "ASG will now be evaluated for up to 120 minutes during the judgment period.") >> true
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.deleteAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment succeeded for ASG 'the_seaward-v003'.", "Deployment was successful.")
    }

    def 'should not delete previous ASG if specified not to'() {
        workflowOperations.addFiredTimerNames(['waitAfterEurekaChange'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 3, startUpTimeoutMinutes: 40),
                        new DisableAsgStep(targetAsg: AsgRoleInCluster.Previous)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}'] + createAsgLog + '{"step":1}' + fullCapacityScaleUpLog + [
                '{"step":2}',
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                "Deployment was successful."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment succeeded for ASG 'the_seaward-v003'.", "Deployment was successful.")
    }

    def 'should rollback deployment for full traffic decision to not proceed'() {
        workflowOperations.addFiredTimerNames(['waitAfterEurekaChange'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 3, startUpTimeoutMinutes: 40),
                        new DisableAsgStep(targetAsg: AsgRoleInCluster.Previous),
                        new JudgmentStep(durationMinutes: 240),
                        new DeleteAsgStep(targetAsg: AsgRoleInCluster.Previous)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}'] + createAsgLog + '{"step":1}' + fullCapacityScaleUpLog + [
                '{"step":2}',
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                '{"step":3}',
                "ASG will now be evaluated for up to 240 minutes during the judgment period.",
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.startAsgAnalysis('the_seaward', 'gob@bluth.com') >> new ScheduledAsgAnalysis(
                "ASG analysis for 'the_seaward' cluster.", new DateTime())
        then: 1 * mockActivities.askIfDeploymentShouldProceed(_, 'gob@bluth.com', 'the_seaward-v003',
                "ASG will now be evaluated for up to 240 minutes during the judgment period.") >> false
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002') >> true
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003') >> true
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable.")
    }

    def 'should not rollback if previous ASG has disappeared'() {
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                steps: [
                        new CreateAsgStep(),
                        new ResizeStep(capacity: 1, startUpTimeoutMinutes: 30)
                ]
        )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['{"step":0}'] + createAsgLog + '{"step":1}' + canaryScaleUpLog +
                "Previous ASG 'the_seaward-v002' could not be enabled." +
                "Deployment was rolled back due to error: java.lang.IllegalStateException: Something really went wrong!"
        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> {
            throw new IllegalStateException('Something really went wrong!')
        }
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002') >> {
            throw new AmazonServiceException('AutoScalingGroup name not found - no such group: the_seaward-v002')
        }
        then: 0 * mockActivities.disableAsg(userContext, 'the_seaward-v003') >> true
        then: 1 * mockActivities.sendNotification(_, 'gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back due to error: java.lang.IllegalStateException: Something really went wrong!"
        )
    }
}
