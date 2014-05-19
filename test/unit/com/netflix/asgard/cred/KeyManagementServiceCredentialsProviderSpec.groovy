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
import com.amazonaws.auth.BasicSessionCredentials
import com.netflix.asgard.ConfigService
import com.netflix.asgard.RestClientService
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class KeyManagementServiceCredentialsProviderSpec extends Specification {

    ConfigService configService = Mock(ConfigService)
    RestClientService restClientService = Mock(RestClientService)
    LocalFileReader localFileReader = Mock(LocalFileReader)
    Clock clock = Mock(Clock)
    InputStream keyStoreInputStream = new ByteArrayInputStream('key store file contents would be here'.bytes)
    String endpoint = 'https://akms.example.com:7102/creds;app=asgard;keySet=aws'
    String responseBody = '''\
                <com.netflix.cryptexclient.wsclient.domain.WSSessionCredential>
                   <awsAccessKey>JABBATHEHUTT</awsAccessKey>
                   <awsSecretKey>H+HANSOLOwookiee</awsSecretKey>
                   <sessionToken>AQ*****Zn8lgU=</sessionToken>
                   <expirationEpoch>1390360309000</expirationEpoch>
                </com.netflix.cryptexclient.wsclient.domain.WSSessionCredential>'''.stripIndent()
    KeyManagementServiceCredentialsProvider provider = new KeyManagementServiceCredentialsProvider(configService,
            restClientService, localFileReader, clock)

    def 'should initially fetch credentials from a key management service over HTTPS with a local SSL keystore'() {

        provider.sessionCredentials = null
        provider.sessionCredentialsExpiration = null

        when:
        BasicSessionCredentials credentials = provider.credentials as BasicSessionCredentials

        then:
        credentials instanceof BasicSessionCredentials
        credentials.AWSAccessKeyId == 'JABBATHEHUTT'
        credentials.AWSSecretKey == 'H+HANSOLOwookiee'
        credentials.sessionToken == 'AQ*****Zn8lgU='
        provider.sessionCredentials.sessionToken == 'AQ*****Zn8lgU='
        interaction {
            getCredentialsInteractions()
        }
    }

    def 'should check if a new session is needed if there are already session credentials'() {

        provider.sessionCredentials = new BasicSessionCredentials('JABBATHEHUTT', 'H+HANSOLOwookiee', 'AQ*****Zn8lgU=')
        provider.sessionCredentialsExpiration = new Date(1390360888000)

        when:
        BasicSessionCredentials credentials = provider.credentials as BasicSessionCredentials

        then:
        1 * clock.currentTimeMillis() >> 1390360887000
        credentials instanceof BasicSessionCredentials
        credentials.AWSAccessKeyId == 'JABBATHEHUTT'
        credentials.AWSSecretKey == 'H+HANSOLOwookiee'
        credentials.sessionToken == 'AQ*****Zn8lgU='
        provider.sessionCredentials.sessionToken == 'AQ*****Zn8lgU='
        interaction {
            getCredentialsInteractions()
        }
    }

    def 'provider refresh should fetch credentials again'() {

        when:
        provider.refresh()

        then:
        provider.sessionCredentials.sessionToken == 'AQ*****Zn8lgU='
        provider.sessionCredentials.AWSAccessKeyId == 'JABBATHEHUTT'
        provider.sessionCredentials.AWSSecretKey == 'H+HANSOLOwookiee'
        interaction {
            getCredentialsInteractions()
        }
    }

    def 'should throw an exception if not fully configured to use key management service'() {

        when:
        BasicSessionCredentials credentials = provider.credentials as BasicSessionCredentials

        then:
        1 * configService.isOnline() >> online
        1 * configService.keyManagementServiceEndpoint >> theEndpoint
        1 * configService.keyManagementServicePort >> port
        1 * configService.keyManagementSslKeyStoreFilePath >> filePath
        1 * configService.keyManagementSslKeystorePassword >> password
        thrown(AmazonClientException)
        0 * _
        credentials == null

        where:
        online | theEndpoint       | port | filePath                  | password
        false  | 'https://kms/key' | 7103 | '/home/.ssl/keystore.jks' | 'changeit'
        true   | null              | null | null                      | null
        true   | ''                | 0    | ''                        | ''
        true   | null              | 7103 | '/home/.ssl/keystore.jks' | 'changeit'
        true   | 'https://kms/key' | null | '/home/.ssl/keystore.jks' | 'changeit'
        true   | 'https://kms/key' | 7103 | null                      | 'changeit'
        true   | 'https://kms/key' | 7103 | '/home/.ssl/keystore.jks' | null
        true   | ''                | 7103 | '/home/.ssl/keystore.jks' | 'changeit'
        true   | 'https://kms/key' | 0    | '/home/.ssl/keystore.jks' | 'changeit'
        true   | 'https://kms/key' | 7103 | ''                        | 'changeit'
        true   | 'https://kms/key' | 7103 | '/home/.ssl/keystore.jks' | ''
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    private getCredentialsInteractions() {
        provider.sessionCredentialsExpiration == new Date(1390360309000)
        1 * configService.isOnline() >> true
        1 * configService.keyManagementServiceEndpoint >> 'https://akms.example.com:7102/creds;app=asgard;keySet=aws'
        1 * configService.keyManagementServicePort >> 7013
        1 * configService.keyManagementSslKeyStoreFilePath >> '/home/parzival/.asgardDevelopment/asgard-dev.keystore'
        1 * configService.keyManagementSslKeystorePassword >> 'changeit'
        1 * localFileReader.openInputStreamForFilePath('/home/parzival/.asgardDevelopment/asgard-dev.keystore') >>
                keyStoreInputStream
        1 * restClientService.getWithSsl(keyStoreInputStream, 'changeit', 7013, endpoint) >> responseBody
        0 * _
    }
}
