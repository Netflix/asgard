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
package com.netflix.asgard.model

/**
 * Strategies for aggregating instances for monitoring by services like AppDynamics.
 */
enum MonitorBucketType {

    application, cluster

    String getDescription() {
        "Aggregate monitoring data by ${name()}"
    }

    /**
     * Takes a string returns the matching MonitorBucketType object, or null if no match exists.
     *
     * @param typeName a String such as application or cluster
     * @return Region a matching Region object, or null if no match found
     */
    static MonitorBucketType byName(String name) {
        return name ? MonitorBucketType.values().find { it.name() == name } as MonitorBucketType : null
    }

    static MonitorBucketType getDefaultForOldApps() { application }

    static MonitorBucketType getDefaultForNewApps() { cluster }
}
