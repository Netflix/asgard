/*
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.asgard.model

import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.netflix.glisten.InterfaceBasedWorkflowClient
import com.netflix.glisten.WorkflowExecutionCreationCallback
import spock.lang.Specification

class SwfWorkflowSpec extends Specification {

    InterfaceBasedWorkflowClient mockInterfaceBasedWorkflowClient = Mock(InterfaceBasedWorkflowClient)
    WorkflowExecutionCreationCallback callback = new WorkflowExecutionCreationCallback() {
        @Override
        void call(WorkflowExecution workflowExecution) { }
    }
    SwfWorkflow swfWorkflow = new SwfWorkflow(mockInterfaceBasedWorkflowClient, callback)

    void 'should construct client workflow'() {
        when:
        swfWorkflow.getClient()

        then:
        1 * mockInterfaceBasedWorkflowClient.asWorkflow(callback, null)
    }
}
