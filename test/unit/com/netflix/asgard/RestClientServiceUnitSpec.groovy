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
import org.apache.http.HttpResponse
import org.apache.http.HttpVersion
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicStatusLine
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
class RestClientServiceUnitSpec extends Specification {

    HttpClient httpClient = Mock(HttpClient)
    HttpResponse httpResponse = Mock(HttpResponse)
    ClientConnectionManager clientConnectionManager = Mock(ClientConnectionManager)
    SchemeRegistry schemeRegistry = new SchemeRegistry() // Final class is hard to mock, and not necessary this time
    RestClientService restClientService = new RestClientService(httpClient: httpClient)

    def "should post name value pairs"() {

        when:
        RestResponse restResponse = restClientService.postAsNameValuePairs('http://fakeblock.com',
                [type: 'wood', user: 'George Maharis'])

        then:
        restResponse == new RestResponse(200, '{ "knock": "unknown" }')

        and:
        2 * httpResponse.entity >> new StringEntity('{ "knock": "unknown" }')
        2 * httpResponse.statusLine >> new BasicStatusLine(HttpVersion.HTTP_1_1, 200, 'OK')
        1 * httpClient.execute(_) >> {
            HttpPost request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com'
            assert request.entity.content.text == 'type=wood&user=George+Maharis'
            httpResponse
        }
        0 * _
    }

    def "should throw for HTTP error"() {

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

    def 'should get with SSL'() {
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
