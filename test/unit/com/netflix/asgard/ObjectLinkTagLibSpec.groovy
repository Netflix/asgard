package com.netflix.asgard

import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(ObjectLinkTagLib)
class ObjectLinkTagLibSpec extends Specification {

    def 'should handle url with colon in param'() {
        when:
        grailsApplication.metaClass.getControllerNamesToContextParams = { -> ['fastProperty': []] }
        String output = applyTemplate('<g:linkObject type="fastProperty" name="|prop:8888">aprop</g:linkObject>')

        then:
        output == '<a href="/fastProperty/show/%7Cprop%3A8888" class="fastProperty" ' +
                'title="Show details of this Fast Property">aprop</a>'
    }
}
