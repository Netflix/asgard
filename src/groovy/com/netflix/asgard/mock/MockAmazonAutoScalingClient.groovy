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
package com.netflix.asgard.mock

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.ClientConfiguration
import com.amazonaws.ResponseMetadata
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest
import com.amazonaws.services.autoscaling.model.DeletePolicyRequest
import com.amazonaws.services.autoscaling.model.DeleteScheduledActionRequest
import com.amazonaws.services.autoscaling.model.DescribeAdjustmentTypesRequest
import com.amazonaws.services.autoscaling.model.DescribeAdjustmentTypesResult
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesRequest
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesResult
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult
import com.amazonaws.services.autoscaling.model.DescribeMetricCollectionTypesRequest
import com.amazonaws.services.autoscaling.model.DescribeMetricCollectionTypesResult
import com.amazonaws.services.autoscaling.model.DescribePoliciesRequest
import com.amazonaws.services.autoscaling.model.DescribePoliciesResult
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult
import com.amazonaws.services.autoscaling.model.DescribeScalingProcessTypesRequest
import com.amazonaws.services.autoscaling.model.DescribeScalingProcessTypesResult
import com.amazonaws.services.autoscaling.model.DescribeScheduledActionsRequest
import com.amazonaws.services.autoscaling.model.DescribeScheduledActionsResult
import com.amazonaws.services.autoscaling.model.DescribeTerminationPolicyTypesResult
import com.amazonaws.services.autoscaling.model.DisableMetricsCollectionRequest
import com.amazonaws.services.autoscaling.model.EnableMetricsCollectionRequest
import com.amazonaws.services.autoscaling.model.ExecutePolicyRequest
import com.amazonaws.services.autoscaling.model.Instance
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest
import com.amazonaws.services.autoscaling.model.PutScalingPolicyResult
import com.amazonaws.services.autoscaling.model.PutScheduledUpdateGroupActionRequest
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest
import com.amazonaws.services.autoscaling.model.SetInstanceHealthRequest
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest
import com.amazonaws.services.autoscaling.model.SuspendedProcess
import com.amazonaws.services.autoscaling.model.TerminateInstanceInAutoScalingGroupRequest
import com.amazonaws.services.autoscaling.model.TerminateInstanceInAutoScalingGroupResult
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest
import org.codehaus.groovy.grails.web.json.JSONArray
import org.joda.time.format.ISODateTimeFormat

class MockAmazonAutoScalingClient extends AmazonAutoScalingClient {

    private Collection<AutoScalingGroup> mockAsgs
    private Collection<LaunchConfiguration> mockLaunchConfigs

    private List<AutoScalingGroup> loadMockAsgs() {

        JSONArray jsonArray = Mocks.parseJsonString(MockAutoScalingGroups.DATA)
        return jsonArray.collect {
            new AutoScalingGroup().withAutoScalingGroupName(it.autoScalingGroupName).
                    withDesiredCapacity(it.desiredCapacity).withDefaultCooldown(it.defaultCooldown).
                    withMinSize(it.minSize).withMaxSize(it.maxSize).
                    withHealthCheckGracePeriod(it.healthCheckGracePeriod).
                    withAvailabilityZones(it.availabilityZones as List).withHealthCheckType(it.healthCheckType).
                    withLaunchConfigurationName(it.launchConfigurationName).
                    withCreatedTime(ISODateTimeFormat.dateTimeParser().parseDateTime(it.createdTime).toDate()).
                    withLoadBalancerNames(it.loadBalancerNames as List).
                    withInstances(it.instances.collect { def inst ->
                        new Instance().withAvailabilityZone(inst.availabilityZone).withHealthStatus(inst.healthStatus).
                                withInstanceId(inst.instanceId).
                                withLaunchConfigurationName(inst.launchConfigurationName).
                                withLifecycleState(inst.lifecycleState as String)
                    }).
                    withSuspendedProcesses(it.suspendedProcesses.collect { def suspendedProcess ->
                        new SuspendedProcess().withProcessName(suspendedProcess.processName).
                                withSuspensionReason(suspendedProcess.suspensionReason)
                    })
        }

    }

    private List<LaunchConfiguration> loadMockLaunchConfigs() {
        JSONArray jsonArray = MockFileUtils.parseJsonFile('mockLaunchConfigs.json') as JSONArray
        jsonArray.collect {
            new LaunchConfiguration().withLaunchConfigurationName(it.launchConfigurationName).
                    withImageId(it.imageId).withKeyName(it.keyName).
                    withSecurityGroups(it.securityGroups as List).
                    withUserData(it.userData).withInstanceType(it.instanceType).
                    withCreatedTime(ISODateTimeFormat.dateTimeParser().parseDateTime(it.createdTime).toDate())
        }
    }

    MockAmazonAutoScalingClient(BasicAWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        super(awsCredentials as BasicAWSCredentials, clientConfiguration)
        mockAsgs = loadMockAsgs()
        mockLaunchConfigs = loadMockLaunchConfigs()
    }

    void setEndpoint(String endpoint) { }

    void putScheduledUpdateGroupAction(PutScheduledUpdateGroupActionRequest putScheduledUpdateGroupActionRequest) { }

    void setDesiredCapacity(SetDesiredCapacityRequest setDesiredCapacityRequest) { }

    void deletePolicy(DeletePolicyRequest deletePolicyRequest) { }

    void deleteScheduledAction(DeleteScheduledActionRequest deleteScheduledActionRequest) { }

    DescribeLaunchConfigurationsResult describeLaunchConfigurations(
            DescribeLaunchConfigurationsRequest describeLaunchConfigurationsRequest) {

        List<String> names = describeLaunchConfigurationsRequest.launchConfigurationNames
        if (names) {
            return new DescribeLaunchConfigurationsResult().withLaunchConfigurations(
                    mockLaunchConfigs.findAll { it.launchConfigurationName in names })
        }
        new DescribeLaunchConfigurationsResult().withLaunchConfigurations(mockLaunchConfigs)
    }

    DescribeScalingProcessTypesResult describeScalingProcessTypes(
            DescribeScalingProcessTypesRequest describeScalingProcessTypesRequest) { null }

    DescribeAutoScalingGroupsResult describeAutoScalingGroups(
            DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest) {
        List<String> names = describeAutoScalingGroupsRequest.autoScalingGroupNames
        if (names) {
            return new DescribeAutoScalingGroupsResult().withAutoScalingGroups(
                    mockAsgs.findAll { it.autoScalingGroupName in names })
        }
        new DescribeAutoScalingGroupsResult().withAutoScalingGroups(mockAsgs)
    }

    DescribePoliciesResult describePolicies(
            DescribePoliciesRequest describePoliciesRequest) { new DescribePoliciesResult() }

    void enableMetricsCollection(EnableMetricsCollectionRequest enableMetricsCollectionRequest) { }

    TerminateInstanceInAutoScalingGroupResult terminateInstanceInAutoScalingGroup(
            TerminateInstanceInAutoScalingGroupRequest terminateInstanceInAutoScalingGroupRequest) { null }

    DescribeScalingActivitiesResult describeScalingActivities(
            DescribeScalingActivitiesRequest describeScalingActivitiesRequest) { new DescribeScalingActivitiesResult() }

    void executePolicy(ExecutePolicyRequest executePolicyRequest) { }

    DescribeMetricCollectionTypesResult describeMetricCollectionTypes(
            DescribeMetricCollectionTypesRequest describeMetricCollectionTypesRequest) { null }

    DescribeAdjustmentTypesResult describeAdjustmentTypes(
            DescribeAdjustmentTypesRequest describeAdjustmentTypesRequest) { null }

    void deleteAutoScalingGroup(DeleteAutoScalingGroupRequest deleteAutoScalingGroupRequest) { }

    void createAutoScalingGroup(CreateAutoScalingGroupRequest createAutoScalingGroupRequest) { }

    DescribeAutoScalingInstancesResult describeAutoScalingInstances(
            DescribeAutoScalingInstancesRequest describeAutoScalingInstancesRequest) { null }

    void deleteLaunchConfiguration(DeleteLaunchConfigurationRequest deleteLaunchConfigurationRequest) { }

    PutScalingPolicyResult putScalingPolicy(PutScalingPolicyRequest putScalingPolicyRequest) { null }

    void setInstanceHealth(SetInstanceHealthRequest setInstanceHealthRequest) { }

    void updateAutoScalingGroup(UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest) { }

    DescribeScheduledActionsResult describeScheduledActions(
            DescribeScheduledActionsRequest describeScheduledActionsRequest) {
        new DescribeScheduledActionsResult(scheduledUpdateGroupActions: [])
    }

    void suspendProcesses(SuspendProcessesRequest suspendProcessesRequest) { }

    void resumeProcesses(ResumeProcessesRequest resumeProcessesRequest) { }

    void createLaunchConfiguration(CreateLaunchConfigurationRequest createLaunchConfigurationRequest) { }

    void disableMetricsCollection(DisableMetricsCollectionRequest disableMetricsCollectionRequest) { }

    DescribeLaunchConfigurationsResult describeLaunchConfigurations() { new DescribeLaunchConfigurationsResult() }

    DescribeScalingProcessTypesResult describeScalingProcessTypes() { null }

    DescribeAutoScalingGroupsResult describeAutoScalingGroups() {
        describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest())
    }

    DescribeScalingActivitiesResult describeScalingActivities() { null }

    DescribeMetricCollectionTypesResult describeMetricCollectionTypes() { null }

    DescribeAdjustmentTypesResult describeAdjustmentTypes() { null }

    DescribeAutoScalingInstancesResult describeAutoScalingInstances() { null }

    DescribeScheduledActionsResult describeScheduledActions() {
        new DescribeScheduledActionsResult(scheduledUpdateGroupActions: [])
    }

    void shutdown() { }

    ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) { null }

    DescribeTerminationPolicyTypesResult describeTerminationPolicyTypes() {
        new DescribeTerminationPolicyTypesResult().withTerminationPolicyTypes(['OldestFirst', 'NewestFirst', 'Default'])
    }
}
