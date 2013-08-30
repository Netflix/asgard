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
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.grails.ConfigUtils

class ApiTokenFilters {

    private static final String API_TOKEN_PARAM = 'asgardApiToken'

    def filters = {
        all(controller: '*', action: '*') {
            before = {
                String apiToken = params[API_TOKEN_PARAM]
                if (!apiToken) {
                    return true
                }
                ApiToken token = ApiToken.fromApiTokenString(apiToken)
                try {
                    SecurityUtils.subject.login(token)
                    return true
                } catch (AuthenticationException e) {
                    log.warn('Failed to authenticate API Key', e)
                    render(status: 401, test: e.message)
                }
                false
            }
            after = {
                String apiToken = request[API_TOKEN_PARAM]
                if (apiToken) {
                    // For API tokens, login is only valid for duration of one request.
                    SecurityUtils.subject?.logout()
                    ConfigUtils.removePrincipal(SecurityUtils.subject?.principal)
                }

                // If the last value is falsy and there is no explicit return statement then this filter method will
                // return a falsy value and cause requests to fail silently.
                return true
            }
        }
    }
}
