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
package com.netflix.asgard

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.netflix.asgard.auth.ApiToken
import java.util.concurrent.TimeUnit
import org.joda.time.DateTime
import org.springframework.beans.factory.InitializingBean

/**
 * Service class for validating {@link ApiToken} objects
 */
class ApiTokenService implements InitializingBean {

    static transactional = false

    def configService
    def emailerService

    /**
     * Time based expiring cache of tokens that have triggered an email alert. This allows only one alert to be sent out
     * per configured interval.
     */
    private Cache<String, String> tokensAlertsSent

    void afterPropertiesSet() {
        tokensAlertsSent = new CacheBuilder()
                .expireAfterWrite(configService.apiTokenExpiryWarningIntervalMinutes, TimeUnit.MINUTES)
                .maximumSize(256)
                .build()
    }

    /**
     * Checks if an API token is valid for any of the encryption keys.
     *
     * @param apiToken A token object to check
     * @return true if the token is valid for any of the encryption keys, false otherwise.
     */
    boolean tokenValid(ApiToken apiToken) {
        configService.apiEncryptionKeys.find { String encryptionKey -> apiToken.isValid(encryptionKey) } != null
    }

    /**
     * Sends a warning email if an API token is near the specified warning threshold.
     *
     * @param apiToken The token to check.
     */
    void checkExpiration(ApiToken apiToken) {
        DateTime expiryWarningThreshold = new DateTime().plusDays(configService.apiTokenExpiryWarningThresholdDays)
        if (apiToken.expires.isBefore(expiryWarningThreshold) &&
                !tokensAlertsSent.getIfPresent(apiToken.credentials)) {
            emailerService.sendUserEmail(apiToken.email,
                    "${configService.canonicalServerName} API key is about to expire",
                    "The following ${configService.canonicalServerName} API key is about to expire:\n\n" +
                    "Key: ${apiToken.credentials}\n" +
                    "Purpose: ${apiToken.purpose}\n" +
                    "Registered by: ${apiToken.username}\n" +
                    "Expires: ${apiToken.expiresReadable}\n\n" +
                    "Please generate a new token.")
            tokensAlertsSent.put(apiToken.credentials, apiToken.credentials)
        }
    }
}
