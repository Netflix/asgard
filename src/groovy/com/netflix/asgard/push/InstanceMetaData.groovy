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
package com.netflix.asgard.push

import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Interval

class InstanceMetaData {
    com.amazonaws.services.ec2.model.Instance instance
    InstanceState state
    DateTime lastChange
    DateTime lastPeriodicLogTime = new DateTime()
    String healthCheckUrl

    void setState(InstanceState state) {
        this.state = state
        resetTimer()
    }

    void resetTimer() {
        lastChange = new DateTime()
    }

    def getId() { instance?.instanceId }

    Duration getTimeSinceChange() {
        new Duration(lastChange, new DateTime())
    }

    Boolean isItTimeForPeriodicLogging() {
        Duration timeSinceLastLog = new Interval(lastPeriodicLogTime, new DateTime()).toDuration()
        if (Duration.standardMinutes(4).isShorterThan(timeSinceLastLog)) {
            lastPeriodicLogTime = new DateTime()
            return true
        }
        return false
    }
}
