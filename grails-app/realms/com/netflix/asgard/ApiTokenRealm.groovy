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
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.AuthenticationToken
import org.apache.shiro.authc.SimpleAuthenticationInfo
import org.joda.time.DateTime
import org.springframework.beans.factory.InitializingBean

/**
 * Security realm for validating Asgard api tokes. Takes care of sending out expiration warnings when a token is close
 * to expiration.
 */
class ApiTokenRealm implements InitializingBean {

    static authTokenClass = ApiToken

    def configService
    def emailerService
    def secretService

    /**
     * Time based expiring cache of tokens that have triggered an email alert. This allows only one alert to be sent out
     * per configured interval.
     */
    private Cache<String, String> tokensAlertsSent

    void afterPropertiesSet() {
        tokensAlertsSent =  new CacheBuilder()
                .expireAfterWrite(configService.apiTokenExpiryWarningIntervalMinutes, TimeUnit.MINUTES)
                .maximumSize(256)
                .build()
    }

    def authenticate(AuthenticationToken authToken) {
        if (authToken == null) {
            throw new AuthenticationException('API Key cannot be null')
        }

        ApiToken apiToken = (ApiToken)authToken
        checkKeyValid(apiToken)

        new SimpleAuthenticationInfo(apiToken.username, apiToken.credentials, 'ApiTokenRealm')
    }

    private void checkKeyValid(ApiToken apiToken) {
        if (!isKeyValid(apiToken)) {
            String message = apiToken.expired ? 'API Token has expired' : 'API Token is invalid'
            throw new AuthenticationException(message)
        }
        DateTime expiryWarningThreshold = new DateTime().plusDays(configService.apiTokenExpiryWarningThresholdDays)
        if (apiToken.expires.isBefore(expiryWarningThreshold) &&
                !tokensAlertsSent.getIfPresent(apiToken.credentials)) {
            // TODO add server name to message
            emailerService.sendUserEmail(apiToken.email, 'Asgard API key is about to expire',
                    "The following Asgard API key is about to expire:\n\n" +
                    "Key: ${apiToken.credentials}\n" +
                    "Purpose: ${apiToken.purpose}\n" +
                    "Registered by: ${apiToken.username}\n" +
                    "Expires: ${apiToken.expiresISOFormatted}\n\n" +
                    "Please generate a new key.")
            tokensAlertsSent.put(apiToken.credentials, apiToken.credentials)
        }
    }

    private boolean isKeyValid(ApiToken apiToken) {
        secretService.apiEncryptionKeys.find { String encryptionKey -> apiToken.isValid(encryptionKey) } != null
    }
}
