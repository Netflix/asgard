package com.netflix.asgard

import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(ObjectLinkTagLib)
class ObjectLinkTagLibSpec extends Specification {

    def 'should generate link'() {
        when:
        grailsApplication.metaClass.getControllerNamesToContextParams = { -> ['instance': []] }
        String output = applyTemplate('<g:linkObject type="instance" name="i-12345678">aprop</g:linkObject>')

        then:
        output == '<a href="/instance/show/i-12345678" class="instance" ' +
                'title="Show details of this Instance">aprop</a>'
    }

    def 'should generate fast property link'() {
        when:
        grailsApplication.metaClass.getControllerNamesToContextParams = { -> ['fastProperty': []] }
        String output = applyTemplate('<g:linkObject type="fastProperty" name="|prop:8888">aprop</g:linkObject>')

        then:
        output == '<a href="/fastProperty/show?name=%7Cprop%3A8888" class="fastProperty" ' +
                'title="Show details of this Fast Property">aprop</a>'
    }
}
