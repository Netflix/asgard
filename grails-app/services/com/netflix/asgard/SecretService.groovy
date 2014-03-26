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

import com.netflix.asgard.cred.SshRemoteFileReader
import org.springframework.beans.factory.InitializingBean

class SecretService implements InitializingBean {

    static transactional = false
    def configService
    def environmentService
    SshRemoteFileReader sshRemoteFileReader

    String loadBalancerUserName
    String loadBalancerPassword

    void afterPropertiesSet() {
        if (configService.online && !environmentService.instanceId) {
            sshRemoteFileReader = sshRemoteFileReader ?: new SshRemoteFileReader()
            if (configService.loadBalancerUsernameFile && configService.loadBalancerPasswordFile) {
                loadBalancerUserName = fetchRemote(configService.loadBalancerUsernameFile)
                loadBalancerPassword = fetchRemote(configService.loadBalancerPasswordFile)
            }
        }
    }

    private String fetchRemote(String fileName) {
        String user = configService.secretRemoteUser
        String server = configService.secretRemoteServer
        String directory = configService.secretRemoteDirectory
        String secretValue = sshRemoteFileReader.fetch(user, server, directory, fileName)
        Check.notEmpty(secretValue as String, fileName)
    }
}
