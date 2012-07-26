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

import com.netflix.asgard.ShiroTestUtil
import org.apache.shiro.subject.Subject
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
        ApiToken parsedKey = ApiToken.fromApiTokenString(apiToken.generateTokenString())

        then:
        parsedKey.isValid('key')
    }

    def 'should fail when invalid key used'() {
        ApiToken apiToken = new ApiToken('TestScript', 'testDL@netflix.com', 30, 'key')

        when:
        ApiToken parsedKey = ApiToken.fromApiTokenString(apiToken.generateTokenString())

        then:
        !parsedKey.isValid('invalidKey')
    }

    def 'should handle when username and email the same'() {
        ApiToken apiToken = new ApiToken('TestScript', 'testUser@netflix.com', 30, 'key')

        when:
        ApiToken parsedKey = ApiToken.fromApiTokenString(apiToken.generateTokenString())

        then:
        parsedKey.isValid('key')
    }

    def 'should throw IllegalArgumentException for invalid format'() {
        when:
        ApiToken.fromApiTokenString('bad token')

        then:
        IllegalArgumentException e = thrown()
        e.message == 'Invalid token format'
    }

}
