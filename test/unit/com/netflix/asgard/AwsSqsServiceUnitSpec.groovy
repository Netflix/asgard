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

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest
import com.amazonaws.services.sqs.model.GetQueueAttributesResult
import com.amazonaws.services.sqs.model.QueueAttributeName
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.SimpleQueue
import com.netflix.asgard.model.SqsPolicyToSendMessageFromTopic
import spock.lang.Specification

class AwsSqsServiceUnitSpec extends Specification {

    AwsSqsService awsSqsService = new AwsSqsService()
    AmazonSQS mockAmazonSQS = Mock(AmazonSQS)
    UserContext userContext = UserContext.auto()

    def setup() {
        awsSqsService.awsClient = new MultiRegionAwsClient({ mockAmazonSQS })
        awsSqsService.taskService = new TaskService() {
            def runTask(UserContext userContext, String name, Closure work, Link link, Task existingTask) {
                work(new Task())
            }
        }
        awsSqsService.caches = new Caches(new MockCachedMapBuilder([:]))
        awsSqsService.accountNumber = '170000000000'
    }

    def 'should create and delete queue'() {
        AwsSqsService awsSqsService = Mocks.awsSqsService()
        String name = 'things-to-do'

        expect:
        awsSqsService.getQueue(userContext, name) == null

        when:
        awsSqsService.createQueue(userContext, name, 30, 0)

        then:
        awsSqsService.getQueue(userContext, name) == new SimpleQueue(region: userContext.region,
                accountNumber: Mocks.TEST_AWS_ACCOUNT_ID, name: name).
                withAttributes([VisibilityTimeout: '30', DelaySeconds: '0'])

        when:
        awsSqsService.deleteQueue(userContext, name)

        then:
        awsSqsService.getQueue(userContext, name) == null
    }

    def 'should add policy to SQS queue to allow SNS topic to send messages to it'() {
        awsSqsService.configService = Mock(ConfigService) {
            getAwsAccountNumber() >> '170000000000'
        }
        SqsPolicyToSendMessageFromTopic policy = new SqsPolicyToSendMessageFromTopic(
                queueArn: 'arn:aws:sqs:us-west-1:170000000000:testSQS',
                topicArn: 'arn:aws:sns:us-west-2:170000000000:testSNS')

        when:
        awsSqsService.addSnsToSqsPolicy(userContext, policy, null)

        then:
        1 * mockAmazonSQS.setQueueAttributes(new SetQueueAttributesRequest(
                queueUrl: 'https://sqs.us-west-1.amazonaws.com/170000000000/testSQS',
                attributes: [(QueueAttributeName.Policy.name()) : policy.toString()]))
        2 * mockAmazonSQS.getQueueAttributes(new GetQueueAttributesRequest(
                queueUrl: 'https://sqs.us-west-1.amazonaws.com/170000000000/testSQS', attributeNames: ['All'])) >>
                new GetQueueAttributesResult(attributes: [:])
        0 * mockAmazonSQS._
    }
}
