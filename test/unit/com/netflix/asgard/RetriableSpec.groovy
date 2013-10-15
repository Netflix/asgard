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

import spock.lang.Specification
import spock.lang.Unroll

class RetriableSpec extends Specification {

    @Unroll("exponential backoff base multiplier #baseMultiplier results in delays #delays")
    def 'test delays'() {

        NoSleepRetriable retriable = new NoSleepRetriable(maxRetries: 5, work: { throw new IOException() },
                exponentialBackoffBaseMultiplier: baseMultiplier)

        when:
        retriable.performWithRetries()

        then:
        thrown(CollectedExceptions)
        delays == retriable.delays

        where:
        baseMultiplier | delays
        -1             | [64, 64, 64, 64]
        0              | [64, 64, 64, 64]
        1              | [64, 64, 64, 64]
        2              | [64, 128, 256, 512]
        3              | [64, 192, 576, 1728]
    }

    def 'exponential backoff delay should never exceed maximum'() {

        NoSleepRetriable retriable = new NoSleepRetriable(maxRetries: 13, work: { throw new IOException() },
                exponentialBackoffBaseMultiplier: 3)

        when:
        retriable.performWithRetries()

        then:
        thrown(CollectedExceptions)
        [64, 192, 576, 1728, 5184, 15552, 46656, 139968, 419904, 1259712, 3600000, 3600000] == retriable.delays
    }

    def 'exception handler should not be called if work succeeds the first time'() {

        Retriable retriable = new Retriable(work: { })

        when:
        retriable.performWithRetries()

        then:
        notThrown(Throwable)
    }

    def 'all exceptions should be thrown in a batch'() {

        Closure work = { int i -> throw new IllegalStateException("Failed attempt #${i + 1}") }
        Retriable retriable = new NoSleepRetriable(work: work)

        when:
        retriable.performWithRetries()

        then:
        CollectedExceptions collectedExceptions = thrown(CollectedExceptions)
        'Failed retriable process retriable process 3 times' == collectedExceptions.message
        ['Failed attempt #1', 'Failed attempt #2', 'Failed attempt #3'] == collectedExceptions.exceptions*.message
        [IllegalStateException, IllegalStateException, IllegalStateException] == collectedExceptions.exceptions*.class
    }

    def 'exception handler should run differently for each failed attempt'() {

        List thrownExceptionIndices = []
        Closure handleException = { Exception e, int numberOfFailedAttemptsSoFar ->
            thrownExceptionIndices << numberOfFailedAttemptsSoFar
        }
        Retriable retriable = new NoSleepRetriable(work: { throw new IOException() }, handleException: handleException)

        when:
        retriable.performWithRetries()

        then:
        thrown(CollectedExceptions)
        [1, 2, 3] == thrownExceptionIndices
    }

    def 'count of attempts should not exceed maximum retries'() {

        int attempts = 0
        Retriable retriable = new NoSleepRetriable(maxRetries: 5, work: {
            attempts++
            throw new IOException('No soup for you')
        })

        when:
        retriable.performWithRetries()

        then:
        thrown(CollectedExceptions)
        5 == attempts
    }

    def 'retries should stop after failing once and then succeeding once, and success result should be returned'() {

        int completionsCount = 0
        int attemptsCount = 0
        Closure work = { int iterationCounter ->
            attemptsCount++
            if (iterationCounter == 0) {
                throw new IllegalStateException('Failed the first time')
            }
            completionsCount++
            42
        }

        when:
        Retriable<Integer> retriable = new NoSleepRetriable(work: work)
        Integer result = retriable.performWithRetries()

        then:
        2 == attemptsCount
        1 == completionsCount
        42 == result
    }

    def 'default values should be used when not overridden in constructor'() {

        when:
        Retriable retriable = new NoSleepRetriable(firstDelayMillis: 50, name: 'Unit test workload')

        then:
        2 == retriable.exponentialBackoffBaseMultiplier
        60 * 60 * 1000 == retriable.maxDelayMillis
        3 == retriable.maxRetries
        50 == retriable.firstDelayMillis
        'Unit test workload' == retriable.name
    }

    /**
     * Version of Retriable that doesn't actually sleep between retries, but captures the delays for testing.
     */
    private class NoSleepRetriable extends Retriable {
        List<Integer> delays = []
        void delay(int milliseconds) {
            delays << milliseconds
        }
    }
}
