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
package com.netflix.asgard.plugin

import com.netflix.asgard.model.LaunchContext

/**
 * Interface for plugin implementations for creating user data from various cloud object inputs.
 */
interface AdvancedUserDataProvider {

    /**
     * Constructs a user data string based on a collection of various inputs.
     *
     * @param deploymentContext collection of various cloud object inputs
     * @return user data string
     */
    String buildUserDataForCloudObjects(LaunchContext deploymentContext)
}
