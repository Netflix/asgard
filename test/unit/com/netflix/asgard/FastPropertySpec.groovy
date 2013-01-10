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
import groovy.xml.MarkupBuilder
import spock.lang.Specification

class FastPropertySpec extends Specification {

    private FastProperty constructFastProperty(String key, String value, String env, String appId, String regionCode, String stack, String countries,
                String updatedBy, String sourceOfUpdate, String cmcTicket) {
        StringWriter writer = new StringWriter()
        final MarkupBuilder builder = new MarkupBuilder(writer)
        builder.property {
            builder.key(key)
            builder.value(value)
            builder.env(env)
            builder.appId(appId)
            builder.region(regionCode)
            builder.stack(stack)
            builder.countries(countries)
            builder.updatedBy(updatedBy)
            builder.sourceOfUpdate(sourceOfUpdate)
            builder.cmcTicket(cmcTicket)
        }
        String xmlString = writer.toString()
        GPathResult xml = XML.parse(xmlString) as GPathResult
        FastProperty.fromXml(xml)
    }

    def 'should construct fast property with all values'() {
        FastProperty actualFastProperty = constructFastProperty('dial', '11', 'test', 'tap', 'eu-west-1', 'spinal',
                'UK', 'cmccoy', 'asgard', '123')
        FastProperty expectedFastProperty = new FastProperty(key: 'dial', value: '11', env: 'test', appId: 'tap',
                region: 'eu-west-1', stack: 'spinal', countries: 'UK', updatedBy: 'cmccoy', sourceOfUpdate: 'asgard',
                cmcTicket: '123')

        expect:
        actualFastProperty == expectedFastProperty
        actualFastProperty.id == 'dial|tap|test|eu-west-1||spinal|UK'
    }

    def 'should construct fast property missing optional values'() {
        FastProperty actualFastProperty = constructFastProperty('dial', '11', 'test', 'tap', 'eu-west-1', null,
                null, null, null, null)
        FastProperty expectedFastProperty = new FastProperty(key: 'dial', value: '11', env: 'test', appId: 'tap',
                region: 'eu-west-1')

        expect:
        actualFastProperty == expectedFastProperty
        actualFastProperty.id == 'dial|tap|test|eu-west-1|||'
    }

    def 'should construct id for valid values'() {
        FastProperty fastProperty = constructFastProperty('dial', '11', 'test', 'tap', 'eu-west-1', 'spinal',
                'UK', 'cmccoy', 'asgard', '123')

        expect:
        fastProperty.id == 'dial|tap|test|eu-west-1||spinal|UK'
    }

    def 'should construct id for invalid values'() {
        FastProperty fastProperty = constructFastProperty('_dial_', '11?', 'test:', '*tap*', 'eu-west-1', '!spinal!',
                'UK', 'cmccoy', 'asgard', '123')

        expect:
        fastProperty.id == '_dial_|*tap*|test:|eu-west-1||!spinal!|UK'
    }
}
