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
import com.amazonaws.auth.BasicSessionCredentials
import com.netflix.asgard.ConfigService
import com.netflix.asgard.RestClientService
import spock.lang.Specification

/**
 * Tests for KeyManagementServiceAssumeRoleCredentialsProvider.
 */
@SuppressWarnings(["GroovyAccessibility", "GroovyAssignabilityCheck"])
class KeyManagementServiceAssumeRoleCredentialsProviderSpec extends Specification {

    ConfigService configService = Mock(ConfigService)
    RestClientService restClientService = Mock(RestClientService)
    LocalFileReader localFileReader = Mock(LocalFileReader)
    Clock clock = Mock(Clock)
    KeyManagementServiceCredentialsProvider initialProvider = Mock(KeyManagementServiceCredentialsProvider)
    def provider = new KeyManagementServiceAssumeRoleCredentialsProvider(configService, restClientService,
            initialProvider)

    void 'should need a new session if session credentials are missing'() {
        provider.sessionCredentials = null

        expect:
        provider.needsNewSession()
    }

    void 'should need a new session if there are already creds but initial provider needs a new session'() {
        provider.sessionCredentials = new BasicSessionCredentials('JABBATHEHUTT', 'H+HANSOLOwookiee', 'AQ*****Zn8lgU=')

        when:
        boolean needsNewSession = provider.needsNewSession()

        then:
        needsNewSession
        1 * initialProvider.needsNewSession() >> true
        0 * _
    }

    void 'should need a new session if the time is close to running out'() {
        provider.sessionCredentials = new BasicSessionCredentials('JABBATHEHUTT', 'H+HANSOLOwookiee', 'AQ*****Zn8lgU=')
        provider.sessionCredentialsExpiration = new Date(1390360888000)

        when:
        boolean needsNewSession = provider.needsNewSession()

        then:
        needsNewSession
        1 * initialProvider.needsNewSession() >> false
        1 * initialProvider.currentTimeMillis() >> 1390360887000
    }

    void 'should not need a new session if the expiration time is a long time in the future'() {
        provider.sessionCredentials = new BasicSessionCredentials('JABBATHEHUTT', 'H+HANSOLOwookiee', 'AQ*****Zn8lgU=')
        provider.sessionCredentialsExpiration = new Date(1400000000000)

        when:
        boolean needsNewSession = provider.needsNewSession()

        then:
        !needsNewSession
        1 * initialProvider.needsNewSession() >> false
        1 * initialProvider.currentTimeMillis() >> 1390360887000
    }

    void 'should return cached credentials if no new session is needed'() {

        AWSCredentials credentials = new BasicSessionCredentials('JABBA', 'HANSOLO', 'AQ****U=')
        provider.sessionCredentials = credentials
        provider.sessionCredentialsExpiration = new Date(1400000000000)

        when:
        AWSCredentials result = provider.getCredentials()

        then:
        result.is credentials
        1 * initialProvider.needsNewSession() >> false
        1 * initialProvider.currentTimeMillis() >> 1390360887000
    }

    void 'should fail to get new session credentials if not fully configured'() {

        when:
        provider.getCredentials()

        then:
        initialProvider.credentials >> creds
        configService.assumeRoleArn >> arn
        configService.assumeRoleSessionName >> session
        thrown(AmazonClientException)

        where:
        arn        | session             | creds
        null       | 'asgardtestsession' | new BasicSessionCredentials('JABBA', 'HANSOLO', 'AQ****U=')
        ''         | 'asgardtestsession' | new BasicSessionCredentials('JABBA', 'HANSOLO', 'AQ****U=')
        'rolearn1' | ''                  | new BasicSessionCredentials('JABBA', 'HANSOLO', 'AQ****U=')
        'rolearn1' | null                | new BasicSessionCredentials('JABBA', 'HANSOLO', 'AQ****U=')
        'rolearn1' | 'asgardtestsession' | null
    }

    void 'should fail to refresh credentials if not fully configured'() {

        when:
        provider.refresh()

        then:
        1 * initialProvider.refresh()
        initialProvider.credentials >> creds
        configService.assumeRoleArn >> arn
        configService.assumeRoleSessionName >> session
        thrown(AmazonClientException)

        where:
        arn        | session             | creds
        null       | 'asgardtestsession' | new BasicSessionCredentials('JABBA', 'HANSOLO', 'AQ****U=')
        ''         | 'asgardtestsession' | new BasicSessionCredentials('JABBA', 'HANSOLO', 'AQ****U=')
        'rolearn1' | ''                  | new BasicSessionCredentials('JABBA', 'HANSOLO', 'AQ****U=')
        'rolearn1' | null                | new BasicSessionCredentials('JABBA', 'HANSOLO', 'AQ****U=')
        'rolearn1' | 'asgardtestsession' | null
    }
}
