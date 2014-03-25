/*
 * Copyright 2012 Netflix, Inc.
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

/**
 * Logic for determining whether the system is currently in a state good enough to accept user traffic.
 */
class HealthcheckService implements BackgroundProcessInitializer {

    static transactional = false

    def serverService

    /**
     * Cached result of periodic health check calculation.
     */
    Boolean readyForTraffic = false

    /**
     * Registered procedures that should be called each time the health check runs. Each closure is expected to have
     * this signature:
     * <p>
     * callback(Boolean wasHealthyLastTime, Boolean isHealthyNow)
     */
    Map<String, Closure> callbackNamesToCallbacks = [:]

    /**
     * Adds a new named callback to the map of callbacks to call each time the health check runs.
     *
     * @param name the name of the registered callback
     * @param callback the Closure to execute, which should take two Boolean parameters like
     *          callback(Boolean wasHealthyLastTime, Boolean isHealthyNow)
     */
    void registerCallback(String name, Closure callback) {
        callbackNamesToCallbacks.put(name, callback)
    }

    /**
     * Starts the background thread that periodically checks the health of the server, updates the readyForTraffic flag,
     * and calls any registered callbacks that need to be executed when health changes.
     */
    void initializeBackgroundProcess() {
        start()
    }

    private void start() {
        Thread.startDaemon('Healthcheck') {
            //noinspection GroovyInfiniteLoopStatement
            while (true) {
                checkHealthAndInvokeCallbacks()
                sleep 5000
            }
        }.priority = Thread.MIN_PRIORITY
    }

    /**
     * Performs the health check to determine whether or not the system is currently ready for traffic, then calls any
     * registered callbacks that need to respond to health check events.
     *
     * @return true if the system is ready for traffic, false otherwise
     * @see ServerService#shouldCacheLoadingBlockUserRequests()
     */
    void checkHealthAndInvokeCallbacks() {
        Boolean wasHealthyLastTime = readyForTraffic
        Boolean isHealthyNow = !serverService.shouldCacheLoadingBlockUserRequests()
        readyForTraffic = isHealthyNow
        for (Closure callback in callbackNamesToCallbacks.values()) {
            callback(wasHealthyLastTime, isHealthyNow)
        }
    }
}
