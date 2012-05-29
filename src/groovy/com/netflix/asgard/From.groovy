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
 * General types of data sources to pull from, with varying degrees of correctness, thread safety, and performance.
 */
enum From {

    /**
     * Return a shared cached list.
     * More performant, less thread safe, less up-to-date.
     */
    CACHE,

    /**
     * Return the most up-to-date data from Amazon Web Services.
     * Least performant, more up-to-date, more heap utilization, causes better consistency across threads.
     */
    AWS,

    /**
     * Return the most up-to-date data from Amazon Web Services.
     * Least performant, more up-to-date, less heap utilization, does not help consistency across threads,.
     */
    AWS_NOCACHE
}
