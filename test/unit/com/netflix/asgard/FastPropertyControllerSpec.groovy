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

import com.netflix.asgard.mock.Mocks
import grails.test.mixin.TestFor
import groovy.util.slurpersupport.GPathResult
import spock.lang.Specification

@TestFor(FastPropertyController)
class FastPropertyControllerSpec extends Specification {

    void setup() {
        TestUtils.setUpMockRequest()
        controller.fastPropertyService = Mocks.fastPropertyService()
    }

    GPathResult mockXmlSingle(String key) {
        new XmlSlurper().parseText("""<property>
  <propertyId>${key}|junit|test||||</propertyId>
  <key>${key}</key>
  <value>123</value>
  <env>test</env>
  <appId>junit</appId>
  <countries></countries>
  <serverId></serverId>
  <updatedBy>junit</updatedBy>
  <stack></stack>
  <region></region>
  <sourceOfUpdate>junit</sourceOfUpdate>
  <cmcTicket></cmcTicket>
  <ts>2011-09-27T23:00:10.650Z</ts>
</property>""")
    }

    def 'list should sort by key'() {
        given:
        controller.fastPropertyService = Mocks.fastPropertyService()

        when:
        final Map actual = controller.list()

        then:
        actual.fastProperties*.key == ['greeting.language', 'netflix.epic.plugin.limits.maxInstance']
    }

    def 'show should return data'() {
        given:
        Map expected = [
                fastProperty: FastProperty.fromXml(mockXmlSingle('1'))
        ]
        controller.fastPropertyService = Mock(FastPropertyService)
        controller.params.id = 'hello'

        when:
        Map actual = controller.show()

        //noinspection GroovyPointlessArithmetic
        then:
        1 * controller.fastPropertyService.get(!null, !null) >> FastProperty.fromXml(mockXmlSingle('1'))
        actual.fastProperty.id == expected.fastProperty.id
    }

    def 'create should return list of appNames and regionOptions' () {
        given:
        final List<String> expectedAppNames = ['abcache', 'api', 'aws_stats', 'cryptex', 'helloworld', 'ntsuiboot',
                'videometadata']
        final List expectedRegionOptions = (Region.values() as List) +
                [code: 'us-nflx-1', description: 'us-nflx-1 (Netflix Data Center)']

        controller.fastPropertyService = Mocks.fastPropertyService()
        controller.grailsApplication = Mocks.grailsApplication()

        when:
        final Map actual = controller.create()

        then:
        actual.appNames == expectedAppNames
        actual.regionOptions == expectedRegionOptions
    }

//    def 'save should call platform-service REST API'() {
//        given:
//        FastPropertyController.metaClass.getParams = {
//            [
//                    key: ' property ',
//                    value: ' value ',
//                    appId: 'app-id',
//                    fastPropertyRegion: 'region',
//                    stack: 'stack',
//                    countries: 'countries',
//                    updatedBy: 'user'
//            ]
//        }
//
//        controller.fastPropertyService = Mock(FastPropertyService)
//
//        when:
//        controller.save()
//
//        then:
//        1 * controller.fastPropertyService.create(!null, 'property', 'value', 'app-id', 'region', 'stack', 'countries',
//                'user')
//    }
}
