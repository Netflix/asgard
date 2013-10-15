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

import com.netflix.asgard.model.WorkflowExecutionBeanOptions
import spock.lang.Specification

class TaskServiceSpec extends Specification {

    def setup() {
        Retriable.mixin(NoDelayRetriableMixin)
    }

    TaskService service = new TaskService(awsSimpleWorkflowService: Mock(AwsSimpleWorkflowService))

    def 'should get task by ID'() {
        when:
        Task task = service.getTaskById('123')

        then:
        task.name == 'clean your room'
        1 * service.awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('123') >>
                Mock(WorkflowExecutionBeanOptions) {
                    asTask() >> new Task(name: 'clean your room')
                }
    }

    def 'should retry get task by ID'() {
        when:
        Task task = service.getTaskById('123')

        then:
        task.name == 'clean your room'
        2 * service.awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('123') >> null
        1 * service.awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('123') >>
                Mock(WorkflowExecutionBeanOptions) {
                    asTask() >> new Task(name: 'clean your room')
                }

    }

    def 'should fail with null'() {
        when:
        service.getTaskById('123') == null

        then:
        3 * service.awsSimpleWorkflowService.getWorkflowExecutionInfoByTaskId('123') >> null
    }
}
