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
package com.netflix.asgard.flow

import com.amazonaws.services.simpleworkflow.flow.DataConverter
import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions
import com.amazonaws.services.simpleworkflow.flow.generic.GenericWorkflowClientExternal
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowType
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
class InterfaceBasedWorkflowClientSpec extends Specification {

    InterfaceBasedWorkflowClient<TestWorkflow> client
    WorkflowExecutionCreationCallback callback
    WorkflowExecution workflowExecution

    void setup() {
        WorkflowDescriptionTemplate workflowDescriptionTemplate = new TestWorkflowDescriptionTemplate()
        workflowExecution = new WorkflowExecution(workflowId: '123')
        WorkflowType workflowType = new WorkflowType()
        StartWorkflowOptions options = new StartWorkflowOptions()
        DataConverter dataConverter = Mock(DataConverter)
        GenericWorkflowClientExternal genericClient = Mock(GenericWorkflowClientExternal) {
            startWorkflow(_) >> workflowExecution
        }

        client = new InterfaceBasedWorkflowClient(TestWorkflow, workflowDescriptionTemplate, workflowExecution,
                workflowType, options, dataConverter, genericClient, new WorkflowTags())

        callback = Mock(WorkflowExecutionCreationCallback)
    }

    def 'callback should not be called when client is converted to workflow without a callback'() {

        when:
        client.asWorkflow().go('Rhaegar', new WrappingObject(nestedName: 'Targaryen'))

        then:
        0 * callback.call(_)
    }

    def 'callback should be called with WorkflowExecution object when client is converted to workflow'() {

        when:
        client.asWorkflow(callback).go('Rhaegar', new WrappingObject(nestedName: 'Targaryen'))

        then:
        1 * callback.call(workflowExecution)
    }
}
