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
import org.joda.time.DateTime
import spock.lang.Specification
import spock.lang.Unroll

class FastPropertySpec extends Specification {

    def 'should validate for valid values'() {
        expect:
        new FastProperty(key: 'dial', value: 'value', env: 'test', appId: 'tap', region: 'eu-west-1', stack: 'spinal',
                countries: 'UK').validate()
    }

    def 'should fail to validate for invalid values used in ID'() {
        when:
        new FastProperty(key: '_dial_', value: 'value', env: 'test:', appId: '*tap*', region: 'eu-west-1', stack: '!spinal!',
                countries: 'UK').validate()

        then:
        IllegalStateException e = thrown()
        e.message == "Attributes that form a Fast Property ID can only include letters, numbers, dots, underscores, " +
                "and hyphens. The following values are not allowed: appId = '*tap*', env = 'test:', stack = '!spinal!'"
    }

    def 'should fail to validate for missing key'() {
        when:
        new FastProperty(env: 'test:', appId: '*tap*', region: 'eu-west-1', stack: '!spinal!',
                countries: 'UK').validate()

        then:
        IllegalStateException e = thrown()
        e.message == 'A Fast Property key is required.'
    }

    def 'should fail to validate for missing value'() {
        when:
        new FastProperty(key: '_dial_', env: 'test:', appId: '*tap*', region: 'eu-west-1', stack: '!spinal!',
                countries: 'UK').validate()

        then:
        IllegalStateException e = thrown()
        e.message == 'A Fast Property value is required.'
    }

    def 'should construct xml with properties used for creation'() {
        String expectedXml = '''\
                    <property>
                      <appId>tap</appId>
                      <cmcTicket>123</cmcTicket>
                      <countries>UK</countries>
                      <env>test</env>
                      <key>dial</key>
                      <region>eu-west-1</region>
                      <serverId>server1</serverId>
                      <sourceOfUpdate>cmccoy</sourceOfUpdate>
                      <stack>spinal</stack>
                      <updatedBy>cmccoy</updatedBy>
                      <value>11</value>
                    </property>'''.stripIndent()

        expect:
        new FastProperty(key: 'dial', env: 'test', appId: 'tap', region: 'eu-west-1', stack: 'spinal', countries: 'UK',
                serverId: 'server1', value: '11', updatedBy: 'cmccoy', sourceOfUpdate: 'cmccoy', cmcTicket: '123').
                toXml() == expectedXml
    }

    def 'should construct fast property from XML'() {
        String xml = '''\
                    <property>
                      <appId>tap</appId>
                      <cmcTicket>123</cmcTicket>
                      <countries>UK</countries>
                      <env>test</env>
                      <key>dial</key>
                      <propertyId>id</propertyId>
                      <region>eu-west-1</region>
                      <serverId>server1</serverId>
                      <sourceOfUpdate>cmccoy</sourceOfUpdate>
                      <stack>spinal</stack>
                      <updatedBy>cmccoy</updatedBy>
                      <value>11</value>
                      <constraints>{length: {min: 2, max: 10}}</constraints>
                    </property>'''.stripIndent()

        expect:
        FastProperty.fromXml(XML.parse(xml) as GPathResult) == new FastProperty(key: 'dial', env: 'test', appId: 'tap',
                region: 'eu-west-1', stack: 'spinal', countries: 'UK', serverId: 'server1', value: '11',
                updatedBy: 'cmccoy', sourceOfUpdate: 'cmccoy', cmcTicket: '123', ts: '', id: 'id', ami: '', asg: '',
                cluster: '', ttl: '', zone: '', constraints: '{length: {min: 2, max: 10}}')
    }

    def 'should determine if fast property has advanced attributes'() {
        expect:
        !new FastProperty(key: 'dial', value: '11', updatedBy: 'cmccoy', stack: 'spinal', appId: 'tap',
                region: 'eu-west-1').hasAdvancedAttributes()
        new FastProperty(key: 'dial', value: '11', updatedBy: 'cmccoy', stack: 'spinal', appId: 'tap',
                region: 'eu-west-1', ttl: '999').hasAdvancedAttributes()
        new FastProperty(key: 'dial', value: '11', updatedBy: 'cmccoy', stack: 'spinal', appId: 'tap',
                region: 'eu-west-1', asg: 'asg').hasAdvancedAttributes()
    }

    def 'should calculate expiration'() {
        expect:
        new FastProperty(ts: '2013-01-18T00:59:46.771Z', ttl: '999').expires == new DateTime('2013-01-18T01:16:25.771Z')
    }

    def 'should not calculate without ttl'() {
        expect:
        new FastProperty(ts: '2013-01-18T00:59:46.771Z').expires == null
    }

    @Unroll("should set ttl in seconds when to unit of measure is #unitOfMeasure")
    def 'should set ttl according to unit of measure'() {
        FastProperty fastProperty = new FastProperty()

        when:
        fastProperty.setTtlInUnits(3, unitOfMeasure)

        then:
        fastProperty.ttl == ttl

        where:
        unitOfMeasure   | ttl
        'Seconds'       | '3'
        'Minutes'       | '180'
        'Hours'         | '10800'
        'Days'          | '259200'
        'Weeks'         | '1814400'
    }

    def 'should fail to set ttl with invalid unit of measure'() {
        FastProperty fastProperty = new FastProperty()

        when:
        fastProperty.setTtlInUnits(3, 'DogYears')

        then:
        thrown(IllegalArgumentException)

    }

    def 'should default to seconds when setting ttl without a unit of measure'() {
        FastProperty fastProperty = new FastProperty()

        when:
        fastProperty.setTtlInUnits(3, null)

        then:
        fastProperty.ttl == '3'
    }
}
