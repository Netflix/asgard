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
