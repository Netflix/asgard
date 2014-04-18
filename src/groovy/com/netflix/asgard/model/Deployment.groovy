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
import com.netflix.asgard.deployment.steps.DeploymentStep
import groovy.transform.Canonical
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.joda.time.DateTime

/**
 * Attributes that describe a deployment workflow execution.
 */
@Canonical(excludes='token')
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
    final List<DeploymentStep> steps

    static Pattern stepPattern = ~/.*\{"step":([0-9]+)\}/

    /** Construct JSON that represents a step index */
    static constructStepJson(int stepIndex) {
        """{"step":${stepIndex}}"""
    }

    /** @return step index from JSON */
    static Integer parseStepIndex(String logMessage) {
        Matcher matcher = logMessage =~ stepPattern
        if (matcher.matches()) {
            String stepIndex = matcher[0][1]
            return Integer.parseInt(stepIndex)
        }
        null
    }

    /** AWS Simple Workflow Service activity token needed to complete a manual activity */
    String token

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

    String getRegionCode() {
        region.code
    }

    /** @return list of lists of log messages grouped by step */
    List<List<String>> getLogForSteps() {
        List<List<String>> logForSteps = [[]]
        int currentStepIndex = 0
        log.each {
            Integer stepIndex = parseStepIndex(it)
            if (stepIndex != null) {
                currentStepIndex = stepIndex
                logForSteps[currentStepIndex] = []
            } else {
                logForSteps[currentStepIndex] << it
            }
        }
        logForSteps
    }
}
