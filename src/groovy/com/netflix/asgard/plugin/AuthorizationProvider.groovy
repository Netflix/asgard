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
package com.netflix.asgard.plugin

import javax.servlet.http.HttpServletRequest

/**
 * Strategy object for making decisions if a user is allowed to access the requested resource.
 *
 * Implementations of this class can be wired up in Config.groovy in the entry plugins/authorizationProviders.
 */
interface AuthorizationProvider {

    /**
     * Validates if the current user is allowed to access the requested resource.
     *
     * @param request The http request
     * @param controllerName The name of the controller being access
     * @param actionName The name of the action being invoked
     * @return true if access is allowed to this resource, false otherwise
     */
    boolean isAuthorized(HttpServletRequest request, String controllerName, String actionName)

}
