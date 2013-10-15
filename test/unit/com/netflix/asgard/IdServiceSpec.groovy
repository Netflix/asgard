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

import com.netflix.asgard.model.SimpleDbSequenceLocator
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(IdService)
class IdServiceSpec extends Specification {

    void "should provide next ID"() {
        service.awsSimpleDbService = Mock(AwsSimpleDbService)

        when:
        String result = service.nextId(UserContext.auto(Region.US_EAST_1), SimpleDbSequenceLocator.Task)

        then:
        result == '42'
        1 * service.awsSimpleDbService.incrementAndGetSequenceNumber(_, SimpleDbSequenceLocator.Task) >> 42
    }

    void "should return UUID and email with error if nextId results in an error."() {
        service.awsSimpleDbService = Mock(AwsSimpleDbService)
        service.emailerService = Mock(EmailerService)

        when:
        String result = service.nextId(UserContext.auto(Region.US_EAST_1), SimpleDbSequenceLocator.Task)

        then:
        result ==~ /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/
        1 * service.awsSimpleDbService.incrementAndGetSequenceNumber(_, SimpleDbSequenceLocator.Task) >> {
            throw new IllegalStateException('AWS is down. No sequence ID for you!')
        }
        1 * service.emailerService.sendExceptionEmail(
                'java.lang.IllegalStateException: AWS is down. No sequence ID for you!', _)
    }
}
