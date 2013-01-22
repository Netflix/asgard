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
package com.netflix.asgard.auth

import com.netflix.asgard.mock.ShiroTestUtil
import org.apache.shiro.subject.Subject
import org.joda.time.DateTime
import spock.lang.Specification

class ApiTokenSpec extends Specification {

    def setup() {
        Subject subject = Mock(Subject)
        subject.principal >> 'testUser@netflix.com'
        ShiroTestUtil.setSubject(subject)
    }

    def cleanup() {
        ShiroTestUtil.tearDownShiro()
    }

    def 'should generate and parse a key correctly'() {
        ApiToken apiToken = new ApiToken('TestScript', 'testDL@netflix.com', 30, 'key')

        when:
        String tokenString = apiToken.tokenString
        List<String> tokens = tokenString.tokenize(':')

        then:
        tokens.size() == 5
        tokens[0] == 'TestScript'
        ApiToken.TOKEN_DATE_FORMAT.parseDateTime(tokens[1]) == new DateTime().plusDays(30).withMillisOfDay(0)
        tokens[2] == 'testUser@netflix.com'
        tokens[3] ==~ /[a-zA-Z0-9\%]{8,24}/
        tokens[4] == 'testDL@netflix.com'

        when:
        ApiToken parsedToken = ApiToken.fromApiTokenString(URLDecoder.decode(tokenString))

        then:
        parsedToken.isValid('key')
    }

    def 'should handle when username and email the same'() {
        ApiToken apiToken = new ApiToken('TestScript', 'testUser@netflix.com', 30, 'key')

        when:
        String tokenString = apiToken.tokenString
        List<String> tokens = tokenString.tokenize(':')

        then:
        tokens.size() == 4
        tokens[0] == 'TestScript'
        ApiToken.TOKEN_DATE_FORMAT.parseDateTime(tokens[1]) == new DateTime().plusDays(30).withMillisOfDay(0)
        tokens[2] == 'testUser@netflix.com'
        tokens[3] ==~ /[a-zA-Z0-9\%]{8,24}/

        when:
        ApiToken parsedToken = ApiToken.fromApiTokenString(URLDecoder.decode(tokenString))

        then:
        parsedToken.isValid('key')
    }

    def 'should validate a token that expires 800 years from now'() {
        String semiEternalToken = 'TestScript:2833-12-20:testUser@netflix.com:xh20q18Y:testDL@netflix.com'

        when:
        ApiToken parsedToken = ApiToken.fromApiTokenString(URLDecoder.decode(semiEternalToken))

        then:
        parsedToken.isValid('key')
    }

    def 'should fail when invalid key used'() {
        ApiToken apiToken = new ApiToken('TestScript', 'testDL@netflix.com', 30, 'key')

        when:
        ApiToken parsedToken = ApiToken.fromApiTokenString(apiToken.tokenString)

        then:
        !parsedToken.isValid('invalidKey')
    }

    def 'should throw IllegalArgumentException for invalid format'() {
        when:
        ApiToken.fromApiTokenString('bad token')

        then:
        IllegalArgumentException e = thrown()
        e.message == 'Invalid token format'
    }

    def 'should correctly flag expired tokens'() {
        String expiredToken = 'TestScript:2012-08-05:testUser@netflix.com:CoH6XbjE:testDL@netflix.com'

        when:
        ApiToken parsedToken = ApiToken.fromApiTokenString(expiredToken)

        then:
        parsedToken.expired
        !parsedToken.isValid('key')
    }

}
