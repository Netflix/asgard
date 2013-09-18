/*
 * Copyright 2012 Netflix, Inc.
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

import com.amazonaws.services.simpleworkflow.model.HistoryEvent
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfo
import com.netflix.asgard.model.SwfWorkflowTags
import com.netflix.glisten.EventAttributes
import com.netflix.glisten.HistoryAnalyzer
import com.netflix.glisten.WorkflowTags
import grails.converters.JSON
import grails.converters.XML

class WorkflowExecutionController {

    def awsSimpleWorkflowService

    def index = { redirect(action: 'list', params: params) }

    def list = {
        List<WorkflowExecutionInfo> closedExecutions =
            awsSimpleWorkflowService.closedWorkflowExecutions.sort { it.closeTimestamp }.reverse()
        List<WorkflowExecutionInfo> openExecutions =
            awsSimpleWorkflowService.openWorkflowExecutions.sort { it.startTimestamp }.reverse()
        Map result = [
                closedWorkflowExecutions: closedExecutions,
                openWorkflowExecutions: openExecutions
        ]
        withFormat {
            html { result }
            xml { new XML(result).render(response) }
            json { new JSON(result).render(response) }
        }
    }

    def show = {
        String runId = params.runId
        String workflowId = params.workflowId
        WorkflowExecution workflowExecution = new WorkflowExecution(workflowId: workflowId, runId: runId)
        List<HistoryEvent> events = awsSimpleWorkflowService.getExecutionHistory(workflowExecution)
        List<EventAttributes> eventAttributes = events.collect { new EventAttributes(it) }
        HistoryAnalyzer historyAnalyzer = HistoryAnalyzer.of(events)
        WorkflowTags tags = new SwfWorkflowTags().withTags(historyAnalyzer.tags)
        Map result = [
                events: events,
                filteredEvents: eventAttributes,
                historyAnalyzer: historyAnalyzer,
                swfWorkflowId: workflowId,
                swfRunId: runId,
                workflowDescription: tags.constructTags()
        ]
        if (!events) {
            Requests.renderNotFound('Workflow History', workflowExecution.toString(), this)
        } else {
            withFormat {
                html { result }
                xml { new XML(result).render(response) }
                json { new JSON(result).render(response) }
            }
        }
    }

}
