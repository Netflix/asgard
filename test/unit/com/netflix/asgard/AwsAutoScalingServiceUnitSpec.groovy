/*
 * Copyright 2012 Netflix, Inc.
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

import com.amazonaws.services.autoscaling.AmazonAutoScaling
import com.amazonaws.services.autoscaling.model.Alarm
import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult
import com.amazonaws.services.autoscaling.model.DescribePoliciesResult
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest
import com.amazonaws.services.autoscaling.model.ScalingPolicy
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest
import com.amazonaws.services.autoscaling.model.SuspendedProcess
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest
import com.amazonaws.services.cloudwatch.model.Dimension
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.AlarmData
import com.netflix.asgard.model.AlarmData.ComparisonOperator
import com.netflix.asgard.model.AlarmData.Statistic
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.AutoScalingProcessType
import com.netflix.asgard.model.ScalingPolicyData
import com.netflix.asgard.model.StackItem
import spock.lang.Specification

@SuppressWarnings(["GroovyAssignabilityCheck"])
class AwsAutoScalingServiceUnitSpec extends Specification {

    AwsAutoScalingService awsAutoScalingService

    def 'should update ASG with proper AWS requests'() {
        final mockAmazonAutoScalingClient = Mock(AmazonAutoScaling)
        mockAmazonAutoScalingClient.describeAutoScalingGroups(_ as DescribeAutoScalingGroupsRequest) >> {
            List<SuspendedProcess> suspendedProcesses = AutoScalingProcessType.with { [AZRebalance, AddToLoadBalancer] }
                    .collect { new SuspendedProcess().withProcessName(it.name()) }
            new DescribeAutoScalingGroupsResult().withAutoScalingGroups(new AutoScalingGroup()
                    .withAutoScalingGroupName('hiyaworld-example-v042').withSuspendedProcesses(suspendedProcesses)
            )
        }
        mockAmazonAutoScalingClient.describePolicies(_) >> {[]}

        awsAutoScalingService = Mocks.newAwsAutoScalingService()
        awsAutoScalingService.awsClient = new MultiRegionAwsClient({mockAmazonAutoScalingClient})

        //noinspection GroovyAccessibility
        when:
        awsAutoScalingService.updateAutoScalingGroup(Mocks.userContext(),
                new AutoScalingGroupData('hiyaworld-example-v042', null, 31, 153, [], 'EC2', 17, [], [],
                42, null, ['us-feast'], 256, [], 'newlaunchConfiguration', [], [:], [], [:], []),
                AutoScalingProcessType.with { [Launch, AZRebalance] },
                AutoScalingProcessType.with { [AddToLoadBalancer] })

        then:

        // Terminate should not be affected because a new state was not specified
        // AZRebalance should not be suspended because it is already suspended

        // Launch should be suspended and nothing else
        1 * mockAmazonAutoScalingClient.suspendProcesses({ SuspendProcessesRequest request ->
            [AutoScalingProcessType.Launch.name()] == request.scalingProcesses
        })
        0 * mockAmazonAutoScalingClient.suspendProcesses(_)

        // AddToLoadBalancer should be resumed and nothing else
        1 * mockAmazonAutoScalingClient.resumeProcesses({ ResumeProcessesRequest request ->
            [AutoScalingProcessType.AddToLoadBalancer.name()] == request.scalingProcesses
        })
        0 * mockAmazonAutoScalingClient.resumeProcesses(_)

        // everything else is updated with the appropriate request
        1 * mockAmazonAutoScalingClient.updateAutoScalingGroup({ UpdateAutoScalingGroupRequest request ->
            request == new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName('hiyaworld-example-v042')
                .withLaunchConfigurationName('newlaunchConfiguration')
                .withMinSize(31)
                .withMaxSize(153)
                .withDesiredCapacity(42)
                .withDefaultCooldown(256)
                .withAvailabilityZones("us-feast")
                .withHealthCheckType('EC2')
                .withHealthCheckGracePeriod(17)
                .withTerminationPolicies([])
        })
        0 * mockAmazonAutoScalingClient.updateAutoScalingGroup(_)

    }

    def 'should create launch config and ASG'() {
        final mockAmazonAutoScalingClient = Mock(AmazonAutoScaling)
        mockAmazonAutoScalingClient.describeLaunchConfigurations(_) >> {
            new DescribeLaunchConfigurationsResult()
        }
        mockAmazonAutoScalingClient.describeAutoScalingGroups(_) >> {
            new DescribeAutoScalingGroupsResult()
        }
        awsAutoScalingService = Mocks.newAwsAutoScalingService()
        awsAutoScalingService.awsClient = new MultiRegionAwsClient({mockAmazonAutoScalingClient})

        final AutoScalingGroup groupTemplate = new AutoScalingGroup().withAutoScalingGroupName('helloworld-example').
                withAvailabilityZones([]).withLoadBalancerNames([]).
                withMaxSize(0).withMinSize(0).withDefaultCooldown(0)
        final LaunchConfiguration launchConfigTemplate = new LaunchConfiguration().withImageId('ami-deadbeef').
                withInstanceType('m1.small').withKeyName('keyName').withSecurityGroups([]).withUserData('')

        when:
        final CreateAutoScalingGroupResult result = awsAutoScalingService.createLaunchConfigAndAutoScalingGroup(
                Mocks.userContext(), groupTemplate, launchConfigTemplate, [AutoScalingProcessType.Terminate])

        then:
        null == result.autoScalingCreateException
        null == result.launchConfigCreateException
        null == result.launchConfigDeleteException
        'helloworld-example' == result.autoScalingGroupName
        result.launchConfigName =~ /helloworld-example-20[0-9]{12}/
        !result.launchConfigDeleted
        result.launchConfigCreated
        result.autoScalingGroupCreated
        result.toString() =~
                /Launch Config 'helloworld-example-20[0-9]{12}' has been created. Auto Scaling Group 'helloworld-example' has been created. /

        1 * mockAmazonAutoScalingClient.suspendProcesses({ SuspendProcessesRequest request ->
            [AutoScalingProcessType.Terminate.name()] == request.scalingProcesses &&
            'helloworld-example' == request.autoScalingGroupName
        })
        0 * mockAmazonAutoScalingClient.suspendProcesses(_)
    }

    def 'should get scaling policies'() {
        awsAutoScalingService = new AwsAutoScalingService()
        final mockAmazonAutoScalingClient = Mock(AmazonAutoScaling)
        awsAutoScalingService.awsClient = new MultiRegionAwsClient({mockAmazonAutoScalingClient})
        final AwsCloudWatchService mockAwsCloudWatchService = Mock(AwsCloudWatchService)
        awsAutoScalingService.awsCloudWatchService = mockAwsCloudWatchService

        mockAmazonAutoScalingClient.describePolicies(_) >> {
            new DescribePoliciesResult(scalingPolicies: [
                new ScalingPolicy(policyName: 'scale-up-hw_v046-25-300', autoScalingGroupName: 'hw_v046',
                        alarms: [new Alarm(alarmName: 'alarm1')], adjustmentType: 'PercentChangeInCapacity',
                        scalingAdjustment: 25),
                new ScalingPolicy(policyName: 'scale-down-hw_v046-15-300', autoScalingGroupName: 'hw_v046',
                        alarms: [new Alarm(alarmName: 'alarm2'), new Alarm(alarmName: 'alarm3')],
                        adjustmentType: 'PercentChangeInCapacity', scalingAdjustment: -15),
            ])
        }

        when:
        final Set<ScalingPolicyData> actualScalingPolicies = awsAutoScalingService.
                getScalingPolicyDatas(Mocks.userContext(), 'hw_v046') as Set

        then:
        actualScalingPolicies == [
            new ScalingPolicyData(policyName: 'scale-up-hw_v046-25-300', autoScalingGroupName: 'hw_v046',
                    adjustment: 25, alarms: [
                            new AlarmData(
                                    alarmName: 'alarm1',
                                    comparisonOperator: ComparisonOperator.GreaterThanThreshold,
                                    metricName: 'CPUUtilization',
                                    namespace: 'AWS/EC2',
                                    statistic: Statistic.Average,
                                    period: 300,
                                    evaluationPeriods: 1,
                                    threshold: 78,
                                    actionArns: [],
                                    autoScalingGroupName: 'hw_v046'
                            )
                    ]),
            new ScalingPolicyData(policyName: 'scale-down-hw_v046-15-300', autoScalingGroupName: 'hw_v046',
                    adjustment: -15, alarms: [
                            new AlarmData(
                                    alarmName: 'alarm2',
                                    comparisonOperator: ComparisonOperator.LessThanThreshold,
                                    metricName: 'CPUUtilization',
                                    namespace: 'AWS/EC2',
                                    statistic: Statistic.Average,
                                    period: 300,
                                    evaluationPeriods: 1,
                                    threshold: 22,
                                    actionArns: [],
                                    autoScalingGroupName: 'hw_v046'
                            ),
                            new AlarmData(
                                    alarmName: 'alarm3',
                                    comparisonOperator: ComparisonOperator.LessThanThreshold,
                                    metricName: 'CPUUtilization',
                                    namespace: 'AWS/EC2',
                                    statistic: Statistic.Average,
                                    period: 300,
                                    evaluationPeriods: 1,
                                    threshold: 23,
                                    actionArns: [],
                                    autoScalingGroupName: 'hw_v046'
                            )
                    ]),
        ] as Set

        1 * mockAwsCloudWatchService.getAlarms(_, ['alarm1','alarm2', 'alarm3']) >> {[
                new MetricAlarm(alarmName: 'alarm1', threshold: 78, comparisonOperator: 'GreaterThanThreshold',
                    statistic: 'Average', dimensions: [new Dimension(name: AlarmData.DIMENSION_NAME_FOR_ASG,
                            value: 'hw_v046')]),
                new MetricAlarm(alarmName: 'alarm2', threshold: 22, comparisonOperator: 'LessThanThreshold',
                    statistic: 'Average', dimensions: [new Dimension(name: AlarmData.DIMENSION_NAME_FOR_ASG,
                            value: 'hw_v046')]),
                new MetricAlarm(alarmName: 'alarm3', threshold: 23, comparisonOperator: 'LessThanThreshold',
                    statistic: 'Average', dimensions: [new Dimension(name: AlarmData.DIMENSION_NAME_FOR_ASG,
                            value: 'hw_v046')])
        ]}
        0 * mockAwsCloudWatchService.getAlarms(_, _)
    }

}
