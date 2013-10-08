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
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow
import com.amazonaws.services.simpleworkflow.model.ActivityTask
import com.amazonaws.services.simpleworkflow.model.ActivityTaskStatus
import com.amazonaws.services.simpleworkflow.model.ActivityTypeDetail
import com.amazonaws.services.simpleworkflow.model.ActivityTypeInfos
import com.amazonaws.services.simpleworkflow.model.CountClosedWorkflowExecutionsRequest
import com.amazonaws.services.simpleworkflow.model.CountOpenWorkflowExecutionsRequest
import com.amazonaws.services.simpleworkflow.model.CountPendingActivityTasksRequest
import com.amazonaws.services.simpleworkflow.model.CountPendingDecisionTasksRequest
import com.amazonaws.services.simpleworkflow.model.DecisionTask
import com.amazonaws.services.simpleworkflow.model.DeprecateActivityTypeRequest
import com.amazonaws.services.simpleworkflow.model.DeprecateDomainRequest
import com.amazonaws.services.simpleworkflow.model.DeprecateWorkflowTypeRequest
import com.amazonaws.services.simpleworkflow.model.DescribeActivityTypeRequest
import com.amazonaws.services.simpleworkflow.model.DescribeDomainRequest
import com.amazonaws.services.simpleworkflow.model.DescribeWorkflowExecutionRequest
import com.amazonaws.services.simpleworkflow.model.DescribeWorkflowTypeRequest
import com.amazonaws.services.simpleworkflow.model.DomainDetail
import com.amazonaws.services.simpleworkflow.model.DomainInfo
import com.amazonaws.services.simpleworkflow.model.DomainInfos
import com.amazonaws.services.simpleworkflow.model.GetWorkflowExecutionHistoryRequest
import com.amazonaws.services.simpleworkflow.model.History
import com.amazonaws.services.simpleworkflow.model.ListActivityTypesRequest
import com.amazonaws.services.simpleworkflow.model.ListClosedWorkflowExecutionsRequest
import com.amazonaws.services.simpleworkflow.model.ListDomainsRequest
import com.amazonaws.services.simpleworkflow.model.ListOpenWorkflowExecutionsRequest
import com.amazonaws.services.simpleworkflow.model.ListWorkflowTypesRequest
import com.amazonaws.services.simpleworkflow.model.PendingTaskCount
import com.amazonaws.services.simpleworkflow.model.PollForActivityTaskRequest
import com.amazonaws.services.simpleworkflow.model.PollForDecisionTaskRequest
import com.amazonaws.services.simpleworkflow.model.RecordActivityTaskHeartbeatRequest
import com.amazonaws.services.simpleworkflow.model.RegisterActivityTypeRequest
import com.amazonaws.services.simpleworkflow.model.RegisterDomainRequest
import com.amazonaws.services.simpleworkflow.model.RegisterWorkflowTypeRequest
import com.amazonaws.services.simpleworkflow.model.RequestCancelWorkflowExecutionRequest
import com.amazonaws.services.simpleworkflow.model.RespondActivityTaskCanceledRequest
import com.amazonaws.services.simpleworkflow.model.RespondActivityTaskCompletedRequest
import com.amazonaws.services.simpleworkflow.model.RespondActivityTaskFailedRequest
import com.amazonaws.services.simpleworkflow.model.RespondDecisionTaskCompletedRequest
import com.amazonaws.services.simpleworkflow.model.Run
import com.amazonaws.services.simpleworkflow.model.SignalWorkflowExecutionRequest
import com.amazonaws.services.simpleworkflow.model.StartWorkflowExecutionRequest
import com.amazonaws.services.simpleworkflow.model.TerminateWorkflowExecutionRequest
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionCount
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionDetail
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfos
import com.amazonaws.services.simpleworkflow.model.WorkflowTypeDetail
import com.amazonaws.services.simpleworkflow.model.WorkflowTypeInfos

class MockAmazonSimpleWorkflowClient implements AmazonSimpleWorkflow {

    MockAmazonSimpleWorkflowClient(BasicAWSCredentials awsCredentials) {

    }

    MockAmazonSimpleWorkflowClient(BasicAWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        this(awsCredentials)
    }

    @Override
    ActivityTypeInfos listActivityTypes(ListActivityTypesRequest listActivityTypesRequest) {
        new ActivityTypeInfos()
    }

    @Override
    DomainDetail describeDomain(DescribeDomainRequest describeDomainRequest) {
        return null
    }

    @Override
    void respondActivityTaskFailed(RespondActivityTaskFailedRequest respondActivityTaskFailedRequest) {

    }

    @Override
    PendingTaskCount countPendingDecisionTasks(CountPendingDecisionTasksRequest countPendingDecisionTasksRequest) {
        return null
    }

    @Override
    void terminateWorkflowExecution(TerminateWorkflowExecutionRequest terminateWorkflowExecutionRequest) {

    }

    @Override
    WorkflowExecutionDetail describeWorkflowExecution(
            DescribeWorkflowExecutionRequest describeWorkflowExecutionRequest) {
        return null
    }

    @Override
    void shutdown() {

    }

    @Override
    ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
        return null
    }

    @Override
    WorkflowExecutionInfos listClosedWorkflowExecutions(
            ListClosedWorkflowExecutionsRequest listClosedWorkflowExecutionsRequest) {
        new WorkflowExecutionInfos()
    }

    @Override
    ActivityTaskStatus recordActivityTaskHeartbeat(
            RecordActivityTaskHeartbeatRequest recordActivityTaskHeartbeatRequest) {
        return null
    }

    @Override
    DecisionTask pollForDecisionTask(PollForDecisionTaskRequest pollForDecisionTaskRequest) {
        return null
    }

    @Override
    DomainInfos listDomains(ListDomainsRequest listDomainsRequest) {
        DomainInfo domainInfo = new DomainInfo(name: 'asgard', description: 'Autobots roll out', status: 'REGISTERED')
        new DomainInfos().withDomainInfos(domainInfo)
    }

    @Override
    void requestCancelWorkflowExecution(RequestCancelWorkflowExecutionRequest requestCancelWorkflowExecutionRequest) {

    }

    @Override
    WorkflowTypeDetail describeWorkflowType(DescribeWorkflowTypeRequest describeWorkflowTypeRequest) {
        return null
    }

    @Override
    void deprecateActivityType(DeprecateActivityTypeRequest deprecateActivityTypeRequest) {

    }

    @Override
    WorkflowExecutionCount countClosedWorkflowExecutions(
            CountClosedWorkflowExecutionsRequest countClosedWorkflowExecutionsRequest) {
        return null
    }

    @Override
    PendingTaskCount countPendingActivityTasks(CountPendingActivityTasksRequest countPendingActivityTasksRequest) {
        return null
    }

    @Override
    void respondActivityTaskCanceled(RespondActivityTaskCanceledRequest respondActivityTaskCanceledRequest) {

    }

    @Override
    void respondDecisionTaskCompleted(RespondDecisionTaskCompletedRequest respondDecisionTaskCompletedRequest) {

    }

    @Override
    void respondActivityTaskCompleted(RespondActivityTaskCompletedRequest respondActivityTaskCompletedRequest) {

    }

    @Override
    ActivityTask pollForActivityTask(PollForActivityTaskRequest pollForActivityTaskRequest) {
        return null
    }

    @Override
    WorkflowExecutionCount countOpenWorkflowExecutions(
            CountOpenWorkflowExecutionsRequest countOpenWorkflowExecutionsRequest) {
        return null
    }

    @Override
    ActivityTypeDetail describeActivityType(DescribeActivityTypeRequest describeActivityTypeRequest) {
        return null
    }

    @Override
    void registerDomain(RegisterDomainRequest registerDomainRequest) {
        // do nothing
    }

    @Override
    void registerActivityType(RegisterActivityTypeRequest registerActivityTypeRequest) {

    }

    @Override
    WorkflowExecutionInfos listOpenWorkflowExecutions(
            ListOpenWorkflowExecutionsRequest listOpenWorkflowExecutionsRequest) {
        new WorkflowExecutionInfos()
    }

    @Override
    History getWorkflowExecutionHistory(GetWorkflowExecutionHistoryRequest getWorkflowExecutionHistoryRequest) {
        return null
    }

    @Override
    void setEndpoint(String endpoint) throws IllegalArgumentException {

    }

    @Override
    void deprecateWorkflowType(DeprecateWorkflowTypeRequest deprecateWorkflowTypeRequest) {

    }

    @Override
    void deprecateDomain(DeprecateDomainRequest deprecateDomainRequest) {

    }

    @Override
    void registerWorkflowType(RegisterWorkflowTypeRequest registerWorkflowTypeRequest) {

    }

    @Override
    WorkflowTypeInfos listWorkflowTypes(ListWorkflowTypesRequest listWorkflowTypesRequest) {
        new WorkflowTypeInfos()
    }

    @Override
    Run startWorkflowExecution(StartWorkflowExecutionRequest startWorkflowExecutionRequest) {
        return null
    }

    @Override
    void signalWorkflowExecution(SignalWorkflowExecutionRequest signalWorkflowExecutionRequest) {

    }
}
