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
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionStartedEventAttributes
import spock.lang.Specification

class EventAttributesSpec extends Specification {

    EventAttributes workflowExecutionStartedEventAttributes = new EventAttributes(
            new HistoryEvent(workflowExecutionStartedEventAttributes:
                    new WorkflowExecutionStartedEventAttributes(input: 'slartibartfast'))
    )
    EventAttributes activityTaskCompletedEventAttributes = new EventAttributes(
            new HistoryEvent(activityTaskCompletedEventAttributes:
                    new ActivityTaskCompletedEventAttributes(result: '42'))
    )
    EventAttributes decisionTaskCompletedEventAttributes = new EventAttributes(
            new HistoryEvent(decisionTaskCompletedEventAttributes:
                    new DecisionTaskCompletedEventAttributes(executionContext: 'deciding'))
    )

    def 'should return the value from the populated history event attributes'() {
        expect:
        workflowExecutionStartedEventAttributes.input == 'slartibartfast'
        activityTaskCompletedEventAttributes.result == '42'
        decisionTaskCompletedEventAttributes.executionContext == 'deciding'
    }

    def 'should indicate decision history event'() {
        expect:
        !workflowExecutionStartedEventAttributes.isDecision()
        !activityTaskCompletedEventAttributes.isDecision()
        decisionTaskCompletedEventAttributes.isDecision()
    }

}
