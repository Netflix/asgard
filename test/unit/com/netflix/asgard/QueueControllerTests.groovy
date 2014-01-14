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
import grails.test.mixin.TestFor
import org.junit.Before

@TestFor(QueueController)
class QueueControllerTests {

    @Before
    void setUp() {
        TestUtils.setUpMockRequest()
        controller.awsSqsService = Mocks.awsSqsService()
    }

    void testShow() {
        def params = controller.params
        params.id = 'goofy'
        def attrs = controller.show()
        SimpleQueue queue = attrs.queue
        assert 'goofy' == queue.name

        Map<String, String> attributes = queue.attributes
        assert '0' == attributes.ApproximateNumberOfMessages
        assert '0' == attributes.ApproximateNumberOfMessagesNotVisible
        assert '1260317889' == attributes.CreatedTimestamp
        assert '1311624999' == attributes.LastModifiedTimestamp
        assert '8192' == attributes.MaximumMessageSize
        assert '345600' == attributes.MessageRetentionPeriod
        assert 'arn:aws:sqs:us-east-1:179000000000:goofy' == attributes.QueueArn
        assert '3600' == attributes.VisibilityTimeout

        Map<String, String> humanReadableAttributes = queue.humanReadableAttributes

        assert '0' == humanReadableAttributes['Approximate Number Of Messages']
        assert '0' == humanReadableAttributes['Approximate Number Of Messages Not Visible']
        assert humanReadableAttributes['Created Timestamp'] ==~ /2009-12-0(8|9)\ \d{2}:\d{2}:\d{2}\ [A-Z]{3,4}/
        assert humanReadableAttributes['Last Modified Timestamp'] ==~ /2011-07-2(5|6)\ \d{2}:\d{2}:\d{2}\ [A-Z]{3,4}/
        assert '8192 bytes' == humanReadableAttributes['Maximum Message Size']

        // TODO why are these format results different in Jenkins and IntelliJ?
        assert humanReadableAttributes['Message Retention Period'] in ['4d', '96h']

        assert 'arn:aws:sqs:us-east-1:179000000000:goofy' == humanReadableAttributes['Queue Arn']
        assert '3600 seconds' == humanReadableAttributes['Visibility Timeout']
    }

    void testShowNonExistent() {
        def p = controller.params
        p.id = 'doesntexist'
        controller.show()
        assert '/error/missing' == view
        assert "Queue 'doesntexist' not found in us-east-1 test" == controller.flash.message
    }
}
