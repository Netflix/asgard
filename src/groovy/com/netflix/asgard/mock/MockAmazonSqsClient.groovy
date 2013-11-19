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
package com.netflix.asgard.mock

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.CreateQueueRequest
import com.amazonaws.services.sqs.model.CreateQueueResult
import com.amazonaws.services.sqs.model.DeleteQueueRequest
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest
import com.amazonaws.services.sqs.model.GetQueueAttributesResult
import com.amazonaws.services.sqs.model.GetQueueUrlRequest
import com.amazonaws.services.sqs.model.GetQueueUrlResult
import com.amazonaws.services.sqs.model.ListQueuesRequest
import com.amazonaws.services.sqs.model.ListQueuesResult
import com.netflix.asgard.Region
import com.netflix.asgard.model.SimpleQueue

class MockAmazonSqsClient extends AmazonSQSClient {

    private Collection<SimpleQueue> mockQueues

    private List<SimpleQueue> loadMockQueues() {
        [SimpleQueue.fromUrl('https://sqs.us-east-1.amazonaws.com/179000000000/goofy').withAttributes([
                ApproximateNumberOfMessages: '0',
                ApproximateNumberOfMessagesNotVisible: '0',
                CreatedTimestamp: '1260317889',
                LastModifiedTimestamp: '1311624999',
                MaximumMessageSize: '8192',
                MessageRetentionPeriod: '345600',
                QueueArn: 'arn:aws:sqs:us-east-1:179000000000:goofy',
                VisibilityTimeout: '3600'
        ])]
    }

    MockAmazonSqsClient(BasicAWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        super(awsCredentials as BasicAWSCredentials, clientConfiguration)
        mockQueues = loadMockQueues()
    }

    @Override
    void setEndpoint(String endpoint) {

    }

    @Override
    ListQueuesResult listQueues(ListQueuesRequest request) {
        new ListQueuesResult().withQueueUrls(mockQueues.collect { it.url })
    }

    @Override
    public GetQueueAttributesResult getQueueAttributes(GetQueueAttributesRequest getQueueAttributesRequest)
            throws AmazonServiceException, AmazonClientException {
        String queueUrl = getQueueAttributesRequest.queueUrl
        SimpleQueue found = mockQueues.find { it.url == queueUrl }
        if (found) {
            return new GetQueueAttributesResult().withAttributes(found?.attributes)
        } else {
            throw new AmazonServiceException('Status Code: 400, AWS Request ID: 123unittest, ' +
                        'AWS Error Code: InvalidQueue.NotFound, AWS Error Message: ' +
                        "The Queue '${queueUrl}' does not exist")
        }
    }

    @Override
    CreateQueueResult createQueue(CreateQueueRequest createQueueRequest) {
        String account = Mocks.TEST_AWS_ACCOUNT_ID
        String name = createQueueRequest.queueName
        Map<String, String> attributes = createQueueRequest.attributes
        SimpleQueue queue = new SimpleQueue(region: Region.US_EAST_1, accountNumber: account, name: name).
                withAttributes(attributes)
        mockQueues << queue
        new CreateQueueResult().withQueueUrl(queue.url)
    }

    @Override
    void deleteQueue(DeleteQueueRequest deleteQueueRequest) {
        SimpleQueue queue = mockQueues.find { it.url == deleteQueueRequest.queueUrl }
        mockQueues.remove(queue)
    }

    @Override
    GetQueueUrlResult getQueueUrl(GetQueueUrlRequest getQueueUrlRequest) {
        SimpleQueue queue = mockQueues.find { it.name == getQueueUrlRequest.queueName }
        new GetQueueUrlResult().withQueueUrl(queue.url)
    }
}
