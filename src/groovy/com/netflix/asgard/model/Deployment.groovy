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
package com.netflix.asgard.model

import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.netflix.asgard.Region
import com.netflix.asgard.Time
import groovy.transform.Canonical
import org.joda.time.DateTime

/**
 * Attributes that describe a deployment workflow execution.
 */
@Canonical
class Deployment {
    final String id
    final String clusterName
    final Region region
    final WorkflowExecution workflowExecution
    final String description
    final String owner
    final Date startTime
    final Date updateTime
    final String status
    final List<String> log

    /**
     * @return indication that workflow is done running
     */
    boolean isDone() {
        !'running'.equalsIgnoreCase(status)
    }

    /**
     * @return text that indicates how long the workflow ran
     */
    String getDurationString() {
        DateTime endTime = isDone() ? new DateTime(updateTime) : Time.now()
        Time.format(new DateTime(startTime), endTime)
    }
}
