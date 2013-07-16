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
            work()
        }
        new Settable()
    }

    @Override
    <T> DoTry<T> doTry(Promise promise, Closure<? extends Promise<T>> work) {
        new LocalDoTry(work)
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
        work()
    }

    @Override
    <T> Promise<T> retry(Closure<? extends Promise<T>> work) {
        work()
    }
}
