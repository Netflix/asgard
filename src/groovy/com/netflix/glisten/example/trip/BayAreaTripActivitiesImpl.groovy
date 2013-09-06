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
package com.netflix.glisten.example.trip

import com.amazonaws.services.simpleworkflow.flow.annotations.ManualActivityCompletion
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.netflix.glisten.Activity
import com.netflix.glisten.SwfActivity

/**
 * SWF activity implementations for the BayAreaTripWorkflow example.
 */
class BayAreaTripActivitiesImpl implements BayAreaTripActivities {

    @Delegate Activity activity = new SwfActivity()

    // In a real ActivitiesImpl this would probably be an injected service rather than a map with mutable state
    Map<String, Integer> hikeNameToLengthInSteps = [:]

    // In a real ActivitiesImpl this would probably be an injected service
    Closure<Boolean> isWinner = { new Random().nextBoolean() }

    @Override
    String goTo(String name, BayAreaLocation location) {
        "${name} went to ${location}."
    }
    @Override
    String enjoy(String something) {
        "And enjoyed ${something}."
    }

    @Override
    String hike(String somewhere) {
        int stepsTaken = 0
        int totalStepsForHike = hikeNameToLengthInSteps[somewhere] ?: 100
        while (stepsTaken < totalStepsForHike) {
            activity.recordHeartbeat("Took ${++stepsTaken} steps.")
        }
        "And hiked ${somewhere}."
    }

    @Override
    String win(String game) {
        if (isWinner()) {
            return "And won ${game}."
        } else {
            throw new IllegalStateException("And lost ${game}.")
        }
    }

    @ManualActivityCompletion
    @Override
    boolean askYesNoQuestion(String question) {
        sendManualActivityCompletionInfo(activity.taskToken, activity.workflowExecution)
        true // does not matter what is returned here because the result will be supplied manually
    }

    private void sendManualActivityCompletionInfo(String taskToken, WorkflowExecution workflowExecution) {
        // send info needed to complete task manually
    }
}
