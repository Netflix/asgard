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

import com.netflix.asgard.model.ApplicationInstance
import grails.test.mixin.TestFor
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
@TestFor(InstanceController)
class InstanceControllerSpec extends Specification {

    MergedInstanceGroupingService mergedInstanceGroupingService
    MergedInstance appLess
    MergedInstance hello
    MergedInstance helloAgain
    MergedInstance goodbye
    MergedInstance sorry

    void setup() {
        mergedInstanceGroupingService = Mock(MergedInstanceGroupingService)
        controller.mergedInstanceGroupingService = mergedInstanceGroupingService
        appLess = new MergedInstance()
        hello = new MergedInstance(null, new ApplicationInstance(appName: 'hello'))
        helloAgain = new MergedInstance(null, new ApplicationInstance(appName: 'hello', port: 8080))
        goodbye = new MergedInstance(null, new ApplicationInstance(appName: 'goodbye'))
        sorry = new MergedInstance(null, new ApplicationInstance(appName: 'sorry'))
    }

    def 'list with no id should show all merged instances'() {
        when:
        Map result = controller.list()

        then:
        result.instanceList == [hello, appLess]
        1 * mergedInstanceGroupingService.getMergedInstances(_) >> [appLess, hello]
        0 * _
    }

    def 'list with "noApp" should only show app-less instances'() {
        params.put('id', InstanceController.NO_APP_ID)

        when:
        Map result = controller.list()

        then:
        result.instanceList == [appLess]
        1 * mergedInstanceGroupingService.getMergedInstances(_) >> [hello, appLess]
        0 * _
    }

    def 'list with multiple apps should only request a list of relevant instances, sorted by app name'() {
        params.put('id', 'sorry,goodbye,hello')

        when:
        Map result = controller.list()

        then: 'one method call is made for each app name, and the instances are then sorted by app name'
        result.instanceList == [goodbye, hello, helloAgain, sorry]
        1 * mergedInstanceGroupingService.getMergedInstances(_, 'sorry') >> [sorry]
        1 * mergedInstanceGroupingService.getMergedInstances(_, 'goodbye') >> [goodbye]
        1 * mergedInstanceGroupingService.getMergedInstances(_, 'hello') >> [hello, helloAgain]
        0 * _
    }

    def 'apps should list unique sorted app names of instances'() {

        when:
        Map result = controller.apps()

        then:
        result.appNames == ['goodbye', 'hello', 'sorry']
        result.noAppId == '_noapp'
        1 * mergedInstanceGroupingService.getMergedInstances(_) >> [hello, goodbye, appLess, sorry, helloAgain]
        0 * _
    }
}
