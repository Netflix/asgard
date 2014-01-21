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

import com.netflix.asgard.cred.LocalFileReader
import com.netflix.asgard.cred.SshRemoteFileReader
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class SecretServiceUnitSpec extends Specification {

    static final String ACCESS_ID = 'accessId'
    static final String ACCESS_ID_FILENAME = ".${ACCESS_ID}"
    static final String SECRET_KEY = 'secretKey'
    static final String SECRET_KEY_FILENAME = ".${SECRET_KEY}"
    static final String SECRET_DIR = 'asgardtmp'
    static final String SERVER = 'server'
    static final String REMOTE_DIR = 'directory'
    static final String USER = 'user'
    static final String LOAD_BALANCER_USERNAME_FILE = 'lbuserfile'
    static final String LOAD_BALANCER_USER = 'lbuser'
    static final String LOAD_BALANCER_PASSWORD_FILE = 'lbpassfile'
    static final String LOAD_BALANCER_PASSWORD = 'lbpass'

    def configService = Mock(ConfigService)
    LocalFileReader localFileReader = Mock(LocalFileReader)
    SshRemoteFileReader remoteFileReader = Mock(SshRemoteFileReader)
    SecretService secretService = new SecretService(configService: configService, localFileReader: localFileReader,
            sshRemoteFileReader: remoteFileReader)

    def 'should not initialize if offline'() {
        configService.online >> false

        when:
        secretService.afterPropertiesSet()

        then:
        secretService.awsCredentials == null
    }

    def 'should read keys if specified directly in the config file'() {
        setupKeysInConfig()

        when:
        secretService.afterPropertiesSet()

        then:
        secretService.awsCredentials.accessKey == ACCESS_ID
        secretService.awsCredentials.secretKey == SECRET_KEY
    }

    def 'should read keys from files on local file system'() {
        setupKeyFilenames()
        configService.secretLocalDirectory >> SECRET_DIR

        when:
        secretService.afterPropertiesSet()

        then:
        secretService.awsCredentials.accessKey == ACCESS_ID
        secretService.awsCredentials.secretKey == SECRET_KEY
        1 * localFileReader.readFirstLine(SECRET_DIR, ACCESS_ID_FILENAME) >> ACCESS_ID
        1 * localFileReader.readFirstLine(SECRET_DIR, SECRET_KEY_FILENAME) >> SECRET_KEY
    }

    def 'should throw exception if local file contents empty'() {
        setupKeyFilenames()
        configService.secretLocalDirectory >> SECRET_DIR

        when:
        secretService.afterPropertiesSet()

        then:
        NullPointerException e = thrown()
        e.message == "ERROR: Trying to use String with null ${ACCESS_ID_FILENAME}"
        localFileReader.readFirstLine(SECRET_DIR, ACCESS_ID_FILENAME) >> null
        localFileReader.readFirstLine(SECRET_DIR, SECRET_KEY_FILENAME) >> null
    }

    def 'should retrieve values from remote server'() {
        setupKeyFilenames()
        configService.loadBalancerUsernameFile >> LOAD_BALANCER_USERNAME_FILE
        configService.loadBalancerPasswordFile >> LOAD_BALANCER_PASSWORD_FILE
        setupRemoteServerInfo()
        mockFetchRemote(ACCESS_ID_FILENAME, ACCESS_ID)
        mockFetchRemote(SECRET_KEY_FILENAME, SECRET_KEY)
        mockFetchRemote(LOAD_BALANCER_USERNAME_FILE, LOAD_BALANCER_USER)
        mockFetchRemote(LOAD_BALANCER_PASSWORD_FILE, LOAD_BALANCER_PASSWORD)

        when:
        secretService.afterPropertiesSet()

        then:
        secretService.awsCredentials.accessKey == ACCESS_ID
        secretService.awsCredentials.secretKey == SECRET_KEY
        secretService.loadBalancerUserName == LOAD_BALANCER_USER
        secretService.loadBalancerPassword == LOAD_BALANCER_PASSWORD
    }

    def 'should throw exception if remote file contents empty'() {
        setupKeyFilenames()
        setupRemoteServerInfo()
        mockFetchRemote(ACCESS_ID_FILENAME, '')

        when:
        secretService.afterPropertiesSet()

        then:
        IllegalArgumentException e = thrown()
        e.message == "ERROR: Illegal empty string for ${ACCESS_ID_FILENAME}"
    }

    private setupKeysInConfig() {
        configService.online >> true
        configService.accessId >> ACCESS_ID
        configService.secretKey >> SECRET_KEY
    }

    private setupRemoteServerInfo() {
        configService.secretRemoteServer >> SERVER
        configService.secretRemoteDirectory >> REMOTE_DIR
        configService.secretRemoteUser >> USER
    }

    private setupKeyFilenames() {
        configService.online >> true
        configService.accessIdFileName >> ACCESS_ID_FILENAME
        configService.secretKeyFileName >> SECRET_KEY_FILENAME
    }

    private mockFetchRemote(String filename, String contents) {
        remoteFileReader.fetch(USER, SERVER, REMOTE_DIR, filename) >> contents
    }
}
