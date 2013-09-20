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

import com.amazonaws.services.simpleworkflow.flow.DecisionContext
import com.amazonaws.services.simpleworkflow.flow.WorkflowClock
import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.core.Settable
import com.amazonaws.services.simpleworkflow.flow.worker.CurrentDecisionContext
import com.netflix.asgard.flow.example.HelloWorldActivities
import spock.lang.Specification

class SwfWorkflowSpec extends Specification {

    Workflow<HelloWorldActivities> workflow = SwfWorkflow.of(HelloWorldActivities)

    def 'should get activities'() {
        expect:
        workflow.activities instanceof AsyncCaller
    }

    def 'waitFor should construct valid Functor'() {
        boolean workWasDone = false

        when:
        Promise<String> result = workflow.waitFor(Promise.asPromise('ready')) {
            workWasDone = true
            Promise.asPromise('test')
        }

        then:
        thrown(IllegalStateException)
    }

    def 'should result wrap with Promise and only one promise'() {
        expect:
        workflow.promiseFor('test').get() == Promise.asPromise('test').get()
        workflow.promiseFor(123).get() == Promise.asPromise(123).get()
        workflow.promiseFor(Promise.asPromise('test')).get() == Promise.asPromise('test').get()
    }

    def 'doTry should construct valid TryCatchFinally'() {
        boolean workWasDone = false
        boolean catchWasDone = false
        Throwable caught = null
        boolean finallyWasDone = false

        when:
        DoTry<String> doTry = workflow.doTry {
            workWasDone = true
            Settable<String> notReady = new Settable('tried')
            notReady.ready = false
            notReady
        } withCatch { Throwable t ->
            caught = t
            catchWasDone = true
            Promise.asPromise('caught')
        } withFinally {
            finallyWasDone = true
        }

        then:
        thrown(IllegalStateException)
    }

    def 'should start timer'() {
        CurrentDecisionContext.CURRENT.set(Mock(DecisionContext))
        WorkflowClock workflowClock = Mock(WorkflowClock)
        ((SwfWorkflow) workflow).overrideDecisionContext = Mock(DecisionContext) {
            getWorkflowClock() >> workflowClock
        }

        when:
        workflow.timer(10)

        then:
        1 * workflowClock.createTimer(10)
    }

    def 'should retry'() {
        CurrentDecisionContext.CURRENT.set(Mock(DecisionContext))
        WorkflowClock workflowClock = Mock(WorkflowClock)
        ((SwfWorkflow) workflow).overrideDecisionContext = Mock(DecisionContext) {
            getWorkflowClock() >> workflowClock
        }

        when:
        workflow.retry {
            Promise.Void()
        }

        then:
        thrown(IllegalStateException)
    }
}
