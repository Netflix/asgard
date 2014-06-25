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

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal
import com.amazonaws.services.simpleworkflow.model.ChildPolicy
import com.google.common.collect.Lists
import com.netflix.asgard.model.SimpleDbSequenceLocator
import com.netflix.asgard.model.WorkflowExecutionBeanOptions
import com.netflix.asgard.plugin.TaskFinishedListener
import java.rmi.RemoteException
import java.util.concurrent.ConcurrentLinkedQueue
import org.apache.http.HttpStatus
import org.codehaus.groovy.runtime.StackTraceUtils

/**
 * Service singleton that manages tasks that can run in the foreground and background. Running and
 * completed task lists can be retrieved. All task output is logged (to stdout for now). Running background
 * tasks can be cancelled.
 */
class TaskService {

    static transactional = false
    private static final Collection<String> NON_ALERTABLE_ERROR_CODES = ['DBInstanceAlreadyExists',
            'DuplicateLoadBalancerName', 'InvalidChangeBatch', 'InvalidDBInstanceState', 'InvalidDBSnapshotState',
            'InvalidGroup.Duplicate', 'InvalidGroup.InUse', 'InvalidInput',  'InvalidParameterValue', 'ValidationError']

    Caches caches
    def awsSimpleWorkflowService
    def configService
    def emailerService
    def environmentService
    def flowService
    def idService
    def grailsApplication
    def objectMapper
    def pluginService
    def restClientService
    def serverService

    Integer numberOfCompletedTasksToRetain = 500

    private Queue<Task> running = new ConcurrentLinkedQueue<Task>()
    private Queue<Task> completed = new ConcurrentLinkedQueue<Task>()

    Task startTask(UserContext userContext, String name, Closure work, Link link = null) {
        Task task = newTask(userContext, name, link)
        log.debug "Starting task '${name}'(${work.getParameterTypes()}) from ${Thread.currentThread().name} ..."
        Thread.start("Task:${task.name}") {
            started(task)
            log.debug "Running task '${task.name}' in thread ${task.thread.name} ..."
            doWork(work, task)
            if (task.status != 'failed') {
                completed(task)
            }
        }
        task
    }

    /** Creates or continues a task to perform work and runs it in this thread. Exceptions are logged & propagated. */
    def runTask(UserContext userContext, String name, Closure work, Link link = null, Task existingTask = null) {
        if (existingTask) {
            existingTask.log(name)
            return doWork(work, existingTask)
        } else {
            Task task = newTask(userContext, name, link)
            def result = doWork(work, task)
            if (task.status != 'failed') {
                completed(task)
            }
            return result
        }
    }

    /** Performs and tracks work in an existing Task object. */
    def doWork(Closure work, Task task) {
        try {
            return work(task)
        } catch (CancelledException ignored) {
            // Thrown if task is cancelled while sleeping. Not an error.
        } catch (Exception e) {
            if (task.status != 'failed' && task.name) {
                // Tasks can be nested. We only want to capture the failure once.
                // Unnamed tasks should not be marked completed. They are useful when you need to reuse code based on
                // tasks without actually using the task system (like in an AWS SWF workflow with its own task system).
                exception(task, e)
            }
            throw e
        }
    }

    private Task newTask(UserContext userContext, String name, Link link) {
        if (!userContext.ticket && !userContext.internalAutomation &&
                grailsApplication.config.cloud.accountName in grailsApplication.config.cloud.highRiskAccountNames) {
            String msg = grailsApplication.config.cloud.trackingTicketRequiredMessage ?: 'Tracking ticket required'
            throw new ValidationException(msg)
        }
        String id = idService.nextId(userContext, SimpleDbSequenceLocator.Task)
        Date date = environmentService.currentDate
        Task task = new Task(id: id, userContext: userContext, name: name, status: 'starting',
                startTime: date, env: grailsApplication.config.cloud.accountName, objectType: link?.type,
                objectId: link?.id
        )
        return task
    }

    private void started(Task task) {
        task.thread = Thread.currentThread()
        task.log("Started on thread ${task.thread.name}.")
        task.status = 'running'
        running << task
    }

    private void completed(Task task) {
        task.log("Completed in ${task.durationString}.")
        task.status = 'completed'
        finish(task)
    }

    private void exception(Task task, Exception e) {
        String msg = e.toString()
        Throwable cause = e.cause
        Integer nestingLevel = 0
        while (cause != null && nestingLevel < 6) {
            msg += " caused by ${cause}"
            cause = cause.cause
            nestingLevel++
        }
        log.error(msg, StackTraceUtils.sanitize(e))
        String debugData = msg
        if (task) {
            debugData = "${task.summary}\n${task.logAsString}"
            task.log("Exception: ${msg}")
            fail(task)
        }
        if (deservesSystemAlert(e)) {
            emailerService.sendExceptionEmail(debugData, e)
        }
    }

    private Boolean deservesSystemAlert(Exception e) {
        if (e instanceof AmazonServiceException && e.errorCode in NON_ALERTABLE_ERROR_CODES) {
            return false
        }
        return !(e instanceof NonAlertable)
    }

    private void fail(Task task) {
        task.status = 'failed'
        finish(task)
    }

    private void finish(Task task) {
        task.operation = ''
        running.remove(task)
        completed.add(task)
        // Keep the completed collection fairly short
        if (completed.size() > numberOfCompletedTasksToRetain) {
            completed.poll()
        }
        for (TaskFinishedListener taskFinishedListener in pluginService?.taskFinishedListeners) {
            try {
                taskFinishedListener.taskFinished(task)
            } catch (Exception e) {
                String className = taskFinishedListener.getClass().simpleName
                log.error "Task finished listener ${className} failed for task ${task.id} ${task.summary}", e
                emailerService.sendExceptionEmail("Task finished plugin error: $e", e)
            }
        }
    }

    /**
     * @return only in-memory running tasks on the local system (no SWF workflow executions)
     */
    List<Task> getLocalRunningInMemory() {
        Lists.newArrayList(running)
    }

    /**
     * @return all the running in-memory tasks from all the other remote Asgard machines
     */
    List<Task> getRemoteRunningInMemory() {
        serverService.listRemoteServerNamesAndPorts().collect {
            String url = "http://${it}/task/runningInMemory.json"
            String json = restClientService.getJsonAsText(url)
            log.debug "Remote tasks from ${url}: ${json}"
            if (json) {
                return objectMapper.reader(Task).readValues(json) as List<Task>
            }
            []
        }.flatten() as List<Task>
    }

    /**
     * @return a single concatenated list of all locally cached running tasks (without tasks on other Asgard servers)
     */
    List<Task> getAllRunningInCache() {
        localRunningInMemory
    }

    /**
     * @return a list of all running tasks from the AWS Simple Workflow Service
     */
    List<Task> getAllRunningInSwf() {
        awsSimpleWorkflowService.openWorkflowExecutions.collect {
            new WorkflowExecutionBeanOptions(it).asTask()
        }
    }

    /**
     * @return a single concatenated list of all in-memory running tasks from the local and all the remote servers
     */
    List<Task> getAllRunningInMemory() {
        localRunningInMemory + remoteRunningInMemory
    }

    /**
     * @return all running tasks including local and remote in-memory tasks, and cached SWF workflow executions
     */
    Collection<Task> getAllRunning() {
        allRunningInMemory + allRunningInSwf
    }

    /**
     * @return all completed tasks (including SWF workflow executions within a recent time period)
     */
    Collection<Task> getAllCompleted() {
        completed + awsSimpleWorkflowService.closedWorkflowExecutions.collect {
            new WorkflowExecutionBeanOptions(it).asTask()
        }
    }

    /**
     * Looks up a task by its ID.
     *
     * @param id for task
     * @return task or null if no task was found
     */
    Task getTaskById(String id) {
        if (!id) { return null }
        try {
            new Retriable<Task>(
                    work: {
                        Task task = getLocalTaskById(id) ?: getWorkflowTaskById(id) ?: getRemoteTaskById(id)
                        if (!task) { throw new IllegalArgumentException("There is no task with id ${id}.") }
                        task
                    },
                    firstDelayMillis: 300
            ).performWithRetries()
        } catch (CollectedExceptions ignore) {
            return null
        }
    }

    /**
     * Looks up a task from the workflow executions in Amazon Simple Workflow.
     *
     * @param id the unique ID of the task to find
     * @return a matching Task for an SWF workflow execution, or null if none found
     */
    Task getWorkflowTaskById(String id) {
        if (!id) { return null }
        awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId(id)?.asTask()
    }

    /**
     * Looks up a local in-memory task by its ID, either in the collection of running tasks or completed tasks.
     *
     * @param id the unique ID of the task to find
     * @return a matching Task that is running in-memory in the local JVM machine, or null if none found
     */
    Task getLocalTaskById(String id) {
        if (!id) { return null }
        running.find { it.id == id } ?: completed.find { it.id == id }
    }

    /**
     * Looks for a matching Task on all remote systems and returns the first one found, or null if none found.
     *
     * @param id the unique ID of the task to find
     * @return a matching Task that is running in-memory on a remote machine, or null if none found
     */
    Task getRemoteTaskById(String id) {
        if (!id) { return null }
        serverService.listRemoteServerNamesAndPorts().findResult {
            String url = "http://${it}/task/runningInMemory/${id}.json"
            String json = restClientService.getJsonAsText(url)
            log.debug "Remote task from ${url}: ${json}"
            if (json) {
                Task task = objectMapper.reader(com.netflix.asgard.Task).readValue(json) as Task
                task.server = it
                return task
            }
            null
        } as Task
    }

    Collection<Task> getRunningTasksByObject(Link link, Region region) {
        Closure matcher = { it.objectType == link.type && it.objectId == link.id && it.userContext.region == region }
        // This is incomplete because it should include tasks running on other Asgard instances.
        // The cluster screen uses this, so changing it to call remote Asgard instances would impact performance of the
        // cluster screen. To solve the performance problem of the remote Asgard instances, either cache the running
        // tasks of remote Asgards, or refactor all long-running tasks into SWF workflows so the need to call remote
        // Asgards for in-memory task lists goes away.
        allRunningInCache.findAll(matcher).sort { it.startTime }
    }

    /**
     * Interrupts and stops a running task, be it local in-memory, remote in-memory, or a workflow execution in
     * Amazon Simple Workflow Service
     *
     * @param userContext who, where, why
     * @param task the task to cancel
     */
    void cancelTask(UserContext userContext, Task task) {
        String cancelledByMessage = "Cancelled by ${userContext.username ?: 'user'}@${userContext.clientHostName}"
        if (task.workflowExecution) {
            WorkflowClientExternal client = flowService.getWorkflowClient(task.workflowExecution)
            client.terminateWorkflowExecution(cancelledByMessage, task.toString(), ChildPolicy.TERMINATE)
        } else if (task.server) {
            String url = "http://${task.server}/task/cancel"
            int statusCode = restClientService.post(url, [id: task.id, format: 'json'])
            if (statusCode != HttpStatus.SC_OK) {
                String msg = "Error trying to cancel remote task ${task.id} '${task.name}' on '${task.server}' " +
                        "Response status code was ${statusCode}"
                throw new RemoteException(msg)
            }
        } else if (task.thread) {
            try {
                task.thread.interrupt()
                task.log(cancelledByMessage, environmentService.currentDate)
                fail(task)
            } catch (CancelledException ignored) {
                // Thrown if task is cancelled while sleeping. Not an error.
            } catch (Exception e) {
                exception(task, e)
            }
        } else {
            String msg = "ERROR: Unable to cancel task lacking workflowExecution, thread, or server of origin: ${task}"
            throw new IllegalStateException(msg)
        }
    }
}
