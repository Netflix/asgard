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

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.netflix.asgard.Time
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.builder.ReflectionToStringBuilder
import org.apache.commons.logging.LogFactory

/**
 * Easy-to-use wrapper around making a remote call over SSH (Secure Shell).
 */
class SshCaller {

    private static final log = LogFactory.getLog(this)
    private final JSch jSch
    private final String userHome

    /**
     * Constructor.
     *
     * @param jSch (optional) the Java Secure Shell object to operate upon, or a new JSch object if not specified
     * @param userHome the path to the local user home directory
     */
    SshCaller(JSch jSch = new JSch(), String userHome = System.getProperty('user.home')) {
        this.jSch = jSch
        this.userHome = userHome
    }

    /**
     * Performs a remote SSH call.
     *
     * @param userName the user name for logging in to the remote server
     * @param server the name of the server to call
     * @param command the command to execute on the remote server
     * @param password (optional) the user's password to access the remote server, or null if a local RSA key should be
     *          used for passwordless authentication to the remote server
     * @return the output of the remote call, e.g., the contents of a remote file
     */
    String call(String userName, String server, String command, String password = null) {

        String result = null
        Session session = null
        ChannelExec channel = null
        Integer exitStatus = null
        try {
            String dotSshDir = "${userHome}/.ssh"

            // setKnownHosts takes a String or an InputStream. A GString sometimes fails to make Groovy choose the
            // correct overloaded method, so call toString() explicitly.
            jSch.setKnownHosts("${dotSshDir}/known_hosts".toString())

            session = jSch.getSession(userName, server, 22)
            password ? session.password = password : jSch.addIdentity("${dotSshDir}/id_rsa")
            session.connect()
            channel = session.openChannel('exec') as ChannelExec
            channel.setCommand(command)
            channel.setInputStream(null)
            InputStream stdout = channel.inputStream
            InputStream stderr = channel.errStream
            channel.connect(3 * 1000)
            waitForChannelClosed(channel)
            exitStatus = channel.exitStatus
            result = IOUtils.readLines(exitStatus == 0 ? stdout : stderr).join('\n')
        } catch (Exception e) {
            log.error("Error using SSH to connect to ${server}: ${e}", e)
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
        if (exitStatus != 0) {
            log.error "SSH call exited unexpectedly, exit status was ${result}"
            throw new JSchException(result)
        } else {
            log.debug "SSH to ${server} successful"
        }
        result
    }

    /**
     * Waits a while for a channel to close, and throws a {@link JSchException} if closing takes too long.
     *
     * @param channel the channel that is expected to close soon
     */
    private void waitForChannelClosed(ChannelExec channel) {
        for (int i = 0; !channel.isClosed(); i++) {
            Time.sleepCancellably(500)
            if (i > 60) {
                String channelDebug = ReflectionToStringBuilder.toString(channel)
                throw new JSchException("Timeout waiting for SSH channel to close: ${channelDebug}")
            }
        }
    }
}
