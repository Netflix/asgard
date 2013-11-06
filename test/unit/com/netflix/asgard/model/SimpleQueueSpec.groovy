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
package com.netflix.asgard.model

import com.netflix.asgard.Region
import com.netflix.asgard.Time
import org.joda.time.DateTime
import spock.lang.Specification

class SimpleQueueSpec extends Specification {

    def 'should parse from URL'() {
        SimpleQueue queue = SimpleQueue.fromUrl('https://sqs.us-east-1.amazonaws.com/179000000000/cloudBatchTestQueue1')

        expect:
        queue.region == 'us-east-1'
        queue.accountNumber == '179000000000'
        queue.name == 'cloudBatchTestQueue1'
        queue.arn == 'arn:aws:sqs:us-east-1:179000000000:cloudBatchTestQueue1'
    }

    def 'should parse from ARN'() {
        SimpleQueue queue = SimpleQueue.fromArn('arn:aws:sqs:us-east-1:179000000000:cloudBatchTestQueue1')

        expect:
        queue.region == 'us-east-1'
        queue.accountNumber == '179000000000'
        queue.name == 'cloudBatchTestQueue1'
        queue.arn == 'arn:aws:sqs:us-east-1:179000000000:cloudBatchTestQueue1'
        queue.url == 'https://sqs.us-east-1.amazonaws.com/179000000000/cloudBatchTestQueue1'
    }

    def 'should not parse from invalid ARN'() {
        SimpleQueue queue = SimpleQueue.fromArn('1arn:aws:sqs:us-east-1:179000000000:cloudBatchTestQueue1')

        expect:
        queue == null
    }

    void 'should construct URL'() {
        SimpleQueue queue = new SimpleQueue(region: Region.defaultRegion(), accountNumber: '179000000000',
                name: 'cloudBatchTestQueue1')

        expect:
        queue.url == 'https://sqs.us-east-1.amazonaws.com/179000000000/cloudBatchTestQueue1'
    }

    void testHumanReadableAttributes() {
        String timestamp = '1234566789'
        String url = 'https://sqs.us-east-1.amazonaws.com/179000000000/com_netflix_log4n_test_queue_error'
        SimpleQueue queue = SimpleQueue.fromUrl(url)
        queue.attributes = [
                ApproximateNumberOfMessages: '11908',
                ApproximateNumberOfMessagesNotVisible: '10515',
                QueueArn: 'arn:aws:sqs:us-east-1:179000000000:com_netflix_log4n_test_queue_error',
                GarbageMadeUpAttributeName: '10',
                VisibilityTimeout: '45',
                MaximumMessageSize: '2048',
                MessageRetentionPeriod: '3600',
                CreatedTimestamp: timestamp,
                LastModifiedTimestamp: timestamp
        ]

        expect:
        queue.humanReadableAttributes == [
                'Approximate Number Of Messages': '11908',
                'Approximate Number Of Messages Not Visible': '10515',
                'Queue Arn': 'arn:aws:sqs:us-east-1:179000000000:com_netflix_log4n_test_queue_error',
                'Garbage Made Up Attribute Name': '10',
                'Visibility Timeout': '45 seconds',
                'Maximum Message Size': '2048 bytes',
                'Message Retention Period': '1h',
                /* using Time.format here makes this test independent of timezone it is running in */
                'Created Timestamp': Time.format(new DateTime(Long.parseLong(timestamp) * 1000)),
                'Last Modified Timestamp': Time.format(new DateTime(Long.parseLong(timestamp) * 1000))
        ]
    }
}
