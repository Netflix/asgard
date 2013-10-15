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
package com.netflix.asgard.model

import com.amazonaws.services.simpleworkflow.model.HistoryEvent
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfo
import com.netflix.asgard.EntityType
import com.netflix.asgard.Task
import com.netflix.glisten.HistoryAnalyzer
import com.netflix.glisten.LogMessage
import groovy.transform.Canonical

/**
 * Attributes specified when dealing with workflow executions.
 */
@Canonical class WorkflowExecutionBeanOptions {

    final WorkflowExecutionInfo executionInfo
    final List<HistoryEvent> events = []

    SwfWorkflowTags getTags() {
        SwfWorkflowTags swfWorkflowTags = new SwfWorkflowTags()
        swfWorkflowTags.withTags(executionInfo.tagList)
        swfWorkflowTags
    }

    /**
     * Constructs a task based on the SWF workflow execution
     *
     * @return task that represents the workflow execution
     */
    Task asTask() {
        SwfWorkflowTags swfWorkflowTags = new SwfWorkflowTags()
        swfWorkflowTags.withTags(executionInfo.tagList)
        String status = executionInfo.closeStatus?.toLowerCase() ?: 'running'
        Task task = new Task(id: swfWorkflowTags.id, workflowExecution: executionInfo.execution,
                name: swfWorkflowTags.desc, userContext: swfWorkflowTags.user, status: status,
                startTime: executionInfo.startTimestamp, updateTime: executionInfo.closeTimestamp)
        if (swfWorkflowTags.link) {
            EntityType entityType = EntityType.fromName(swfWorkflowTags.link.type?.name())
            task.with {
                objectType = entityType
                objectId = swfWorkflowTags.link.id
            }
        }
        if (events) {
            List<LogMessage> logMessages = HistoryAnalyzer.of(events).logMessages
            boolean isDone = executionInfo.closeTimestamp != null
            String currentOperation = isDone || !logMessages ? '' : logMessages.last().text
            Date lastTime = isDone ? executionInfo.closeTimestamp : logMessages.last().timestamp
            task.with {
                log = logMessages*.toString()
                updateTime = lastTime
                operation = currentOperation
            }
        }
        task
    }

}
