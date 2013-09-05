/*
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.asgard.flow.example.trip

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions

/**
 * SWF activities for the BayAreaTripWorkflow example.
 */
@Activities(version = "1.0")
@ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = -1L,
        defaultTaskStartToCloseTimeoutSeconds = 300L)
interface BayAreaTripActivities {

    /**
     * Ask a question and get a boolean answer.
     * The implementation is an example of a manual task which halts the workflow gets the result from an external
     * interaction (a user or service for example).
     * It is common to increase the defaultTaskStartToCloseTimeoutSeconds to allow the external entity time to react.
     *
     * @param question asked
     * @return true for yes and false for no
     */
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = -1L,
            defaultTaskStartToCloseTimeoutSeconds = 86400L)
    boolean askYesNoQuestion(String question)

    /**
     * Describes a persons trip to a location.
     *
     * @param name of person taking trip
     * @param location to go to
     * @return text describing what happened
     */
    String goTo(String name, BayAreaLocation location)

    /**
     * Describes a hike somewhere.
     * The implementation is an example of using heartbeats to allow an activity to update the workflow on its status.
     * It is common to provide a relatively short defaultTaskHeartbeatTimeoutSeconds when heartbeats are expected.
     *
     * @param somewhere to hike
     * @return text describing what happened
     */
    @ActivityRegistrationOptions(defaultTaskScheduleToStartTimeoutSeconds = -1L,
            defaultTaskStartToCloseTimeoutSeconds = 300L, defaultTaskHeartbeatTimeoutSeconds = 30L)
    String hike(String somewhere)

    /**
     * Describes enjoying something.
     *
     * @param something to enjoy
     * @return text describing what happened
     */
    String enjoy(String something)

    /**
     * Describes an attempt to win something.
     * Used by the workflow as an example of retrying an operation.
     *
     * @param game to win
     * @return text describing the win
     * @throws IllegalStateException in the event of a loss
     */
    String win(String game)
}
