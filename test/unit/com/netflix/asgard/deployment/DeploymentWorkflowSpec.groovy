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
import com.netflix.asgard.flow.LocalWorkflow
import com.netflix.asgard.model.InstancePriceType
import com.netflix.asgard.push.PushException
import spock.lang.Specification

class DeploymentWorkflowSpec extends Specification {

    DeploymentActivities mockActivities = Mock(DeploymentActivities)
    DeploymentWorkflow workflow = new DeploymentWorkflowImpl(workflow: LocalWorkflow.of(mockActivities))

    UserContext userContext = UserContext.auto(Region.US_WEST_2)

    AsgDeploymentNames asgDeploymentNames = new AsgDeploymentNames(
        previousAsgName: 'the_seaward-v002', previousLaunchConfigName: 'the_seaward-v002-20130626140848',
        previousVpcZoneIdentifier: 'subnet-baadd00d,subnet-defaced1',
        nextAsgName: 'the_seaward-v003', nextLaunchConfigName: 'the_seaward-v003-20130626140848',
        nextVpcZoneIdentifier: 'subnet-baadd00d,subnet-defaced1'
    )

    LaunchConfigurationOptions lcOverrides = new LaunchConfigurationOptions(securityGroups: ['sg-defec8ed'])

    AutoScalingGroupOptions asgOverrides = new AutoScalingGroupOptions(availabilityZones: ['us-west2a', 'us-west2b'],
        minSize: 2, desiredCapacity: 3, maxSize: 4)

    List<String> logForCreatingAsg = [
            "Starting deployment for Cluster 'the_seaward'.",
            "Creating Launch Configuration 'the_seaward-v003-20130626140848'.",
            "Creating Auto Scaling Group 'the_seaward-v003' initially with 0 instances.",
            "Copying Scaling Policies.",
            "Copying Scheduled Actions.",
            "New ASG 'the_seaward-v003' was successfully created."
    ]

    List<String> logForCanaryScaleUp = [
        "Canary testing will now be performed.",
        "Scaling 'the_seaward-v003' to 1 instance.",
        "Waiting up to 30 minutes for 1 instance.",
    ]

    List<String> logForCanaryCompletion = [
            "ASG 'the_seaward-v003' is at canary capacity.",
            "ASG health will be evaluated after 60 minutes.",
            "Canary capacity assessment period for ASG 'the_seaward-v003' has completed."
    ]
    List<String> logForFullCapacityScaleUp = [
        "Scaling to full capacity.",
        "Scaling 'the_seaward-v003' to 3 instances.",
        "Waiting up to 40 minutes for 3 instances.",
    ]

    private createAsgInteractions() {
        with(mockActivities) {
            1 * getAsgDeploymentNames(userContext, 'the_seaward',
                    'internal', ['us-west2a', 'us-west2b']) >> asgDeploymentNames
            1 * createLaunchConfigForNextAsg(userContext, asgDeploymentNames, lcOverrides,
                    InstancePriceType.ON_DEMAND) >> 'the_seaward-v003-20130626140848'
            1 * createNextAsgForCluster(userContext, asgDeploymentNames, asgOverrides, false, false) >>
                    'the_seaward-v003'
            1 * copyScalingPolicies(userContext, asgDeploymentNames) >> 0
            1 * copyScheduledActions(userContext, asgDeploymentNames) >> 0
        }
    }

    def 'should execute full deployment'() {
        DeploymentOptions deploymentOptions = new DeploymentOptions(clusterName: 'the_seaward', delayDuration: 10,
                subnetPurpose: 'internal', initialTrafficPrevented: false, azRebalanceSuspended: false,
                instancePriceType: InstancePriceType.ON_DEMAND, doCanary: true,
                canaryCapacity: 1, canaryStartUpTimeout: 30, canaryAssessmentDuration: 60,
                desiredCapacityStartUpTimeout: 40, desiredCapacityAssessmentDuration: 120,
                fullTrafficAssessmentDuration: 240, scaleUp: ProceedPreference.Yes,
                disablePreviousAsg: ProceedPreference.Yes, deletePreviousAsg: ProceedPreference.Yes)

        when:
        workflow.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)

        then:
        workflow.logHistory == ['Waiting 10 minutes before starting deployment.'] +
                logForCreatingAsg + logForCanaryScaleUp + logForCanaryCompletion + logForFullCapacityScaleUp + [
                "ASG 'the_seaward-v003' is at full capacity.",
                "ASG health will be evaluated after 120 minutes.",
                "Full capacity assessment period for ASG 'the_seaward-v003' has completed.",
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                "ASG health will be evaluated after 240 minutes.",
                "Full traffic assessment period for ASG 'the_seaward-v003' has completed.",
                "Deleting ASG 'the_seaward-v002'.",
        ]
        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 1)
        then: 2 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 1) >> ''
        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 2 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.deleteAsg(userContext, 'the_seaward-v002')
    }

    def 'should execute deployment without canary or delay'() {
        DeploymentOptions deploymentOptions = new DeploymentOptions(clusterName: 'the_seaward',
                subnetPurpose: 'internal', initialTrafficPrevented: false, azRebalanceSuspended: false,
                instancePriceType: InstancePriceType.ON_DEMAND, doCanary: false,
                desiredCapacityStartUpTimeout: 40, desiredCapacityAssessmentDuration: 120,
                fullTrafficAssessmentDuration: 240, scaleUp: ProceedPreference.Yes,
                disablePreviousAsg: ProceedPreference.Yes, deletePreviousAsg: ProceedPreference.Yes)

        when:
        workflow.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)

        then:
        workflow.logHistory == logForCreatingAsg + logForFullCapacityScaleUp + [
                "ASG 'the_seaward-v003' is at full capacity.",
                "ASG health will be evaluated after 120 minutes.",
                "Full capacity assessment period for ASG 'the_seaward-v003' has completed.",
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                "ASG health will be evaluated after 240 minutes.",
                "Full traffic assessment period for ASG 'the_seaward-v003' has completed.",
                "Deleting ASG 'the_seaward-v002'.",
        ]
        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 2 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.deleteAsg(userContext, 'the_seaward-v002')
    }

    def 'should execute canary without scaling up'() {
        DeploymentOptions deploymentOptions = new DeploymentOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                subnetPurpose: 'internal', initialTrafficPrevented: false, azRebalanceSuspended: false,
                instancePriceType: InstancePriceType.ON_DEMAND, doCanary: true,
                canaryCapacity: 1, canaryStartUpTimeout: 30, canaryAssessmentDuration: 60,
                scaleUp: ProceedPreference.No)

        when:
        workflow.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)

        then:
        workflow.logHistory == logForCreatingAsg + logForCanaryScaleUp + logForCanaryCompletion + [
                "Rolling back to 'the_seaward-v002'.",
                "ASG 'the_seaward-v002' was not disabled. Full traffic health check will not take place.",
        ]
        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 1)
        then: 2 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 1) >> ''
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
    }

    def 'should display error and rollback deployment if there is an error checking health'() {
        DeploymentOptions deploymentOptions = new DeploymentOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                subnetPurpose: 'internal', initialTrafficPrevented: false, azRebalanceSuspended: false,
                instancePriceType: InstancePriceType.ON_DEMAND, doCanary: true,
                canaryCapacity: 1, canaryStartUpTimeout: 30, canaryAssessmentDuration: 60,
                scaleUp: ProceedPreference.No)

        when:
        workflow.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)

        then:
        workflow.logHistory == logForCreatingAsg + [
                "Canary testing will now be performed.",
                "Scaling 'the_seaward-v003' to 1 instance.",
                "Something really went wrong!",
                "Waiting up to 30 minutes for 1 instance.",
                "ASG 'the_seaward-v003' was not at capacity after 30 minutes.",
                "Rolling back to 'the_seaward-v002'.",
                "ASG 'the_seaward-v002' was not disabled. Full traffic health check will not take place."
        ]
        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 1)
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 1) >> {
            throw new IllegalStateException('Something really went wrong!')
        }
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
    }

    def 'should retry health check if not ready yet.'() {
        DeploymentOptions deploymentOptions = new DeploymentOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                subnetPurpose: 'internal', initialTrafficPrevented: false, azRebalanceSuspended: false,
                instancePriceType: InstancePriceType.ON_DEMAND, doCanary: false,
                scaleUp: ProceedPreference.Yes, disablePreviousAsg: ProceedPreference.No,
                desiredCapacityStartUpTimeout: 40)

        when:
        workflow.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)

        then:
        workflow.logHistory == logForCreatingAsg + logForFullCapacityScaleUp + [
                "ASG 'the_seaward-v003' is at full capacity.",
                "Full capacity assessment period for ASG 'the_seaward-v003' has completed.",
                "ASG 'the_seaward-v002' was not disabled. Full traffic health check will not take place.",
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> {
                throw new PushException('Not healthy Yet')
            }
        then: 2 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
    }

    def 'should rollback and notify if health check fails at the end of an assessment period set to auto proceed.'() {
        DeploymentOptions deploymentOptions = new DeploymentOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                subnetPurpose: 'internal', initialTrafficPrevented: false, azRebalanceSuspended: false,
                instancePriceType: InstancePriceType.ON_DEMAND, doCanary: true,
                canaryCapacity: 1, canaryStartUpTimeout: 30, canaryAssessmentDuration: 60,
                scaleUp: ProceedPreference.Yes)

        when:
        workflow.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)

        then:
        workflow.logHistory == logForCreatingAsg + logForCanaryScaleUp + [
                "ASG 'the_seaward-v003' is at canary capacity.",
                "ASG health will be evaluated after 60 minutes.",
                "Woah, I thought it was healthy!?",
                "Canary capacity assessment period for ASG 'the_seaward-v003' has completed.",
                "Rolling back to 'the_seaward-v002'.",
                "ASG 'the_seaward-v002' was not disabled. Full traffic health check will not take place."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 1)
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 1) >> {
            ''
        }
        then: 5 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 1) >> {
            throw new PushException('Woah, I thought it was healthy!?')
        }
        then: 1 * mockActivities.sendNotification('gob@bluth.com', 'the_seaward-v003',
                "Asgard deployment for 'the_seaward-v003' will not proceed due to error.",
                'Woah, I thought it was healthy!?')
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
    }

    def 'should rollback for canary start up time out'() {
        DeploymentOptions deploymentOptions = new DeploymentOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                subnetPurpose: 'internal', initialTrafficPrevented: false, azRebalanceSuspended: false,
                instancePriceType: InstancePriceType.ON_DEMAND, doCanary: true,
                canaryCapacity: 1, canaryStartUpTimeout: 30, canaryAssessmentDuration: 60,
                scaleUp: ProceedPreference.No)

        when:
        workflow.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)

        then:
        workflow.logHistory == logForCreatingAsg + logForCanaryScaleUp + [
                "ASG 'the_seaward-v003' was not at capacity after 30 minutes.",
                "Rolling back to 'the_seaward-v002'.",
                "ASG 'the_seaward-v002' was not disabled. Full traffic health check will not take place.",
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 1)
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 1) >> null
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
    }

    def 'should rollback for desired capacity start up time out'() {
        DeploymentOptions deploymentOptions = new DeploymentOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com', desiredCapacityStartUpTimeout: 40,
                subnetPurpose: 'internal', initialTrafficPrevented: false, azRebalanceSuspended: false,
                instancePriceType: InstancePriceType.ON_DEMAND, scaleUp: ProceedPreference.Yes)

        when:
        workflow.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)

        then:
        workflow.logHistory == logForCreatingAsg + logForFullCapacityScaleUp + [
                "ASG 'the_seaward-v003' was not at capacity after 40 minutes.",
                "Rolling back to 'the_seaward-v002'.",
                "ASG 'the_seaward-v002' was not disabled. Full traffic health check will not take place.",
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> null
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
    }

    def 'should rollback for canary decision to not proceed'() {
        DeploymentOptions deploymentOptions = new DeploymentOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                subnetPurpose: 'internal', initialTrafficPrevented: false, azRebalanceSuspended: false,
                instancePriceType: InstancePriceType.ON_DEMAND, doCanary: true,
                canaryCapacity: 1, canaryStartUpTimeout: 30, canaryAssessmentDuration: 60,
                scaleUp: ProceedPreference.Ask)

        when:
        workflow.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)

        then:
        workflow.logHistory == logForCreatingAsg + logForCanaryScaleUp + logForCanaryCompletion + [
                "Awaiting health decision for 'the_seaward-v003'.",
                "Rolling back to 'the_seaward-v002'.",
                "ASG 'the_seaward-v002' was not disabled. Full traffic health check will not take place.",
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 1)
        then: 2 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 1) >> ''
        then: 1 * mockActivities.askIfDeploymentShouldProceed('gob@bluth.com', 'the_seaward-v003',
                "Canary capacity assessment period for ASG 'the_seaward-v003' has completed.", '') >> false
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
    }

    def 'should continue deployment for canary decision to proceed'() {
        DeploymentOptions deploymentOptions = new DeploymentOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                subnetPurpose: 'internal', initialTrafficPrevented: false, azRebalanceSuspended: false,
                instancePriceType: InstancePriceType.ON_DEMAND, doCanary: true,
                canaryCapacity: 1, canaryStartUpTimeout: 30, canaryAssessmentDuration: 60,
                desiredCapacityStartUpTimeout: 40,
                scaleUp: ProceedPreference.Ask, disablePreviousAsg: ProceedPreference.No)

        when:
        workflow.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)

        then:
        workflow.logHistory == logForCreatingAsg + logForCanaryScaleUp + logForCanaryCompletion +
                ["Awaiting health decision for 'the_seaward-v003'."] + logForFullCapacityScaleUp + [
                "ASG 'the_seaward-v003' is at full capacity.",
                "Full capacity assessment period for ASG 'the_seaward-v003' has completed.",
                "ASG 'the_seaward-v002' was not disabled. Full traffic health check will not take place."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 1, 1, 1)
        then: 2 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 1) >> ''
        then: 1 * mockActivities.askIfDeploymentShouldProceed('gob@bluth.com', 'the_seaward-v003',
                    "Canary capacity assessment period for ASG 'the_seaward-v003' has completed.", '') >> true
        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 2 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
    }

    def 'should rollback deployment for full capacity decision to not proceed'() {
        DeploymentOptions deploymentOptions = new DeploymentOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                subnetPurpose: 'internal', initialTrafficPrevented: false, azRebalanceSuspended: false,
                instancePriceType: InstancePriceType.ON_DEMAND, doCanary: false,
                scaleUp: ProceedPreference.Yes, disablePreviousAsg: ProceedPreference.Ask,
                deletePreviousAsg: ProceedPreference.Yes, desiredCapacityStartUpTimeout: 40, )

        when:
        workflow.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)

        then:
        workflow.logHistory == logForCreatingAsg + logForFullCapacityScaleUp + [
                "ASG 'the_seaward-v003' is at full capacity.",
                "Full capacity assessment period for ASG 'the_seaward-v003' has completed.",
                "Awaiting health decision for 'the_seaward-v003'.",
                "Rolling back to 'the_seaward-v002'.",
                "ASG 'the_seaward-v002' was not disabled. Full traffic health check will not take place."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.askIfDeploymentShouldProceed('gob@bluth.com', 'the_seaward-v003',
                "Full capacity assessment period for ASG 'the_seaward-v003' has completed.", '') >> false
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
    }

    def 'should continue with full capacity decision to proceed'() {
        DeploymentOptions deploymentOptions = new DeploymentOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                subnetPurpose: 'internal', initialTrafficPrevented: false, azRebalanceSuspended: false,
                instancePriceType: InstancePriceType.ON_DEMAND, doCanary: false,
                scaleUp: ProceedPreference.Yes, disablePreviousAsg: ProceedPreference.Ask,
                deletePreviousAsg: ProceedPreference.Yes, desiredCapacityStartUpTimeout: 40, )

        when:
        workflow.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)

        then:
        workflow.logHistory == logForCreatingAsg + logForFullCapacityScaleUp + [
                "ASG 'the_seaward-v003' is at full capacity.",
                "Full capacity assessment period for ASG 'the_seaward-v003' has completed.",
                "Awaiting health decision for 'the_seaward-v003'.",
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                "Full traffic assessment period for ASG 'the_seaward-v003' has completed.",
                "Deleting ASG 'the_seaward-v002'.",
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.askIfDeploymentShouldProceed('gob@bluth.com', 'the_seaward-v003',
                "Full capacity assessment period for ASG 'the_seaward-v003' has completed.", '') >> true
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.deleteAsg(userContext, 'the_seaward-v002')
    }

    def 'should not delete previous ASG if specified not to'() {
        DeploymentOptions deploymentOptions = new DeploymentOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                subnetPurpose: 'internal', initialTrafficPrevented: false, azRebalanceSuspended: false,
                instancePriceType: InstancePriceType.ON_DEMAND, doCanary: false,
                scaleUp: ProceedPreference.Yes, disablePreviousAsg: ProceedPreference.Yes,
                deletePreviousAsg: ProceedPreference.No, desiredCapacityStartUpTimeout: 40, )

        when:
        workflow.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)

        then:
        workflow.logHistory == logForCreatingAsg + logForFullCapacityScaleUp + [
                "ASG 'the_seaward-v003' is at full capacity.",
                "Full capacity assessment period for ASG 'the_seaward-v003' has completed.",
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                "Full traffic assessment period for ASG 'the_seaward-v003' has completed.",
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
    }

    def 'should rollback deployment for full traffic decision to not proceed'() {
        DeploymentOptions deploymentOptions = new DeploymentOptions(clusterName: 'the_seaward',
                notificationDestination: 'gob@bluth.com',
                subnetPurpose: 'internal', initialTrafficPrevented: false, azRebalanceSuspended: false,
                instancePriceType: InstancePriceType.ON_DEMAND, doCanary: false,
                scaleUp: ProceedPreference.Yes, disablePreviousAsg: ProceedPreference.Yes,
                deletePreviousAsg: ProceedPreference.Ask, desiredCapacityStartUpTimeout: 40, )

        when:
        workflow.deploy(userContext, deploymentOptions, lcOverrides, asgOverrides)

        then:
        workflow.logHistory == logForCreatingAsg + logForFullCapacityScaleUp + [
                "ASG 'the_seaward-v003' is at full capacity.",
                "Full capacity assessment period for ASG 'the_seaward-v003' has completed.",
                "Disabling ASG 'the_seaward-v002'.",
                "Waiting 90 seconds for clients to stop using instances.",
                "Full traffic assessment period for ASG 'the_seaward-v003' has completed.",
                "Awaiting health decision for 'the_seaward-v003'.",
                "Rolling back to 'the_seaward-v002'."
        ]

        interaction {
            createAsgInteractions()
        }
        0 * _

        then: 1 * mockActivities.resizeAsg(userContext, 'the_seaward-v003', 2, 3, 4)
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.reasonAsgIsUnhealthy(userContext, 'the_seaward-v003', 3) >> ''
        then: 1 * mockActivities.askIfDeploymentShouldProceed('gob@bluth.com', 'the_seaward-v003',
                "Full traffic assessment period for ASG 'the_seaward-v003' has completed.", '') >> false
        then: 1 * mockActivities.enableAsg(userContext, 'the_seaward-v002')
        then: 1 * mockActivities.disableAsg(userContext, 'the_seaward-v003')
    }

}
