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
import com.netflix.asgard.Check
import com.netflix.asgard.ConfigService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import org.apache.commons.logging.LogFactory

/**
 * {@link com.amazonaws.auth.AWSCredentialsProvider} implementation that provides credentials by reading them from
 * local hidden files that are not Config.groovy and therefore probably not checked into version control. This is useful
 * for cases most of the server configuration needs to be in a versioned Config.groovy but the local keys need to be
 * highly secured and only stored locally on a long-lived Asgard server that few people can access.
 */
class LocalFilesCredentialsProvider extends AbstractCredentialsProvider {

    private static final log = LogFactory.getLog(this)

    /**
     * The lock for a thread to obtain when attempting to fetch credentials. On app initialization, a large number of
     * AWS client objects will be getting initialized in separate threads, and they'll all need to share the same
     * credentials provider, so there's no need to overload the credentials source multiple times.
     */
    private final Semaphore permitToFetchCredentials = new Semaphore(1)

    /**
     * The thread-safe indicator of whether the credentials have been fetched yet.
     */
    private final CountDownLatch credentialsAreFetched = new CountDownLatch(1)

    /**
     * Credentials are cached in this provider object forever, with the assumption that credentials read from a local
     * disk are never expected to change while the application is running.
     */
    protected AWSCredentials credentials

    /**
     * Abstraction for reading a local file. This can be overridden in unit tests.
     */
    private LocalFileReader localFileReader

    /**
     * Constructor with configuration.
     *
     * @param configService the means for looking up the info necessary for reading the correct local files
     * @param fileReader the means for reading the contents of a local file, defaulting to a new LocalFileReader
     */
    LocalFilesCredentialsProvider(ConfigService configService, LocalFileReader fileReader = new LocalFileReader()) {
        super(configService)
        this.localFileReader = fileReader
    }

    @Override
    AWSCredentials getCredentials() {
        ensureCredentialsAreLoaded()
        credentials
    }

    private void ensureCredentialsAreLoaded() {
        // Avoid synchronizing if credentials are already loaded.
        if (credentials) { return }

        String directory = configService.secretLocalDirectory
        String accessIdFileName = configService.accessIdFileName
        String secretKeyFileName = configService.secretKeyFileName

        log.debug "secretLocalDirectory=${directory} accessIdFileName=${accessIdFileName}" +
                "secretKeyFileName=${secretKeyFileName}"

        if (directory && accessIdFileName && secretKeyFileName) {

            // Try to obtain the lock for getting these credentials. If another thread is already holding the lock, then
            // let that other thread do the work. No need to fetch credentials a second time right afterward. However,
            // this thread needs to wait until the fetching process has completed in the other thread before returning.
            boolean obtainedPermitToFetchCredentials = permitToFetchCredentials.tryAcquire()
            afterAttemptedPermitAcquisition()
            if (obtainedPermitToFetchCredentials) {
                try {
                    credentials = fetchCredentials(directory, accessIdFileName, secretKeyFileName)
                } finally {
                    permitToFetchCredentials.release()
                    credentialsAreFetched.countDown()
                }
            } else {
                // Some other thread must be fetching the credentials. This thread should wait until the fetch is done.
                // Then assume the first thread was successful in fetching credentials.
                credentialsAreFetched.await()
            }
        } else {
            throw new AmazonClientException("Unable to load AWS credentials from local files")
        }
    }

    /**
     * Fetches the credentials from local files. This protected method can be overridden by subclasses to enable
     * unit testing multithreaded behavior.
     *
     * @param directory the path to the local directory to look in
     * @param accessIdFileName the name of the file containing the access ID
     * @param secretKeyFileName the name of the file containing the secret key
     * @return credentials from disk
     */
    protected AWSCredentials fetchCredentials(String directory, String accessIdFileName, String secretKeyFileName) {
        log.debug 'Fetching AWS credentials from local files'
        String accessId = localFileReader.readFirstLine(directory, accessIdFileName)
        String secretKey = localFileReader.readFirstLine(directory, secretKeyFileName)
        Check.notEmpty(accessId, accessIdFileName)
        Check.notEmpty(secretKey, secretKeyFileName)
        new BasicAWSCredentials(accessId, secretKey)
    }

    /**
     * Extension hook for subclasses to override in order to add behavior after trying to acquire the permit for the
     * current thread. Useful for unit testing multithreaded behavior.
     */
    protected void afterAttemptedPermitAcquisition() { }
}
