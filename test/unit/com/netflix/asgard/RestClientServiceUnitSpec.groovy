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
package com.netflix.asgard

import com.netflix.asgard.mock.MockKeyStoreFileBinaryContents
import groovy.util.slurpersupport.GPathResult
import org.apache.http.HttpResponse
import org.apache.http.HttpVersion
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicStatusLine
import org.codehaus.groovy.grails.web.json.JSONElement
import spock.lang.Ignore
import spock.lang.Specification

@Ignore
@SuppressWarnings("GroovyAssignabilityCheck")
class RestClientServiceUnitSpec extends Specification {

    BasicStatusLine okStatusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, 200, 'OK')
    BasicStatusLine notFoundStatusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, 404, 'NOT_FOUND')
    HttpClient httpClient = Mock(HttpClient)
    HttpResponse httpResponse = Mock(HttpResponse)
    ClientConnectionManager clientConnectionManager = Mock(ClientConnectionManager)
    SchemeRegistry schemeRegistry = new SchemeRegistry() // Final class is hard to mock, and not necessary this time
    ConfigService configService = Mock(ConfigService)
    RestClientService restClientService = new RestClientService(configService: configService, httpClient: httpClient)

    void 'getAsXml should get and parse XML'() {

        when:
        GPathResult appsXml = restClientService.getAsXml('http://fakeblock.com')

        then:
        2 * httpResponse.entity >> new StringEntity('<apps><app>fakeblock</app><app>helloworld</app></apps>')
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpGet request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com'
            assert request.getHeaders('Content-Type')[0].value == 'application/xml; charset=UTF-8'
            assert request.getHeaders('Accept')[0].value == 'application/xml; charset=UTF-8'
            httpResponse
        }
        appsXml.app[0].text() == 'fakeblock'
        appsXml.app[1].text() == 'helloworld'
        0 * _
    }

    void 'getAsXml should support extraHeaders'() {

        when:
        GPathResult appsXml = restClientService.getAsXml('http://fakeblock.com', 300,
                ['x-api-key':'VzcPaqYjsLK47IW3JTkv6OMyxVnmmC'])

        then:
        2 * httpResponse.entity >> new StringEntity('<apps><app>fakeblock</app><app>helloworld</app></apps>')
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpGet request = it[0]
            assert request.getHeaders("x-api-key")[0].value == 'VzcPaqYjsLK47IW3JTkv6OMyxVnmmC'
            httpResponse
        }
        appsXml.app[0].text() == 'fakeblock'
        appsXml.app[1].text() == 'helloworld'
        0 * _
    }

    void 'getAsXml should return null if request for XML returns content that cannot be parsed'() {

        when:
        GPathResult appsXml = restClientService.getAsXml('http://fakeblock.com')

        then:
        2 * httpResponse.entity >> new StringEntity('blah blah this is not XML')
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> httpResponse
        appsXml == null
        0 * _
    }

    void 'getAsXml should return null if XML response status code is not 200'() {

        when:
        GPathResult appsXml = restClientService.getAsXml('http://fakeblock.com')

        then:
        1 * httpResponse.entity >> new StringEntity('<apps><app>fakeblock</app><app>helloworld</app></apps>')
        1 * httpResponse.statusLine >> new BasicStatusLine(HttpVersion.HTTP_1_1, 404, 'NOT_FOUND')
        1 * httpClient.execute(_) >> httpResponse
        appsXml == null
        0 * _
    }

    void 'getAsJson should get and parse JSON'() {

        when:
        JSONElement appJson = restClientService.getAsJson('http://fakeblock.com')

        then:
        2 * httpResponse.entity >> new StringEntity('{"app":"fakeblock"}')
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpGet request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com'
            assert request.getHeaders('Content-Type')[0].value == 'application/json; charset=UTF-8'
            assert request.getHeaders('Accept')[0].value == 'application/json; charset=UTF-8'
            httpResponse
        }
        appJson.app == 'fakeblock'
        0 * _
    }

    void 'getAsJson should support extra headers'() {

        when:
        JSONElement appJson = restClientService.getAsJson('http://fakeblock.com', 300,
                ['x-api-key':'VzcPaqYjsLK47IW3JTkv6OMyxVnmmC'])

        then:
        2 * httpResponse.entity >> new StringEntity('{"app":"fakeblock"}')
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpGet request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com'
            assert request.getHeaders('Content-Type')[0].value == 'application/json; charset=UTF-8'
            assert request.getHeaders('Accept')[0].value == 'application/json; charset=UTF-8'
            assert request.getHeaders("x-api-key")[0].value == 'VzcPaqYjsLK47IW3JTkv6OMyxVnmmC'
            httpResponse
        }
        appJson.app == 'fakeblock'
        0 * _
    }

    void 'getAsJson should return null if request for JSON returns content that cannot be parsed'() {

        when:
        JSONElement appJson = restClientService.getAsJson('http://fakeblock.com')

        then:
        2 * httpResponse.entity >> new StringEntity("{,,;'app':'fakeblock',,,}")
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> httpResponse
        appJson == null
        0 * _
    }

    void 'getAsJson should return null if JSON response status code is not 200'() {

        when:
        JSONElement appJson = restClientService.getAsJson('http://fakeblock.com')

        then:
        1 * httpResponse.entity >> new StringEntity('{"app":"fakeblock"}')
        1 * httpResponse.statusLine >> notFoundStatusLine
        1 * httpClient.execute(_) >> httpResponse
        appJson == null
        0 * _
    }

    void 'getJsonAsText should get raw JSON'() {

        when:
        String appJson = restClientService.getJsonAsText('http://fakeblock.com')

        then:
        2 * httpResponse.entity >> new StringEntity('{"app":"fakeblock"}')
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpGet request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com'
            assert request.getHeaders('Content-Type')[0].value == 'application/json; charset=UTF-8'
            assert request.getHeaders('Accept')[0].value == 'application/json; charset=UTF-8'
            httpResponse
        }
        appJson == '{"app":"fakeblock"}'
        0 * _
    }

    void 'getJsonAsText should return raw text from JSON request even if the content cannot be parsed'() {

        when:
        String appJson = restClientService.getJsonAsText('http://fakeblock.com')

        then:
        2 * httpResponse.entity >> new StringEntity("{,,;'app':'fakeblock',,,}")
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpGet request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com'
            assert request.getHeaders('Content-Type')[0].value == 'application/json; charset=UTF-8'
            assert request.getHeaders('Accept')[0].value == 'application/json; charset=UTF-8'
            httpResponse
        }
        appJson == "{,,;'app':'fakeblock',,,}"
        0 * _
    }

    void 'getJsonAsText should return null if JSON text response status code is not 200'() {

        when:
        String result = restClientService.getJsonAsText('http://fakeblock.com')

        then:
        1 * httpResponse.entity >> new StringEntity('{"app":"fakeblock"}')
        1 * httpResponse.statusLine >> notFoundStatusLine
        1 * httpClient.execute(_) >> httpResponse
        result == null
        0 * _
    }

    void 'getAsText should get plain text over HTTP'() {

        when:
        String result = restClientService.getAsText('http://fakeblock.com')

        then:
        2 * httpResponse.entity >> new StringEntity('Welcome to Fakeblock')
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpGet request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com'
            assert request.getHeaders('Content-Type')[0].value == 'text/plain; charset=UTF-8'
            assert request.getHeaders('Accept')[0].value == 'text/plain; charset=UTF-8'
            httpResponse
        }
        result == 'Welcome to Fakeblock'
        0 * _
    }

    void 'getAsText should support extra headers'() {

        when:
        String result = restClientService.getAsText('http://fakeblock.com', 300,
            ['x-api-key':'VzcPaqYjsLK47IW3JTkv6OMyxVnmmC'])

        then:
        2 * httpResponse.entity >> new StringEntity('Welcome to Fakeblock')
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpGet request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com'
            assert request.getHeaders('Content-Type')[0].value == 'text/plain; charset=UTF-8'
            assert request.getHeaders('Accept')[0].value == 'text/plain; charset=UTF-8'
            assert request.getHeaders("x-api-key")[0].value == 'VzcPaqYjsLK47IW3JTkv6OMyxVnmmC'
            httpResponse
        }
        result == 'Welcome to Fakeblock'
        0 * _
    }

    void 'getAsText should return null if text response status code is not 200'() {

        when:
        String result = restClientService.getAsText('http://fakeblock.com')

        then:
        1 * httpResponse.entity >> new StringEntity('Welcome to Fakeblock')
        1 * httpResponse.statusLine >> notFoundStatusLine
        1 * httpClient.execute(_) >> httpResponse
        result == null
        0 * _
    }

    void "postAsNameValuePairs should post name value pairs"() {

        when:
        RestResponse restResponse = restClientService.postAsNameValuePairs('http://fakeblock.com',
                [type: 'wood', user: 'George Maharis'])

        then:
        restResponse == new RestResponse(200, '{ "knock": "unknown" }')

        and:
        2 * httpResponse.entity >> new StringEntity('{ "knock": "unknown" }')
        2 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpPost request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com'
            assert request.entity.content.text == 'type=wood&user=George+Maharis'
            httpResponse
        }
        0 * _
    }

    void "postAsNameValuePairs should throw for HTTP error"() {

        when:
        restClientService.postAsNameValuePairs('http://fakeblock.com', [type: 'wood', user: 'George Maharis'])

        then:
        thrown(IllegalStateException)

        and:
        1 * httpClient.execute(_) >> {
            throw new IllegalStateException("Uh oh!")
        }
        0 * _
    }

    void "post should post name value pairs"() {

        when:
        int responseCode = restClientService.post('http://fakeblock.com',
                [type: 'wood', user: 'George Maharis'])

        then:
        responseCode == 200

        and:
        2 * httpResponse.entity >> new StringEntity('{ "knock": "unknown" }')
        2 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpPost request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com'
            assert request.entity.content.text == 'type=wood&user=George+Maharis'
            httpResponse
        }
        0 * _
    }

    void "post should support extra headers"() {

        when:
        int responseCode = restClientService.post('http://fakeblock.com',
                [type: 'wood', user: 'George Maharis'],
                ['x-api-key':'VzcPaqYjsLK47IW3JTkv6OMyxVnmmC'])

        then:
        responseCode == 200

        and:
        2 * httpResponse.entity >> new StringEntity('{ "knock": "unknown" }')
        2 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpPost request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com'
            assert request.entity.content.text == 'type=wood&user=George+Maharis'
            assert request.getHeaders("x-api-key")[0].value == 'VzcPaqYjsLK47IW3JTkv6OMyxVnmmC'
            httpResponse
        }
        0 * _
    }

    void "post should throw for HTTP error"() {

        when:
        restClientService.post('http://fakeblock.com', [type: 'wood', user: 'George Maharis'])

        then:
        thrown(IllegalStateException)

        and:
        1 * httpClient.execute(_) >> {
            throw new IllegalStateException("Uh oh!")
        }
        0 * _
    }

    void 'put should send a PUT request to a URL'() {

        when:
        int responseCode = restClientService.put('http://fakeblock.com/knock')

        then:
        responseCode == 200

        and:
        1 * httpResponse.entity >> new StringEntity('{ "knock": "unknown" }')
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpPut request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com/knock'
            assert request.entity == null
            httpResponse
        }
        0 * _
    }

    void 'put should support extra headers'() {

        when:
        int responseCode = restClientService.put('http://fakeblock.com/knock',
                ['x-api-key':'VzcPaqYjsLK47IW3JTkv6OMyxVnmmC'])

        then:
        responseCode == 200

        and:
        1 * httpResponse.entity >> new StringEntity('{ "knock": "unknown" }')
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpPut request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com/knock'
            assert request.entity == null
            assert request.getHeaders("x-api-key")[0].value == 'VzcPaqYjsLK47IW3JTkv6OMyxVnmmC'
            httpResponse
        }
        0 * _
    }

    void 'delete'() {

        when:
        int responseCode = restClientService.delete('http://fakeblock.com/knock')

        then:
        responseCode == 200

        and:
        1 * httpResponse.entity >> new StringEntity('{ "knock": "unknown" }')
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpDelete request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com/knock'
            httpResponse
        }
        0 * _
    }

    void 'delete should support extra headers'() {

        when:
        int responseCode = restClientService.delete('http://fakeblock.com/knock',
                ['x-api-key':'VzcPaqYjsLK47IW3JTkv6OMyxVnmmC'])

        then:
        responseCode == 200

        and:
        1 * httpResponse.entity >> new StringEntity('{ "knock": "unknown" }')
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> {
            HttpDelete request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com/knock'
            assert request.getHeaders("x-api-key")[0].value == 'VzcPaqYjsLK47IW3JTkv6OMyxVnmmC'
            httpResponse
        }
        0 * _
    }

    void 'getResponseCode should just return the response HTTP status code'() {

        when:
        Integer result = restClientService.getResponseCode('http://fakeblock.com')

        then:
        1 * configService.restClientTimeoutMillis >> 1000
        1 * httpResponse.entity >> new StringEntity('{ "knock": "unknown" }')
        1 * httpResponse.statusLine >> okStatusLine
        1 * httpClient.execute(_) >> httpResponse
        result == 200
        0 * _
    }

    void 'checkOkayResponseCode should only return true for 200'() {

        expect:
        restClientService.checkOkayResponseCode(code) == isOk

        where:
        code | isOk
        200  | true
        null | false
        404  | false
        201  | false
        204  | false
    }

    void 'should get with SSL'() {
        InputStream keyStoreInputStream = new ByteArrayInputStream(MockKeyStoreFileBinaryContents.BYTE_ARRAY)
        String endpoint = 'https://akms.example.com:7102/creds;app=asgard;keySet=aws'
        String responseBody = '''\
        <com.netflix.cryptexclient.wsclient.domain.WSSessionCredential>
           <awsAccessKey>ASIAJABBATHEHUTTR2D2</awsAccessKey>
           <awsSecretKey>H+HANSOLODARTHVADERprincessleiachewbacca</awsSecretKey>
           <sessionToken>AQ**************************Zn8lgU=</sessionToken>
           <expirationEpoch>1390360309000</expirationEpoch>
        </com.netflix.cryptexclient.wsclient.domain.WSSessionCredential>'''.stripIndent()

        when:
        String result = restClientService.getWithSsl(keyStoreInputStream, 'changeit', 7013, endpoint)

        then:
        1 * httpClient.connectionManager >> clientConnectionManager
        1 * clientConnectionManager.schemeRegistry >> schemeRegistry
        1 * httpClient.execute({ it.method == 'GET' && it.getURI() == new URI(endpoint) }) >> httpResponse
        2 * httpResponse.entity >> new StringEntity(responseBody)
        result == responseBody
    }
}
