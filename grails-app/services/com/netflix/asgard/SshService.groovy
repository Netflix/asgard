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

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.builder.ReflectionToStringBuilder

class SshService {

    static transactional = false

    String call(String userName, String server, String command, String password = null) {

        String result = null
        Session session = null
        ChannelExec channel = null
        int exitStatus
        try {
            String dotSshDir = "${System.getProperty('user.home')}/.ssh"
            JSch jsch = new JSch()

            // setKnownHosts takes a String or an InputStream. A GString sometimes fails to make Groovy choose the
            // correct overloaded method, so call toString() explicitly.
            jsch.setKnownHosts("${dotSshDir}/known_hosts".toString())

            session = jsch.getSession(userName, server, 22)
            password ? session.password = password : jsch.addIdentity("${dotSshDir}/id_rsa")
            session.connect()
            channel = session.openChannel("exec") as ChannelExec
            channel.command = command
            channel.inputStream = null
            InputStream stdout = channel.inputStream
            InputStream stderr = channel.errStream
            channel.connect(3 * 1000)
            waitForChannelClosed(channel)
            exitStatus = channel.exitStatus
            result = IOUtils.readLines(exitStatus == 0 ? stdout : stderr).join('\n')
        } catch (Exception e) {
            log.error "Error using ssh to connect to ${server}"
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
        if (exitStatus != 0) {
            log.error "ssh call exited unexpectedly, result was ${result}"
            throw new JSchException(result)
        } else {
            log.debug "ssh to ${server} successful"
        }
        return result
    }

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
