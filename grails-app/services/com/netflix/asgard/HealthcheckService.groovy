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

    Boolean readyForTraffic = false

    void initializeBackgroundProcess() {
        start()
    }

    private void start() {
        Thread.startDaemon('Healthcheck') {
            //noinspection GroovyInfiniteLoopStatement
            while (true) {
                readyForTraffic = checkHealth()
                sleep 5000
            }
        }.priority = Thread.MIN_PRIORITY
    }

    private Boolean checkHealth() {
        !serverService.shouldCacheLoadingBlockUserRequests()
    }
}
