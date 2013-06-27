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
import spock.lang.Specification

class TaskSpec extends Specification {

    Closure<String> wrapMessages = { "[\"java.util.ArrayList\",[${it}]]" as String }
    String message1 = '"starting task"'
    String message2 = message1 + ',"doing task"'
    String message3 = message2 + ',"finished"'

    def 'should populate Task from SWF workflow'() {
        when:
        Task actualTask = Task.fromSwf(new WorkflowExecutionDetail(executionInfo: new WorkflowExecutionInfo(
                execution: new WorkflowExecution(runId: 'abc', workflowId: 'def'),
                tagList: [
                        '{"desc":"Give it away, give it away, give it away now!"}',
                        '{"user":{"region":"US_WEST_2","internalAutomation":true}}',
                        '{"link":{"type":{"name":"cluster"},"id":"123"}}'
                ],
                startTimestamp: new Date(1372230630000)
            ),
            latestActivityTaskTimestamp: new Date(1372230634000)
        ), [
                [new Date(1372230631000), message1],
                [new Date(1372230632000), message2],
                [new Date(1372230633000), message3],
        ].collect {
            new HistoryEvent(eventTimestamp: it[0],
                    decisionTaskCompletedEventAttributes: new DecisionTaskCompletedEventAttributes(
                            executionContext: wrapMessages(it[1])))
        })

        then:
        actualTask.runId == 'abc'
        actualTask.workflowId == 'def'
        actualTask.name == 'Give it away, give it away, give it away now!'
        actualTask.userContext == UserContext.auto(Region.US_WEST_2)
        actualTask.status == 'running'
        actualTask.startTime == new Date(1372230630000)
        actualTask.updateTime == new Date(1372230634000)
        actualTask.log == [
                '2013-06-26_00:10:31 starting task',
                '2013-06-26_00:10:32 doing task',
                '2013-06-26_00:10:33 finished'
        ]
        actualTask.operation == 'finished'
        actualTask.objectId == '123'
        actualTask.objectType.name() == 'cluster' // making an EntityType from the tags makes the closures unequal
    }
}
