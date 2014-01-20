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
import com.jcraft.jsch.Session
import spock.lang.Specification

class SshCallerSpec extends Specification {

    def "should perform an SSH call"() {

        JSch jSch = Mock(JSch)
        Session session = Mock(Session)
        ChannelExec channel = Mock(ChannelExec)

        SshCaller sshCaller = new SshCaller(jSch, '/Users/wwatts')

        when:
        String output = sshCaller.call('parzival', 'oasis101', 'cat /home/parzival/.lutus/.tombofhorrors')

        then:
        'copperkey' == output
        1 * jSch.setKnownHosts('/Users/wwatts/.ssh/known_hosts')
        1 * jSch.getSession('parzival', 'oasis101', 22) >> session
        1 * jSch.addIdentity('/Users/wwatts/.ssh/id_rsa')
        1 * session.connect()
        1 * session.openChannel('exec') >> channel
        1 * channel.setCommand('cat /home/parzival/.lutus/.tombofhorrors')
        1 * channel.setInputStream(null)
        1 * channel.getInputStream() >> new ByteArrayInputStream('copperkey' as byte[])
        1 * channel.getErrStream() >> Mock(InputStream)
        1 * channel.connect(3000)
        1 * channel.isClosed() >> true
        1 * channel.getExitStatus() >> 0
        1 * channel.disconnect()
        1 * session.disconnect()
        0 * _
    }
}
