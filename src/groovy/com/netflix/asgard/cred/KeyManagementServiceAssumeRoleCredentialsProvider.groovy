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
import com.amazonaws.services.securitytoken.AWSSecurityTokenService
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest
import com.amazonaws.services.securitytoken.model.AssumeRoleResult
import com.amazonaws.services.securitytoken.model.Credentials
import com.netflix.asgard.ConfigService
import com.netflix.asgard.RestClientService
import org.apache.commons.logging.LogFactory

/**
 * {@link com.amazonaws.auth.AWSCredentialsProvider} implementation that provides short-lived credentials for
 * interacting with a different AWS account (through AssumeRole) than the account whose credentials are provided by the
 * key management service.
 *
 * @see KeyManagementServiceCredentialsProvider
 * @see com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
 */
class KeyManagementServiceAssumeRoleCredentialsProvider extends AbstractCredentialsProvider {

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
     * The provider of the initial credentials that will allow us to call STS for session credentials for a different
     * AWS account.
     */
    KeyManagementServiceCredentialsProvider keyManagementServiceCredentialsProvider

    /**
     * Constructs a new KeyManagementServiceAssumeRoleCredentialsProvider which will call the configured key management
     * service endpoint with the configured SSL keystore file stored locally, in order to fetch session credentials that
     * are only valid for a few hours, and then get refreshed with new credentials before expiration. Those credentials
     * in turn are used to AssumeRole to get Security Token Service (STS) credentials to interact with a different
     * AWS account.
     *
     * @param configService the means for looking up key management service endpoint and the location of the local
     *          keystore file
     * @param restClientService the means to make a call over HTTPS to the key management service
     * @param keyManagementServiceCredentialsProvider the kms provider to use for fetching the initial credentials (can
     *          be null to create a new one)
     */
    KeyManagementServiceAssumeRoleCredentialsProvider(ConfigService configService, RestClientService restClientService,
            KeyManagementServiceCredentialsProvider keyManagementServiceCredentialsProvider = null) {

        super(configService)
        this.keyManagementServiceCredentialsProvider = keyManagementServiceCredentialsProvider ?:
                new KeyManagementServiceCredentialsProvider(configService, restClientService, new LocalFileReader(),
                        new Clock())
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
        keyManagementServiceCredentialsProvider.refresh()
        startSession()
    }

    private void startSession() {

        AWSCredentials credsForSts = keyManagementServiceCredentialsProvider.credentials
        String roleArn = configService.assumeRoleArn
        String roleSessionName = configService.assumeRoleSessionName

        if (credsForSts && roleArn && roleSessionName) {
            log.debug 'Fetching AssumeRole AWS credentials from STS based on credentials from key management service'
            AWSSecurityTokenService securityTokenService = new AWSSecurityTokenServiceClient(credsForSts)
            AssumeRoleRequest request = new AssumeRoleRequest(roleArn: roleArn, roleSessionName: roleSessionName)
            AssumeRoleResult result = securityTokenService.assumeRole(request)
            Credentials stsCredentials = result.credentials
            sessionCredentials = new BasicSessionCredentials(stsCredentials.accessKeyId, stsCredentials.secretAccessKey,
                    stsCredentials.sessionToken)
            sessionCredentialsExpiration = stsCredentials.expiration
        } else {
            String msg = 'Unable to load AssumeRole AWS credentials from STS based on key management credentials'
            throw new AmazonClientException(msg)
        }
    }

    /**
     * Returns true if a new STS session needs to be started. A new STS session
     * is needed when no session has been started yet, or if the last session is
     * within 60 seconds of expiring.
     *
     * @return True if a new STS session needs to be started.
     */
    protected boolean needsNewSession() {
        if (!sessionCredentials || keyManagementServiceCredentialsProvider.needsNewSession()) {
            return true
        }

        long currentTimeMillis = keyManagementServiceCredentialsProvider.currentTimeMillis()
        long millisecondsRemaining = sessionCredentialsExpiration.time - currentTimeMillis
        millisecondsRemaining < (60 * 1000)
    }
}
