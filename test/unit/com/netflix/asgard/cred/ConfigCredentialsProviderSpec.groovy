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
package com.netflix.asgard.cred

import com.amazonaws.AmazonClientException
import com.amazonaws.auth.AWSCredentials
import com.netflix.asgard.ConfigService
import spock.lang.Specification
import spock.lang.Unroll

class ConfigCredentialsProviderSpec extends Specification {

    def 'should get credentials from config service'() {
        ConfigService configService = Mock(ConfigService)

        when:
        AWSCredentials credentials = new ConfigCredentialsProvider(configService).credentials

        then:
        credentials.getAWSAccessKeyId() == '1234'
        credentials.getAWSSecretKey() == 'opensesame'
        1 * configService.getAccessId() >> '1234'
        1 * configService.getSecretKey() >> 'opensesame'
    }

    @Unroll("should throw an exception if configured access id is '#accessId' and secret key is '#secretKey'")
    def "should throw an exception if not configured"() {
        ConfigService configService = Mock(ConfigService)

        when:
        new ConfigCredentialsProvider(configService).credentials

        then:
        thrown(AmazonClientException)
        1 * configService.getAccessId() >> accessId
        1 * configService.getSecretKey() >> secretKey

        where:
        accessId | secretKey
        null     | null
        ''       | null
        null     | ''
        'hello'  | null
        'hello'  | ''
        null     | 'opensesame'
        ''       | 'opensesame'
    }
}
