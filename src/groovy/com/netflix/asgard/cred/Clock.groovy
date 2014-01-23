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
package com.netflix.asgard.cred

/**
 * Wrapper around time-checking functions, to enable mocking of this behavior in unit tests.
 */
class Clock {

    /**
     * @return the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC
     */
    long currentTimeMillis() {
        System.currentTimeMillis()
    }
}
