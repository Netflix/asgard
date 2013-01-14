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

import grails.converters.XML
import groovy.util.slurpersupport.GPathResult
import spock.lang.Specification

class FastPropertySpec extends Specification {

    def 'should construct id for valid values'() {
        expect:
        new FastProperty(key: 'dial', env: 'test', appId: 'tap', region: 'eu-west-1', stack: 'spinal', countries: 'UK',
                serverId: 'server1').id == 'dial|tap|test|eu-west-1|server1|spinal|UK'
    }

    def 'should construct id missing optional values'() {
        expect:
        new FastProperty(key: 'dial', value: '11', env: 'test', appId: 'tap', region: 'eu-west-1').
                id == 'dial|tap|test|eu-west-1|||'
    }

    def 'should construct id for invalid values'() {
        expect:
        new FastProperty(key: '_dial_', env: 'test:', appId: '*tap*', region: 'eu-west-1', stack: '!spinal!',
                countries: 'UK').id == '_dial_|*tap*|test:|eu-west-1||!spinal!|UK'
    }

    def 'should validate for valid values'() {
        expect:
        new FastProperty(key: 'dial', env: 'test', appId: 'tap', region: 'eu-west-1', stack: 'spinal',
                countries: 'UK').validateId()
    }

    def 'should fail to validate for invalid values'() {
        when:
        new FastProperty(key: '_dial_', env: 'test:', appId: '*tap*', region: 'eu-west-1', stack: '!spinal!',
                countries: 'UK').validateId()

        then:
        IllegalStateException e = thrown()
        e.message == "Attributes that form a Fast Property ID can only include letters, numbers, dots, underscores, " +
                "and hyphens. The following values are not allowed: appId = '*tap*', env = 'test:', stack = '!spinal!'"
    }

    def 'should construct xml with properties used for creation'() {
        String expectedXml = '''\
                    <property>
                      <key>dial</key>
                      <value>11</value>
                      <env>test</env>
                      <appId>tap</appId>
                      <region>eu-west-1</region>
                      <stack>spinal</stack>
                      <countries>UK</countries>
                      <updatedBy>cmccoy</updatedBy>
                      <sourceOfUpdate>cmccoy</sourceOfUpdate>
                      <cmcTicket>123</cmcTicket>
                    </property>'''.stripIndent()

        expect:
        new FastProperty(key: 'dial', env: 'test', appId: 'tap', region: 'eu-west-1', stack: 'spinal', countries: 'UK',
                serverId: 'server1', value: '11', updatedBy: 'cmccoy', sourceOfUpdate: 'cmccoy', cmcTicket: '123').
                toXml() == expectedXml
    }

    def 'should construct fast property from XML'() {
        String xml = '''\
                    <property>
                      <key>dial</key>
                      <value>11</value>
                      <env>test</env>
                      <appId>tap</appId>
                      <region>eu-west-1</region>
                      <stack>spinal</stack>
                      <countries>UK</countries>
                      <updatedBy>cmccoy</updatedBy>
                      <sourceOfUpdate>cmccoy</sourceOfUpdate>
                      <cmcTicket>123</cmcTicket>
                      <serverId>server1</serverId>
                    </property>'''.stripIndent()

        expect:
        FastProperty.fromXml(XML.parse(xml) as GPathResult) == new FastProperty(key: 'dial', env: 'test', appId: 'tap',
                region: 'eu-west-1', stack: 'spinal', countries: 'UK', serverId: 'server1', value: '11',
                updatedBy: 'cmccoy', sourceOfUpdate: 'cmccoy', cmcTicket: '123')
    }
}
