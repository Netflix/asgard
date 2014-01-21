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
import com.amazonaws.auth.BasicAWSCredentials
import com.netflix.asgard.ConfigService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import spock.lang.Specification

class SshCredentialsProviderSpec extends Specification {

    AtomicInteger permitAcquisitionAttemptCount = new AtomicInteger(0)
    ConfigService configService = Mock(ConfigService)
    SshRemoteFileReader remoteFileReader = Mock(SshRemoteFileReader)
    SshCredentialsProvider provider
    Runnable runnableFetchProcess = new Runnable() {
        void run() {
            provider.credentials
        }
    }
    Thread thread1 = new Thread(runnableFetchProcess, 'thread1')
    Thread thread2 = new Thread(runnableFetchProcess, 'thread2')
    Thread thread3 = new Thread(runnableFetchProcess, 'thread3')

    def "should get credentials over SSH if properly configured, and cache them for subsequent requests"() {
        provider = new SshCredentialsProvider(configService, remoteFileReader)

        when:
        AWSCredentials credentials1 = provider.getCredentials()
        AWSCredentials credentials2 = provider.getCredentials()

        then:
        1 * configService.online >> true
        1 * configService.accessIdFileName >> '.tombofhorrors'
        1 * configService.secretKeyFileName >> '.joust'
        1 * configService.secretRemoteUser >> 'parzival'
        1 * configService.secretRemoteServer >> 'oasis101'
        1 * configService.secretRemoteDirectory >> '/home/parzival/.lutus'
        1 * remoteFileReader.fetch('parzival', 'oasis101', '/home/parzival/.lutus', '.tombofhorrors') >> 'lichking'
        1 * remoteFileReader.fetch('parzival', 'oasis101', '/home/parzival/.lutus', '.joust') >> 'copperkey'
        0 * _
        credentials1.AWSAccessKeyId == 'lichking'
        credentials1.AWSSecretKey == 'copperkey'
        credentials1.is(credentials2)
    }

    def "should throw an exception if remote server SSH capability is not fully configured"() {
        provider = new SshCredentialsProvider(configService, remoteFileReader)

        when:
        AWSCredentials credentials = provider.credentials

        then:
        1 * configService.online >> online
        1 * configService.accessIdFileName >> accessIdFileName
        1 * configService.secretKeyFileName >> secretKeyFileName
        1 * configService.secretRemoteUser >> user
        1 * configService.secretRemoteServer >> server
        1 * configService.secretRemoteDirectory >> directory
        thrown(AmazonClientException)
        0 * _
        credentials == null

        where:
        online | accessIdFileName | secretKeyFileName | user       | server     | directory
        false  | '.tombofhorrors' | '.joust'          | 'parzival' | 'oasis101' | '/home/parzival/.lutus'
        true   | null             | '.joust'          | 'parzival' | 'oasis101' | '/home/parzival/.lutus'
        true   | '.tombofhorrors' | null              | 'parzival' | 'oasis101' | '/home/parzival/.lutus'
        true   | '.tombofhorrors' | '.joust'          | null       | 'oasis101' | '/home/parzival/.lutus'
        true   | '.tombofhorrors' | '.joust'          | 'parzival' | null       | '/home/parzival/.lutus'
        true   | '.tombofhorrors' | '.joust'          | 'parzival' | 'oasis101' | null
        true   | null             | null              | null       | null       | null
        true   | ''               | ''                | ''         | ''         | ''
    }

    @SuppressWarnings("GroovyAccessibility")
    def 'later threads should wait for first thread to finish fetching credentials, and should not fetch again'() {
        CountDownLatch firstThreadIsFetching = new CountDownLatch(1)
        CountDownLatch secondThreadHasTriedPermitAcquisition = new CountDownLatch(1)

        Closure<AWSCredentials> fetchWithCoordination = { String user, String server, String directory,
                String accessIdFileName, String secretKeyFileName ->

            // For this test, now is the time to signal the second thread to start
            firstThreadIsFetching.countDown()

            // Don't let this first thread finish fetching until the second thread has tried to get a permit
            secondThreadHasTriedPermitAcquisition.await()

            String accessId = remoteFileReader.fetch(user, server, directory, accessIdFileName)
            String secretKey = remoteFileReader.fetch(user, server, directory, secretKeyFileName)
            new BasicAWSCredentials(accessId, secretKey)
        }

        Closure waitForProperTestingState = {
            if (permitAcquisitionAttemptCount.get() >= 1) {
                // The first thread must have attempted to get the permit, so this must be a later thread
                secondThreadHasTriedPermitAcquisition.countDown()
            }
            permitAcquisitionAttemptCount.incrementAndGet()
        }

        provider = new MultiThreadedSshRemoteCredentialsProvider(configService, remoteFileReader,
                waitForProperTestingState, fetchWithCoordination)

        when:
        thread1.start()
        firstThreadIsFetching.await()
        thread2.start() // Now 1 and 2 should both be running
        // Thread 1 is now waiting in "waitForProperTestingState" until thread 2 has tried to acquire a permit
        thread1.join() // Now thread 1 is dead

        // Avoid calling getter, because the getter contains the code that runs in the test threads
        AWSCredentials credentialsAfterThread1 = provider.@credentials

        thread2.join() // Now thread 2 is dead
        AWSCredentials credentialsAfterThread2 = provider.@credentials

        thread3.run() // Start and finish a third thread after the first two are dead
        AWSCredentials credentialsAfterThread3 = provider.@credentials

        then:
        credentialsAfterThread1.is(credentialsAfterThread2)
        credentialsAfterThread1.is(credentialsAfterThread3)
        credentialsAfterThread1.AWSAccessKeyId == 'lichking'
        credentialsAfterThread1.AWSSecretKey == 'copperkey'
        2 == permitAcquisitionAttemptCount.get()

        and: 'configuration should be read for the two concurrent threads but not the third serial thread'
        2 * configService.online >> true
        2 * configService.secretRemoteUser >> 'parzival'
        2 * configService.secretRemoteServer >> 'oasis100'
        2 * configService.secretRemoteDirectory >> '/home/parzival/.lutus'
        2 * configService.accessIdFileName >> '.tombofhorrors'
        2 * configService.secretKeyFileName >> '.joust'

        and: 'files should only be read once'
        1 * remoteFileReader.fetch('parzival', 'oasis100', '/home/parzival/.lutus', '.tombofhorrors') >> 'lichking'
        1 * remoteFileReader.fetch('parzival', 'oasis100', '/home/parzival/.lutus', '.joust') >> 'copperkey'
        0 * _
    }
}

/**
 * A version of SshCredentialsProvider for multi-threaded unit testing, with a function that can be overridden
 * to wait until a certain condition is met during a test case. This is part of the mechanism for a correct
 * "deterministic dance" of waiting between threads.
 * http://briancoyner.github.io/blog/2011/11/21/multi-thread-unit-test/
 */
class MultiThreadedSshRemoteCredentialsProvider extends SshCredentialsProvider {

    /**
     * The extra behavior that should happen after a thread tries to fetch credentials, in order to choreograph
     * the deterministic dance of thread behaviors in a test without sleeping.
     */
    Closure waitForProperTestingState

    /**
     * The algorithm for fetching local credentials, passed in to the constructor of this method in order to enable
     * unit testing of multithreaded behavior.
     */
    Closure<AWSCredentials> fetchWithCoordination

    /**
     * Constructor with configuration closure for waiting for proper testing state between threads.
     *
     * @param configService the means for looking up the info necessary for reading the correct local files
     * @param fileReader the means for reading the contents of a remote file
     * @param waitForProperTestingState closure to execute after attempting to acquire permit
     * @param fetchWithCoordination closure to execute when fetching credentials
     */
    protected MultiThreadedSshRemoteCredentialsProvider(ConfigService configService, SshRemoteFileReader fileReader,
            Closure waitForProperTestingState, Closure<AWSCredentials> fetchWithCoordination) {
        super(configService, fileReader)
        this.waitForProperTestingState = waitForProperTestingState
        this.fetchWithCoordination = fetchWithCoordination
    }

    @Override
    protected void afterAttemptedPermitAcquisition() {
        waitForProperTestingState()
    }

    @Override
    protected AWSCredentials fetchCredentials(String user, String server, String directory, String accessIdFileName,
                                              String secretKeyFileName) {
        fetchWithCoordination(user, server, directory, accessIdFileName, secretKeyFileName)
    }
}
