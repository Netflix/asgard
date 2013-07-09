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
import com.google.common.collect.Lists
import com.netflix.asgard.model.SimpleDbSequenceLocator
import com.netflix.asgard.plugin.TaskFinishedListener
import java.util.concurrent.ConcurrentLinkedQueue
import org.codehaus.groovy.runtime.StackTraceUtils

/**
 * Service singleton that manages tasks that can run in the foreground and background. Running and
 * completed task lists can be retrieved. All task output is logged (to stdout for now). Running background
 * tasks can be cancelled.
 */
class TaskService {

    static transactional = false
    private static final Collection<String> NON_ALERTABLE_ERROR_CODES = ['DBInstanceAlreadyExists',
            'DuplicateLoadBalancerName', 'InvalidDBInstanceState', 'InvalidDBSnapshotState', 'InvalidGroup.Duplicate',
            'InvalidGroup.InUse', 'InvalidParameterValue', 'ValidationError']

    def awsSimpleDbService
    def emailerService
    def grailsApplication
    def pluginService

    Integer numberOfCompletedTasksToRetain = 500

    /** The location of the sequence number in SimpleDB */
    final SimpleDbSequenceLocator sequenceLocator = new SimpleDbSequenceLocator(region: Region.defaultRegion(),
            domainName: 'CLOUD_TASK_SEQUENCE', itemName: 'task_id', attributeName: 'value')

    private Queue<Task> running = new ConcurrentLinkedQueue<Task>()
    private Queue<Task> completed = new ConcurrentLinkedQueue<Task>()

    Task startTask(UserContext userContext, String name, Closure work, Link link = null) {
        Task task = newTask(userContext, name, link)
        //println "Starting task '${name}'(${work.getParameterTypes()}) from ${Thread.currentThread().name} ..."
        Thread.start("Task:${task.name}") {
            //println "Running task '${task.name}' in thread ${task.thread.name} ..."
            started(task)
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

    private String nextTaskId(UserContext userContext) {
        try {
            return awsSimpleDbService.incrementAndGetSequenceNumber(userContext, sequenceLocator)
        } catch (Exception e) {
            emailerService.sendExceptionEmail(e.toString(), e)
            return UUID.randomUUID().toString()
        }
    }

    private Task newTask(UserContext userContext, String name, Link link) {
        if (!userContext.ticket && !userContext.internalAutomation &&
                grailsApplication.config.cloud.accountName in grailsApplication.config.cloud.highRiskAccountNames) {
            String msg = grailsApplication.config.cloud.trackingTicketRequiredMessage ?: 'Tracking ticket required'
            throw new ValidationException(msg)
        }

        Task task = new Task(id: nextTaskId(userContext), userContext: userContext, name: name, status: 'starting',
                startTime: new Date(), env: grailsApplication.config.cloud.accountName, objectType: link?.type,
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
        if (task.email) {
            emailerService.sendUserEmail(task.email, task.summary, task.logAsString)
        }
        for(TaskFinishedListener taskFinishedListener in pluginService?.taskFinishedListeners) {
            try {
                taskFinishedListener.taskFinished(task)
            } catch (Exception e) {
                emailerService.sendExceptionEmail("Task finished plugin error: $e", e)
            }
        }
    }

    def getRunning = { Lists.newArrayList(running) }

    def getCompleted = { Lists.newArrayList(completed) }

    Task getTaskById(String id) {
        Task task = running.find { it.id == id }
        if (!task) { task = completed.find { it.id == id } }

        // If we hit a race condition where a task id is used before the task is running then wait and try again.
        if (!task) {
            sleep 1000
            task = running.find { it.id == id }
        }

        task
    }

    Collection<Task> getRunningTasksByObject(Link link, Region region) {
        Closure matcher = { it.objectType == link.type && it.objectId == link.id && it.userContext.region == region }
        running.findAll(matcher).sort { it.startTime }
    }

    void cancelTask(UserContext userContext, Task task) {
        try {
            task.thread.interrupt()
            task.log("Cancelled by ${userContext.clientHostName}")
            fail(task)
        } catch (CancelledException ignored) {
            // Thrown if task is cancelled while sleeping. Not an error.
        } catch (Exception e) {
            exception(task, e)
        }
    }
}
