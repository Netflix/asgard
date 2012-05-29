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

import grails.converters.JSON
import grails.converters.XML

@RegionAgnostic class TaskController {

    def taskService

    // the delete, save and update actions only accept POST requests
    def static allowedMethods = [cancel:'POST']

    def index = { redirect(action:list, params:params) }

    def list = {
        Collection<Task> running = taskService.getRunning().reverse()
        Collection<Task> completed = taskService.getCompleted().reverse()

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
        String id = params.id
        Task task = taskService.getTaskById(id)
        if (!task) {
            Requests.renderNotFound('Task', id, this)
            return
        } else {
            withFormat {
                html { return [ 'task' : task ] }
                xml { new XML(task).render(response) }
                json {
                    def simpleTask = [
                            log:task.log,
                            status: task.status,
                            operation: task.operation,
                            durationString: task.durationString,
                            updateTime: Time.format(task.updateTime)
                    ]
                    render(simpleTask as JSON)
                }
            }
        }
    }

    def cancel = {
        String id = params.id
        UserContext userContext = UserContext.of(request)
        Task task = taskService.getTaskById(id)
        if (!task) {
            Requests.renderNotFound('Task', "${id}", this)
            return
        } else {
            taskService.cancelTask(userContext, task)
            flash.message = "Task '${id}:${task.name}' canceled."
        }

        if (task.objectId && task.objectType) {
            redirect(controller: task.objectType.name(), action: show, params: [id: task.objectId])
        } else {
            redirect(action:list)
        }
    }

    def runningCount = {
        render "" + taskService.getRunning().size()
    }
}
