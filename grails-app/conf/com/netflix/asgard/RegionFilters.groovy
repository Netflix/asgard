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

class RegionFilters {

    private static MONTH_IN_SECONDS = 60 * 60 * 24 * 30

    def configService
    def grailsApplication

    def filters = {
        all(controller: '*', action: '*') {
            before = {
                // Choose a region based on request parameter, then cookie, then default
                Region region = Region.withCode(params.region) ?: Region.withCode(request.getCookie('region')) ?:
                        Region.defaultRegion()

                // Store the region in the cookie and in the request
                response.setCookie('region', region.code, MONTH_IN_SECONDS)
                request.region = region
                request.regions = Region.values()
                request.discoveryExists = configService.doesRegionalDiscoveryExist(region)

                // Redirect deprecated browser-based web requests to new canonical format.
                // Automated scripts will need to be found and edited before changing behavior of JSON and XML URLs.
                if (!params.region &&
                        request.format == 'html' &&
                        request.method == 'GET' &&
                        actionName && /* Avoid redirecting twice when both action and region are missing */
                        grailsApplication.controllerNamesToContextParams[(controllerName)].contains('region')) {
                    params.region = region.code
                    redirect(controller: controllerName, action: actionName, params: params)

                    // Don't execute the controller method for this request
                    return false
                }

                // If the last value is falsy and there is no explicit return statement then this filter method will
                // return a falsy value and cause requests to fail silently.
                return true
            }
        }
    }
}
