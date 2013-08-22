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
package com.netflix.asgard.flow

import com.amazonaws.services.simpleworkflow.flow.JsonDataConverter
import com.amazonaws.services.simpleworkflow.model.HistoryEvent
import com.google.common.collect.ImmutableList
import groovy.transform.Canonical

/**
 * This wrapper around AWS SWF Flow HistoryEvents makes it easier get information about the workflow as a whole.
 */
@Canonical
class HistoryAnalyzer {

    static HistoryAnalyzer of(List<HistoryEvent> events) {
        new HistoryAnalyzer(ImmutableList.copyOf(events))
    }

    final ImmutableList<HistoryEvent> events

    /**
     * @return workflow log taken from the history
     */
    List<LogMessage> getLogMessages() {
        int oldMessageCount = 0
        events.inject([]) { list, event ->
            String failureReason = event?.workflowExecutionFailedEventAttributes?.reason
            if (failureReason) {
                list << new LogMessage(timestamp: event.eventTimestamp, text: failureReason)
            }
            String executionContext = event?.decisionTaskCompletedEventAttributes?.executionContext
            if (executionContext) {
                List<String> messages = executionContext ? new JsonDataConverter().fromData(executionContext, List) : []
                if (oldMessageCount > messages.size()) {
                    // The message list should accumulate and only get larger. If that didn't happen then the workflow
                    // is likely nondeterministic for some reason.
                    oldMessageCount = 0
                    String msg = '-- Log Restarted (typically due to a nondeterministic workflow).'
                    list << new LogMessage(timestamp: event.eventTimestamp, text: msg)
                }
                List<String> newMessages = messages.subList(oldMessageCount, messages.size())
                oldMessageCount = messages.size()
                newMessages.each {
                    list << new LogMessage(timestamp: event.eventTimestamp, text: it)
                }
            }
            list
        }
    }

    /**
     * @return tags for the workflow
     */
    List<String> getTags() {
        HistoryEvent decisionTaskCompletedEvents = events.find { it.workflowExecutionStartedEventAttributes }
        decisionTaskCompletedEvents.workflowExecutionStartedEventAttributes.tagList
    }

    /**
     * @param event to get the elapsed time for
     * @return the number of seconds from the start of the workflow until this event occurred
     */
    long getElapsedSeconds(HistoryEvent event) {
        (event.eventTimestamp.time - events[0].eventTimestamp.time)/1000
    }
}

