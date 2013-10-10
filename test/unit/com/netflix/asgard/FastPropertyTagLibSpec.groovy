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

import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(FastPropsTagLib)
class FastPropertyTagLibSpec extends Specification {

    def 'no config spec should create empty output'() {
        given:
        ConfigService configService = new ConfigService(grailsApplication: [config: [platform: [:]]])
        tagLib.configService = configService

        when:
        String output = applyTemplate('<g:extLinkToPropertiesConsole />')

        then:
        output == ''
    }

    def 'valid config value should create link markup'() {
        given:
        ConfigService configService = new ConfigService(grailsApplication: [
                config: [
                        cloud: [accountName: 'test'],
                        platform: [fastPropertyConsoleUrls: [test: 'http://sometestlocation/']]
                ]
        ])
        tagLib.configService = configService

        when:
        String output = applyTemplate('<g:extLinkToPropertiesConsole />')

        then:
        output == '<li class="menuButton"><a href="http://sometestlocation/" target="_blank"' +
                ' class="fastProperties">Fast Properties</a></li>'
    }

    def 'missing config value for current account should create empty output'() {
        given:
        ConfigService configService = new ConfigService(grailsApplication: [
                config: [
                        cloud: [accountName: 'test'],
                        platform: [fastPropertyConsoleUrls: [beta: 'http://somebetalocation/']]
                ]
        ])
        tagLib.configService = configService

        when:
        String output = applyTemplate('<g:extLinkToPropertiesConsole />')

        then:
        output == ''
    }
}
