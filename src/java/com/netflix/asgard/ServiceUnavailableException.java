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
package com.netflix.asgard;

/**
 * Exception that occurs when a service dependency cannot be reached.
 */
public class ServiceUnavailableException extends Exception {

    /**
     * Constructs the exception as a general failure of a named service.
     *
     * @param serviceName the name of the unavailable service
     */
    public ServiceUnavailableException(String serviceName) {
        this(serviceName, null);
    }

    /**
     * Constructs the exception with a specific message.
     *
     * @param serviceName the name of the unavailable service
     * @param msg the error message captured from the failure
     */
    public ServiceUnavailableException(String serviceName, String msg) {
        this(serviceName, msg, null);
    }

    /**
     * Constructs the exception with an existing throwable.
     *
     * @param serviceName the name of the unavailable service
     * @param msg the error message captured from the failure
     * @param throwable the existing problem that should be wrapped in this exception for extra context and typing
     */
    public ServiceUnavailableException(String serviceName, String msg, Throwable throwable) {
        super((msg == null || msg.isEmpty()) ? serviceName + " could not be contacted." : msg, throwable);
    }
}
