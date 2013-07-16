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
package com.netflix.asgard.flow.example

import com.netflix.asgard.flow.LocalWorkflow
import spock.lang.Specification

class HelloWorldWorkflowSpec extends Specification {

    def 'should execute Workflow without SWF'() {

        HelloWorldActivities mockActivities = Mock(HelloWorldActivities)
        HelloWorldWorkflow helloWorldWorkflow = new HelloWorldWorkflowImpl(
                workflow: LocalWorkflow.of(mockActivities))

        when:
        helloWorldWorkflow.helloWorld('Spock Test')

        then:
        with(mockActivities) {
            1 * getHello() >> 'Hi'
            1 * printHello('Hi There Spock Test')
            1 * printHello('cluster1, cluster2')
            1 * getClusterNames() >> ['cluster1', 'cluster2']
            1 * throwException() >> { throw new IllegalStateException('uh oh') }
            1 * printHello('doCatch java.lang.IllegalStateException: uh oh')
            1 * printHello('doFinally')
            1 * takeNap(3L)
            1 * printHello('Awake now!')
        }
        0 * _

    }

}
