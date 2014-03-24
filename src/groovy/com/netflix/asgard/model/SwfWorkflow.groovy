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

import com.amazonaws.services.simpleworkflow.flow.DynamicWorkflowClientExternal
import com.netflix.glisten.InterfaceBasedWorkflowClient
import com.netflix.glisten.WorkflowExecutionCreationCallback
import groovy.transform.Canonical

/**
 * Wraps a workflow client and tags. Ensures that WorkflowExecutionCreationCallbacks are supplied to clients before
 * executing.
 *
 * @param < T > SWF Workflow interface
 */
@Canonical
class SwfWorkflow<T> {

    final InterfaceBasedWorkflowClient<T> client
    final WorkflowExecutionCreationCallback workflowExecutionCreationCallback

    /**
     * @return workflow client as the interface for the workflow that can execute the workflow through method calls
     */
    T getClient(DynamicWorkflowClientExternal dynamicWorkflowClientExternal = null) {
        client.asWorkflow(workflowExecutionCreationCallback, dynamicWorkflowClientExternal)
    }

    /**
     * @return tags on the workflow
     */
    SwfWorkflowTags getTags() {
        (SwfWorkflowTags) client.workflowTags
    }
}
