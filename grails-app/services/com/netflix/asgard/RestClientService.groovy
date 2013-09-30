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

import com.netflix.asgard.format.JsonpStripper
import grails.converters.JSON
import grails.converters.XML
import groovy.util.slurpersupport.GPathResult
import java.security.Security
import java.util.concurrent.TimeUnit
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.conn.params.ConnManagerPNames
import org.apache.http.conn.params.ConnRoutePNames
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
import org.apache.http.message.BasicNameValuePair
import org.apache.http.params.HttpConnectionParams
import org.apache.http.util.EntityUtils
import org.springframework.beans.factory.InitializingBean
import sun.net.InetAddressCachePolicy

@SuppressWarnings('ImportFromSunPackages')
class RestClientService implements InitializingBean {

    static transactional = false

    def configService

    // Change to PoolingClientConnectionManager after upgrade to http-client 4.2.
    final ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager()
    final DefaultHttpClient httpClient = new DefaultHttpClient(connectionManager)


    public void afterPropertiesSet() throws Exception {
        if (configService.proxyHost) {
            final HttpHost proxy = new HttpHost(configService.proxyHost, configService.proxyPort, 'http')
            httpClient.params.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy)
        }
        // Switch to ClientPNames.CONN_MANAGER_TIMEOUT when upgrading http-client 4.2
        httpClient.params.setLongParameter(ConnManagerPNames.TIMEOUT, configService.httpConnPoolTimeout)

        // This retry handler only retries in a few specific failure cases, but it's better than nothing.
        httpClient.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
        // If the AWS Java SDK upgrades to httpclient 4.2.* then here's the new way to set up the client with retries.
        /*
        PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager()
        HttpClient baseClient = new DefaultHttpClient(connectionManager)
        HttpClient httpClient = new AutoRetryHttpClient(baseClient, new DefaultServiceUnavailableRetryStrategy())
        */

        avoidLongCachingOfDnsResults()
        connectionManager.maxTotal = configService.httpConnPoolMaxSize
        connectionManager.defaultMaxPerRoute = configService.httpConnPoolMaxForRoute
    }

    /**
     * We often operate in an environment where we expect resolution of DNS names for remote dependencies to change
     * frequently, so it's best to tell the JVM to avoid caching DNS results internally.
     */
    private avoidLongCachingOfDnsResults() {
        //noinspection GroovyAccessibility
        InetAddressCachePolicy.cachePolicy = InetAddressCachePolicy.NEVER // Groovy doesn't care about privates
        Security.setProperty('networkaddress.cache.ttl', '0')
    }

    def getAsXml(String uri, Integer timeoutMillis = 10000, boolean swallowException = true) {
        try {
            String content = get(uri, 'application/xml; charset=UTF-8', timeoutMillis)
            return content ? XML.parse(content) as GPathResult : null
        } catch (Exception e) {
            log.error "GET from ${uri} failed: ${e}"
            if (swallowException) {
                return null
            } else {
                throw e
            }
        }
    }

    def getAsJson(String uri, Integer timeoutMillis = 10000) {
        try {
            String content = get(uri, 'application/json; charset=UTF-8', timeoutMillis)

            // Strip JSONP padding if needed.
            return content ? JSON.parse(new JsonpStripper(content).stripPadding()) : null
        } catch (Exception e) {
            log.error "GET from ${uri} failed: ${e}"
            return null
        }
    }

    String getAsText(String uri, Integer timeoutMillis = 10000) {
        try {
            return get(uri, 'text/plain; charset=UTF-8', timeoutMillis)
        } catch (Exception e) {
            log.error "GET from ${uri} failed: ${e}"
            return null
        }
    }

    private String get(String uri, String contentType, Integer timeoutMillis) {
        HttpGet httpGet = getWithTimeout(uri, timeoutMillis)
        httpGet.setHeader('Content-Type', contentType)
        httpGet.setHeader('Accept', contentType)
        executeAndProcessResponse(httpGet) { HttpResponse httpResponse ->
            if (readStatusCode(httpResponse) == HttpURLConnection.HTTP_OK) {
                HttpEntity httpEntity = httpResponse.getEntity()
                return EntityUtils.toString(httpEntity)
            }
            return null
        }
    }

    /**
     * Convenience method to create a http-client HttpGet with the timeout parameters set
     *
     * @param uri The uri to connect to.
     * @param timeoutMillis The value to use as socket and connection timeout when making the request
     * @return HttpGet object with parameters set
     */
    private HttpGet getWithTimeout(String uri, int timeoutMillis) {
        HttpGet httpGet = new HttpGet(uri)
        HttpConnectionParams.setConnectionTimeout(httpGet.params, timeoutMillis)
        HttpConnectionParams.setSoTimeout(httpGet.params, timeoutMillis)
        httpGet
    }

    /**
     * Template method to execute a HttpUriRequest object (HttpGet, HttpPost, etc.), process the response with a
     * closure, and perform the cleanup necessary to return the connection to the pool.
     *
     * @param request An http action to execute with httpClient
     * @param responseHandler Handles the response from the request and provides the return value for this method
     * @return The return value of executing responseHandler
     */
    private Object executeAndProcessResponse(HttpUriRequest request, Closure responseHandler) {
        try {
            HttpResponse httpResponse = httpClient.execute(request)
            Object retVal = responseHandler(httpResponse)
            // Ensure the connection gets released to the manager.
            EntityUtils.consume(httpResponse.entity)
            return retVal
        } catch (Exception e) {
            request.abort()
            throw e
        } finally {
            // Save memory per http://stackoverflow.com/questions/4999708/httpclient-memory-management
            connectionManager.closeIdleConnections(60, TimeUnit.SECONDS)
        }
    }

    /**
     * @param uriPath the remote destination
     * @param query the name-value pairs to pass in the post body
     * @return int the HTTP response code
     */
    int post(String uriPath, Map<String, String> query) {
        HttpPost httpPost = new HttpPost(uriPath)
        httpPost.setEntity(new UrlEncodedFormEntity(query.collect { key, value ->
            new BasicNameValuePair(key, value)
        }))
        executePost(httpPost)
    }

    /**
     * @param uriPath the remote destination
     * @param xml the XML string to pass in the post body, excluding the xml header line
     * @return post response
     */
    RestResponse postAsXml(String uriPath, String xml) {
        StringEntity entity = new StringEntity(
                '<?xml version="1.0" encoding="UTF-8" ?>' + xml, "application/xml", "UTF-8")
        HttpPost httpPost = new HttpPost(uriPath)
        httpPost.setEntity(entity)
        RestResponse restResponse = null
        executeAndProcessResponse(httpPost) {
            logErrors(httpPost, it)
            restResponse = new RestResponse(it.statusLine.statusCode, it.entity.content.getText())
        }
        restResponse
    }

    private int executePost(HttpPost httpPost) {
        executeAndProcessResponse(httpPost) { HttpResponse httpResponse ->
            logErrors(httpPost, httpResponse)
            readStatusCode(httpResponse)
        } as int
    }

    private logErrors(HttpPost httpPost, HttpResponse httpResponse) {
        if (readStatusCode(httpResponse) >= 300) {
            log.error("POST to ${httpPost.URI.path} failed: ${readStatusCode(httpResponse)} " +
                    "${httpResponse.statusLine.reasonPhrase}. Content: ${httpPost.entity}")
        }
    }

    int put(String uri) {
        executeAndProcessResponse(new HttpPut(uri), readStatusCode) as int
    }

    int delete(String uri) {
        executeAndProcessResponse(new HttpDelete(uri), readStatusCode) as int
    }

    Integer getResponseCode(String url) {
        Integer statusCode = null
        try {
            int timeoutMillis = configService.restClientTimeoutMillis
            statusCode = executeAndProcessResponse(getWithTimeout(url, timeoutMillis), readStatusCode) as Integer
        } catch (Exception ignored) {
            // Ignore and return null
        }
        statusCode
    }

    Closure readStatusCode = { HttpResponse httpResponse ->
        httpResponse.statusLine.statusCode
    }

    /**
     * Checks an HTTP response code to see if it means "OK".
     *
     * @param responseCode the HTTP response code to check
     * @return true if the code is 200 (OK)
     */
    Boolean checkOkayResponseCode(Integer responseCode) {
        responseCode == 200
    }

    /**
     * Checks the HTTP response code for a URL to see if it's 200 (OK), trying up to three times to see if the URL is
     * generally OK or not.
     *
     * @param url the URL to call
     * @return the response code returned at least once from the URL
     */
    Integer getRepeatedResponseCode(String url) {
        Integer responseCode = getResponseCode(url)
        if (checkOkayResponseCode(responseCode)) {
            return responseCode
        }
        // First try failed but that might have been a network fluke.
        // If the next two staggered attempts pass, then assume the host is healthy.
        int timeoutMillis = configService.restClientTimeoutMillis
        Time.sleepCancellably timeoutMillis
        responseCode = getResponseCode(url)
        if (checkOkayResponseCode(responseCode)) {
            // First try failed, second try passed. Use the tie-breaker as the final answer.
            Time.sleepCancellably timeoutMillis
            return getResponseCode(url)
        }
        // First two tries both failed. Give up and return the latest failure code.
        return responseCode
    }

}
