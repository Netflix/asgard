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

import org.joda.time.Duration

enum InstanceState {
    initial      (unlimited()),
    unregistered (unlimited()),
    terminated   (minutes(70)),
    pending      (minutes(50)),
    running      (minutes(35)),
    registered   (minutes(60)),
    snoozing     (unlimited()),
    ready        (unlimited())

    /**
     * The maximum time before an attempt to EXIT a given state is considered a failure.
     */
    final Duration timeOutToExitState

    InstanceState(Duration timeOutToExitState) {
        this.timeOutToExitState = timeOutToExitState
    }

    private static Duration minutes(int minutes) { Duration.standardMinutes(minutes) }
    private static Duration unlimited() { Duration.standardDays(999999) }
}
