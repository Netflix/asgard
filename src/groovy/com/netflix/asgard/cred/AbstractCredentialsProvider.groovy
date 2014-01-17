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

import com.amazonaws.auth.AWSCredentialsProvider
import com.netflix.asgard.ConfigService

/**
 * Implements credential provider methods that are common to several provider implementations.
 */
abstract class AbstractCredentialsProvider implements AWSCredentialsProvider {

    /**
     * Container for all the configured values that tell the system how best to obtain credentials.
     */
    protected final ConfigService configService

    /**
     * Constructor.
     *
     * @param configService the source of runtime configuration options for various means of fetching credentials.
     */
    AbstractCredentialsProvider(ConfigService configService) {
        this.configService = configService
    }

    /**
     * Some implementations have nothing logical to do when it's time to refresh the keys, so by default do nothing at
     * refresh time.
     */
    @Override
    void refresh() { }

    /**
     * Same implementation as {@link com.amazonaws.auth.EnvironmentVariableCredentialsProvider} because the toString
     * value is used in logging from {@link com.amazonaws.auth.AWSCredentialsProviderChain}.
     *
     * @return the name of the concrete implementation class
     */
    @Override
    String toString() {
        getClass().simpleName
    }
}
