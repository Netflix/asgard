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

import org.apache.commons.logging.LogFactory
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.ReadableInstant

class StopWatch {

    private static final log = LogFactory.getLog(this)

    String name
    String activity
    ReadableInstant start
    ReadableInstant stop
    Boolean verbose

    static StopWatch start(String name, String activity, Boolean enabled = true, Boolean verbose = false) {
        StopWatch stopWatch
        if (enabled) {
            stopWatch = new StopWatch(name: name, activity: activity, verbose: verbose)
        } else {
            stopWatch = new DisabledStopWatch()
        }
        stopWatch.startActivity(activity)
        stopWatch
    }

    void startActivity(String activity) {
        this.activity = activity
        stop = null
        start = new DateTime()
    }

    Period reportAndRestart(String activity) {
        Period period = stopAndReport()
        startActivity(activity)
        period
    }

    Period stopAndReport() {
        stop = new DateTime()
        Period period = new Period(start, stop)
        if (verbose) {
            log.info "$name $activity took ${period.seconds < 1 ? '<1s' : Time.format(period)}"
        }
        period
    }
}

class DisabledStopWatch extends StopWatch {
    void startActivity(String activity) { }
    Period reportAndRestart(String activity) { null }
    Period stopAndReport() { null }
}
