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

import org.apache.shiro.SecurityUtils

class SettingsFilters {

    def configService
    def pluginService
    def grailsApplication

    def filters = {
        all(controller: '*', action: '*') {
            before = {
                String accountName = configService.accountName
                request.env = accountName
                request.envStyle = params.envStyle ?: configService.envStyle
                List<String> accountNamesForSpot = grailsApplication.config.cloud.awsAccountNamesForSpotUsage ?: []
                request.spotInstancesAreAppropriate = accountName in accountNamesForSpot
                request.ticketRequired = accountName in grailsApplication.config.cloud.highRiskAccountNames
                request.build = grailsApplication.config.build.id
                request.allowSdbPropertyEdit = false
                request.ticketLabel = configService.ticketLabel
                request.fullTicketLabel = configService.fullTicketLabel
                request.bleskJavaScriptUrl = configService.bleskJavaScriptUrl
                request.bleskDataUrl = configService.bleskDataUrl
                request.authenticationEnabled = (pluginService.authenticationProvider != null)
                request.apiTokenEnabled = configService.apiTokenEnabled
                boolean authenticated = SecurityUtils.subject?.authenticated
                request.requireLoginForEdit = configService.authenticationRequiredForEdit && !authenticated
                request.targetUri = request.forwardURI + (request.queryString ? "?${request.queryString}" : '')

                // If the last value is falsy and there is no explicit return statement then this filter method will
                // return a falsy value and cause requests to fail silently.
                return true
            }
        }
    }
}
