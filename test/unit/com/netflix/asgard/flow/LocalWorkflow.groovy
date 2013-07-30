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

import com.amazonaws.services.simpleworkflow.flow.common.FlowDefaults
import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.core.Settable
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy
import groovy.transform.Canonical

/**
 * Local implementation sufficient to run unit tests without a real SWF dependency.
 */
@Canonical
class LocalWorkflow<A> extends Workflow<A> {

    final A activities

    static <T> LocalWorkflow<T> of(T activities) {
        new LocalWorkflow<T>(activities)
    }

    @Override
    <T> Promise<T> promiseFor(T object) {
        if (object == null) { return new Settable() }
        boolean isPromise = Promise.isAssignableFrom(object.getClass())
        isPromise ? (Promise) object : Promise.asPromise(object)
    }

    @Override
    <T> Promise<T> waitFor(Promise<?> promise, Closure<? extends Promise<T>> work) {
        if (promise.isReady()) {
            return work()
        }
        new Settable()
    }

    @Override
    <T> DoTry<T> doTry(Promise promise, Closure<? extends Promise<T>> work) {
        if (promise.isReady()) {
            return new LocalDoTry(work)
        }
        new LocalDoTry({ Promise.Void() })
    }

    @Override
    <T> DoTry<T> doTry(Closure<? extends Promise<T>> work) {
        new LocalDoTry(work)
    }

    @Override
    Promise<Void> timer(long delaySeconds) {
        Promise.Void()
    }

    @Override
    <T> Promise<T> retry(RetryPolicy retryPolicy, Closure<? extends Promise<T>> work) {
        int maximumAttempts = FlowDefaults.EXPONENTIAL_RETRY_MAXIMUM_ATTEMPTS
        if (retryPolicy.respondsTo('getMaximumAttempts')) {
            maximumAttempts = retryPolicy.maximumAttempts
        }
        recursingRetry(retryPolicy, work, null, maximumAttempts, 1)
    }

    private <T> Promise<T> recursingRetry(RetryPolicy retryPolicy, Closure<? extends Promise<T>> work, Throwable t1,
            int maximumAttempts, int attemptCount) {
        if (maximumAttempts > 0 && attemptCount > maximumAttempts) {
            throw t1
        }
        if (!t1 || retryPolicy.isRetryable(t1)) {
            try {
                return work()
            } catch(Throwable t2) {
                recursingRetry(retryPolicy, work, t2, maximumAttempts, attemptCount + 1)
            }
        } else {
            throw t1
        }
    }
}
