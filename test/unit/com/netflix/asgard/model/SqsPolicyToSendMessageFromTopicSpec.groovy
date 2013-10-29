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

import spock.lang.Specification

class SqsPolicyToSendMessageFromTopicSpec extends Specification {

    def 'should construct policy to receive from topic'() {
        expect:
        new SqsPolicyToSendMessageFromTopic(queueArn: 'arn:aws:sqs:us-east-1:170000000000:asgard-finished-tasks',
                topicArn: 'arn:aws:sns:us-east-1:170000000001:asgard-finished-tasks').
                toString() == '''\
{
    "Version": "2008-10-17",
    "Id": "arn:aws:sqs:us-east-1:170000000000:asgard-finished-tasks/SQSDefaultPolicy",
    "Statement": [
        {
            "Sid": "arn:aws:sns:us-east-1:170000000001:asgard-finished-tasks/SNStoSQS",
            "Effect": "Allow",
            "Principal": {
                "AWS": "*"
            },
            "Action": "sqs:SendMessage",
            "Resource": "arn:aws:sqs:us-east-1:170000000000:asgard-finished-tasks",
            "Condition": {
                "ArnEquals": {
                    "aws:SourceArn": "arn:aws:sns:us-east-1:170000000001:asgard-finished-tasks"
                }
            }
        }
    ]
}'''.replaceAll('\\s+', '')
    }
}
