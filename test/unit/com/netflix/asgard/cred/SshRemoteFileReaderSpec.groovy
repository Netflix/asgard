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

import spock.lang.Specification

class SshRemoteFileReaderSpec extends Specification {

    def "should make an SSH call to fetch file contents"() {
        SshCaller sshCaller = Mock(SshCaller)
        SshRemoteFileReader reader = new SshRemoteFileReader(sshCaller)

        when:
        String output = reader.fetch('wade', 'oasis101', '/home/wade/.lutus', '.tombofhorrors')

        then:
        'copperkey' == output
        1 * sshCaller.call('wade', 'oasis101', 'cat /home/wade/.lutus/.tombofhorrors') >> 'copperkey'
        0 * _
    }
}
