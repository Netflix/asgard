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

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.Canonical
import org.joda.time.DateTime

/**
 * Attributes of a scheduled ASG analysis.
 */
@Canonical
class ScheduledAsgAnalysis {

    /** An identifier for the scheduled ASG analysis. */
    final String name

    /** Indicated when the scheduled ASG analysis was created. */
    @JsonIgnore final DateTime created

    /** An easily serializable version of the created DateTime. */
    long getCreatedMillis() {
        created.millis
    }

    /** Allows Jackson to create a ScheduledAsgAnalysis from JSON. */
    @JsonCreator
    static ScheduledAsgAnalysis of(@JsonProperty('name') String name,
            @JsonProperty('createdMillis') long createdMillis) {
        new ScheduledAsgAnalysis(name, new DateTime(createdMillis))
    }
}
