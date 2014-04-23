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

import com.fasterxml.jackson.databind.ObjectMapper
import grails.converters.JSON
import grails.converters.XML
import org.apache.http.HttpStatus

class TaskController {

    def taskService
    ObjectMapper objectMapper

    static allowedMethods = [cancel: 'POST']

    def index() {
        redirect(action: 'list', params: params)
    }

    def list() {
        Collection<Task> runningTasks = taskService.getAllRunning()
        Collection<Task> completedTasks = taskService.getAllCompleted()

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

    def show() {
        String id = params.id
        Task task = taskService.getTaskById(id)
        if (!task) {
            Requests.renderNotFound('Task', id, this)
        } else {
            String updateTime = task.updateTime ? Time.format(task.updateTime) : ''
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

    def cancel(String id) {
        UserContext userContext = UserContext.of(request)
        Task task = taskService.getTaskById(id)
        if (!task) {
            Requests.renderNotFound('Task', "${id}", this)
            return
        }
        taskService.cancelTask(userContext, task)
        String message = "Task '${id}:${task.name}' canceled."
        withFormat {
            //noinspection GroovyAssignabilityCheck
            form {
                flash.message = message
                Map redirectParams
                if (task.objectId && task.objectType) {
                    redirectParams = [controller: task.objectType.name(), action: 'show', params: [id: task.objectId]]
                } else {
                    redirectParams = [action: 'list']
                }
                redirect(redirectParams)
            }
            xml { render(contentType: 'application/xml', { result(message) }) }
            json { render(contentType: 'application/json', { [result: message] }) }
        }
    }

    def runningCount() {
        render taskService.getLocalRunningInMemory().size().toString()
    }

    /**
     * This JSON-only output method is intended for use by other Asgard instances that need to look up running in-memory
     * tasks of other servers in the same cluster.
     *
     * @param id if specified, JSON for a single task will be returned, or a 404 if not found; otherwise a JSON array of
     *          all Task objects will be returned
     */
    def runningInMemory(String id) {
        request.withFormat {
            json {
                def result
                if (id) {
                    result = taskService.getLocalTaskById(id)
                    if (!result) {
                        response.status = HttpStatus.SC_NOT_FOUND
                    }
                } else {
                    result = taskService.localRunningInMemory
                }
                render objectMapper.writer().writeValueAsString(result)
            }
        }
    }
}
