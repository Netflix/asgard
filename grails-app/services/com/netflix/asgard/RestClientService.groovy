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
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicNameValuePair
import org.apache.http.params.HttpConnectionParams
import org.apache.http.params.HttpParams
import org.apache.http.util.EntityUtils
import org.codehaus.groovy.grails.web.json.JSONElement

class RestClientService {

    static transactional = false

    GPathResult getAsXml(String uri, Integer timeoutMillis = 10000) {
        try {
            String content = get(uri, 'application/xml; charset=UTF-8', timeoutMillis)
            return content ? XML.parse(content) as GPathResult : null
        } catch (Exception e) {
            log.error "GET from ${uri} failed: ${e}"
            return null
        }
    }

    JSONElement getAsJson(String uri, Integer timeoutMillis = 10000) {
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
        HttpClient httpClient = new DefaultHttpClient()
        HttpParams httpParams = httpClient.getParams()
        HttpConnectionParams.setConnectionTimeout(httpParams, timeoutMillis)
        HttpConnectionParams.setSoTimeout(httpParams, timeoutMillis)
        HttpGet httpGet = new HttpGet(uri)
        httpGet.setHeader('Content-Type', contentType)
        httpGet.setHeader('Accept', contentType)
        HttpResponse httpResponse = httpClient.execute(httpGet)
        httpResponse.statusLine.statusCode
        if (httpResponse.statusLine.statusCode == HttpURLConnection.HTTP_OK) {
            HttpEntity httpEntity = httpResponse.getEntity()
            String output = EntityUtils.toString(httpEntity)
            return output
        }
        return null
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
        HttpClient httpClient = new DefaultHttpClient()
        HttpResponse httpResponse = httpClient.execute(httpPost)

        final int statusCode = httpResponse.statusLine.statusCode
        if (statusCode >= 300) {
            log.error("POST to ${httpPost.URI.path} failed: ${statusCode} ${httpResponse.statusLine.reasonPhrase}. " +
                    "Content: ${httpPost.entity}")
        }

        statusCode
    }

    int put(String uri) {
        //println "PUT to $uri"
        URLConnection conn = new URI(uri).toURL().openConnection()
        conn.requestMethod = "PUT"
        conn.connect()
        //if (conn.responseCode != conn.HTTP_OK) {
            //if (conn.responseCode >= 300)
            //    println "PUT to ${uri} failed: ${conn.responseCode}: ${conn.responseMessage}"
        //}
        conn.disconnect()
        conn.responseCode
    }

    int delete(String uri) {
        //println "DELETE of $uri"
        URLConnection conn = new URI(uri).toURL().openConnection()
        conn.requestMethod = "DELETE"
        conn.connect()
        //if (conn.responseCode != conn.HTTP_OK) {
            //if (conn.responseCode >= 300)
            //    println "DELETE of ${uri} failed: ${conn.responseCode}: ${conn.responseMessage}"
        //}
        conn.disconnect()
        conn.responseCode
    }

    Integer getResponseCode(String url) {
        try {
            URLConnection conn = new URL(url).openConnection()
            conn.requestMethod = "GET" // or HEAD, but some apps don't implement it!
            conn.connectTimeout = 2000 // 2 second connect timeout
            conn.readTimeout = 2000 // 2 second read timeout
            //println "checking health @ ${url} => ${conn.responseCode}"
            //if (conn.responseCode == conn.HTTP_OK) {
            //    conn.headerFields.each {println it}
            //    conn.inputStream.withStream {println it.text}
            //}
            //conn.disconnect()
            return conn.responseCode
        } catch (Exception e) {
            //println "checking health @ ${url} failed: ${e}"
            return null
        }
    }
}
