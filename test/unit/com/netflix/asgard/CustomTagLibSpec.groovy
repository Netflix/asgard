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
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import spock.lang.Specification

@TestFor(CustomTagLib)
class CustomTagLibSpec extends Specification {

    def 'should generate html tooltip with body text'() {
        when:
        String output = applyTemplate('<g:tip>Do not <blink>click here</blink></g:tip>')

        then:
        output == '<span class="tip"><img src="/images/tango/16/apps/help-browser.png" />' +
                '<span class="template">Do not <blink>click here</blink></span></span>'
    }

    def 'should generate tooltip with value attribute'() {
        when:
        String output = applyTemplate('<g:tip value="Do not click here"/>')

        then:
        output == '<span class="tip"><img src="/images/tango/16/apps/help-browser.png" />' +
                '<span class="template">Do not click here</span></span>'
    }

    def 'should generate tooltip with tip style'() {
        when:
        String output = applyTemplate('<g:tip tipStyle="outrageous">This is really important</g:tip>')

        then:
        output == '<span class="tip" data-tip-style="outrageous"><img src="/images/tango/16/apps/help-browser.png" />' +
                '<span class="template">This is really important</span></span>'
    }

    def 'tip missing value and body should throw exception'() {
        when:
        applyTemplate('<g:tip/>')

        then:
        GrailsTagException e = thrown(GrailsTagException)
        e.message == 'Tip tag requires either a [value] attribute or a body'

    }
}
