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
import com.amazonaws.services.simpleworkflow.flow.DynamicWorkflowClientExternal
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.netflix.glisten.InterfaceBasedWorkflowClient
import com.netflix.glisten.WorkflowClientExternalToWorkflowInterfaceAdapter
import com.netflix.glisten.WorkflowClientFactory
import com.netflix.glisten.example.trip.BayAreaTripWorkflow
import spock.lang.Specification

class FlowServiceUnitSpec extends Specification {

    WorkflowClientFactory workflowClientFactory = new WorkflowClientFactory(Mock(AmazonSimpleWorkflow))
    FlowService flowService = new FlowService(workflowClientFactory: workflowClientFactory)


    def 'should get new workflow client'() {
        UserContext userContext = new UserContext(username: 'rtargaryen')
        Link link = new Link(type: EntityType.cluster, id: '123')
        DynamicWorkflowClientExternal dynamicWorkflowClient = Mock(DynamicWorkflowClientExternal)
        List<String> expectedTagList = [
                '{"link":{"type":{"name":"cluster"},"id":"123"}}',
                '{"user":{"ticket":null,"username":"rtargaryen","clientHostName":null,"clientIpAddress":null,\
"region":null,"internalAutomation":null}}'
        ]
        flowService.idService = Mock(IdService)

        when:
        def client = flowService.getNewWorkflowClient(userContext, BayAreaTripWorkflow, link)

        then:
        client instanceof InterfaceBasedWorkflowClient
        client.workflowExecution != null
        client.schedulingOptions.tagList == expectedTagList

        when:
        def workflow = client.asWorkflow(null, dynamicWorkflowClient)

        then:
        workflow instanceof WorkflowClientExternalToWorkflowInterfaceAdapter

        when:
        workflow.start('John Snow', [])

        then:
        1 * dynamicWorkflowClient.startWorkflowExecution(['John Snow', []] as Object[], _)
    }

    def 'should get an existing workflow client'() {
        expect:
        flowService.getWorkflowClient(new WorkflowExecution()) instanceof WorkflowClientExternal
    }

    def 'should get a ManualActivityCompletionClient'() {
        expect:
        flowService.getManualActivityCompletionClient('123') instanceof ManualActivityCompletionClient
    }

}
