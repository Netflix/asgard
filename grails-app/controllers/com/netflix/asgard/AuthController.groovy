package com.netflix.asgard

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.AuthenticationToken
import org.apache.shiro.grails.ConfigUtils

class AuthController {
    static final String AUTH_TARGET_URL = 'AUTH_TARGET_URL'

    def pluginService

    def index = { redirect(action: 'login', params: params) }

    def beforeInterceptor = {
        if (!pluginService.authenticationProvider) {
            render(status: 403, text: 'Authentication is not configured.')
            return false
        }
    }

    def login = {
        session[AUTH_TARGET_URL] = params.targetUri
        redirect(url: pluginService.authenticationProvider.loginUrl(request))
    }

    def signIn = {
        AuthenticationToken authToken = pluginService.authenticationProvider.tokenFromRequest(request)
        String targetUri = session[AUTH_TARGET_URL] ?: '/'
        session.removeAttribute(AUTH_TARGET_URL)

        try {
            SecurityUtils.subject.login(authToken)
            redirect(uri: targetUri)
        } catch (AuthenticationException e) {
            log.error("Authentication failed for token ${authToken}", e)
            render(status: 401, text: "Authentication failed with message ${e.message}")
        }
    }

    def signOut = {
        def principal = SecurityUtils.subject?.principal
        SecurityUtils.subject?.logout()
        ConfigUtils.removePrincipal(principal)
        String logoutUrl = pluginService.authenticationProvider.logoutUrl(request) ?: params.targetUri
        String redirectUri = logoutUrl ?: '/'
        redirect(uri: redirectUri)
    }

    def unauthorized = {
        forward action: 'login'
    }
}
