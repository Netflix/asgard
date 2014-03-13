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

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

class TaskSpec extends Specification {

    void 'should create duration string based on time delta'() {

        Date startDate = new Date(start)
        Date updateDate = new Date(update)

        expect:
        duration == new Task(startTime: startDate, updateTime: updateDate, status: "completed").durationString

        where:
        start         | update        | duration
        1290061831444 | 1290066308555 | "1h 14m 37s"
        1290066320888 | 1290066324475 | "3s"
        1290066324444 | 1290239124555 | "2d"
        1290329325444 | 1290329325444 | "0s"
        1290329325444 | 1290329325555 | "0s"
        1290239124333 | 1290239315222 | "3m 10s"
        1290239315666 | 1290242925333 | "1h 9s"
        1290239315666 | 1290329325555 | "1d 1h 9s"
    }

    void 'should output a logical summary sentence with username if available'() {

        when:
        Task task = new Task(name: 'Create ASG helloworld-v001', env: 'prod',
                status: 'completed', userContext: new UserContext(region: Region.US_WEST_2, username: 'hsimpson',
                clientHostName: 'laptop-hsimpson'))

        then:
        task.summary == 'Asgard task completed in prod us-west-2 by hsimpson: Create ASG helloworld-v001'
    }

    void 'should output a logical summary sentence with user hostname if no username is available'() {

        when:
        Task task1 = new Task(name: 'Create ASG helloworld-v001', env: 'prod',
                status: 'completed', userContext: new UserContext(region: Region.US_WEST_2, username: null,
                clientHostName: 'laptop-hsimpson'))

        then:
        task1.summary == 'Asgard task completed in prod us-west-2 by laptop-hsimpson: Create ASG helloworld-v001'
    }

    void 'a Jackson ObjectMapper should construct a Task object from a JSON string'() {

        String json = '''\
                {"id":"555",\
                "name":"Create ASG helloworld-v001",\
                "env":"prod",\
                "status":"completed",\
                "email":"hi@example.com",\
                "objectType":{"name":"cluster"},"objectId":"helloworld",\
                "userContext":{"ticket":"CMC-123","username":"hsimpson","clientHostName":"laptop-hsimpson",\
                "region":"us-west-2","internalAutomation":false,"clientIpAddress":"1.2.3.4"},\
                "log":["Starting task to take over the world","Propaganda posted","World domination in progress",\
                "Suppressing insurgents","Earth conquered"],\
                "operation":"Earth conquered",\
                "startTime":"1394486241000",\
                "updateTime":"1394486242000"}'''.stripIndent()

        when:
        Task task = new ObjectMapper().readValue(json, Task)

        then:
        task == new Task(id: '555',
                name: 'Create ASG helloworld-v001',
                env: 'prod',
                status: 'completed',
                userContext: new UserContext(ticket: 'CMC-123', username: 'hsimpson', clientHostName: 'laptop-hsimpson',
                        region: Region.US_WEST_2, clientIpAddress: '1.2.3.4', internalAutomation: false),
                objectType: EntityType.cluster,
                objectId: 'helloworld',
                email: 'hi@example.com',
                log: ['Starting task to take over the world', 'Propaganda posted',
                        'World domination in progress', 'Suppressing insurgents', 'Earth conquered'],
                operation: 'Earth conquered',
                startTime: new Date(1394486241000),
                updateTime: new Date(1394486242000)
        )
    }

    void 'Jackson should be able to round-trip convert a Task list to a JSON array and back to the same Task list'() {

        List<Task> tasks = [
                new Task(id: '555', name: 'Create ASG helloworld-v001', env: 'prod', status: 'running',
                        userContext: new UserContext(ticket: 'CMC-123', username: 'hsimpson',
                                clientHostName: 'laptop-hsimpson', region: Region.US_EAST_1, clientIpAddress: '1.2.3.4',
                                internalAutomation: false),
                        objectType: EntityType.cluster, objectId: 'helloworld', email: 'hi@example.com',
                        log: ['Get ready', 'Go!'], operation: 'Go!',
                        startTime: new Date(1394486251000), updateTime: new Date(1394486252000)
                ),
                new Task(id: '555', name: 'Disable ASG helloworld-v000', env: 'prod', status: 'completed',
                        userContext: new UserContext(ticket: 'CMC-456', username: 'hsimpson',
                                clientHostName: 'laptop-hsimpson', region: Region.US_WEST_2, clientIpAddress: '1.2.3.4',
                                internalAutomation: false),
                        objectType: EntityType.cluster, objectId: 'helloworld', email: 'hi@example.com',
                        log: ['Preparing to work', 'Doing work', 'Work is done', 'Drinking'], operation: 'Drinking',
                        startTime: new Date(1394486261000), updateTime: new Date(1394486262000)
                )
        ]
        ObjectMapper mapper = new ObjectMapper()

        when:
        String json = mapper.writeValueAsString(tasks)
        List<Task> result = mapper.readValue(json, mapper.typeFactory.constructCollectionType(List, Task))

        then:
        result == tasks
    }

    void 'should get able to parse a JSON blob into a Task even if some fields are missing'() {
        String json = '''\
                {"id":"555",\
                "name":"Create ASG helloworld-v001",\
                "email":"hi@example.com",\
                "objectId":"helloworld",\
                "operation":"Earth conquered",\
                "updateTime":"1394486242000"}'''.stripIndent()

        when:
        Task task = new ObjectMapper().readValue(json, Task)

        then:
        task == new Task(id: '555',
                name: 'Create ASG helloworld-v001',
                objectId: 'helloworld',
                email: 'hi@example.com',
                operation: 'Earth conquered',
                updateTime: new Date(1394486242000)
        )
    }

    void 'should be able to parse a JSON blob into a Task even if there are extra unknown Task fields'() {
        String json = '{"id":"555","magicHead":"Amazing"}'

        when:
        Task task = new ObjectMapper().readValue(json, Task)

        then:
        task == new Task(id: '555')
    }

    void 'should be able to parse a JSON blob into a Task even if there are extra unknown UserContext fields'() {
        String json = '{"id":"555","userContext":{"captain":"picard","ticket":"CMC-123","region":"us-west-2"}}'

        when:
        Task task = new ObjectMapper().readValue(json, Task)

        then:
        task == new Task(id: '555', userContext: new UserContext(ticket: 'CMC-123', region: Region.US_WEST_2))
    }
}
