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

import org.apache.http.HttpResponse
import org.apache.http.HttpVersion
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicStatusLine
import spock.lang.Specification

class RestClientServiceUnitSpec extends Specification {

    HttpClient mockHttpClient = Mock(HttpClient)
    HttpResponse mockHttpResponse = Mock(HttpResponse)

    RestClientService restClientService = new RestClientService(httpClient: mockHttpClient)

    def "should post name value pairs"() {

        when:
        RestResponse response = restClientService.postAsNameValuePairs('http://fakeblock.com',
                [type: 'wood', user: 'George Maharis'])

        then:
        response == new RestResponse(200, '{ "knock": "unknown" }')

        and:
        2 * mockHttpResponse.entity >> new StringEntity('{ "knock": "unknown" }')
        2 * mockHttpResponse.statusLine >> new BasicStatusLine(HttpVersion.HTTP_1_1, 200, 'OK')
        1 * mockHttpClient.execute(_) >> {
            HttpPost request = it[0]
            assert request.getURI().toString() == 'http://fakeblock.com'
            assert request.entity.content.text == 'type=wood&user=George+Maharis'
            mockHttpResponse
        }
        0 * _
    }

    def "should throw for HTTP error"() {

        when:
        restClientService.postAsNameValuePairs('http://fakeblock.com', [type: 'wood', user: 'George Maharis'])

        then:
        thrown(IllegalStateException)

        and:
        1 * mockHttpClient.execute(_) >> {
            throw new IllegalStateException("Uh oh!")
        }
        0 * _
    }
}
