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
package com.netflix.asgard.model

import com.netflix.asgard.Region

class SimpleQueueTests extends GroovyTestCase {

    void testUrlToName() {
        assert 'cloudBatchTestQueue1' ==
                new SimpleQueue('https://sqs.us-east-1.amazonaws.com/179000000000/cloudBatchTestQueue1').name
    }

    void testNameToUrl() {
        assert 'https://sqs.us-east-1.amazonaws.com/179000000000/cloudBatchTestQueue1' ==
                new SimpleQueue(Region.defaultRegion(), '179000000000', 'cloudBatchTestQueue1').url
    }

    void testHumanReadableAttributes() {

        String url = 'https://sqs.us-east-1.amazonaws.com/179000000000/com_netflix_log4n_test_queue_error'
        SimpleQueue queue = new SimpleQueue(url)
        queue.attributes = [
                ApproximateNumberOfMessages: '11908',
                ApproximateNumberOfMessagesNotVisible: '10515',
                QueueArn: 'arn:aws:sqs:us-east-1:179000000000:com_netflix_log4n_test_queue_error',
                GarbageMadeUpAttributeName: '10',
                VisibilityTimeout: '45',
                MaximumMessageSize: '2048',
                MessageRetentionPeriod: '3600',
                CreatedTimestamp: '1234566789',
                LastModifiedTimestamp: '1234566789'
        ]
        Map<String, String> actualHumanReadableMap = queue.humanReadableAttributes
        Map<String, String> pstExpectedResult = [
                'Approximate Number Of Messages': '11908',
                'Approximate Number Of Messages Not Visible': '10515',
                'Queue Arn': 'arn:aws:sqs:us-east-1:179000000000:com_netflix_log4n_test_queue_error',
                'Garbage Made Up Attribute Name': '10',
                'Visibility Timeout': '45 seconds',
                'Maximum Message Size': '2048 bytes',
                'Message Retention Period': '1h',
                'Created Timestamp': '2009-02-13 15:13:09 PST',
                'Last Modified Timestamp': '2009-02-13 15:13:09 PST']

        // Some testing servers use UTC timezone
        Map<String, String> utcExpectedResult = pstExpectedResult.clone() as Map
        utcExpectedResult['Created Timestamp'] = '2009-02-13 23:13:09 UTC'
        utcExpectedResult['Last Modified Timestamp'] = '2009-02-13 23:13:09 UTC'

        assert pstExpectedResult == actualHumanReadableMap || utcExpectedResult == actualHumanReadableMap
    }
}
