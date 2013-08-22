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

import com.amazonaws.services.simpleworkflow.model.ActivityTaskCompletedEventAttributes
import com.amazonaws.services.simpleworkflow.model.DecisionTaskCompletedEventAttributes
import com.amazonaws.services.simpleworkflow.model.HistoryEvent
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionFailedEventAttributes
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionStartedEventAttributes
import spock.lang.Specification

class HistoryAnalyzerSpec extends Specification {

    long time = new Date().time
    Closure<Long> incrementTimeBySeconds = { time + (it * 1000) }
    Closure<String> wrapMessages = { "[\"java.util.ArrayList\",[${it}]]" as String }
    String message1 = '"starting task","here1","here2","pre clusterNames received","here3","here4","here5"'
    String message2 = message1 + ',"hello received1","hello received2","clusterNames received","nap finished"'
    String message3 = message2 + ',"retry done"'

    def 'should clear old logs if log history changes'() {

        String alternateHistory = message1 + ',"clusterNames received","retry done"'

        when:
        HistoryAnalyzer historyAnalyzer = HistoryAnalyzer.of([
                [0, message1],
                [4, message2],
                [9, alternateHistory],
        ].collect {
            new HistoryEvent(eventTimestamp: new Date(incrementTimeBySeconds(it[0])),
                    decisionTaskCompletedEventAttributes: new DecisionTaskCompletedEventAttributes(
                            executionContext: wrapMessages(it[1])))
        })

        then:
        historyAnalyzer.logMessages == [
                [0, "starting task"],
                [0, "here1"],
                [0, "here2"],
                [0, "pre clusterNames received"],
                [0, "here3"],
                [0, "here4"],
                [0, "here5"],
                [4, "hello received1"],
                [4, "hello received2"],
                [4, "clusterNames received"],
                [4, "nap finished"],
                [9, '-- Log Restarted (typically due to a nondeterministic workflow).'],
                [9, "starting task"],
                [9, "here1"],
                [9, "here2"],
                [9, "pre clusterNames received"],
                [9, "here3"],
                [9, "here4"],
                [9, "here5"],
                [9, "clusterNames received"],
                [9, "retry done"],
        ].collect {
            new LogMessage(timestamp: new Date(incrementTimeBySeconds(it[0])), text: it[1])
        }
    }

    def 'should show a workflow execution failure'() {
        List<HistoryEvent> events = [
                [0, message1],
                [4, message2],
        ].collect {
            new HistoryEvent(eventTimestamp: new Date(incrementTimeBySeconds(it[0])),
                    decisionTaskCompletedEventAttributes: new DecisionTaskCompletedEventAttributes(
                            executionContext: wrapMessages(it[1])))
        }
        events << new HistoryEvent(eventTimestamp: new Date(incrementTimeBySeconds(11)),
                workflowExecutionFailedEventAttributes: new WorkflowExecutionFailedEventAttributes(
                        reason: 'Something went horribly wrong!')
        )

        when:
        HistoryAnalyzer historyAnalyzer = HistoryAnalyzer.of(events)

        then:
        historyAnalyzer.logMessages == [
                [0, "starting task"],
                [0, "here1"],
                [0, "here2"],
                [0, "pre clusterNames received"],
                [0, "here3"],
                [0, "here4"],
                [0, "here5"],
                [4, "hello received1"],
                [4, "hello received2"],
                [4, "clusterNames received"],
                [4, "nap finished"],
                [11, 'Something went horribly wrong!'],
        ].collect {
            new LogMessage(timestamp: new Date(incrementTimeBySeconds(it[0])), text: it[1])
        }
    }

    def 'should construct log from history'() {

        when:
        HistoryAnalyzer historyAnalyzer = HistoryAnalyzer.of([
                [0, message1],
                [4, message2],
                [4, message2],
                [5, message2],
                [5, message2],
                [6, message2],
                [8, message2],
                [8, message2],
                [9, message3],
                [9, message3],
        ].collect {
            new HistoryEvent(eventTimestamp: new Date(incrementTimeBySeconds(it[0])),
                    decisionTaskCompletedEventAttributes: new DecisionTaskCompletedEventAttributes(
                            executionContext: wrapMessages(it[1])))
        } )

        then:
        historyAnalyzer.logMessages == [
                [0, "starting task"],
                [0, "here1"],
                [0, "here2"],
                [0, "pre clusterNames received"],
                [0, "here3"],
                [0, "here4"],
                [0, "here5"],
                [4, "hello received1"],
                [4, "hello received2"],
                [4, "clusterNames received"],
                [4, "nap finished"],
                [9, "retry done"],
        ].collect {
            new LogMessage(timestamp: new Date(incrementTimeBySeconds(it[0])), text: it[1])
        }
    }

    def 'should return elapsed time of events'() {
        HistoryEvent workflowExecutionStartedEvent = new HistoryEvent(workflowExecutionStartedEventAttributes:
                new WorkflowExecutionStartedEventAttributes(input: 'slartibartfast'), eventTimestamp: new Date(100000))
        HistoryEvent activityTaskCompletedEvent = new HistoryEvent(activityTaskCompletedEventAttributes:
                new ActivityTaskCompletedEventAttributes(result: '42'), eventTimestamp: new Date(103000))
        HistoryEvent decisionTaskCompletedEvent = new HistoryEvent(decisionTaskCompletedEventAttributes:
                new DecisionTaskCompletedEventAttributes(executionContext: 'deciding'),
                eventTimestamp: new Date(107000))
        HistoryAnalyzer historyAnalyzer = HistoryAnalyzer.of([workflowExecutionStartedEvent,
                activityTaskCompletedEvent, decisionTaskCompletedEvent])

        expect:
        historyAnalyzer.getElapsedSeconds(workflowExecutionStartedEvent) == 0
        historyAnalyzer.getElapsedSeconds(activityTaskCompletedEvent) == 3
        historyAnalyzer.getElapsedSeconds(decisionTaskCompletedEvent) == 7
    }

    def 'should return the workflow description'() {
        HistoryEvent workflowExecutionStartedEvent = new HistoryEvent(workflowExecutionStartedEventAttributes:
                new WorkflowExecutionStartedEventAttributes(input: 'slartibartfast', tagList: [
                        'hitchhiker', 'guide', 'galaxy'
                ]))
        HistoryEvent activityTaskCompletedEvent = new HistoryEvent(activityTaskCompletedEventAttributes:
                new ActivityTaskCompletedEventAttributes(result: '42'))
        HistoryAnalyzer historyAnalyzer = HistoryAnalyzer.of([workflowExecutionStartedEvent,
                activityTaskCompletedEvent])

        expect:
        historyAnalyzer.tags == ['hitchhiker', 'guide', 'galaxy']
    }
}
