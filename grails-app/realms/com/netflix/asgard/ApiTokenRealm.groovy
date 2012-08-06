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

import com.netflix.asgard.auth.ApiToken
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.AuthenticationToken
import org.apache.shiro.authc.SimpleAuthenticationInfo
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Security realm for validating Asgard api tokens. Sends out expiration warnings when a token is near expiration.
 */
class ApiTokenRealm implements ApplicationContextAware {

    static authTokenClass = ApiToken

    ApplicationContext applicationContext

    def authenticate(AuthenticationToken authToken) {
        if (authToken == null) {
            throw new AuthenticationException('API Key cannot be null')
        }

        ApiToken apiToken = (ApiToken) authToken
        // Realms are not lazy loaded so have to load this way to prevent eager loading of SecretService.
        ApiTokenService apiTokenService = applicationContext.getBean(ApiTokenService)
        if (!apiTokenService.tokenValid(apiToken)) {
            String message = apiToken.expired ? 'API Token has expired' : 'API Token is invalid'
            throw new AuthenticationException(message)
        }
        apiTokenService.checkExpiration(apiToken)

        new SimpleAuthenticationInfo(apiToken.username, apiToken.credentials, 'ApiTokenRealm')
    }
}
