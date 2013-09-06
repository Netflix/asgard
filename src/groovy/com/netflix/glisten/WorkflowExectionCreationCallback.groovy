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
package com.netflix.glisten

import com.amazonaws.services.simpleworkflow.model.WorkflowExecution

/**
 * Overridable behavior to run on a WorkflowExecution immediately after creation. For example, an application may wish
 * to add the WorkflowExecution object to a cache.
 */
interface WorkflowExecutionCreationCallback {

    /**
     * Execute the desired callback logic on the WorkflowExecution object.
     *
     * @param workflowExecution the SWF workflow execution that has just been created
     */
    void call(WorkflowExecution workflowExecution)
}
