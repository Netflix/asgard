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
package com.netflix.asgard.server

import com.netflix.asgard.ConfigService
import com.netflix.asgard.Requests
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import org.springframework.beans.factory.InitializingBean

/**
 * Detects and redirects a browser GET request for a deprecated server name to a replacement server name.
 */
class DeprecatedServerNames implements InitializingBean {

    ConfigService configService

    /**
     * A cached copy of the regular expression pattern for identifying a request URL as using a deprecated server name.
     */
    Pattern deprecatedUrlPattern

    @Override
    void afterPropertiesSet() throws Exception {
        deprecatedUrlPattern = createPatternForMatchingDeprecatedUrl()
    }

    /**
     * @return regular expression pattern for identifying a request URL as using a deprecated server name
     */
    Pattern createPatternForMatchingDeprecatedUrl() {
        Map<String, String> oldNamesToNewNames = configService.deprecatedServerNamesToReplacements
        if (oldNamesToNewNames) {
            Set<String> deprecatedServerNames = oldNamesToNewNames.keySet()
            return Pattern.compile(".*[:][/][/](${deprecatedServerNames.join('|')})[:/?].*")
        }
        // Match nothing. http://stackoverflow.com/questions/940822/regular-expression-syntax-for-match-nothing
        return Pattern.compile('(?!)')
    }

    /**
     * If the request has a server name matching one of the deprecated names, and the request is a GET request from a
     * web browser, then this method determines the full URL of the request and returns a new URL with a replaced
     * server name. If the request does not match all the criteria, this method returns null.
     *
     * @param request the HttpServletRequest
     * @return
     */
    String replaceDeprecatedServerName(HttpServletRequest request) {
        // Only spend time processing more about the request if it is using a deprecated server name.
        Matcher matcher = deprecatedUrlPattern.matcher(request.requestURL)
        if (matcher.matches()) {
            boolean isGet = request.method == 'GET'
            boolean isBrowserRequest = Requests.isBrowser(request)
            if (isBrowserRequest && isGet) {
                String deprecatedServerName = matcher.group(1)
                String newServerName = getReplacementServerName(deprecatedServerName)
                return getFullUrl(request).replace(deprecatedServerName, newServerName)
            }
        }
        null
    }

    /**
     * Gets the new server name that replaces the specified deprecated server name.
     *
     * @param deprecatedServerName the old server name that is no longer preferred
     * @return the new canonical server name for the same server as the deprecated name
     */
    String getReplacementServerName(String deprecatedServerName) {
        configService.deprecatedServerNamesToReplacements[deprecatedServerName]
    }

    /**
     * Gets the full original URL of the user's request including the protocol, server, path, and query params. This may
     * involve unwrapping a deeply nested request from within the chain of request wrappers, in order to find one with
     * a URL that does not mention Grails internal URL components.
     * <p>
     * Cannot get the fragment after the hash symbol (#) because browsers do not send that to the server.
     *
     * @param request the user's request object containing the information to determine the original full URL
     * @return the original full URL of the HTTP request, suitable for sending cross-server redirects
     */
    String getFullUrl(HttpServletRequest request) {
        String queryString = request.queryString
        getNestedRequestWithActualUrl(request).requestURL + (queryString ? "?${queryString}" : '')
    }

    /**
     * Gets the request object that is nested zero or more levels deep within the getRequest().getRequest()... wrapper
     * chain, down to where the request object wrapper has a request URL that no longer includes the extra suffix
     * ".dispatch" which is only used by the internals of Grails and should never be part of a visible URL for a user.
     *
     * @param request the HttpServletRequest that may be a wrapper around more deeply nested HttpServletRequest objects
     * @return the HttpServletRequest object that has a request URL that does not end in Grails suffix ".dispatch"
     */
    HttpServletRequest getNestedRequestWithActualUrl(HttpServletRequest request) {
        if (request instanceof HttpServletRequestWrapper &&
                request.requestURL.toString().endsWith('.dispatch')) {

            return getNestedRequestWithActualUrl(request.request as HttpServletRequest)
        }
        request
    }
}
