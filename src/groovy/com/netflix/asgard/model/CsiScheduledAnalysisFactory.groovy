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

import groovy.json.JsonSlurper
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

/**
 * Used to create a ScheduledAsgAnalysis from JSON returned by the Critical Systems Inspector (CSI).
 */
class CsiScheduledAnalysisFactory {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern('MMM dd, yyyy hh:mm:ss a').
            withZone(DateTimeZone.UTC)

    /**
     * Creates a ScheduledAsgAnalysis from JSON.
     *
     * @param json from the Critical Systems Inspector (CSI)
     * @return attributes about the analysis that was started
     */
    ScheduledAsgAnalysis fromJson(String json) {
        def parameters = new JsonSlurper().parseText(json).parameters
        String name = parameters.schedulername
        DateTime created = dateTimeFormatter.parseDateTime(parameters.created)
        new ScheduledAsgAnalysis(name, created)
    }
}
