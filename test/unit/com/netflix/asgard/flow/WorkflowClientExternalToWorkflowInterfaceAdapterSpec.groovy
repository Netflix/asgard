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

import com.amazonaws.services.simpleworkflow.flow.DynamicWorkflowClientExternal
import com.amazonaws.services.simpleworkflow.flow.annotations.Execute
import com.amazonaws.services.simpleworkflow.flow.annotations.GetState
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions
import com.netflix.asgard.UserContext
import groovy.transform.Canonical
import spock.lang.Specification

class WorkflowClientExternalToWorkflowInterfaceAdapterSpec extends Specification {

    def 'should construct workflow description'() {
        DynamicWorkflowClientExternal client = Mock(DynamicWorkflowClientExternal)
        def adapter = new WorkflowClientExternalToWorkflowInterfaceAdapter(client, TestWorkflow,
                new TestWorkflowDescriptionTemplate())

        when:
        adapter.go(null, 'Rhaegar', new WrappingObject(nestedName: 'Targaryen'))

        then:
        1 * client.startWorkflowExecution(_, _) >> { List<?> args ->
            assert args[1].tagList == ['{"desc":"Describe workflow for \'Rhaegar\' \'Targaryen\'"}']
        }
    }

    def 'should return execution state'() {
        def client = Mock(DynamicWorkflowClientExternal)
        def adapter = new WorkflowClientExternalToWorkflowInterfaceAdapter(client, TestWorkflow)

        when:
        adapter.getLogHistory()

        then:
        1 * client.getWorkflowExecutionState(List)
    }
}

@com.amazonaws.services.simpleworkflow.flow.annotations.Workflow
@WorkflowRegistrationOptions(defaultExecutionStartToCloseTimeoutSeconds = 60L)
interface TestWorkflow {
    @Execute(version = "1.0")
    void go(UserContext userContext, String name, WrappingObject wrappingObject)

    @GetState
    List<String> getLogHistory()
}

class TestWorkflowDescriptionTemplate extends WorkflowDescriptionTemplate implements TestWorkflow {
    void go(UserContext userContext, String name, WrappingObject wrappingObject) {
        description = "Describe workflow for '${name}' '${wrappingObject.nestedName}'"
    }
}

@Canonical
class WrappingObject {
    String nestedName
}
