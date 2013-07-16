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

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient
import com.amazonaws.services.simpleworkflow.model.ActivityTypeInfos
import com.amazonaws.services.simpleworkflow.model.DomainInfo
import com.amazonaws.services.simpleworkflow.model.DomainInfos
import com.amazonaws.services.simpleworkflow.model.ListActivityTypesRequest
import com.amazonaws.services.simpleworkflow.model.ListClosedWorkflowExecutionsRequest
import com.amazonaws.services.simpleworkflow.model.ListDomainsRequest
import com.amazonaws.services.simpleworkflow.model.ListOpenWorkflowExecutionsRequest
import com.amazonaws.services.simpleworkflow.model.ListWorkflowTypesRequest
import com.amazonaws.services.simpleworkflow.model.RegisterDomainRequest
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfos
import com.amazonaws.services.simpleworkflow.model.WorkflowTypeInfos

class MockAmazonSimpleWorkflowClient extends AmazonSimpleWorkflowClient {

    MockAmazonSimpleWorkflowClient(BasicAWSCredentials awsCredentials) {
        super(awsCredentials)
    }

    MockAmazonSimpleWorkflowClient(BasicAWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        this(awsCredentials)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ActivityTypeInfos listActivityTypes(ListActivityTypesRequest listActivityTypesRequest) {
        new ActivityTypeInfos()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    WorkflowExecutionInfos listClosedWorkflowExecutions(
            ListClosedWorkflowExecutionsRequest listClosedWorkflowExecutionsRequest) {
        new WorkflowExecutionInfos()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    DomainInfos listDomains(ListDomainsRequest listDomainsRequest) {
        String domainName = "asgard_${System.getProperty('user.name')}"
        DomainInfo domainInfo = new DomainInfo(name: domainName, description: 'Autobots roll out', status: 'REGISTERED')
        new DomainInfos().withDomainInfos(domainInfo)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void registerDomain(RegisterDomainRequest registerDomainRequest) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    WorkflowExecutionInfos listOpenWorkflowExecutions(
            ListOpenWorkflowExecutionsRequest listOpenWorkflowExecutionsRequest) {
        new WorkflowExecutionInfos()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    WorkflowTypeInfos listWorkflowTypes(ListWorkflowTypesRequest listWorkflowTypesRequest) {
        new WorkflowTypeInfos()
    }
}
