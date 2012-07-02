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
package com.netflix.asgard.plugin

import com.netflix.asgard.Task

/**
 * Observer interface to execute code when a task is finished. Listeners are registered under
 * plugins/taskFinishedListeners in Config.groovy.
 */
interface TaskFinishedListener {

    /**
     * Method to call when a task is finished.
     *
     * @param task The finished task (can be completed or failed)
     */
    void taskFinished(Task task)

}
