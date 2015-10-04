/*
 * Copyright 2013 Netflix, Inc.
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
import grails.test.mixin.TestFor
import org.apache.http.HttpStatus
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
@TestFor(TaskController)
class TaskControllerSpec extends Specification {

    TaskService taskService = Mock(TaskService)

    ObjectMapper objectMapper = new ObjectMapper()

    private Task task1 = new Task(id: '789', name: 'Create ASG helloworld-v001', env: 'prod', status: 'running',
            userContext: new UserContext(ticket: 'CMC-123', username: 'hsimpson',
                    clientHostName: 'laptop-hsimpson', region: Region.US_EAST_1,
                    clientIpAddress: '1.2.3.4', internalAutomation: false),
            objectType: EntityType.cluster, objectId: 'helloworld', email: 'hi@example.com',
            log: ['Get ready', 'Go!'], operation: 'Go!',
            startTime: new Date(1394486251000), updateTime: new Date(1394486252000)
    )

    private Task task2 = new Task(id: '456', name: 'Disable ASG helloworld-v000', env: 'prod', status: 'completed',
            userContext: new UserContext(ticket: 'CMC-456', username: 'hsimpson',
                    clientHostName: 'laptop-hsimpson', region: Region.US_WEST_2,
                    clientIpAddress: '1.2.3.4', internalAutomation: false),
            email: 'hi@example.com',
            log: ['Preparing to work', 'Doing work', 'Work is done', 'Drinking'], operation: 'Drinking',
            startTime: new Date(1394486261000), updateTime: new Date(1394486262000)
    )

    private String task1Json = '{"id":"789","userContext":{"ticket":"CMC-123","username":"hsimpson",' +
            '"clientHostName":"laptop-hsimpson","clientIpAddress":"1.2.3.4","region":"us-east-1",' +
            '"internalAutomation":false},"env":"prod","name":"Create ASG helloworld-v001","status":"running",' +
            '"startTime":1394486251000,"updateTime":1394486252000,"email":"hi@example.com","operation":"Go!",' +
            '"objectType":{"name":"cluster"},"objectId":"helloworld",' +
            '"log":["Get ready","Go!"],"server":null}'

    private String task2Json = '{"id":"456","userContext":{"ticket":"CMC-456","username":"hsimpson",' +
            '"clientHostName":"laptop-hsimpson","clientIpAddress":"1.2.3.4","region":"us-west-2",' +
            '"internalAutomation":false},"env":"prod",' +
            '"name":"Disable ASG helloworld-v000","status":"completed","startTime":1394486261000,' +
            '"updateTime":1394486262000,"email":"hi@example.com","operation":"Drinking",' +
            '"objectType":null,"objectId":null,' +
            '"log":["Preparing to work","Doing work","Work is done","Drinking"],"server":null}'

    void setup() {
        TestUtils.setUpMockRequest()
        controller.taskService = taskService
        controller.objectMapper = objectMapper
    }

    void 'should show task info by id'() {
        Task task = new Task(id: '123')

        when:
        controller.params.id = '123'
        def result = controller.show()

        then:
        result == [task: task]
        1 * taskService.getTaskById('123') >> task
        0 * _
        response.status == HttpStatus.SC_OK
    }

    void 'non-existent task should render a "not found" response'() {

        when:
        controller.params.id = 'nosuchluck'
        controller.show()

        then:
        1 * taskService.getTaskById('nosuchluck') >> null
        0 * _
        view == '/error/missing'
        response.status == HttpStatus.SC_NOT_FOUND
    }

    void 'should render JSON array for in-memory task list'() {
        request.format = "json"

        when:
        controller.runningInMemory()

        then:
        1 * taskService.getLocalRunningInMemory() >> [task1, task2]
        0 * _
        response.text == "[${task1Json},${task2Json}]"
        response.status == HttpStatus.SC_OK
    }

    void 'should render empty JSON array for zero-length in-memory task list'() {
        request.format = "json"

        when:
        controller.runningInMemory()

        then:
        1 * taskService.getLocalRunningInMemory() >> []
        0 * _
        response.text == '[]'
        response.status == 200
    }

    void 'should render single JSON task object for specific found task ID'() {
        request.format = "json"

        when:
        controller.runningInMemory('789')

        then:
        1 * taskService.getLocalTaskById('789') >> task1
        0 * _
        response.text == task1Json
        response.status == 200
    }

    void 'should indicate that a specific local task is not found'() {
        request.format = "json"

        when:
        controller.runningInMemory('nosuchluck')

        then:
        1 * taskService.getLocalTaskById('nosuchluck') >> null
        0 * _
        response.text == 'null'
        response.status == 404
    }

    void 'should cancel a local in-memory running task and redirect to the relevant object'() {
        response.format = "form"

        when:
        request.method = 'POST'
        controller.cancel('789')

        then:
        1 * taskService.getTaskById('789') >> task1
        1 * taskService.cancelTask(_, task1)
        0 * _
        flash.message == "Task '789:Create ASG helloworld-v001' canceled."
        response.redirectUrl == '/cluster/show/helloworld'
    }

    void 'should cancel a local in-memory running task and redirect to a generic result'() {
        response.format = "form"

        when:
        request.method = 'POST'
        controller.cancel('456')

        then:
        1 * taskService.getTaskById('456') >> task2
        1 * taskService.cancelTask(_, task2)
        0 * _
        flash.message == "Task '456:Disable ASG helloworld-v000' canceled."
        response.redirectUrl == '/task/list'
    }

    void 'should fail to cancel a non-existent local in-memory running task'() {
        when:
        request.method = 'POST'
        controller.cancel('nosuchluck')

        then:
        1 * taskService.getTaskById('nosuchluck') >> null
        0 * _
        flash.message == "Task 'nosuchluck' not found in us-east-1 test"
        response.redirectUrl == null
        response.status == HttpStatus.SC_NOT_FOUND
        view == '/error/missing'
    }

    void 'should cancel a task and render JSON output'() {
        response.format = "json"

        when:
        request.method = 'POST'
        controller.cancel('456')

        then:
        1 * taskService.getTaskById('456') >> task2
        1 * taskService.cancelTask(_, task2)
        0 * _

        and:
        flash.message == null
        response.redirectUrl == null
        objectMapper.readValue(response.text, Map) == [
                result: "Task '456:Disable ASG helloworld-v000' canceled."
        ]
    }
}
