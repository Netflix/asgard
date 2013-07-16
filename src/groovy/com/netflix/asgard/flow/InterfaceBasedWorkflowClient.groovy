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
import com.amazonaws.services.simpleworkflow.flow.DynamicWorkflowClientExternal
import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternalBase
import com.amazonaws.services.simpleworkflow.flow.generic.GenericWorkflowClientExternal
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowType

/**
 * The SWF Flow way to create WorkflowClientExternalToWorkflowInterfaceAdapters.
 *
 * @param < T > Type of SWF workflow (the java interface with AWS annotations that the workflow implements)
 * @see WorkflowClientExternalToWorkflowInterfaceAdapter
 */
class InterfaceBasedWorkflowClient<T> extends WorkflowClientExternalBase {

    final Class<T> workflowInterface
    final WorkflowDescriptionTemplate workflowDescriptionTemplate
    final WorkflowTags workflowTags

    InterfaceBasedWorkflowClient(Class<T> workflowInterface, WorkflowDescriptionTemplate workflowDescriptionTemplate,
            WorkflowExecution workflowExecution, WorkflowType workflowType, StartWorkflowOptions options,
            DataConverter dataConverter, GenericWorkflowClientExternal genericClient, workflowTags = new WorkflowTags()) {
        super(workflowExecution, workflowType, options, dataConverter, genericClient)
        this.workflowInterface = workflowInterface
        this.workflowDescriptionTemplate = workflowDescriptionTemplate
        this.workflowTags = workflowTags
    }

    /**
     * @param client override for specifying an alternate client (useful during testing)
     * @return an SWF workflow client that looks like the workflow interface
     */
    public T asWorkflow(DynamicWorkflowClientExternal client = null) {
        new WorkflowClientExternalToWorkflowInterfaceAdapter(client ?: dynamicWorkflowClient, workflowInterface,
                workflowDescriptionTemplate, workflowTags) as T
    }
}
