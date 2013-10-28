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

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow
import com.amazonaws.services.simpleworkflow.model.ActivityType
import com.amazonaws.services.simpleworkflow.model.ActivityTypeDetail
import com.amazonaws.services.simpleworkflow.model.ActivityTypeInfo
import com.amazonaws.services.simpleworkflow.model.ActivityTypeInfos
import com.amazonaws.services.simpleworkflow.model.DescribeActivityTypeRequest
import com.amazonaws.services.simpleworkflow.model.DescribeDomainRequest
import com.amazonaws.services.simpleworkflow.model.DescribeWorkflowExecutionRequest
import com.amazonaws.services.simpleworkflow.model.DescribeWorkflowTypeRequest
import com.amazonaws.services.simpleworkflow.model.DomainInfo
import com.amazonaws.services.simpleworkflow.model.DomainInfos
import com.amazonaws.services.simpleworkflow.model.ExecutionTimeFilter
import com.amazonaws.services.simpleworkflow.model.GetWorkflowExecutionHistoryRequest
import com.amazonaws.services.simpleworkflow.model.History
import com.amazonaws.services.simpleworkflow.model.HistoryEvent
import com.amazonaws.services.simpleworkflow.model.ListActivityTypesRequest
import com.amazonaws.services.simpleworkflow.model.ListClosedWorkflowExecutionsRequest
import com.amazonaws.services.simpleworkflow.model.ListDomainsRequest
import com.amazonaws.services.simpleworkflow.model.ListOpenWorkflowExecutionsRequest
import com.amazonaws.services.simpleworkflow.model.ListWorkflowTypesRequest
import com.amazonaws.services.simpleworkflow.model.RegisterDomainRequest
import com.amazonaws.services.simpleworkflow.model.TagFilter
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionDetail
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfo
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfos
import com.amazonaws.services.simpleworkflow.model.WorkflowType
import com.amazonaws.services.simpleworkflow.model.WorkflowTypeDetail
import com.amazonaws.services.simpleworkflow.model.WorkflowTypeInfo
import com.amazonaws.services.simpleworkflow.model.WorkflowTypeInfos
import com.netflix.asgard.cache.CacheInitializer
import com.netflix.asgard.model.SwfWorkflowTags
import com.netflix.asgard.model.WorkflowExecutionBeanOptions
import com.netflix.asgard.retriever.AwsResultsRetriever
import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.Promise
import org.joda.time.DateTime
import org.springframework.beans.factory.InitializingBean

/**
 * Service for interacting with Amazon Simple Workflow (SWF) for tracking cloud changes and executing long-running and
 * complex processes.
 */
class AwsSimpleWorkflowService implements CacheInitializer, InitializingBean {

    static transactional = false

    AmazonSimpleWorkflow simpleWorkflowClient
    def awsClientService
    def configService
    Caches caches

    /**
     * Configure service properties
     */
    void afterPropertiesSet() {

        // Workflows are stored only in the default region, so no multi-region support needed here.
        simpleWorkflowClient = awsClientService.create(AmazonSimpleWorkflow)
    }

    /**
     * Set up relevant cache objects to begin retrieving data.
     */
    void initializeCaches() {
        caches.allWorkflowDomains.ensureSetUp({ retrieveDomainsAndEnsureDomainIsRegistered() }, {
            caches.allOpenWorkflowExecutions.ensureSetUp({ retrieveOpenWorkflowExecutions() })
            caches.allClosedWorkflowExecutions.ensureSetUp({ retrieveClosedWorkflowExecutions() })
            caches.allWorkflowTypes.ensureSetUp({ retrieveWorkflowTypes() })
            caches.allActivityTypes.ensureSetUp({ retrieveActivityTypes() })
        })
    }

    // Activity types

    private List<ActivityTypeInfo> retrieveActivityTypes() {
        String domain = configService.simpleWorkflowDomain
        ensureDomainExists(domain)
        def request = new ListActivityTypesRequest(registrationStatus: 'REGISTERED', domain: domain)
        activityTypesRetriever.retrieve(Region.defaultRegion(), request)
    }

    private AwsResultsRetriever activityTypesRetriever =
            new AwsResultsRetriever<ActivityTypeInfo, ListActivityTypesRequest, ActivityTypeInfos>() {

        protected ActivityTypeInfos makeRequest(Region region, ListActivityTypesRequest request) {
            simpleWorkflowClient.listActivityTypes(request)
        }

        protected List<ActivityTypeInfo> accessResult(ActivityTypeInfos result) {
            result.typeInfos
        }

        protected void setNextToken(ListActivityTypesRequest request, String nextToken) {
            request.withNextPageToken(nextToken)
        }

        protected String getNextToken(ActivityTypeInfos result) {
            result.nextPageToken
        }
    }

    /**
     * Gets the list of all cached activity types.
     *
     * @param userContext who, where, why
     * @return cached activity types
     */
    Collection<ActivityTypeInfo> getActivityTypes(UserContext userContext) {
        caches.allActivityTypes.list()
    }

    /**
     * Gets detailed information about an activity type from AWS.
     *
     * @param name of the Activity Type
     * @param version of the Activity Type
     * @return activity type details
     */
    ActivityTypeDetail getActivityTypeDetail(String name, String version) {
        String domain = configService.simpleWorkflowDomain
        ActivityType activityType = new ActivityType(name: name, version: version)
        simpleWorkflowClient.describeActivityType(new DescribeActivityTypeRequest(domain: domain,
                activityType: activityType))
    }

    // Workflow types

    private List<WorkflowType> retrieveWorkflowTypes() {
        String domain = configService.simpleWorkflowDomain
        ensureDomainExists(domain)

        def request = new ListWorkflowTypesRequest(registrationStatus: 'REGISTERED', domain: domain)
        workflowTypesRetriever.retrieve(Region.defaultRegion(), request)
    }

    private AwsResultsRetriever workflowTypesRetriever =
            new AwsResultsRetriever<WorkflowTypeInfo, ListWorkflowTypesRequest, WorkflowTypeInfos>() {

        protected WorkflowTypeInfos makeRequest(Region region, ListWorkflowTypesRequest request) {
            simpleWorkflowClient.listWorkflowTypes(request)
        }

        protected List<WorkflowTypeInfo> accessResult(WorkflowTypeInfos result) {
            result.typeInfos
        }

        protected void setNextToken(ListWorkflowTypesRequest request, String nextToken) {
            request.withNextPageToken(nextToken)
        }

        protected String getNextToken(WorkflowTypeInfos result) {
            result.nextPageToken
        }
    }

    /**
     * Gets the list of all workflow types.
     *
     * @param userContext who, where, why
     * @return cached workflow types
     */
    Collection<WorkflowTypeInfo> getWorkflowTypes(UserContext userContext) {
        caches.allWorkflowTypes.list()
    }

    /**
     * Gets detailed information about a workflow type from AWS.
     *
     * @param name of the Workflow Type
     * @param version of the Workflow Type
     * @return workflow type details
     */
    WorkflowTypeDetail getWorkflowTypeDetail(String name, String version) {
        String domain = configService.simpleWorkflowDomain
        WorkflowType workflowType = new WorkflowType(name: name, version: version)
        simpleWorkflowClient.describeWorkflowType(new DescribeWorkflowTypeRequest(domain: domain,
                workflowType: workflowType))
    }

    /**
     * Gets the workflow execution attributes related to a task by ID, and modifies local list caches appropriately.
     *
     * @param id of the related task
     * @return workflow execution attributes
     */
    WorkflowExecutionBeanOptions getWorkflowExecutionInfoByTaskId(String id) {
        // First check for a closed workflow execution in the cache. They don't get updated.
        WorkflowExecutionInfo workflowExecutionInfo = caches.allClosedWorkflowExecutions.get(id)
        if (!workflowExecutionInfo) {
            SwfWorkflowTags swfWorkflowTags = new SwfWorkflowTags(id: id)
            String tagValue = swfWorkflowTags.constructTag('id')
            Promise<List<WorkflowExecutionInfo>> openExecutions = Dataflow.task {
                getOpenWorkflowExecutionsForTag(tagValue)
            }
            Promise<List<WorkflowExecutionInfo>> closedExecutions = Dataflow.task {
                getClosedWorkflowExecutionsForTag(tagValue)
            }
            List<WorkflowExecutionInfo> executionInfos = closedExecutions.get() ?: openExecutions.get()
            Check.loneOrNone(executionInfos, 'reference')
            if (!executionInfos) { return null }
            workflowExecutionInfo = executionInfos[0]
            updateWorkflowExecutionCaches(workflowExecutionInfo)
        }
        createWorkflowExecutionBeanOptions(workflowExecutionInfo)
    }

    private List<WorkflowExecutionInfo> getOpenWorkflowExecutionsForTag(String tagValue) {
        String domain = configService.simpleWorkflowDomain
        TagFilter tagFilter = new TagFilter(tag: tagValue)
        ListOpenWorkflowExecutionsRequest openRequest = new ListOpenWorkflowExecutionsRequest(domain: domain,
                tagFilter: tagFilter, startTimeFilter: executionTimeFilter)
        simpleWorkflowClient.listOpenWorkflowExecutions(openRequest).executionInfos
    }

    private List<WorkflowExecutionInfo> getClosedWorkflowExecutionsForTag(String tagValue) {
        String domain = configService.simpleWorkflowDomain
        TagFilter tagFilter = new TagFilter(tag: tagValue)
        ListClosedWorkflowExecutionsRequest closedRequest = new ListClosedWorkflowExecutionsRequest(domain: domain,
                tagFilter: tagFilter, closeTimeFilter: executionTimeFilter)
        simpleWorkflowClient.listClosedWorkflowExecutions(closedRequest).executionInfos
    }

    /**
     * Gets the workflow execution attributes for the WorkflowExecution. Wait, what!?
     * The WorkflowExecution is an AWS object that actually holds identifiers for the workflow execution.
     * This method also modifies local list caches appropriately.
     *
     * @param workflowExecution holds identifiers for the workflow execution (really)
     * @return workflow execution attributes
     */
    WorkflowExecutionBeanOptions getWorkflowExecutionInfoByWorkflowExecution(WorkflowExecution workflowExecution) {
        String domain = configService.simpleWorkflowDomain
        DescribeWorkflowExecutionRequest request = new DescribeWorkflowExecutionRequest(domain: domain,
                execution: workflowExecution)
        WorkflowExecutionDetail workflowExecutionDetail = simpleWorkflowClient.describeWorkflowExecution(request)
        WorkflowExecutionInfo workflowExecutionInfo = workflowExecutionDetail.executionInfo
        WorkflowExecutionBeanOptions options = createWorkflowExecutionBeanOptions(workflowExecutionInfo)
        updateWorkflowExecutionCaches(workflowExecutionInfo)
        options
    }

    private WorkflowExecutionBeanOptions createWorkflowExecutionBeanOptions(
            WorkflowExecutionInfo workflowExecutionInfo) {
        if (workflowExecutionInfo) {
            List<HistoryEvent> events = getExecutionHistory(workflowExecutionInfo.execution)
            return new WorkflowExecutionBeanOptions(workflowExecutionInfo, events)
        }
        null
    }

    private void updateWorkflowExecutionCaches(WorkflowExecutionInfo workflowExecutionInfo) {
        if (workflowExecutionInfo == null) { return }
        String key = EntityType.workflowExecution.keyer(workflowExecutionInfo)
        CachedMap<WorkflowExecutionInfo> cacheToPutItemInto = caches.allOpenWorkflowExecutions
        if (workflowExecutionInfo.closeTimestamp) {
            caches.allOpenWorkflowExecutions.remove(key)
            cacheToPutItemInto = caches.allClosedWorkflowExecutions
        }
        cacheToPutItemInto.put(key, workflowExecutionInfo)
    }

    // Open workflow executions
    private List<WorkflowExecutionInfo> retrieveOpenWorkflowExecutions() {
        String domain = configService.simpleWorkflowDomain
        ensureDomainExists(domain)
        def request = new ListOpenWorkflowExecutionsRequest(domain: domain, startTimeFilter: executionTimeFilter)
        openWorkflowExecutionRetriever.retrieve(Region.defaultRegion(), request)
    }

    private AwsResultsRetriever openWorkflowExecutionRetriever =
            new AwsResultsRetriever<WorkflowExecutionInfo, ListOpenWorkflowExecutionsRequest,
                    WorkflowExecutionInfos>() {

        protected WorkflowExecutionInfos makeRequest(Region region, ListOpenWorkflowExecutionsRequest request) {
            simpleWorkflowClient.listOpenWorkflowExecutions(request)
        }

        protected List<WorkflowExecutionInfo> accessResult(WorkflowExecutionInfos result) {
            result.executionInfos
        }

        protected void setNextToken(ListOpenWorkflowExecutionsRequest request, String nextToken) {
            request.withNextPageToken(nextToken)
        }

        protected String getNextToken(WorkflowExecutionInfos result) {
            result.nextPageToken
        }
    }

    /**
     * Gets the list of all open workflow executions.
     *
     * @param userContext who, where, why
     * @return cached open workflow executions
     */
    Collection<WorkflowExecutionInfo> getOpenWorkflowExecutions() {
        caches.allOpenWorkflowExecutions.list().findAll { it.tagList }
    }

    // Closed workflow executions

    private List<WorkflowExecutionInfo> retrieveClosedWorkflowExecutions() {
        String domain = configService.simpleWorkflowDomain
        ensureDomainExists(domain)
        def request = new ListClosedWorkflowExecutionsRequest(domain: domain, closeTimeFilter: executionTimeFilter)
        closedWorkflowExecutionRetriever.retrieve(Region.defaultRegion(), request)
    }

    private ExecutionTimeFilter getExecutionTimeFilter() {
        Date oldestDate = new DateTime().minusDays(configService.workflowExecutionRetentionPeriodInDays).toDate()
        new ExecutionTimeFilter(oldestDate: oldestDate)
    }

    private AwsResultsRetriever closedWorkflowExecutionRetriever =
            new AwsResultsRetriever<WorkflowExecutionInfo, ListClosedWorkflowExecutionsRequest,
                    WorkflowExecutionInfos>() {

        protected WorkflowExecutionInfos makeRequest(Region region, ListClosedWorkflowExecutionsRequest request) {
            simpleWorkflowClient.listClosedWorkflowExecutions(request)
        }

        protected List<WorkflowExecutionInfo> accessResult(WorkflowExecutionInfos result) {
            result.executionInfos
        }

        protected void setNextToken(ListClosedWorkflowExecutionsRequest request, String nextToken) {
            request.withNextPageToken(nextToken)
        }

        protected String getNextToken(WorkflowExecutionInfos result) {
            result.nextPageToken
        }
    }

    /**
     * Gets the list of all closed workflow executions.
     *
     * @param userContext who, where, why
     * @return cached closed workflow executions
     */
    Collection<WorkflowExecutionInfo> getClosedWorkflowExecutions() {
        caches.allClosedWorkflowExecutions.list().findAll { it.tagList }
    }

    // Workflow Domains

    /**
     * Gets all the SWF domains and registers the main domain we need if it's not already in the list.
     *
     * @return info objects for all the registered domains in the default region
     */
    List<DomainInfo> retrieveDomainsAndEnsureDomainIsRegistered() {
        log.debug('Retrieve workflow domains')
        ListDomainsRequest request = new ListDomainsRequest(registrationStatus: 'REGISTERED')
        List<DomainInfo> domains = domainFetcher.retrieve(Region.defaultRegion(), request)
        log.debug("Found workflow domains: ${domains*.name} ${domains}")
        String domain = configService.simpleWorkflowDomain
        log.debug("The important workflow domain should be called ${domain}")
        if (!(domain in domains*.name)) {
            log.debug("${domain} workflow domain is not yet created. Attempting to create it.")
            registerWorkflowDomain()
        }
        domains
    }

    private void ensureDomainExists(String domain) {
        if (caches.allWorkflowDomains.get(domain)) {
            return
        }
        caches.allWorkflowDomains.fill()
    }

    private AwsResultsRetriever domainFetcher = new AwsResultsRetriever<DomainInfo, ListDomainsRequest, DomainInfos>() {

        protected DomainInfos makeRequest(Region region, ListDomainsRequest request) {
            simpleWorkflowClient.listDomains(request)
        }

        protected List<DomainInfo> accessResult(DomainInfos result) {
            result.domainInfos
        }

        protected void setNextToken(ListDomainsRequest request, String nextToken) {
            request.withNextPageToken(nextToken)
        }

        protected String getNextToken(DomainInfos result) {
            result.nextPageToken
        }
    }

    private void registerWorkflowDomain() {
        String domain = configService.simpleWorkflowDomain
        Integer retentionPeriod = configService.workflowExecutionRetentionPeriodInDays
        String description = 'Automation workflows for Asgard-driven cloud deployments'
        RegisterDomainRequest request = new RegisterDomainRequest(name: domain, description: description)
        request.setWorkflowExecutionRetentionPeriodInDays(retentionPeriod.toString())
        log.info("Create workflow domain ${domain}")
        simpleWorkflowClient.registerDomain(request)
    }

    /**
     * Gets the list of all workflow domains.
     *
     * @param userContext who, where, why
     * @return cached domains
     */
    Collection<DomainInfo> getDomains(UserContext userContext) {
        caches.allWorkflowDomains.list()
    }

    /**
     * Gets a specific workflow domain.
     *
     * @param userContext who, where, why
     * @param name the name of the workflow domain to get
     * @param from the strategy for retrieving and caching the data
     * @return the workflow domain
     */
    DomainInfo getDomain(UserContext userContext, String name, From from = From.AWS) {
        if (!name) { return null }
        if (from == From.CACHE) {
            return caches.allWorkflowDomains.get(name)
        }
        DomainInfo domain
        try {
            domain = simpleWorkflowClient.describeDomain(new DescribeDomainRequest(name: name)).domainInfo
        } catch (AmazonServiceException ignored) {
            domain = null
        }
        if (from != From.AWS_NOCACHE) {
            caches.allWorkflowDomains.put(name, domain)
        }
        domain
    }

    /**
     * Gets the execution history for specific workflow run.
     *
     * @param workflowExecution to identify workflow
     * @return execution history
     */
    List<HistoryEvent> getExecutionHistory(WorkflowExecution workflowExecution) {
        if (!workflowExecution) { return [] }
        String domain = configService.simpleWorkflowDomain
        def retriever = new AwsResultsRetriever<HistoryEvent, GetWorkflowExecutionHistoryRequest, History>() {
            @Override
            protected History makeRequest(Region region, GetWorkflowExecutionHistoryRequest request) {
                simpleWorkflowClient.getWorkflowExecutionHistory(request)
            }
            @Override
            protected List<HistoryEvent> accessResult(History result) {
                result.events
            }
            protected void setNextToken(GetWorkflowExecutionHistoryRequest request, String nextToken) {
                request.withNextPageToken(nextToken)
            }
            protected String getNextToken(History result) {
                result.nextPageToken
            }
        }
        retriever.retrieve(null, new GetWorkflowExecutionHistoryRequest(domain: domain, execution: workflowExecution))
    }

}
