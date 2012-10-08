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

import com.amazonaws.services.sns.model.SubscribeRequest
import com.amazonaws.services.sns.model.Subscription
import groovy.transform.Immutable

@Immutable final class SubscriptionData implements Comparable<SubscriptionData> {

    enum Protocol {
        EMAIL('email'), EMAIL_JSON('email-json'), HTTP('http'), HTTPS('https'), SMS('sms'), SQS('sqs')
        final String value
        Protocol(String value) {
            this.value = value
        }
        static Protocol getDefault() { EMAIL }
    }

    String topicArn
    String topicName
    String protocol
    String endpoint
    String arn

    static SubscriptionData fromSubscription(TopicData topic, Subscription subscription) {
        new SubscriptionData(topicArn: topic.arn, topicName: topic.name, protocol: subscription.protocol,
                endpoint: subscription.endpoint, arn: subscription.subscriptionArn)
    }

    SubscribeRequest toSubscribeRequest() {
        new SubscribeRequest(topicArn: topicArn, protocol: protocol, endpoint: endpoint)
    }

    boolean isConfirmed() {
        arn && arn != 'PendingConfirmation'
    }

    int compareTo(SubscriptionData that) {
        this.endpoint <=> that.endpoint ?: this.protocol <=> that.protocol ?: this.topicArn <=> that.topicArn
    }
}
