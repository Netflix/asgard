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

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.SubscribeRequest
import com.netflix.asgard.model.SqsPolicyToSendMessageFromTopic
import com.netflix.asgard.model.SubscriptionData
import spock.lang.Specification

class AwsSnsServiceUnitSpec extends Specification {

    AwsSnsService awsSnsService = new AwsSnsService()
    AmazonSNS mockAmazonSNS = Mock(AmazonSNS)

    def setup() {
        awsSnsService.awsClient = new MultiRegionAwsClient({ mockAmazonSNS })
        awsSnsService.taskService = new TaskService() {
            def runTask(UserContext userContext, String name, Closure work, Link link, Task existingTask) {
                work(new Task())
            }
        }
    }

    def 'should create e-mail subscription and add policy'() {
        SubscriptionData subscription = new SubscriptionData(
                protocol: SubscriptionData.Protocol.EMAIL.value, topicArn: 'topicArn1', endpoint: 'endpoint1')

        when:
        awsSnsService.createSubscription(UserContext.auto(), subscription)

        then:
        1 * mockAmazonSNS.subscribe(new SubscribeRequest(protocol: SubscriptionData.Protocol.EMAIL.value,
                topicArn: 'topicArn1', endpoint: 'endpoint1'))
        0 * _
    }

    def 'should create SQS subscription and add policy'() {
        awsSnsService.awsSqsService = Mock(AwsSqsService)
        SubscriptionData subscription = new SubscriptionData(
                protocol: SubscriptionData.Protocol.SQS.value, topicArn: 'topicArn1', endpoint: 'endpoint1')
        SqsPolicyToSendMessageFromTopic policy = new SqsPolicyToSendMessageFromTopic(
                topicArn: subscription.topicArn, queueArn: subscription.endpoint)

        when:
        awsSnsService.createSubscription(UserContext.auto(), subscription)

        then:
        1 * mockAmazonSNS.subscribe(new SubscribeRequest(protocol: SubscriptionData.Protocol.SQS.value,
                topicArn: 'topicArn1', endpoint: 'endpoint1'))
        1 * awsSnsService.awsSqsService.addSnsToSqsPolicy(_, policy, _)
        0 * _
    }
}
