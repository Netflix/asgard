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
package com.netflix.asgard.push;

import com.netflix.asgard.NonAlertable;

/**
 * Exception thrown when there is a problem doing a push.
 */
public class PushException extends RuntimeException implements NonAlertable {

    /**
     * Constructor with error message.
     *
     * @param message the explanation of what went wrong
     */
    public PushException(String message) {
        super(message);
    }
}
