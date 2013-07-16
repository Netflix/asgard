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

import com.amazonaws.services.simpleworkflow.flow.ActivityExecutionContext
import com.amazonaws.services.simpleworkflow.flow.ActivityExecutionContextProvider
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import spock.lang.Specification

class SwfActivitySpec extends Specification {

    SwfActivity activity = new SwfActivity()
    ActivityExecutionContext context = Mock(ActivityExecutionContext) {
        getTaskToken() >> '123'
        getWorkflowExecution() >> new WorkflowExecution(runId: 'abc', workflowId: 'def')
    }

    def setup() {
        activity.provider = Mock(ActivityExecutionContextProvider) {
            getActivityExecutionContext() >> { context }
        }
    }

    def 'should record heartbeat'() {
        when:
        activity.recordHeartbeat('still going...')

        then:
        1 * context.recordActivityHeartbeat('still going...')
    }

    def 'should get task token'() {
        expect:
        activity.taskToken == '123'
    }

    def 'should get workflow execution'() {
        expect:
        activity.workflowExecution == new WorkflowExecution(runId: 'abc', workflowId: 'def')
    }
}
