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
package com.netflix.asgard.flow

import com.amazonaws.services.simpleworkflow.flow.annotations.Execute
import com.amazonaws.services.simpleworkflow.flow.annotations.GetState
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions
import com.netflix.asgard.flow.example.HelloWorldWorkflow
import java.lang.reflect.Method
import spock.lang.Specification

class ReflectionHelperSpec extends Specification {

    ReflectionHelper reflectionHelper = new ReflectionHelper(HelloWorldWorkflow)

    def 'should return method'() {
        expect:
        reflectionHelper.findMethodForNameAndArgs('helloWorld', ['']).name == 'helloWorld'
        !reflectionHelper.findMethodForNameAndArgs('yoWorld', [''])
        !reflectionHelper.findMethodForNameAndArgs('helloWorld', [0])
    }

    class DoSomethinger {
        void doSomething(String s, Integer i) {}
    }

    def 'should match parameter order' () {
        ReflectionHelper reflectionHelper = new ReflectionHelper(DoSomethinger)

        expect:
        reflectionHelper.findMethodForNameAndArgs('doSomething', ['test', 1])
        !reflectionHelper.findMethodForNameAndArgs('doSomething', [1, 'test'])
    }

    def 'should fail if method does not exist'() {
        when:
        reflectionHelper.findMethodForNameAndArgsOrFail('yoWorld', [''])

        then:
        thrown(IllegalStateException)
    }

    def 'should return annotation on method'() {
        Method method = reflectionHelper.findMethodForNameAndArgs('helloWorld',[''])

        expect:
        reflectionHelper.findAnnotationOnMethod(Execute, method).version() == '1.0'
        !reflectionHelper.findAnnotationOnMethod(GetState, method)
    }

    def 'should fail to find annotation on null method'() {
        when:
        reflectionHelper.findAnnotationOnMethod(Execute, null)

        then:
        thrown(IllegalArgumentException)
    }

    def 'should return annotation on class'() {
        expect:
        reflectionHelper.findAnnotationOnClass(WorkflowRegistrationOptions).
                defaultExecutionStartToCloseTimeoutSeconds() == 60L
        !reflectionHelper.findAnnotationOnClass(GetState)
    }
}
