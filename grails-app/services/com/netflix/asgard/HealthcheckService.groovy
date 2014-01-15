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

import org.joda.time.DateTime

class HealthcheckService implements BackgroundProcessInitializer {

    static transactional = false

    private static final Integer RECENT_MINUTES = 10

    Caches caches
    def configService
    def initService

    Boolean isHealthy = false
    Map<String, String> cacheNamesToProblems = new TreeMap<String, String>()

    void initializeBackgroundProcess() {
        start()
    }

    private void start() {
        Thread.startDaemon('Healthcheck') {
            //noinspection GroovyInfiniteLoopStatement
            while (true) {
                checkCaches()
                sleep 5000
            }
        }.priority = Thread.MIN_PRIORITY
    }

    private checkCaches() {
        try {
            if (initService.cachesFilled()) {
                cacheNamesToProblems.remove('All')
            } else {
                cacheNamesToProblems['All'] = 'Server is starting up'
                isHealthy = false
                return
            }
            DateTime recentTime = new DateTime().minusMinutes(RECENT_MINUTES)
            Map<String, Integer> minimumCounts = configService.healthCheckMinimumCounts
            boolean cachesHealthy = true
            minimumCounts.each { cacheName, threshold ->
                MultiRegionCachedMap multiRegionCachedMap
                try {
                    multiRegionCachedMap = caches[cacheName] as MultiRegionCachedMap
                } catch (MissingPropertyException ignored) {
                    log.error("Invalid cache name ${cacheName} specified for healthCheck in config")
                    cachesHealthy = false
                    cacheNamesToProblems[cacheName] = 'Invalid cache name'
                    return
                }
                try {
                    if (findProblem(multiRegionCachedMap, recentTime, threshold)) {
                        cachesHealthy = false
                    }
                } catch (Exception e) {
                    log.error "Error checking health for ${cacheName}", e
                    cachesHealthy = false
                    cacheNamesToProblems[cacheName] = e.message
                }
            }
            isHealthy = cachesHealthy
        } catch (Exception e) {
            log.error 'Healthcheck threw an exception', e
            isHealthy = false
        }
    }

    private String findProblem(MultiRegionCachedMap multiRegionCachedMap, DateTime recentTime, Integer minSize) {
        CachedMap defaultRegionCachedMap = multiRegionCachedMap.by(Region.defaultRegion())
        // Secondary object caches like Cluster don't have intervals, so assume a common number.
        DateTime beforeLastScheduledFill = new DateTime().minusSeconds(defaultRegionCachedMap.interval ?: 120)
        String problem = ''

        if (defaultRegionCachedMap.active && defaultRegionCachedMap.lastActiveTime.isBefore(beforeLastScheduledFill) &&
                defaultRegionCachedMap.lastFillTime.isBefore(recentTime)) {
            problem = "Cache is actively used but last fill time is ${defaultRegionCachedMap.lastFillTime} which is " +
                    "before ${RECENT_MINUTES} minutes ago at ${recentTime}"
        } else if (defaultRegionCachedMap.size() < minSize) {
            problem = "Cache size is ${defaultRegionCachedMap.size()} which is below minimum size ${minSize}"
        }

        if (problem) {
            cacheNamesToProblems.put(defaultRegionCachedMap.name, problem)
        } else {
            cacheNamesToProblems.remove(defaultRegionCachedMap.name)
        }
        problem
    }
}
