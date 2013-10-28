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

import groovy.transform.Canonical

/**
 * Mechanism for retrying actions multiple times, with customized error handling behavior.
 *
 * @param < T > the return type of the work to be done
 */
@Canonical class Retriable<T> {

    /**
     * The time in milliseconds to wait after the first failed attempt. This is also the base factor for subsequent
     * retry delay lengths when using exponential backoff.
     */
    Integer firstDelayMillis = 64

    /**
     * The name of the process that will be attempted, to be used in error messages.
     */
    String name = 'retriable process'

    /**
     * The base of the exponent to be used for exponential backoff.
     *
     * A value of 1 disables exponential backoff.
     * A value of 2 causes retry delays to grow exponentially based on powers of 2.
     */
    Integer exponentialBackoffBaseMultiplier = 2

    /**
     * The work to try to execute without an exception getting thrown.
     *
     * T work(int iterationCounter)
     */
    Closure<T> work

    /**
     * The number of times to try to perform the work before giving up and re-throwing the exceptions from the attempts.
     */
    Integer maxRetries = 3

    /**
     * (Optional) Strategy for handling exceptions thrown during failed attempts to complete the work.
     */
    Closure handleException = { Exception e, int numberOfFailedAttemptsSoFar -> }

    /**
     * The maximum time in milliseconds to wait between attempts. Defaults to one hour.
     */
    Integer maxDelayMillis = 60 * 60 * 1000

    /**
     * Repeatedly attempts to perform some work without throwing an exception, up to a chosen number of times
     *
     * @return the object returned by the work closure, with unknown type
     */
    T performWithRetries() {
        List<Exception> exceptions = []
        for (int i = 0; i < maxRetries; i++) {
            if (i > 0) {
                int multiplier = Math.max(1, exponentialBackoffBaseMultiplier)
                int calculatedDelayMillis = Math.pow(multiplier, i - 1) * firstDelayMillis
                int delayMillis = Math.min(calculatedDelayMillis, maxDelayMillis)
                delay(delayMillis)
            }
            try {
                return work(i)
            } catch (Exception e) {
                exceptions << e
                if (handleException) {
                    handleException(e, i + 1)
                }
            }
        }

        String processName = name ? "${name} " : ''
        String msg = "Failed retriable process ${processName}${maxRetries} time${maxRetries == 1 ? '' : 's'}"
        throw new CollectedExceptions(msg, exceptions)
    }

    /**
     * Does some sleeping. This method can be overridden by subclasses to substitute behavior for cases such as unit
     * testing.
     *
     * @param milliseconds the number of milliseconds to sleep
     */
    void delay(int milliseconds) {
        Time.sleepCancellably(milliseconds)
    }
}
