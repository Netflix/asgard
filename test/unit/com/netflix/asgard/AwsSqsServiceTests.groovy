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
import com.netflix.asgard.model.SimpleQueue

class AwsSqsServiceTests extends GroovyTestCase {

    UserContext userContext = Mocks.userContext()

    void testCreateAndDeleteQueue() {
        AwsSqsService awsSqsService = Mocks.awsSqsService()
        String name = 'things-to-do'
        assertNull awsSqsService.getQueue(userContext, name)

        awsSqsService.createQueue(userContext, name, 30, 0)
        String account = Mocks.TEST_AWS_ACCOUNT_ID
        Region region = userContext.region
        SimpleQueue expected = new SimpleQueue(region, account, name)
                .withAttributes([VisibilityTimeout: '30', DelaySeconds: '0'])
        SimpleQueue actual = awsSqsService.getQueue(userContext, name)
        assert expected == actual

        awsSqsService.deleteQueue(userContext, name)
        assertNull awsSqsService.getQueue(userContext, name)
    }
}
