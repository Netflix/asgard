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

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.security.SecureRandom
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

/**
 * Handles shared threads for background processes such as cache loading and cloud state clean up.
 */
class ThreadScheduler {

    private final ConfigService configService
    private final Random random = new SecureRandom()
    final ScheduledExecutorService scheduler

    ThreadScheduler(ConfigService configService) {
        this.configService = configService
        int priority = Thread.MIN_PRIORITY
        String prefix = 'background-process-%s'
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(prefix).setPriority(priority).build()
        scheduler = Executors.newScheduledThreadPool(20, threadFactory)
    }

    /**
     * Adds another job to the set of repetitive, scheduled background jobs.
     *
     * @param intervalSeconds the minimum time between operation start times for the specified job
     * @param maxJitterSeconds the upper limit to the random amount of time to wait before the periodic schedule begins,
     *          in order to reduce numerous simultaneous jobs from running together
     * @param job the operation to execute periodically
     */
    void scheduleAtFixedRate(int intervalSeconds, int maxJitterSeconds, Runnable job) {
        // Random jitter should be not be too long, even if the interval is long
        int maxJitterToUse = Math.min(maxJitterSeconds, 15)
        int jitterSeconds = (maxJitterToUse && configService.useJitter) ? random.nextInt(maxJitterToUse) : 0
        scheduler.scheduleAtFixedRate(job, jitterSeconds, intervalSeconds, TimeUnit.SECONDS)
    }

    /**
     * Schedules a job to run once after a delay.
     *
     * @param delaySeconds the number of seconds to wait before the job is allowed to run
     * @param job the operation to execute
     */
    void schedule(int delaySeconds, Runnable job) {
        scheduler.schedule(job, delaySeconds, TimeUnit.SECONDS)
    }
}
