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
package com.netflix.asgard

import com.amazonaws.services.simpleworkflow.model.DecisionTaskCompletedEventAttributes
import com.amazonaws.services.simpleworkflow.model.HistoryEvent
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionDetail
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfo
import com.netflix.asgard.flow.LogMessage
import spock.lang.Specification

class TaskSpec extends Specification {

    Closure<String> wrapMessages = { "[\"java.util.ArrayList\",[${it}]]" as String }
    String message1 = '"starting task"'
    String message2 = message1 + ',"doing task"'
    String message3 = message2 + ',"finished"'

    def 'should populate Task from SWF workflow'() {
        when:
        WorkflowExecutionInfo executionInfo = new WorkflowExecutionInfo(
                execution: new WorkflowExecution(runId: 'abc', workflowId: 'def'),
                tagList: [
                        '{"desc":"Give it away give it away give it away give it away now"}',
                        '{"user":{"region":"US_WEST_2","internalAutomation":true}}',
                        '{"link":{"type":{"name":"cluster"},"id":"123"}}'
                ],
                startTimestamp: new Date(1372230630000)
        )
        List<HistoryEvent> history = [
                [new Date(1372230631000), message1],
                [new Date(1372230632000), message2],
                [new Date(1372230633000), message3],
        ].collect {
            new HistoryEvent(eventTimestamp: it[0],
                    decisionTaskCompletedEventAttributes: new DecisionTaskCompletedEventAttributes(
                            executionContext: wrapMessages(it[1])))
        }
        Task actualTask = Task.fromSwf(new WorkflowExecutionDetail(executionInfo: executionInfo,
            latestActivityTaskTimestamp: new Date(1372230634000)
        ), history)

        then:
        actualTask.runId == 'abc'
        actualTask.workflowId == 'def'
        actualTask.name == 'Give it away give it away give it away give it away now'
        actualTask.userContext == UserContext.auto(Region.US_WEST_2)
        actualTask.status == 'running'
        actualTask.startTime == new Date(1372230630000)
        actualTask.updateTime == new Date(1372230634000)
        actualTask.log == [
                new LogMessage(new Date(1372230631000), 'starting task').toString(),
                new LogMessage(new Date(1372230632000), 'doing task').toString(),
                new LogMessage(new Date(1372230633000), 'finished').toString()
        ]
        actualTask.operation == 'finished'
        actualTask.objectId == '123'
        // Cannot simply compare two Tasks because making an EntityType from the tags makes the closures unequal
        actualTask.objectType.name() == EntityType.cluster.name()
    }
}
