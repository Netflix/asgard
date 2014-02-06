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

import com.amazonaws.AmazonClientException
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.netflix.asgard.ConfigService
import org.apache.commons.logging.LogFactory

/**
 * {@link com.amazonaws.auth.AWSCredentialsProvider} implementation that provides credentials
 * by looking at {@link com.netflix.asgard.ConfigService#getAccessId()} and
 * {@link com.netflix.asgard.ConfigService#getSecretKey()}
 */
class ConfigCredentialsProvider extends AbstractCredentialsProvider {

    private static final log = LogFactory.getLog(this)

    /**
     * Constructor with configuration.
     *
     * @param configService the means for looking up credentials from a config file
     */
    ConfigCredentialsProvider(ConfigService configService) {
        super(configService)
    }

    @Override
    AWSCredentials getCredentials() {

        String awsAccessId = configService.accessId
        String awsSecretKey = configService.secretKey

        if (awsAccessId && awsSecretKey) {
            log.debug 'Fetching AWS credentials from configuration file'
            return new BasicAWSCredentials(awsAccessId, awsSecretKey)
        }

        throw new AmazonClientException("Unable to load AWS credentials from configuration")
    }
}
