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

import com.netflix.asgard.Check

/**
 * Fetches the contents of a remote text file over SSH, assuming conventional SSH passwordless authentication.
 */
class SshRemoteFileReader {

    private SshCaller sshCaller

    /**
     * Constructor.
     *
     * @param sshCaller the means to execute a remote 'cat' command, defaulting to a new SshCaller instance
     */
    SshRemoteFileReader(SshCaller sshCaller = new SshCaller()) {
        this.sshCaller = sshCaller
    }

    /**
     * Fetches the contents of a remote file over SSH, using default passwordless authentication.
     *
     * @param user the name of the user that can access the remote server
     * @param server the name of the remote server to access
     * @param directoryPath the path to the remote file whose contents should be read
     * @param fileName the name of the file whose contents are needed
     * @return contents of the remote file
     */
    String fetch(String user, String server, String directoryPath, String fileName) {
        String command = "cat ${directoryPath}/${fileName}"
        String secretValue = sshCaller.call(user, server, command)
        Check.notEmpty(secretValue as String, fileName)
    }
}
