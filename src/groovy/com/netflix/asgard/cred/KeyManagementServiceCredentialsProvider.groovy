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
import com.amazonaws.auth.AWSSessionCredentials
import com.amazonaws.auth.BasicSessionCredentials
import com.netflix.asgard.ConfigService
import com.netflix.asgard.RestClientService
import groovy.util.slurpersupport.GPathResult
import org.apache.commons.logging.LogFactory

/**
 * {@link com.amazonaws.auth.AWSCredentialsProvider} implementation that provides short-lived credentials by fetching
 * them from a remote key management service via REST calls, whenever credentials are needed or about to expire.
 * <p>
 * Much of this logic is similar in intent to {@link com.amazonaws.auth.STSSessionCredentialsProvider} except for some
 * important differences. Credentials come from an Amazon Key Management Service (AKMS) that is not created or hosted by
 * Amazon. Unlike Amazon's Secure Token Service (STS), AKMS does not require any long-lived AWS credentials. Instead,
 * AKMS requires that the caller utilize a secure keystore file for signing the SSL request, and that the caller operate
 * from within a known IP space. Using STS would just mean we still need to use AKMS to be safe when getting the
 * long-lived keys for calling STS for the session keys, so it wouldn't be much use.
 * <p>
 * Example output from AKMS, to be parsed:
 * <p>
 * <pre>
 * {@code
 * <com.netflix.cryptexclient.wsclient.domain.WSSessionCredential>
 *   <awsAccessKey>ASIAJABBATHEHUTTR2D2</awsAccessKey>
 *   <awsSecretKey>H+HANSOLODARTHVADERprincessleiachewbacca</awsSecretKey>
 *   <sessionToken>AQ**************************Zn8lgU=</sessionToken>
 *   <expirationEpoch>1390360309000</expirationEpoch>
 * </com.netflix.cryptexclient.wsclient.domain.WSSessionCredential>
 *}
 * </pre>
 */
class KeyManagementServiceCredentialsProvider extends AbstractCredentialsProvider {

    final static long THIRTY_MINUTES_IN_MILLISECONDS = 30 * 60 * 1000L

    private static final log = LogFactory.getLog(this)

    /**
     * Current session credentials.
     */
    private AWSSessionCredentials sessionCredentials

    /**
     * Expiration time for the current session credentials.
     */
    private Date sessionCredentialsExpiration

    /**
     * Mechanism for reading the local keystore file, abstracted for ease of unit testing.
     */
    private LocalFileReader localFileReader

    /**
     * The means to make a call over HTTPS to the key management service.
     */
    private RestClientService restClientService

    /**
     * Mechanism for checking time, overridable for ease of unit testing.
     */
    protected Clock clock

    /**
     * Constructs a new KeyManagementServiceCredentialsProvider which will call the configured key management service
     * endpoint with the configured SSL keystore file stored locally, in order to fetch session credentials that are
     * only valid for a few hours, and then get refreshed with new credentials before expiration.
     *
     * @param configService the means for looking up key management service endpoint and the location of the local
     *          keystore file
     * @param restClientService the means to make a call over HTTPS to the key management service
     * @param localFileReader used to read in a local file
     * @param clock used to check the time to predict session expiration
     */
    KeyManagementServiceCredentialsProvider(ConfigService configService, RestClientService restClientService,
                                            LocalFileReader localFileReader = new LocalFileReader(),
                                            Clock clock = new Clock()) {
        super(configService)
        this.localFileReader = localFileReader
        this.restClientService = restClientService
        this.clock = clock
    }

    @Override
    AWSCredentials getCredentials() {
        if (needsNewSession()) {
            startSession()
        }
        sessionCredentials
    }

    @Override
    void refresh() {
        startSession()
    }

    /**
     * Starts a new AWS session by sending an SSL request (signed with a local keystore file) to a proprietary Amazon
     * Key Management Service (AKMS). This class then vends the short lived session credentials sent back from AKMS.
     */
    protected void startSession() {

        boolean online = configService.online
        String endpoint = configService.keyManagementServiceEndpoint
        Integer sslPort = configService.keyManagementServicePort
        String keyStoreFilePath = configService.keyManagementSslKeyStoreFilePath
        String keyStorePassword = configService.keyManagementSslKeystorePassword

        log.debug "online=${online}, endpoint=${endpoint} port=${sslPort} keyStoreFilePath=${keyStoreFilePath}, " +
                "keyStorePassword=${keyStorePassword}"

        if (online && endpoint && sslPort && keyStoreFilePath && keyStorePassword) {
            log.debug 'Fetching AWS credentials from key management service'
            InputStream keyStoreInputStream = localFileReader.openInputStreamForFilePath(keyStoreFilePath)
            String responseBody = restClientService.getWithSsl(keyStoreInputStream, keyStorePassword, sslPort, endpoint)
            GPathResult xml = new XmlSlurper().parseText(responseBody)
            sessionCredentials = credentialsFromXml(xml)
            sessionCredentialsExpiration = expirationFromXml(xml)
        } else {
            throw new AmazonClientException('Unable to load AWS credentials from key management service')
        }
    }

    @SuppressWarnings("GrUnresolvedAccess")
    private credentialsFromXml(GPathResult xml) {
        String accessKey = xml.awsAccessKey.text()
        String secretKey = xml.awsSecretKey.text()
        String sessionToken = xml.sessionToken.text()
        new BasicSessionCredentials(accessKey, secretKey, sessionToken)
    }

    @SuppressWarnings("GrUnresolvedAccess")
    private expirationFromXml(GPathResult xml) {
        new Date(xml.expirationEpoch.text() as Long)
    }

    /**
     * Returns true if a new session needs to be started because the previous credentials are non-existent, expired, or
     * about to expire within 30 minutes.
     *
     * @return true if new temporary credentials are needed
     */
    protected boolean needsNewSession() {
        if (!sessionCredentials) {
            return true
        }

        long millisecondsRemaining = sessionCredentialsExpiration.time - clock.currentTimeMillis()
        millisecondsRemaining < THIRTY_MINUTES_IN_MILLISECONDS
    }

    /**
     * @return the number of milliseconds since Jan 1, 1970 (can be mocked for unit testing)
     */
    long currentTimeMillis() {
        clock.currentTimeMillis()
    }
}
