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

import com.netflix.asgard.Region
import com.netflix.asgard.UserContext
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
            availabilityZones: ['us-west2a', 'us-west2b'], minSize: 2, desiredCapacity: 3, maxSize: 4,
            subnetPurpose: 'internal')

    AutoScalingGroupBeanOptions asgTemplate = new AutoScalingGroupBeanOptions(
            availabilityZones: ['us-west2a', 'us-west2b'], minSize: 2, desiredCapacity: 3, maxSize: 4,
            subnetPurpose: 'internal', launchConfigurationName: 'the_seaward-v003-20130626140848',
            autoScalingGroupName: 'the_seaward-v003')

    List<String> createAsgLog = [
            "Starting deployment for Cluster 'the_seaward'.",
            "Creating Launch Configuration 'the_seaward-v003-20130626140848'.",
            "Creating Auto Scaling Group 'the_seaward-v003' initially with 0 instances.",
            'Copying Scaling Policies and Scheduled Actions.',
            "New ASG 'the_seaward-v003' was successfully created."
    ]

    String canaryScaleUpLog = "Scaling new ASG to canary capacity. Waiting up to 30 minutes for 1 instance."

    String canaryJudgeLog = "ASG will now be evaluated for up to 60 minutes during the canary capacity judgment period."

    String fullCapacityScaleUpLog = "Scaling new ASG to full capacity. Waiting up to 40 minutes for 3 instances."

    private createAsgInteractions() {
        with(mockActivities) {
            1 * getAsgDeploymentNames(userContext, 'the_seaward') >> asgDeploymentNames
            1 * constructLaunchConfigForNextAsg(userContext, asgTemplate, lcInputs) >> lcTemplate
            1 * createLaunchConfigForNextAsg(userContext, asgTemplate, lcTemplate) >> 'the_seaward-v003-20130626140848'
            1 * createNextAsgForClusterWithoutInstances(userContext, asgTemplate) >> 'the_seaward-v003'
            1 * copyScalingPolicies(userContext, asgDeploymentNames) >> 0
            1 * copyScheduledActions(userContext, asgDeploymentNames) >> 0
            1 * startAsgAnalysis('the_seaward', 'gob@bluth.com') >> new ScheduledAsgAnalysis(
                    "ASG analysis for 'the_seaward' cluster.", new DateTime())
        }
    }

    def 'should execute full deployment'() {
        workflowOperations.addFiredTimerNames(['delay', 'waitAfterEurekaChange'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', delayDurationMinutes: 10, doCanary: true,
                canaryCapacity: 1, canaryStartUpTimeoutMinutes: 30, canaryJudgmentPeriodMinutes: 60,
                desiredCapacityStartUpTimeoutMinutes: 40, desiredCapacityJudgmentPeriodMinutes: 120,
                fullTrafficJudgmentPeriodMinutes: 240, scaleUp: ProceedPreference.Yes,
                disablePreviousAsg: ProceedPreference.Yes, deletePreviousAsg: ProceedPreference.Yes)

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['Waiting 10 minutes before starting deployment.'] +
                createAsgLog + canaryScaleUpLog + fullCapacityScaleUpLog + [
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                "Deleting ASG 'the_seaward-v002'.",
                "Deployment was successful."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 1)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> ''
        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.deleteAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Deployment succeeded for ASG 'the_seaward-v003'.", "Deployment was successful.")
    }

    def 'should remind judge to decide at the end of judgment period'() {
        workflowOperations.addFiredTimerNames(['delay', 'judgmentTimeout'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', delayDurationMinutes: 10, doCanary: true,
                canaryCapacity: 1, canaryStartUpTimeoutMinutes: 30, canaryJudgmentPeriodMinutes: 60,
                desiredCapacityStartUpTimeoutMinutes: 40, desiredCapacityJudgmentPeriodMinutes: 120,
                fullTrafficJudgmentPeriodMinutes: 240, scaleUp: ProceedPreference.Ask,
                disablePreviousAsg: ProceedPreference.No)

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == ['Waiting 10 minutes before starting deployment.'] +
                createAsgLog + canaryScaleUpLog + canaryJudgeLog +
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable."

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 1)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> ''
        then: 1 * mockActivities.askIfDeploymentShouldProceed('gob@bluth.com', 'the_seaward-v003',
                "ASG will now be evaluated for up to 60 minutes during the canary capacity judgment period.") >> false
        then:
        1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Canary capacity judgement period for ASG 'the_seaward-v003' has ended.",
                "Please make a decision to proceed or roll back.")
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable.")
    }

    def 'should execute deployment without canary or delay'() {
        workflowOperations.addFiredTimerNames(['waitAfterEurekaChange'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', doCanary: false, desiredCapacityStartUpTimeoutMinutes: 40,
                desiredCapacityJudgmentPeriodMinutes: 120, fullTrafficJudgmentPeriodMinutes: 240,
                scaleUp: ProceedPreference.Yes, disablePreviousAsg: ProceedPreference.Yes,
                deletePreviousAsg: ProceedPreference.Yes)

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == createAsgLog + fullCapacityScaleUpLog + [
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                "Deleting ASG 'the_seaward-v002'.",
                "Deployment was successful."
        ]
        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.deleteAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Deployment succeeded for ASG 'the_seaward-v003'.", "Deployment was successful.")
    }

    def 'should execute canary without scaling up'() {
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', doCanary: true,
                canaryCapacity: 1, canaryStartUpTimeoutMinutes: 30, canaryJudgmentPeriodMinutes: 60,
                scaleUp: ProceedPreference.No)

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == createAsgLog + canaryScaleUpLog + [
                "ASG 'the_seaward-v002' was not disabled. The new ASG is not taking full traffic.",
                "Deployment was successful."
        ]
        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 1)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> ''
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Deployment succeeded for ASG 'the_seaward-v003'.", "Deployment was successful.")
    }

    def 'should display error and rollback deployment if there is an error checking health'() {
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', doCanary: true,
                canaryCapacity: 1, canaryStartUpTimeoutMinutes: 30, canaryJudgmentPeriodMinutes: 60,
                scaleUp: ProceedPreference.No)

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == createAsgLog + canaryScaleUpLog +
                "Deployment was rolled back due to error: java.lang.IllegalStateException: Something really went wrong!"
        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 1)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> {
            throw new IllegalStateException('Something really went wrong!')
        }
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back due to error: java.lang.IllegalStateException: Something really went wrong!"
        )
    }

    def 'should retry health check if not ready yet.'() {
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', doCanary: false,
                scaleUp: ProceedPreference.Yes, disablePreviousAsg: ProceedPreference.No,
                desiredCapacityStartUpTimeoutMinutes: 40)

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == createAsgLog + fullCapacityScaleUpLog + [
                "ASG 'the_seaward-v002' was not disabled. The new ASG is not taking full traffic.",
                "Deployment was successful."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> 'Not healthy Yet'
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Deployment succeeded for ASG 'the_seaward-v003'.", "Deployment was successful.")
    }

    def 'should rollback for canary start up time out'() {
        workflowOperations.addFiredTimerNames(['startupTimeout'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', doCanary: true,
                canaryCapacity: 1, canaryStartUpTimeoutMinutes: 30, canaryJudgmentPeriodMinutes: 60,
                scaleUp: ProceedPreference.No)

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == createAsgLog + canaryScaleUpLog +
                "Deployment was rolled back. ASG 'the_seaward-v003' was not at capacity after 30 minutes."

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 1)
        then: mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> 'Not operational yet.'
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back. ASG 'the_seaward-v003' was not at capacity after 30 minutes.")
    }

    def 'should rollback for desired capacity start up time out'() {
        workflowOperations.addFiredTimerNames(['startupTimeout'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', desiredCapacityStartUpTimeoutMinutes: 40,
                scaleUp: ProceedPreference.Yes)

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == createAsgLog + fullCapacityScaleUpLog +
                "Deployment was rolled back. ASG 'the_seaward-v003' was not at capacity after 40 minutes."

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: (1.._) * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> 'Not healthy Yet'
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back. ASG 'the_seaward-v003' was not at capacity after 40 minutes.")
    }

    def 'should rollback for canary decision to not proceed'() {
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', doCanary: true,
                canaryCapacity: 1, canaryStartUpTimeoutMinutes: 30, canaryJudgmentPeriodMinutes: 60,
                scaleUp: ProceedPreference.Ask)

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == createAsgLog + canaryScaleUpLog + canaryJudgeLog +
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable."

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 1)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> ''
        then: 1 * mockActivities.askIfDeploymentShouldProceed('gob@bluth.com', 'the_seaward-v003',
                "ASG will now be evaluated for up to 60 minutes during the canary capacity judgment period.") >> false
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable.")
    }

    def 'should continue deployment for canary decision to proceed'() {
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', doCanary: true,
                canaryCapacity: 1, canaryStartUpTimeoutMinutes: 30, canaryJudgmentPeriodMinutes: 60,
                desiredCapacityStartUpTimeoutMinutes: 40,
                scaleUp: ProceedPreference.Ask, disablePreviousAsg: ProceedPreference.No)

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == createAsgLog + canaryScaleUpLog + canaryJudgeLog +
                fullCapacityScaleUpLog + [
                "ASG 'the_seaward-v002' was not disabled. The new ASG is not taking full traffic.",
                "Deployment was successful."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 1)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 1) >> ''
        then: 1 * mockActivities.askIfDeploymentShouldProceed('gob@bluth.com', 'the_seaward-v003',
                "ASG will now be evaluated for up to 60 minutes during the canary capacity judgment period.") >> true
        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Deployment succeeded for ASG 'the_seaward-v003'.", "Deployment was successful.")
    }

    def 'should rollback deployment for full capacity decision to not proceed'() {
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', doCanary: false,
                scaleUp: ProceedPreference.Yes, disablePreviousAsg: ProceedPreference.Ask,
                deletePreviousAsg: ProceedPreference.Yes, desiredCapacityStartUpTimeoutMinutes: 40,
                desiredCapacityJudgmentPeriodMinutes: 120)

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == createAsgLog + fullCapacityScaleUpLog + [
                "ASG will now be evaluated for up to 120 minutes during the full capacity judgment period.",
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.askIfDeploymentShouldProceed('gob@bluth.com', 'the_seaward-v003',
                "ASG will now be evaluated for up to 120 minutes during the full capacity judgment period.") >> false
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable.")
    }

    def 'should continue with full capacity decision to proceed'() {
        workflowOperations.addFiredTimerNames(['waitAfterEurekaChange'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', doCanary: false,
                scaleUp: ProceedPreference.Yes, disablePreviousAsg: ProceedPreference.Ask,
                deletePreviousAsg: ProceedPreference.Yes, desiredCapacityStartUpTimeoutMinutes: 40,
                desiredCapacityJudgmentPeriodMinutes: 120)

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == createAsgLog + fullCapacityScaleUpLog + [
                "ASG will now be evaluated for up to 120 minutes during the full capacity judgment period.",
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                "Deleting ASG 'the_seaward-v002'.",
                "Deployment was successful."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.askIfDeploymentShouldProceed('gob@bluth.com', 'the_seaward-v003',
                "ASG will now be evaluated for up to 120 minutes during the full capacity judgment period.") >> true
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.deleteAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Deployment succeeded for ASG 'the_seaward-v003'.", "Deployment was successful.")
    }

    def 'should not delete previous ASG if specified not to'() {
        workflowOperations.addFiredTimerNames(['waitAfterEurekaChange'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', doCanary: false,
                scaleUp: ProceedPreference.Yes, disablePreviousAsg: ProceedPreference.Yes,
                deletePreviousAsg: ProceedPreference.No, desiredCapacityStartUpTimeoutMinutes: 40, )

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == createAsgLog + fullCapacityScaleUpLog + [
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                "Deployment was successful."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Deployment succeeded for ASG 'the_seaward-v003'.", "Deployment was successful.")
    }

    def 'should rollback deployment for full traffic decision to not proceed'() {
        workflowOperations.addFiredTimerNames(['waitAfterEurekaChange'])
        DeploymentWorkflowOptions deploymentOptions = new DeploymentWorkflowOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', doCanary: false,
                scaleUp: ProceedPreference.Yes, disablePreviousAsg: ProceedPreference.Yes,
                deletePreviousAsg: ProceedPreference.Ask, desiredCapacityStartUpTimeoutMinutes: 40,
                fullTrafficJudgmentPeriodMinutes: 240)

        when:
        workflowExecuter.deploy(userContext, deploymentOptions, lcInputs, asgInputs)

        then:
        workflowOperations.logHistory == createAsgLog + fullCapacityScaleUpLog + [
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                "ASG will now be evaluated for up to 240 minutes during the full traffic judgment period.",
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 1 * mockActivities.reasonAsgIsNotOperational(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.stopAsgAnalysis("ASG analysis for 'the_seaward' cluster.")
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.askIfDeploymentShouldProceed('gob@bluth.com', 'the_seaward-v003',
                "ASG will now be evaluated for up to 240 minutes during the full traffic judgment period.") >> false
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
        then: 1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward',
                "Deployment failed for ASG 'the_seaward-v003'.",
                "Deployment was rolled back. Judge decided ASG 'the_seaward-v003' was not viable.")
    }
}
