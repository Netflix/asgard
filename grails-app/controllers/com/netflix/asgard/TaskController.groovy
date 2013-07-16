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

import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal
import com.amazonaws.services.simpleworkflow.model.ChildPolicy
import com.amazonaws.services.simpleworkflow.model.HistoryEvent
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionDetail
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfo
import grails.converters.JSON
import grails.converters.XML

class TaskController {

    AwsSimpleWorkflowService awsSimpleWorkflowService
    FlowService flowService
    def taskService

    // the delete, save and update actions only accept POST requests
    def static allowedMethods = [cancel:'POST']

    def index = { redirect(action: 'list', params:params) }

    def list = {
        Collection<WorkflowExecutionInfo> openExecutions = awsSimpleWorkflowService.openWorkflowExecutions
        Collection<WorkflowExecutionInfo> closedExecutions = awsSimpleWorkflowService.closedWorkflowExecutions

        Collection<Task> runningTasks = taskService.getRunning() + openExecutions.collect { Task.fromSwf(it) }
        Collection<Task> completedTasks = taskService.getCompleted() + closedExecutions.collect { Task.fromSwf(it) }

        List<Task> running = runningTasks.sort { it.startTime }.reverse()
        List<Task> completed = completedTasks.sort { it.updateTime }.reverse().take(100)

        String query = params.query ?: params.id
        if (query) {
            running = running.findAll { Task t -> t.log.any { it.contains(query) } }
            completed = completed.findAll { Task t -> t.log.any { it.contains(query) } }
        }

        def tasks = [ 'runningTaskList' : running,
                      'completedTaskList' : completed ]
        withFormat {
            html { tasks }
            xml { new XML(tasks).render(response) }
            json { new JSON(tasks).render(response) }
        }
    }

    def show = {
        Task task
        if (params.id) {
            String id = params.id
            task = taskService.getTaskById(id)
        } else {
            String runId = params.runId
            String workflowId = params.workflowId
            WorkflowExecution workflowExecution = new WorkflowExecution(runId: runId, workflowId: workflowId)
            WorkflowExecutionDetail workflowExecutionDetail = awsSimpleWorkflowService.
                    getWorkflowExecutionDetail(workflowExecution)
            List<HistoryEvent> events = awsSimpleWorkflowService.getExecutionHistory(workflowExecution)
            task = Task.fromSwf(workflowExecutionDetail, events)
        }
        String updateTime = task.updateTime ? Time.format(task.updateTime) : ''
        if (!task) {
            Requests.renderNotFound('Task', id, this)
            return
        } else {
            withFormat {
                html { return [ 'task' : task ] }
                xml { new XML(task).render(response) }
                json {
                    def simpleTask = [
                            log: task.log,
                            status: task.status,
                            operation: task.operation,
                            durationString: task.durationString,
                            updateTime: updateTime
                    ]
                    render(simpleTask as JSON)
                }
            }
        }
    }

    def cancel = {
        Task task = null
        if (params.id) {
            String id = params.id
            UserContext userContext = UserContext.of(request)
            task = taskService.getTaskById(id)
            if (!task) {
                Requests.renderNotFound('Task', "${id}", this)
                return
            } else {
                taskService.cancelTask(userContext, task)
                flash.message = "Task '${id}:${task.name}' canceled."
            }
            if (task.objectId && task.objectType) {
                redirect(controller: task.objectType.name(), action: 'show', params: [id: task.objectId])
            } else {
                redirect(action: 'list')
            }
        } else {
            String runId = params.runId
            String workflowId = params.workflowId
            WorkflowExecution workflowExecution = new WorkflowExecution(runId: runId, workflowId: workflowId)
            WorkflowExecutionDetail workflowExecutionDetail = awsSimpleWorkflowService.
                    getWorkflowExecutionDetail(workflowExecution)
            task = Task.fromSwf(workflowExecutionDetail)
            WorkflowClientExternal client = flowService.getWorkflowClient(workflowExecution)
            client.terminateWorkflowExecution('Canceled by user.', task.toString(), ChildPolicy.TERMINATE)
            flash.message = "Task '${task.name}' canceled."
            redirect(action: 'show', params: [runId: runId, workflowId: workflowId])
        }
    }

    def runningCount = {
        render '' + taskService.getRunning().size()
    }
}
