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
import java.util.concurrent.TimeUnit
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
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
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
import org.apache.http.message.BasicNameValuePair
import org.apache.http.params.HttpConnectionParams
import org.apache.http.util.EntityUtils
import org.springframework.beans.factory.InitializingBean

class RestClientService implements InitializingBean {

    static transactional = false

    def configService

    // Change to PoolingClientConnectionManager after upgrade to http-client 4.2.
    final ThreadSafeClientConnManager connectionManager = new ThreadSafeClientConnManager()
    final HttpClient httpClient = new DefaultHttpClient(connectionManager)

    public void afterPropertiesSet() throws Exception {
        if (configService.proxyHost) {
            final HttpHost proxy = new HttpHost(configService.proxyHost, configService.proxyPort, 'http')
            httpClient.params.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy)
        }
        // Switch to ClientPNames.CONN_MANAGER_TIMEOUT when upgrading http-client 4.2
        httpClient.params.setLongParameter(ConnManagerPNames.TIMEOUT, configService.httpConnPoolTimeout)
        connectionManager.maxTotal = configService.httpConnPoolMaxSize
        connectionManager.defaultMaxPerRoute = configService.httpConnPoolMaxForRoute
    }

    GPathResult getAsXml(String uri, Integer timeoutMillis = 10000) {
        try {
            String content = get(uri, 'application/xml; charset=UTF-8', timeoutMillis)
            return content ? XML.parse(content) as GPathResult : null
        } catch (Exception e) {
            log.error "GET from ${uri} failed: ${e}"
            return null
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
        } catch (Throwable e) {
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
     * @return int the HTTP response code
     */
    int postAsXml(String uriPath, String xml) {
        StringEntity entity = new StringEntity(
                '<?xml version="1.0" encoding="UTF-8" ?>' + xml, "application/xml", "UTF-8")

        HttpPost httpPost = new HttpPost(uriPath)
        httpPost.setEntity(entity)

        executePost(httpPost)
    }

    private int executePost(HttpPost httpPost) {
        executeAndProcessResponse(httpPost) { HttpResponse httpResponse ->
            int statusCode = readStatusCode(httpResponse)
            if (statusCode >= 300) {
                log.error("POST to ${httpPost.URI.path} failed: ${statusCode} " +
                        "${httpResponse.statusLine.reasonPhrase}. Content: ${httpPost.entity}")
            }
            statusCode
        } as int
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
            statusCode = executeAndProcessResponse(getWithTimeout(url, 2000), readStatusCode) as Integer
        } catch (Exception ignored) {
            // Ignore and return null
        }
        statusCode
    }

    Closure readStatusCode = { HttpResponse httpResponse ->
        httpResponse.statusLine.statusCode
    }
}
