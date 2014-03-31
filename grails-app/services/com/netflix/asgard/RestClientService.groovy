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

import grails.converters.JSON
import grails.converters.XML
import groovy.util.slurpersupport.GPathResult
import java.security.KeyStore
import java.security.Security
import java.util.concurrent.TimeUnit
import org.apache.http.Consts
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.params.ClientPNames
import org.apache.http.conn.params.ConnRoutePNames
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.AutoRetryHttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.message.BasicNameValuePair
import org.apache.http.params.HttpConnectionParams
import org.apache.http.util.EntityUtils
import org.codehaus.groovy.grails.web.json.JSONElement
import org.springframework.beans.factory.InitializingBean
import sun.net.InetAddressCachePolicy

@SuppressWarnings('ImportFromSunPackages')
class RestClientService implements InitializingBean {

    static transactional = false

    final static ContentType TEXT_PLAIN_UTF8 = ContentType.create(ContentType.TEXT_PLAIN.mimeType, Consts.UTF_8)
    final static ContentType APPLICATION_XML_UTF8 = ContentType.create(ContentType.APPLICATION_XML.mimeType,
            Consts.UTF_8)
    final static ContentType APPLICATION_JSON_UTF8 = ContentType.APPLICATION_JSON
    final static Integer DEFAULT_TIMEOUT_MILLIS = 10000

    def configService

    final PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager()
    HttpClient httpClient

    public void afterPropertiesSet() throws Exception {
        HttpClient baseClient = new DefaultHttpClient(connectionManager)
        httpClient = new AutoRetryHttpClient(baseClient, new DefaultServiceUnavailableRetryStrategy())
        if (configService.proxyHost) {
            final HttpHost proxy = new HttpHost(configService.proxyHost, configService.proxyPort, 'http')
            httpClient.params.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy)
        }
        httpClient.params.setLongParameter(ClientPNames.CONN_MANAGER_TIMEOUT, configService.httpConnPoolTimeout)

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

    /**
     * Gets text over HTTP with an XML content type, and uses grails.converters.XML to parse the text.
     *
     * @param uri the URI to connect to
     * @param timeoutMillis the value to use as socket and connection timeout when making the request
     * @return the XML parsed object
     */
    def getAsXml(String uri, Integer timeoutMillis = DEFAULT_TIMEOUT_MILLIS,
                 Map<String,String> extraHeaders = [:]) {
        String content = get(uri, APPLICATION_XML_UTF8, timeoutMillis, extraHeaders)
        try {
            return content ? XML.parse(content) as GPathResult : null
        } catch (Exception e) {
            log.error "Parsing XML content from ${uri} failed: ${content}", e
            return null
        }
    }

    /**
     * Gets text over HTTP with a JSON content type, and uses grails.converters.JSON to parse the text.
     *
     * @param uri the URI to connect to
     * @param timeoutMillis the value to use as socket and connection timeout when making the request
     * @return the JSON parsed object
     */
    JSONElement getAsJson(String uri, Integer timeoutMillis = DEFAULT_TIMEOUT_MILLIS,
                          Map<String,String> extraHeaders = [:]) {
        String content = get(uri, APPLICATION_JSON_UTF8, timeoutMillis, extraHeaders)
        try {
            return content ? JSON.parse(content) : null
        } catch (Exception e) {
            log.error "Parsing JSON content from ${uri} failed: ${content}", e
            return null
        }
    }

    /**
     * Gets JSON text over HTTP using a JSON content type, but leaves the payload in text form for alternate parsing.
     *
     * @param uri the URI to connect to
     * @param timeoutMillis the value to use as socket and connection timeout when making the request
     * @return the raw JSON string
     */
    String getJsonAsText(String uri, Integer timeoutMillis = DEFAULT_TIMEOUT_MILLIS,
                         Map<String,String> extraHeaders = [:]) {
        get(uri, APPLICATION_JSON_UTF8, timeoutMillis, extraHeaders)
    }

    /**
     * Gets text over HTTP using a plain text content type, but leaves the payload in text form for alternate parsing.
     *
     * @param uri the URI to connect to
     * @param timeoutMillis the value to use as socket and connection timeout when making the request
     * @return the raw JSON string
     */
    String getAsText(String uri, Integer timeoutMillis = DEFAULT_TIMEOUT_MILLIS,
                     Map<String,String> extraHeaders = [:]) {
        get(uri, TEXT_PLAIN_UTF8, timeoutMillis, extraHeaders)
    }

    private String get(String uri, ContentType contentType = ContentType.DEFAULT_TEXT,
                       Integer timeoutMillis = DEFAULT_TIMEOUT_MILLIS,
                       Map<String,String> extraHeaders = [:]) {
        try {
            HttpGet httpGet = getWithTimeout(uri, timeoutMillis)
            String contentTypeString = contentType.toString()
            httpGet.setHeader('Content-Type', contentTypeString)
            httpGet.setHeader('Accept', contentTypeString)
            extraHeaders.each { header,value -> httpGet.setHeader(header,value) }
            executeAndProcessResponse(httpGet) { HttpResponse httpResponse ->
                if (readStatusCode(httpResponse) == HttpURLConnection.HTTP_OK) {
                    HttpEntity httpEntity = httpResponse.getEntity()
                    return EntityUtils.toString(httpEntity)
                }
                return null
            }
        } catch (Exception e) {
            log.error "GET from ${uri} failed: ${e}"
            return null
        }
    }

    /**
     * Convenience method to create a http-client HttpGet with the timeout parameters set
     *
     * @param uri the URI to connect to
     * @param timeoutMillis the value to use as socket and connection timeout when making the request
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
     * @param request an http action to execute with httpClient
     * @param responseHandler handles the response from the request and provides the return value for this method
     * @return the return value of executing responseHandler
     */
    private Object executeAndProcessResponse(HttpUriRequest request, Closure responseHandler,
                                             Map<String,String> extraHeaders = [:]) {
        extraHeaders.each { header,value -> request.setHeader(header,value) }
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
     * Posts to the URI with a Map of name-value pairs.
     *
     * @param uriPath the remote destination
     * @param nameValuePairs the name-value pairs to pass in the post body
     * @return int the HTTP response code
     */
    RestResponse postAsNameValuePairs(String uriPath, Map<String, String> nameValuePairs,
                                      Map<String, String> extraHeaders = [:] ) {
        HttpPost httpPost = new HttpPost(uriPath)
        if (nameValuePairs) {
            List<NameValuePair> data = nameValuePairs.collect { new BasicNameValuePair(it.key, it.value) }
            httpPost.setEntity(new UrlEncodedFormEntity(data))
        }

        Closure marshallResponse = {
            logErrors(httpPost, it)
            int statusCode = it.statusLine.statusCode
            String content = it.entity.content.getText()
            new RestResponse(statusCode, content)
        }

        executeAndProcessResponse(httpPost, marshallResponse, extraHeaders)
    }

    /**
     * @param uriPath the remote destination
     * @param query the name-value pairs to pass in the post body
     * @return int the HTTP response code
     */
    int post(String uriPath, Map<String, String> query, Map<String, String> extraHeaders = [:]) {
        postAsNameValuePairs(uriPath, query, extraHeaders).statusCode
    }

    /**
     * @param uriPath the remote destination
     * @param xml the XML string to pass in the post body, excluding the xml header line
     * @return post response
     */
    RestResponse postAsXml(String uriPath, String xml, Map<String,String> extraHeaders = [:]) {
        StringEntity entity = new StringEntity(
                '<?xml version="1.0" encoding="UTF-8" ?>' + xml, "application/xml", "UTF-8")
        HttpPost httpPost = new HttpPost(uriPath)
        httpPost.setEntity(entity)
        RestResponse restResponse = null
        executeAndProcessResponse(httpPost, extraHeaders) {
            logErrors(httpPost, it)
            restResponse = new RestResponse(it.statusLine.statusCode, it.entity.content.getText())
        }
        restResponse
    }

    private logErrors(HttpPost httpPost, HttpResponse httpResponse) {
        if (readStatusCode(httpResponse) >= 300) {
            log.error("POST to ${httpPost.URI.path} failed: ${readStatusCode(httpResponse)} " +
                    "${httpResponse.statusLine.reasonPhrase}. Content: ${httpPost.entity}")
        }
    }

    /**
     * Sends an HTTP PUT request.
     *
     * @param uri the URI to connect to
     * @return the HTTP status code of the response
     */
    int put(String uri, Map<String, String> extraHeaders = [:]) {
        executeAndProcessResponse(new HttpPut(uri), readStatusCode, extraHeaders) as int
    }

    /**
     * Sends an HTTP DELETE request.
     *
     * @param uri the URI to connect to
     * @return the HTTP status code of the response
     */
    int delete(String uri, Map<String, String> extraHeaders = [:]) {
        executeAndProcessResponse(new HttpDelete(uri), readStatusCode, extraHeaders) as int
    }

    /**
     * Performs an HTTP GET request on the specified URL, only to check what kind of HTTP status code gets returned.
     *
     * @param url the URL to connect to
     * @return the HTTP status code of the response
     */
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

    private Closure readStatusCode = { HttpResponse httpResponse ->
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

    /**
     * Gets a REST response body from an HTTPS destination, using an SSL key store, probably read from the local disk.
     *
     * @param keyStoreInputStream the stream containing the SSL key store
     * @param keyStorePassword the password for the SSL key store
     * @param port the port to connect on
     * @param endpoint the HTTPS endpoint to call
     * @return the response body
     */
    String getWithSsl(InputStream keyStoreInputStream, String keyStorePassword, Integer port, String endpoint) {

        KeyStore jks = KeyStore.getInstance('JKS')
        jks.load(keyStoreInputStream, keyStorePassword.toCharArray())
        SSLSocketFactory socketFactory = new SSLSocketFactory(SSLSocketFactory.TLS, jks, keyStorePassword, jks, null,
                null, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
        Scheme scheme = new Scheme('https', port, socketFactory)

        // Resembles global shared state. When we get more variety of HTTPS use cases, we might want to rethink this.
        httpClient.connectionManager.schemeRegistry.register(scheme)

        String response = executeAndProcessResponse(new HttpGet(endpoint), { HttpResponse httpResponse ->
            EntityUtils.toString(httpResponse.entity)
        }) as String
        response
    }
}
