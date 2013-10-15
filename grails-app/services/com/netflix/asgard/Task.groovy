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

import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import groovy.transform.ToString
import java.util.concurrent.CopyOnWriteArrayList
import org.apache.commons.logging.LogFactory
import org.joda.time.DateTime

@ToString class Task {
    private static final logger = LogFactory.getLog(this)

    String id
    WorkflowExecution workflowExecution
    UserContext userContext
    String env
    String name
    Thread thread
    String status
    Date startTime
    Date updateTime
    String email
    String operation
    EntityType objectType
    String objectId
    List<String> log = new CopyOnWriteArrayList<String>()

    def log(String op) {
        updateTime = new Date()
        operation = op
        def updateTimeString = updateTime.format("yyyy-MM-dd_HH:mm:ss")
        log << updateTimeString + ' ' + op
        logger.info "${updateTimeString} ${id}: {Ticket: ${userContext?.ticket?.trim()}} " +
                "{User: ${userContext?.username}} " +
                "{Client: ${userContext?.clientHostName} ${userContext?.clientIpAddress}} " +
                "{Region: ${userContext?.region}} [${name}] ${operation}"
    }

    def getDurationString() {
        DateTime endTime = isDone() ? new DateTime(updateTime) : Time.now()
        Time.format(new DateTime(startTime), endTime)
    }

    String getSummary() {
        "Asgard task ${status} in ${env} ${userContext?.region} by " +
                "${userContext?.username ?: userContext?.clientHostName}: ${name}"
    }

    String getLogAsString() {
        log.join("\n")
    }

    Boolean isDone() {
        'completed'.equalsIgnoreCase(status) || 'failed'.equalsIgnoreCase(status) || 'TIMED_OUT'.
                equalsIgnoreCase(status)
    }

    /**
     * Performs an operation that is known to fail sporadically. Do the operation repeatedly if necessary, with
     * exponential backoff until the operation succeeds or has failed too many times.
     *
     * @param operation a closure containing the steps that need to be executed
     * @param shouldRetryException a closure that takes an Exception and determines whether to retry
     * @param initialSleepMillis the interval between the first two tries in milliseconds, to be doubled after each
     *          failed attempt
     * @param maxRetries the maximum number of times to try the operation
     */
    void tryUntilSuccessful(Closure operation,
                            Closure shouldRetryException = { Exception e -> true }, // Retry any exception by default
                            Integer initialSleepMillis = 64,
                            Integer maxRetries = 10) {
        Integer sleepMillis = initialSleepMillis
        boolean done = false
        int attemptNumber = 1
        while (!done) {
            try {
                operation.call()
                done = true
            } catch (Exception e) {
                if (attemptNumber >= maxRetries) {
                    log "Operation failed ${maxRetries} times. Giving up. Error from last attempt: ${e}"
                    throw e
                } else if (shouldRetryException.call(e)) {
                    String time = sleepMillis < 10000 ? "${sleepMillis} milliseconds" : "${sleepMillis / 1000} seconds"
                    log "Attempt ${attemptNumber} failed. Waiting ${time} before retry after error: ${e}"
                    Time.sleepCancellably(sleepMillis)
                    sleepMillis *= 2
                } else {
                    throw e
                }
            }
            attemptNumber++
        }
    }

}
