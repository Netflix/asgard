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
package com.netflix.asgard

import com.amazonaws.auth.BasicAWSCredentials
import org.springframework.beans.factory.InitializingBean

class SecretService implements InitializingBean {

    static transactional = false
    def configService
    def sshService

    Map<Region,BasicAWSCredentials> awsCredentials = [:]
    String loadBalancerUserName
    String loadBalancerPassword

    /**
     * A list of keys used for encrypting {@link ApiToken} objects. The first item in the list is the one used for encrypting
     * newly generated API Token. Subsequent items in the list are keys used in the past that should be retired
     * eventually.
     */
    List<String> apiEncryptionKeys = []

    void afterPropertiesSet() {
        if (configService.online) {
            Region.values().each { Region region ->
              String awsAccessId = configService.getAccessId(region) ?: fetch(configService.getAccessIdFileName(region))
              String awsSecretKey = configService.getSecretKey(region) ?: fetch(configService.getSecretKeyFileName(region))
              awsCredentials[region] = new BasicAWSCredentials(awsAccessId, awsSecretKey)
            }
            if (configService.loadBalancerUsernameFile && configService.loadBalancerPasswordFile) {
                loadBalancerUserName = fetchRemote(configService.loadBalancerUsernameFile)
                loadBalancerPassword = fetchRemote(configService.loadBalancerPasswordFile)
            }
            if (configService.apiTokenEnabled) {
                apiEncryptionKeys = configService.apiEncryptionKeys ?: fetchList(configService.apiEncryptionKeyFileName)
            }
        }
    }

    /**
     * @return The current in use encryption key
     */
    String getCurrentApiEncryptionKey() {
        apiEncryptionKeys[0]
    }

    private String fetch(String fileName) {
        def localSecretsDirectory = configService.secretLocalDirectory
        if (localSecretsDirectory) {
            String secretValue = new File("${localSecretsDirectory}/${fileName}").readLines()[0]
            return Check.notEmpty(secretValue as String, fileName)
        }

        fetchRemote(fileName)
    }

    private String fetchRemote(String fileName) {
        String user = configService.secretRemoteUser
        String server = configService.secretRemoteServer
        String directory = configService.secretRemoteDirectory

        String command = "cat ${directory}/${fileName}"
        String secretValue = sshService.call(user, server, command)

        Check.notEmpty(secretValue as String, fileName)
    }

    private List<String> fetchList(String fileName) {
        fileName ? fetch(fileName)?.tokenize() : []
    }
}
