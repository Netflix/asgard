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

    def 'should generate link for SQS subscription endpoint in same account'() {
        grailsApplication.metaClass.getControllerNamesToContextParams = { -> ['queue': []] }
        tagLib.configService = Mock(ConfigService) {
            getAwsAccountNumber() >> '170000000000'
        }

        expect:
        applyTemplate('<g:snsSubscriptionEndpoint>arn:aws:sqs:us-west-1:170000000000:testSQSWest\
</g:snsSubscriptionEndpoint>') == '<a href="/queue/show/testSQSWest" region="us-west-1" class="queue" \
title="Show details of this Queue">arn:aws:sqs:us-west-1:170000000000:testSQSWest</a>'
        applyTemplate('<g:snsSubscriptionEndpoint>arn:aws:sqs:us-west-1:170000000001:testSQSWest\
</g:snsSubscriptionEndpoint>') == 'arn:aws:sqs:us-west-1:170000000001:testSQSWest'
        applyTemplate('<g:snsSubscriptionEndpoint>jsnow@thewall.got</g:snsSubscriptionEndpoint>') == 'jsnow@thewall.got'
    }
}
