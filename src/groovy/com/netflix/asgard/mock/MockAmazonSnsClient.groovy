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
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.GetTopicAttributesRequest
import com.amazonaws.services.sns.model.GetTopicAttributesResult
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult
import com.amazonaws.services.sns.model.ListTopicsRequest
import com.amazonaws.services.sns.model.ListTopicsResult
import com.amazonaws.services.sns.model.Topic
import com.netflix.asgard.model.TopicData

class MockAmazonSnsClient extends AmazonSNSClient {

    private Collection<Topic> mockTopics

    private List<Topic> loadMockTopics() {
        [new Topic().withTopicArn('arn:aws:sns:us-east-1:179000000000:abadmin-testConformity-Report')]
    }

    MockAmazonSnsClient(AWSCredentialsProvider credentialsProvider, ClientConfiguration clientConfiguration) {
        super(credentialsProvider, clientConfiguration)
        mockTopics = loadMockTopics()
    }

    void setEndpoint(String endpoint) {

    }

    ListTopicsResult listTopics(ListTopicsRequest request) {
        new ListTopicsResult().withTopics(mockTopics)
    }

    GetTopicAttributesResult getTopicAttributes(GetTopicAttributesRequest getTopicAttributesRequest) {
        String topicArn = getTopicAttributesRequest.topicArn
        Topic found = mockTopics.find { it.topicArn == topicArn }
        if (found) {
            new GetTopicAttributesResult().
                    withAttributes(TopicArn: topicArn, DisplayName: new TopicData(topicArn).name, Owner: '179000000000')
        } else {
            throw new AmazonServiceException("Status Code: 400, AWS Request ID: 123unittest, " +
                        "AWS Error Code: InvalidTopicARN.NotFound, AWS Error Message: " +
                        "The TopicARN '${topicArn}' does not exist")
        }
    }

    public ListSubscriptionsByTopicResult listSubscriptionsByTopic(
            ListSubscriptionsByTopicRequest listSubscriptionsByTopicRequest)
            throws AmazonServiceException, AmazonClientException {
        new ListSubscriptionsByTopicResult()
    }

}
