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
package com.netflix.asgard.collections

import com.netflix.asgard.AppRegistration
import spock.lang.Specification

/**
 * Created by danw on 2/11/14.
 */
class GroupedAppRegistrationSetSpec extends Specification {

    void "app sets are properly sorted"() {
        given:
            def app1 = Mock(AppRegistration)
            app1.getName() >> "aaa"
            def app2 = Mock(AppRegistration)
            app2.getName() >> "bbb"
            def set = new GroupedAppRegistrationSet([app2, app1])

        expect:
            set.first() == app1
            set.last() == app2
    }

    void "app groups are properly populated"() {
        given:
            def app1 = Mock(AppRegistration)
            app1.getGroup() >> "group1"
            def app2 = Mock(AppRegistration)
            app2.getGroup() >> "group1"
            def app3 = Mock(AppRegistration)
            app3.getGroup() >> "group2"
            def app4 = new AppRegistration()
            def appGroups = new GroupedAppRegistrationSet([app1,app2,app3,app4]).groups()

        expect:
            appGroups.keySet().toList() == ['group1', 'group2', '']
            appGroups.group1 == [app1, app2]
            appGroups[""] == [app4]
    }

    void "apps are sorted by tags"() {
        given:
            def app1 = Mock(AppRegistration)
            app1.getTags() >> ['foo', 'bar']
            def tags = new GroupedAppRegistrationSet([app1]).tags()

        expect:
            tags.keySet().toList() == ['foo', 'bar']
            tags.foo == [app1]
            tags.bar == [app1]
    }
}
