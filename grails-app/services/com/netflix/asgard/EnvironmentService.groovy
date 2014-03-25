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
package com.netflix.asgard

import com.amazonaws.util.EC2MetadataUtils

/**
 * Interacts with environment variables and cloud metadata for the current runtime. Easy to mock out for unit testing.
 */
class EnvironmentService {

    static transactional = false

    /**
     * @param key the name of the environment variable whose value is needed
     * @return the environment variable value for the specified key, abstracted into a service class for easier unit
     *          test mocking
     */
    String getEnvironmentVariable(String key) {
        System.getenv(key)
    }

    /**
     * @return the instance ID of the current runtime, or throws an exception if not running in the cloud
     */
    String getInstanceId() {
        EC2MetadataUtils.instanceId
    }

    /**
     * @return the current date
     */
    Date getCurrentDate() {
        new Date()
    }
}
