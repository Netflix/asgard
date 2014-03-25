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
package com.netflix.asgard

import grails.test.mixin.TestFor
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
@TestFor(ServerController)
class ServerControllerSpec extends Specification {

    ServerService serverService = Mock(ServerService)
    TaskService taskService = Mock(TaskService)
    WitherService witherService = Mock(WitherService)
    Properties systemProps
    Map<String, String> env

    void setup() {
        systemProps = new Properties()
        systemProps.setProperty('user.language', 'en')
        systemProps.setProperty('grails.env', 'staging')
        systemProps.setProperty('aws.accessKeyId', 'JABBATHEHUTT')
        systemProps.setProperty('aws.secretKey', 'H+HANSOLOwookiee')
        env = [
                'JAVA_HOME': '/apps/java',
                'AWS_ACCESS_KEY': 'JABBATHEHUTT',
                'AWS_ACCESS_KEY_ID': 'JABBATHEHUTT',
                'AWS_SECRET_KEY': 'H+HANSOLOwookiee',
                'AWS_SECRET_ACCESS_KEY': 'H+HANSOLOwookiee',
                'SHELL': '/bin/bash'
        ]
        controller.serverService = serverService
        controller.witherService = witherService
    }

    def 'should hide sensitive values in system properties and environment variables'() {
        when:
        def attrs = controller.props()

        then:
        1 * serverService.systemProperties >> systemProps
        1 * serverService.environmentVariables >> env
        attrs.environmentVariables == [
                'AWS_ACCESS_KEY': '[hidden]',
                'AWS_ACCESS_KEY_ID': '[hidden]',
                'AWS_SECRET_KEY': '[hidden]',
                'AWS_SECRET_ACCESS_KEY': '[hidden]',
                'JAVA_HOME': '/apps/java',
                'SHELL': '/bin/bash'
        ]
        attrs.systemProperties == [
                'aws.accessKeyId': '[hidden]',
                'aws.secretKey': '[hidden]',
                'grails.env': 'staging',
                'user.language': 'en'
        ]
        0 * _
    }

    void 'should show local in-memory running task count'() {

        controller.taskService = taskService

        when:
        controller.runningTaskCount()

        then:
        response.contentAsString == '3'
        taskService.getLocalRunningInMemory() >> [new Task(), new Task(), new Task()]
    }

    void 'should start wither process'() {

        when:
        controller.startWither()

        then:
        1 * witherService.startWither()
        flash.messages == ['Started withering process to terminate current instance or ASG after tasks are drained']
        0 * _
    }

    void 'should cancel wither process'() {

        when:
        controller.cancelWither()

        then:
        1 * witherService.cancelWither() >> ['Cancelled']
        flash.messages == ['Cancelled']
        0 * _
    }
}
