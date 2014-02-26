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

import com.amazonaws.services.simpledb.model.Attribute
import com.amazonaws.services.simpledb.model.Item
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.MonitorBucketType
import spock.lang.Specification

/**
 * Created by danw on 2/11/14.
 */
class AppRegistrationSpec extends Specification {

    void setupSpec() {
        Mocks.createDynamicMethods()
    }

    void "simpledb items are properly coerced"() {
        setup:
            def appName = "test"
            def attrs = getDefaultItemAttrs()
            def item = new ItemBuilder.Builder().name appName attrs attrs build()

        when:
            def appReg = AppRegistration.from(item)

        then:
            appReg.name == appName
            appReg.group == attrs.group
            appReg.type == attrs.type
            appReg.description == attrs.description
            appReg.owner == attrs.owner
            appReg.email == attrs.email
            appReg.monitorBucketType == MonitorBucketType.application
            appReg.createTime.time == Long.parseLong(attrs.createTs)
            appReg.updateTime.time == Long.parseLong(attrs.updateTs)
            appReg.tags == attrs.tags.split(',').collect { it.trim() }
    }

    Map<String, String> getDefaultItemAttrs() {
        def now = new Date()
        [
                group: "myGroup",
                type: "web application",
                description: "test app",
                owner: "EngTools",
                email: "em@il.com",
                monitorBucketType: "application",
                createTs: "${(now - 10).time}",
                updateTs: "${now.time}",
                tags: "Foo,Bar,Baz,Blah"
        ]
    }

    class ItemBuilder {
        private ItemBuilder() { }

        static class Builder {
            private String name
            private final List<Attribute> attrs = []

            def attrs(Map<String, String> map) {
                map.each { k, v ->
                    attrs.add new Attribute(k, v)
                }
                this
            }

            def name(String name) {
                this.name = name
                this
            }

            Item build() {
                new Item(name, attrs)
            }
        }
    }
}
