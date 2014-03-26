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
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class SecretServiceUnitSpec extends Specification {

    static final String SERVER = 'server'
    static final String REMOTE_DIR = 'directory'
    static final String USER = 'user'
    static final String LOAD_BALANCER_USERNAME_FILE = 'lbuserfile'
    static final String LOAD_BALANCER_USER = 'timmy'
    static final String LOAD_BALANCER_PASSWORD_FILE = 'lbpassfile'
    static final String LOAD_BALANCER_PASSWORD = 'chicken42'

    ConfigService configService = Mock(ConfigService)
    EnvironmentService environmentService = Mock(EnvironmentService)

    SshRemoteFileReader remoteFileReader = Mock(SshRemoteFileReader)
    SecretService secretService = new SecretService(configService: configService, sshRemoteFileReader: remoteFileReader,
            environmentService: environmentService)

    def 'should not initialize if offline'() {
        configService.online >> false

        when:
        secretService.afterPropertiesSet()

        then:
        secretService.loadBalancerUserName == null
    }

    def 'should retrieve values from remote server'() {
        configService.online >> true
        configService.loadBalancerUsernameFile >> LOAD_BALANCER_USERNAME_FILE
        configService.loadBalancerPasswordFile >> LOAD_BALANCER_PASSWORD_FILE
        configService.secretRemoteServer >> SERVER
        configService.secretRemoteDirectory >> REMOTE_DIR
        configService.secretRemoteUser >> USER
        mockFetchRemote(LOAD_BALANCER_USERNAME_FILE, LOAD_BALANCER_USER)
        mockFetchRemote(LOAD_BALANCER_PASSWORD_FILE, LOAD_BALANCER_PASSWORD)

        when:
        secretService.afterPropertiesSet()

        then:
        secretService.loadBalancerUserName == LOAD_BALANCER_USER
        secretService.loadBalancerPassword == LOAD_BALANCER_PASSWORD
    }

    void 'should not make remote calls if offline'() {
        configService.online >> false

        when:
        secretService.afterPropertiesSet()

        then:
        secretService.loadBalancerUserName == null
        secretService.loadBalancerPassword == null
    }

    void 'should not make remote calls if running on an AWS instance'() {
        configService.online >> true
        environmentService.instanceId >> 'i-blah'

        when:
        secretService.afterPropertiesSet()

        then:
        secretService.loadBalancerUserName == null
        secretService.loadBalancerPassword == null
    }

    def 'should throw exception if remote file contents empty'() {
        configService.online >> true
        configService.loadBalancerUsernameFile >> LOAD_BALANCER_USERNAME_FILE
        configService.loadBalancerPasswordFile >> LOAD_BALANCER_PASSWORD_FILE
        configService.secretRemoteServer >> SERVER
        configService.secretRemoteDirectory >> REMOTE_DIR
        configService.secretRemoteUser >> USER
        mockFetchRemote(LOAD_BALANCER_USERNAME_FILE, '')

        when:
        secretService.afterPropertiesSet()

        then:
        IllegalArgumentException e = thrown()
        e.message == "ERROR: Illegal empty string for ${LOAD_BALANCER_USERNAME_FILE}"
    }

    private mockFetchRemote(String filename, String contents) {
        remoteFileReader.fetch(USER, SERVER, REMOTE_DIR, filename) >> contents
    }
}
