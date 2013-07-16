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

import com.amazonaws.services.simpleworkflow.flow.core.AndPromise
import com.amazonaws.services.simpleworkflow.flow.core.OrPromise
import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy

/**
 * Common behavior for an SWF workflow. This enables implementations that are not tied to SWF.
 */
abstract class Workflow<A> {

    final List<String> logHistory = []

    /**
     * Log a message from the workflow.
     *
     * @param message to log
     */
    void status(String message) {
        if (message) {
            logHistory << message
        }
    }

    /**
     * All promises must be ready.
     *
     * @param promises were made
     * @return a single promise that represents all original promises ANDed together
     */
    AndPromise allPromises(Promise<?>... promises) {
        new AndPromise(promises)
    }

    /**
     * Any promise can be ready.
     *
     * @param promises were made
     * @return a single promise that represents all original promises ORed together
     */
    OrPromise anyPromises(Promise<?>... promises) {
        new OrPromise(promises)
    }

    /**
     * @return used to schedule activities from the workflow
     */
    abstract A getActivities()

    /**
     * Wait for it...
     * Execute the closure once the promise is ready. No threads are made to sleep in the process.
     *
     * @param promise that must be ready before work can be done
     * @param work to do
     * @return a promised result of the work
     */
    abstract <T> Promise<T> waitFor(Promise<?> promise, Closure<? extends Promise<T>> work)

    /**
     * Using this with the results from activities avoids code generation that would change the return
     * values of activites to be Promises.
     *
     * @param value that may need to be wrapped with a promise
     * @return guaranteed to be wrapped by one and only one promise
     */
    abstract <T> Promise<T> promiseFor(T value)

    /**
     * Provides exception handling for the work.
     *
     * @param promise that must be ready before work can be done
     * @param work to do
     * @return an object for specifying exception handling
     */
    abstract <T> DoTry<T> doTry(Promise promise, Closure<? extends Promise<T>> work)

    /**
     * Provides exception handling for the work.
     *
     * @param work to do
     * @return an object for specifying exception handling
     */
    abstract <T> DoTry<T> doTry(Closure<? extends Promise<T>> work)

    /**
     * Start a timer for a specified number of seconds. No threads are made to sleep in the process.
     *
     * @param delaySeconds to wait
     * @return a Promise that will be ready when the timer is done
     */
    abstract Promise<Void> timer(long delaySeconds)

    /**
     * Provides retry handling for the work.
     *
     * @param retryPolicy allows you to describe the way retries are performed
     * @param work to do
     * @return a promised result of the work
     */
    abstract <T> Promise<T> retry(RetryPolicy retryPolicy, Closure<? extends Promise<T>> work)

    /**
     * Provides retry handling for the work. A basic exponential retry policy is assumed.
     *
     * @param work to do
     * @return a promised result of the work
     */
    abstract <T> Promise<T> retry(Closure<? extends Promise<T>> work)
}
