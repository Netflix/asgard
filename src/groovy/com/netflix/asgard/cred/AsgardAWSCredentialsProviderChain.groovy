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

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.auth.InstanceProfileCredentialsProvider
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.auth.SystemPropertiesCredentialsProvider
import com.netflix.asgard.ConfigService
import com.netflix.asgard.RestClientService

/**
 * Similar to {@link com.amazonaws.auth.DefaultAWSCredentialsProviderChain} but with additional ways to obtain AWS
 * credentials, such as through local configuration files, hidden local files, SSH calls to a remote server, and SSL
 * REST calls to a remote key management service.
 *
 * AWS credentials provider chain that looks for credentials in this order:
 * <ul>
 *   <li>Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY</li>
 *   <li>Java System Properties - aws.accessKeyId and aws.secretKey</li>
 *   <li>~/.asgard/Config.groovy - secret.accessId and secret.secretKey</li>
 *   <li>Local files in configured secret.localDirectory with names configured as secret.accessIdFileName and
 *          secret.secretKeyFileName</li>
 *   <li>SSH calls as configured secret.remoteUser to secret.remoteServer in secret.remoteDirectory to read
 *          secret.accessIdFileName and secret.secretKeyFileName</li>
 *   <li>Key management service via SSL REST call using local keystore file</li>
 *   <li>Instance profile credentials delivered through the Amazon EC2 metadata service</li>
 * </ul>
 *
 * @see com.amazonaws.auth.EnvironmentVariableCredentialsProvider
 * @see com.amazonaws.auth.SystemPropertiesCredentialsProvider
 * @see com.amazonaws.auth.InstanceProfileCredentialsProvider
 * @see ConfigCredentialsProvider
 * @see LocalFilesCredentialsProvider
 * @see SshCredentialsProvider
 * @see KeyManagementServiceCredentialsProvider
 */
class AsgardAWSCredentialsProviderChain extends AWSCredentialsProviderChain {
    public AsgardAWSCredentialsProviderChain(ConfigService configService, RestClientService restClientService) {
        super(
                // Varargs in Java constructor work against elegance of super call in Groovy subclass
                [
                        new EnvironmentVariableCredentialsProvider(),
                        new SystemPropertiesCredentialsProvider(),
                        new ConfigCredentialsProvider(configService),
                        new LocalFilesCredentialsProvider(configService),
                        new SshCredentialsProvider(configService),
                        new KeyManagementServiceAssumeRoleCredentialsProvider(configService, restClientService),
                        new KeyManagementServiceCredentialsProvider(configService, restClientService),
                        new STSAssumeRoleSessionCredentialsProvider(configService.assumeRoleArn,
                                configService.assumeRoleSessionName),
                        new InstanceProfileCredentialsProvider()
                ] as AWSCredentialsProvider[]
        )
    }
}
