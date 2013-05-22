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

import java.util.concurrent.CountDownLatch
import spock.lang.Specification

class CachedMapSpec extends Specification {

    Integer retrievalCallCount
    Integer permitAcquisitionAttemptCount
    List<List<String>> stoogesCastListsOverTime = [
            ['curly', 'larry', 'moe'],
            ['larry', 'moe', 'shemp'],
            ['joe', 'larry', 'moe']
    ]
    CachedMap cachedMap
    Thread backgroundThread
    Thread userThread

    void setup() {
        retrievalCallCount = 0
        permitAcquisitionAttemptCount = 0
        Runnable runnableFillProcess = new Runnable() {
            void run() {
                cachedMap.fill()
            }
        }
        backgroundThread = new Thread(runnableFillProcess, 'background')
        userThread = new Thread(runnableFillProcess, 'user')
    }

    def 'second thread should wait for the first thread to finish filling, and should not fill a second time'() {

        CountDownLatch backgroundIsRetrieving = new CountDownLatch(1)
        CountDownLatch userThreadHasTriedPermitAcquisition = new CountDownLatch(1)
        Closure retriever = {
            List<String> stooges = stoogesCastListsOverTime[retrievalCallCount]
            retrievalCallCount++

            // For this test, now is the time to signal the user thread to start
            backgroundIsRetrieving.countDown()

            // Don't let this background thread finish retrieving until the user thread has tried to get a fill permit
            userThreadHasTriedPermitAcquisition.await()

            return stooges
        }

        Closure waitForProperTestingState = {
            if (permitAcquisitionAttemptCount >= 1) {
                // Must be a user thread
                userThreadHasTriedPermitAcquisition.countDown()
            }
            permitAcquisitionAttemptCount++
        }

        cachedMap = new MultithreadedTestingCachedMap(Region.EU_WEST_1, EntityType.domain,
                null, null, waitForProperTestingState)
        cachedMap.ensureSetUp(retriever)

        when:
        backgroundThread.start()
        backgroundIsRetrieving.await()
        userThread.start() // Now background and user thread should both be running
        // Background thread will now wait until user thread has tried to acquire a fill permit
        backgroundThread.join()
        List<String> resultAfterBackgroundFill = cachedMap.list().sort()
        userThread.join()
        List<String> resultAfterUserFill = cachedMap.list().sort()

        then:
        ['curly', 'larry', 'moe'] == resultAfterBackgroundFill
        ['curly', 'larry', 'moe'] == resultAfterUserFill
        1 == retrievalCallCount
        2 == permitAcquisitionAttemptCount
    }

    def 'if user thread starts fill after background thread finishes, user result should override older result'() {

        Closure retriever = {
            List<String> stooges = stoogesCastListsOverTime[retrievalCallCount]
            retrievalCallCount++
            return stooges
        }
        cachedMap = new CachedMap(Region.EU_WEST_1, EntityType.domain, null, null)
        cachedMap.ensureSetUp(retriever)

        when:
        backgroundThread.start()
        backgroundThread.join() // Now background thread is dead
        List<String> resultAfterBackgroundFill = cachedMap.list().sort()
        userThread.start()
        userThread.join() // Now user thread is dead
        List<String> resultAfterUserFill = cachedMap.list().sort()

        then:
        ['curly', 'larry', 'moe'] == resultAfterBackgroundFill
        ['larry', 'moe', 'shemp'] == resultAfterUserFill
        2 == retrievalCallCount
    }
}

/**
 * A testable version of CachedMap, with a function that can be overridden to wait until a certain condition is met
 * during a test case. This is part of the mechanism to ensure a correct "deterministic dance" of waiting between
 * threads. http://briancoyner.github.io/blog/2011/11/21/multi-thread-unit-test/
 */
class MultithreadedTestingCachedMap extends CachedMap {

    /**
     * The extra behavior that should happen after a thread tries to acquire a fill permit, in order to choreograph
     * the deterministic dance of thread behaviors in a test without sleeping.
     */
    Closure waitForProperTestingState

    protected MultithreadedTestingCachedMap(Region region, EntityType entityType, Integer interval,
                                ThreadScheduler threadScheduler, Closure waitForProperTestingState) {
        super(region, entityType, interval, threadScheduler)
        this.waitForProperTestingState = waitForProperTestingState
    }

    /**
     * Extension hook for CachedMap subclasses to override in order to add behavior after trying to acquire the fill
     * permit for the current thread. Useful for unit testing multithreaded behavior.
     */
    @Override
    protected void afterAttemptedFillPermitAcquisition() {
        waitForProperTestingState()
    }
}
