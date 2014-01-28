/*
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.asgard.plugin

import com.netflix.asgard.model.ScheduledAsgAnalysis

/**
 * Interface for coordinating the analysis of Auto Scaling Groups in a cluster during a deployment.
 */
interface AsgAnalyzer {

    /**
     * Starts the analysis of Auto Scaling Groups in a cluster.
     *
     * @param clusterName for the ASGs to be analyzed
     * @param notificationDestination where deployment notifications will be sent
     * @return attributes about the analysis that was started
     */
    ScheduledAsgAnalysis startAnalysis(String clusterName, String notificationDestination)

    /**
     * Stops the analysis of Auto Scaling Groups in a cluster.
     *
     * @param name used to identify the scheduled analysis
     */
    void stopAnalysis(String name)
}
