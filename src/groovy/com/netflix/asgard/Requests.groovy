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

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.codehaus.groovy.grails.web.servlet.FlashScope
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder

class Requests {

    static HttpServletRequest getRequest() {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes()
        HttpServletRequest request = webRequest.getCurrentRequest()
        return request
    }

    private static Map getDeepAttributes(def request) {
        Map attrs = [:]
        request.attributeNames.each { attrName ->
            def attributeValue = request.getAttribute(attrName)
            if (attributeValue && !(attributeValue.toString().toLowerCase().contains('password')) ) {
                attrs.put(attrName, attributeValue)
            }
        }
        if (request.respondsTo('getRequest')) { attrs.putAll(getDeepAttributes(request.getRequest())) }
        attrs
    }

    private static Map getDeepHeaders(def request) {
        Map headers = [:]
        request.headerNames.each { headerName ->
            def headerValue = request.getHeader(headerName)
            if (headerValue) { headers.put(headerName, headerValue) }
        }
        if (request.respondsTo('getRequest')) { headers.putAll(getDeepHeaders(request.getRequest())) }
        headers
    }

    private static Map getDeepParameters(def request) {
        Map parameters = request.parameterMap.findAll { !it.key.toLowerCase().contains('password') } as Map
        if (request.respondsTo('getRequest')) { parameters.putAll(getDeepParameters(request.getRequest())) }
        parameters
    }

    static String stringValue(HttpServletRequest request) {
        Map attrs = getDeepAttributes(request)
        Map headers = getDeepHeaders(request)
        Map paramMap = getDeepParameters(request)

        Map props = [
                "METHOD" : request.method,
                "URL" : request.forwardURI,
                "QUERY": request.queryString?.replaceAll(/password=[^&]*/, 'password=XXXXXXX'),
                "PARAMS" : paramMap.sort { it.key.toLowerCase() },
                "ATTRS" : attrs.sort { it.key.toLowerCase() },
                "COOKIES" : request.cookies,
                "HEADERS" : headers.sort { it.key.toLowerCase() },
                "REMOTEADDR" : request.remoteAddr,
                "REMOTEHOST" : request.remoteHost,
                "CLIENTHOST" : getClientHostName(request),
                "CLIENTIP" : getClientIpAddress(request)
        ]
        prettyPrint(props)
    }

    private static String prettyPrint(def value, int indent = 0, String label = null) {
        String spaces = ' ' * indent
        String output = spaces
        if (label) {
            output += "${label} : "
        }
        if (value instanceof Map) {
            output += '[\n'
            value.each { k, v -> output += prettyPrint(v, indent + 5, k) }
            output += "${spaces}]\n"
        } else if (value instanceof Collection || value?.class?.array) {
            output += '[\n'
            value.each { it -> output += prettyPrint(it, indent + 5) }
            output += "${spaces}]\n"
        } else {
            if (value?.hasProperty('name') && value?.hasProperty('value')) {
                output += "${value.name}=${value.value}"
            } else {
                output += value
            }
            output += ';\n'
        }
        output
    }

    static List<String> ensureList(def stringParam) {
        if (!stringParam) { return [] }
        if (stringParam instanceof String) { return [ stringParam ] }
        if (stringParam instanceof List) { return stringParam }
        if (stringParam instanceof String[]) { return stringParam as List }
        throw new IllegalArgumentException("Expected a string or a list for ${stringParam} but got a ${stringParam.class.name}")
    }

    /**
     * Tells the browser not to cache a page because its contents are expected to change frequently.
     *
     * @param response the http servlet response
     */
    static void preventCaching(HttpServletResponse response) {
        response.setHeader('Cache-Control', 'no-cache') // HTTP 1.1
        response.addHeader('Cache-Control', "no-store") // http://stackoverflow.com/questions/866822/why-both-no-cache-and-no-store-should-be-used-in-http-response
        response.setHeader('Pragma', 'no-cache') // HTTP 1.0
        response.setDateHeader ('Expires', 0) // Prevent caching at the proxy server
    }

    /**
     * Takes an existing Grails parameter map that may have values that are too long for Grails to accept in a redirect
     * without failure,
     * and returns a new Grails parameter map with the offending parameters removed.
     *
     * @param parameterMap the GrailsParameterMap that may contain values that are too long for a redirect
     * @return GrailsParameterMap a safer parameter map object for using in a redirect
     */
    static GrailsParameterMap cap(GrailsParameterMap parameterMap) {
        Map shortParams = parameterMap.findAll { it.key.length() <= 1000 && it.value.length() <= 1000 }
        new GrailsParameterMap(shortParams, parameterMap.request)
    }

    static String getBaseUrl(HttpServletRequest request) {
        String port = (request.serverPort && request.serverPort) == 80 ? '' : ":${request.serverPort}"
        "${request.scheme}://${request.serverName}${port}"
    }

    static void appendMessage(FlashScope flash, String message, String delimiter = " ") {
        flash.message = flash.message ? flash.message + "${delimiter}${message}" : message
    }

    static String getClientIpAddress(HttpServletRequest request) {
        request?.getHeader('ns-client-ip') ?: request?.remoteHost
    }

    static String getClientHostName(HttpServletRequest request) {
        InetAddress.getByName(getClientIpAddress(request)).getHostName()
    }

    static void renderNotFound(String itemType, String itemName, def controller, String moreInfo = '') {
        HttpServletRequest request = getRequest()
        String regionCode = request.region
        String env = request.env
        controller.response.status = 404 // Not found
        String message = "${itemType} '${itemName}' not found in ${regionCode} ${env}"
        if (moreInfo) { message += " $moreInfo" }
        controller.withFormat {
            html {
                controller.flash.message = message
                controller.render(view: '/error/missing')
            }
            xml { controller.render(contentType: 'application/xml') { error(message) } }
            json { controller.render(contentType: 'application/json') { [error: message] } }
        }
    }
}
