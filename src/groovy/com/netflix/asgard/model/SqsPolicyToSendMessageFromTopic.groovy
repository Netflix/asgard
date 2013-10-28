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

import groovy.transform.Immutable

/**
 * Constructs an SQS policy allowing an SNS topic to send messages to the policy.
 */
@Immutable class SqsPolicyToSendMessageFromTopic {

    String queueArn
    String topicArn

    String toString() {
        """{
    "Version": "2008-10-17",
    "Id": "${queueArn}/SQSDefaultPolicy",
    "Statement": [
        {
            "Sid": "${topicArn}/SNStoSQS",
            "Effect": "Allow",
            "Principal": {
                "AWS": "*"
            },
            "Action": "sqs:SendMessage",
            "Resource": "${queueArn}",
            "Condition": {
                "ArnEquals": {
                    "aws:SourceArn": "${topicArn}"
                }
            }
        }
    ]
}""".replaceAll('\\s+', '')
    }
}
