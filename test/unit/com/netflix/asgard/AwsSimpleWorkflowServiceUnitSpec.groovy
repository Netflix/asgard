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
package com.netflix.asgard

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow
import com.amazonaws.services.simpleworkflow.model.DescribeWorkflowExecutionRequest
import com.amazonaws.services.simpleworkflow.model.DomainInfo
import com.amazonaws.services.simpleworkflow.model.ExecutionTimeFilter
import com.amazonaws.services.simpleworkflow.model.History
import com.amazonaws.services.simpleworkflow.model.TagFilter
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionDetail
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfo
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfos
import com.netflix.asgard.model.WorkflowExecutionBeanOptions
import com.netflix.asgard.retriever.AwsResultsRetriever
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import spock.lang.Specification

@SuppressWarnings(["GroovyAssignabilityCheck", "GroovyAccessibility"])
class AwsSimpleWorkflowServiceUnitSpec extends Specification {

    AwsSimpleWorkflowService awsSimpleWorkflowService = new AwsSimpleWorkflowService()
    WorkflowExecutionInfo deployGusFring1 = new WorkflowExecutionInfo(tagList: [
            '{"desc":"Los Pollos Hermanos where something delicious is always cooking"}',
            '{"user":{"region":"US_WEST_2"}}',
            '{"link":{"type":{"name":"cluster"},"id":"gus-fring"}}',
            '{"id":"1003"}'
    ])
    WorkflowExecutionInfo deployWalterWhite1 = new WorkflowExecutionInfo(tagList: [
            '{"desc":"I am the one who knocks"}',
            '{"user":{"region":"US_WEST_2"}}',
            '{"link":{"type":{"name":"cluster"},"id":"walter-white"}}',
            '{"id":"1004"}'
    ])
    WorkflowExecutionInfo deployGusFring2 = new WorkflowExecutionInfo(tagList: [
            '{"desc":"Never make the same mistake twice"}',
            '{"user":{"region":"US_WEST_2"}}',
            '{"link":{"type":{"name":"cluster"},"id":"gus-fring"}}',
            '{"id":"1005"}'
    ])
    AmazonSimpleWorkflow mockAmazonSimpleWorkflow = Mock(AmazonSimpleWorkflow)

    def setup() {
        awsSimpleWorkflowService.caches = new Caches(new MockCachedMapBuilder([:], { Mock(CachedMap) }))
        awsSimpleWorkflowService.configService = Mock(ConfigService) {
            getSimpleWorkflowDomain() >> 'Westeros'
            getWorkflowExecutionRetentionPeriodInDays() >> 10
        }
        awsSimpleWorkflowService.simpleWorkflowClient = mockAmazonSimpleWorkflow
    }

    def 'should retrieve domains'() {
        AwsSimpleWorkflowService awsSimpleWorkflowService = Spy(AwsSimpleWorkflowService)
        awsSimpleWorkflowService.@domainFetcher = Mock(AwsResultsRetriever) {
            retrieve(_, _) >> [new DomainInfo(name: 'domain1')]
        }
        awsSimpleWorkflowService.configService = Mock(ConfigService) {
            getSimpleWorkflowDomain() >> 'domain1'
        }
        awsSimpleWorkflowService.simpleWorkflowClient = Mock(AmazonSimpleWorkflow)

        when:
        List<DomainInfo> domains = awsSimpleWorkflowService.retrieveDomainsAndEnsureDomainIsRegistered()

        then:
        domains == [new DomainInfo(name: 'domain1')]
        0 * awsSimpleWorkflowService.simpleWorkflowClient.registerDomain(_)
    }

    def 'should register domain if not found on retrieval'() {
        AwsSimpleWorkflowService awsSimpleWorkflowService = Spy(AwsSimpleWorkflowService)
        awsSimpleWorkflowService.@domainFetcher = Mock(AwsResultsRetriever) {
            retrieve(_, _) >> [new DomainInfo(name: 'domain1')]
        }
        awsSimpleWorkflowService.configService = Mock(ConfigService) {
            getSimpleWorkflowDomain() >> 'domain2'
        }
        awsSimpleWorkflowService.simpleWorkflowClient = Mock(AmazonSimpleWorkflow)

        when:
        awsSimpleWorkflowService.retrieveDomainsAndEnsureDomainIsRegistered()

        then:
        1 * awsSimpleWorkflowService.simpleWorkflowClient.registerDomain(_)
    }

    def 'should get workflow execution info by task ID'() {
        def matchRequest = { it.domain == 'Westeros' && it.tagFilter == new TagFilter(tag: '{"id":"123"}') }
        awsSimpleWorkflowService.simpleWorkflowClient = Mock(AmazonSimpleWorkflow) {
            1 * listOpenWorkflowExecutions({ matchRequest(it) }) >> new WorkflowExecutionInfos(executionInfos: [])
            1 * listClosedWorkflowExecutions({ matchRequest(it) }) >> new WorkflowExecutionInfos(executionInfos: [
                    new WorkflowExecutionInfo(execution: new WorkflowExecution(workflowId: 'abc', runId: 'def'))
            ])
            getWorkflowExecutionHistory(_) >> new History(events: [])
        }

        when:
        WorkflowExecutionBeanOptions options = awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('123')

        then:
        options.executionInfo.execution == new WorkflowExecution(workflowId: 'abc', runId: 'def')
    }

    def 'should return null if workflow execution does not exist for task ID'() {
        awsSimpleWorkflowService.simpleWorkflowClient = Mock(AmazonSimpleWorkflow) {
            1 * listOpenWorkflowExecutions(_) >> new WorkflowExecutionInfos(executionInfos: [])
            1 * listClosedWorkflowExecutions(_) >> new WorkflowExecutionInfos(executionInfos: [])
        }

        when:
        WorkflowExecutionBeanOptions options = awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('123')

        then:
        options == null
    }

    def 'should prefer closed workflow execution info by task ID if also found as open'() {
        def matchRequest = { it.domain == 'Westeros' && it.tagFilter == new TagFilter(tag: '{"id":"123"}') }
        awsSimpleWorkflowService.simpleWorkflowClient = Mock(AmazonSimpleWorkflow) {
            // Open executions may not be searched since a closed execution is found (due to concurrency).
            (0..1) * listOpenWorkflowExecutions({ matchRequest(it) }) >> new WorkflowExecutionInfos(executionInfos: [
                    new WorkflowExecutionInfo(execution: new WorkflowExecution(workflowId: 'abc', runId: 'def'),
                            executionStatus: 'OPEN')
            ])
            1 * listClosedWorkflowExecutions({ matchRequest(it) }) >> new WorkflowExecutionInfos(executionInfos: [
                    new WorkflowExecutionInfo(execution: new WorkflowExecution(workflowId: 'uvw', runId: 'xyz'),
                            executionStatus: 'CLOSED')
            ])
            getWorkflowExecutionHistory(_) >> new History(events: [])
        }

        when:
        WorkflowExecutionBeanOptions options = awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('123')

        then:
        options.executionInfo.execution == new WorkflowExecution(workflowId: 'uvw', runId: 'xyz')
    }

    def 'should error for multiple workflow execution infos by task ID'() {
        def matchRequest = { it.domain == 'Westeros' && it.tagFilter == new TagFilter(tag: '{"id":"123"}') }
        awsSimpleWorkflowService.simpleWorkflowClient = Mock(AmazonSimpleWorkflow) {
            1 * listOpenWorkflowExecutions({ matchRequest(it) }) >> new WorkflowExecutionInfos(executionInfos: [
                    new WorkflowExecutionInfo(execution: new WorkflowExecution(workflowId: 'abc', runId: 'def')),
                    new WorkflowExecutionInfo(execution: new WorkflowExecution(workflowId: 'uvw', runId: 'xyz'))
            ])
            1 * listClosedWorkflowExecutions({ matchRequest(it) }) >> new WorkflowExecutionInfos(executionInfos: [])
            getWorkflowExecutionHistory(_) >> new History(events: [])
        }

        when:
        awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('123')

        then:
        IllegalStateException e = thrown()
        e.message == 'ERROR: Found 2 reference items instead of 0 or 1'
    }

    def 'should get workflow execution info by workflow execution'() {
        WorkflowExecution execution = new WorkflowExecution(workflowId: 'abc', runId: 'def')
        awsSimpleWorkflowService.simpleWorkflowClient = Mock(AmazonSimpleWorkflow) {
            1 * describeWorkflowExecution(new DescribeWorkflowExecutionRequest(domain: 'Westeros',
                    execution: execution)) >> new WorkflowExecutionDetail(executionInfo:
                    new WorkflowExecutionInfo(execution: new WorkflowExecution(workflowId: 'abc', runId: 'def')))
            getWorkflowExecutionHistory(_) >> new History(events: [])
        }

        when:
        WorkflowExecutionBeanOptions options = awsSimpleWorkflowService.getWorkflowExecutionInfoByWorkflowExecution(
                execution)

        then:
        options.executionInfo.execution == new WorkflowExecution(workflowId: 'abc', runId: 'def')
    }

    def 'should cache an open workflow execution info'() {
        WorkflowExecution workflowExecution = new WorkflowExecution(runId: '1234567')
        WorkflowExecutionInfo workflowExecutionInfo = new WorkflowExecutionInfo(execution: workflowExecution)

        when:
        awsSimpleWorkflowService.updateWorkflowExecutionCaches(workflowExecutionInfo)

        then:
        with(awsSimpleWorkflowService) {
            1 * caches.allOpenWorkflowExecutions.put('1234567', workflowExecutionInfo)
            0 * caches._
        }
    }

    def 'should cache a closed workflow execution info and ensure it is removed from the open cache'() {
        WorkflowExecution workflowExecution = new WorkflowExecution(runId: '1234567')
        WorkflowExecutionInfo workflowExecutionInfo = new WorkflowExecutionInfo(execution: workflowExecution,
                closeTimestamp: new Date())

        when:
        awsSimpleWorkflowService.updateWorkflowExecutionCaches(workflowExecutionInfo)

        then:
        with(awsSimpleWorkflowService) {
            1 * caches.allClosedWorkflowExecutions.put('1234567', workflowExecutionInfo)
            1 * caches.allOpenWorkflowExecutions.remove('1234567')
            0 * caches._
        }
    }

    def 'should not cache a null workflow execution info'() {
        when:
        awsSimpleWorkflowService.updateWorkflowExecutionCaches(null)

        then:
        with(awsSimpleWorkflowService) {
            0 * caches._
        }
    }

    def 'should return a workflow execution matching the specified link from the cache'() {

        Link link = new Link(EntityType.cluster, 'gus-fring')

        when:
        WorkflowExecutionInfo result = awsSimpleWorkflowService.getOpenWorkflowExecutionForObjectLink(Region.US_WEST_2,
                link)

        then:
        result == deployGusFring1
        1 * awsSimpleWorkflowService.caches.allOpenWorkflowExecutions.list() >>
                [deployGusFring1, deployWalterWhite1, deployGusFring2]
        1 * awsSimpleWorkflowService.configService.getSimpleWorkflowDomain() >> 'Albuquerque'
        1 * mockAmazonSimpleWorkflow.describeWorkflowExecution(
                new DescribeWorkflowExecutionRequest(domain: 'Albuquerque')) >> new WorkflowExecutionDetail()
        0 * _
    }

    def 'should not return a workflow execution matching the specified link from the cache with the wrong region'() {

        Link link = new Link(EntityType.cluster, 'gus-fring')

        when:
        WorkflowExecutionInfo result = awsSimpleWorkflowService.getOpenWorkflowExecutionForObjectLink(Region.US_WEST_1,
                link)

        then:
        result == null
        1 * awsSimpleWorkflowService.caches.allOpenWorkflowExecutions.list() >>
                [deployGusFring1, deployWalterWhite1, deployGusFring2]
        0 * _
    }

    def 'should return null when no workflow execution found for specified link in open execution cache nor in AWS'() {

        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd-MM-yyyy").withZone(DateTimeZone.UTC)
        ExecutionTimeFilter timeFilter = new ExecutionTimeFilter(
                oldestDate: formatter.parseDateTime('19-09-2013').toDate()
        )
        awsSimpleWorkflowService.filterEndTime = formatter.parseDateTime('29-09-2013')
        Link link = new Link(EntityType.cluster, 'this-isnt-running')

        when:
        WorkflowExecutionInfo result = awsSimpleWorkflowService.getOpenWorkflowExecutionForObjectLink(Region.US_WEST_1,
                link)

        then:
        result == null
        1 * awsSimpleWorkflowService.caches.allOpenWorkflowExecutions.list() >> []
        0 * _
    }
}
