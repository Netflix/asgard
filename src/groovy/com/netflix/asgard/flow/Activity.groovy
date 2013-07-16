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
package com.netflix.asgard.flow

import com.amazonaws.services.simpleworkflow.model.WorkflowExecution

/**
 * Common behavior for an SWF activity. This enables implementations that are not tied to SWF.
 */
public interface Activity {

    /**
     * Record a heartbeat during an activity. This lets a workflow know that the activity is still making progress.
     *
     * @param message supplied with each heartbeat
     */
    void recordHeartbeat(String message)

    /**
     * @return a token specific to the current execution of this activity (useful for completing a manual task)
     */
    String getTaskToken()

    /**
     * @return identification for the current execution of this activity
     */
    WorkflowExecution getWorkflowExecution()
}
