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
import com.amazonaws.services.simpleworkflow.flow.DecisionContextProvider
import com.amazonaws.services.simpleworkflow.flow.DecisionContextProviderImpl
import com.amazonaws.services.simpleworkflow.flow.core.Functor
import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.core.Settable
import com.amazonaws.services.simpleworkflow.flow.interceptors.AsyncExecutor
import com.amazonaws.services.simpleworkflow.flow.interceptors.AsyncRetryingExecutor
import com.amazonaws.services.simpleworkflow.flow.interceptors.AsyncRunnable
import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy
import groovy.transform.Canonical

/**
 * SWF specific implementation.
 */
@Canonical
class SwfWorkflow<A> extends Workflow<A> {

    static <T> SwfWorkflow<T> of(Class<T> activitiesType) {
        new SwfWorkflow<T>(activitiesType)
    }

    final Class<A> activitiesType

    private DecisionContext overrideDecisionContext

    @Lazy private DecisionContextProvider contextProvider = new DecisionContextProviderImpl()
    @Lazy private DecisionContext decisionContext = overrideDecisionContext?: contextProvider.getDecisionContext()

    @Override
    A getActivities() {
        AsyncCaller.of(activitiesType)
    }

    @Override
    <T> Promise<T> waitFor(Promise<?> promise, Closure<? extends Promise<T>> work) {
        new Functor([promise] as Promise[]) {
            @Override
            protected Promise doExecute() {
                work()
            }
        }
    }

    @Override
    <T> Promise<T> promiseFor(T object) {
        if (object == null) { return new Settable() }
        boolean isPromise = Promise.isAssignableFrom(object.getClass())
        isPromise ? (Promise) object : Promise.asPromise(object)
    }

    @Override
    <T> DoTry<T> doTry(Promise promise, Closure<? extends Promise<T>> work) {
        SwfDoTry.execute([promise], work)
    }

    @Override
    <T> DoTry<T> doTry(Closure<? extends Promise<T>> work) {
        doTry(Promise.Void(), work)
    }

    @Override
    Promise<Void> timer(long delaySeconds) {
        decisionContext.workflowClock.createTimer(delaySeconds)
    }

    @Override
    <T> Promise<T> retry(RetryPolicy retryPolicy, Closure<? extends Promise<T>> work) {
        AsyncExecutor executor = new AsyncRetryingExecutor(retryPolicy, decisionContext.workflowClock)
        Settable<T> result = new Settable<T>()
        executor.execute(new AsyncRunnable() {
            @Override
            public void run() throws Throwable {
                result.unchain()
                result.chain(work())
            }
        })
        result
    }

    @Override
    <T> Promise<T> retry(Closure<? extends Promise<T>> work) {
        retry(new ExponentialRetryPolicy(1L), work)
    }
}
