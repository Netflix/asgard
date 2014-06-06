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

import com.amazonaws.services.simpleworkflow.flow.JsonDataConverter
import com.amazonaws.services.simpleworkflow.model.HistoryEvent
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfo
import com.netflix.asgard.EntityType
import com.netflix.asgard.Region
import com.netflix.asgard.Task
import com.netflix.asgard.deployment.DeploymentWorkflowOptions
import com.netflix.asgard.deployment.steps.DeploymentStep
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
            Date lastUpdate = logMessages ? logMessages.last().timestamp : executionInfo.startTimestamp
            Date lastTime = isDone ? executionInfo.closeTimestamp : lastUpdate
            task.with {
                log = logMessages*.toString()
                updateTime = lastTime
                operation = currentOperation
            }
        }
        task
    }

    /**
     * Constructs a deployment based on the SWF workflow execution
     *
     * @return deployment that represents the workflow execution
     */
    Deployment asDeployment() {
        List<DeploymentStep> steps = []
        def input = getInput()
        if (input) {
            DeploymentWorkflowOptions deploymentWorkflowOptions = input[1] // get the second argument to the workflow
            steps = deploymentWorkflowOptions.steps
        }
        SwfWorkflowTags swfWorkflowTags = new SwfWorkflowTags()
        swfWorkflowTags.withTags(executionInfo.tagList)
        String status = executionInfo.closeStatus?.toLowerCase() ?: 'running'
        List<LogMessage> logMessages = events ? HistoryAnalyzer.of(events).logMessages : []
        Date lastUpdate = logMessages ? logMessages.last().timestamp : executionInfo.startTimestamp
        Date lastTime = executionInfo.closeStatus ? executionInfo.closeTimestamp : lastUpdate
        String clusterName = swfWorkflowTags.link?.id
        Region region = swfWorkflowTags?.user?.region
        new Deployment(swfWorkflowTags.id, clusterName, region, executionInfo.execution, swfWorkflowTags.desc,
                swfWorkflowTags?.user?.username, executionInfo.startTimestamp, lastTime, status,
                logMessages*.toString(), steps)
    }

    private Object getInput() {
        if (!events) { return [] }
        JsonDataConverter dataConverter = new JsonDataConverter()
        String workflowInputString = events[0].workflowExecutionStartedEventAttributes?.input
        workflowInputString ? dataConverter.fromData(workflowInputString, Object) : null
    }

}
