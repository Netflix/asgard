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

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.CreateTopicRequest
import com.amazonaws.services.sns.model.CreateTopicResult
import com.amazonaws.services.sns.model.DeleteTopicRequest
import com.amazonaws.services.sns.model.GetTopicAttributesRequest
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult
import com.amazonaws.services.sns.model.ListTopicsRequest
import com.amazonaws.services.sns.model.ListTopicsResult
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.services.sns.model.Subscription
import com.amazonaws.services.sns.model.Topic
import com.amazonaws.services.sns.model.UnsubscribeRequest
import com.netflix.asgard.cache.CacheInitializer
import com.netflix.asgard.model.SqsPolicyToSendMessageFromTopic
import com.netflix.asgard.model.SubscriptionData
import com.netflix.asgard.model.TopicData
import com.netflix.asgard.retriever.AwsResultsRetriever
import org.springframework.beans.factory.InitializingBean

class AwsSnsService implements CacheInitializer, InitializingBean {

    static transactional = false

    private String accountNumber
    MultiRegionAwsClient<AmazonSNS> awsClient
    def grailsApplication
    def awsClientService
    def awsSqsService
    Caches caches
    def configService
    def taskService

    final AwsResultsRetriever subscriptionRetriever = new AwsResultsRetriever<Subscription,
            ListSubscriptionsByTopicRequest, ListSubscriptionsByTopicResult>() {
        protected ListSubscriptionsByTopicResult makeRequest(Region region, ListSubscriptionsByTopicRequest request) {
            awsClient.by(region).listSubscriptionsByTopic(request)
        }
        protected List<Subscription> accessResult(ListSubscriptionsByTopicResult result) {
            result.subscriptions
        }
    }

    final AwsResultsRetriever topicRetriever = new AwsResultsRetriever<Topic, ListTopicsRequest, ListTopicsResult>() {
        protected ListTopicsResult makeRequest(Region region, ListTopicsRequest request) {
            awsClient.by(region).listTopics(request)
        }
        protected List<Topic> accessResult(ListTopicsResult result) {
            result.topics
        }
    }

    void afterPropertiesSet() {
        accountNumber = configService.awsAccountNumber

        awsClient = awsClient ?: new MultiRegionAwsClient<AmazonSNS>( { Region region ->
            AmazonSNS client = awsClientService.create(AmazonSNS)
            client.setEndpoint("sns.${region}.amazonaws.com")
            client
        })
    }

    void initializeCaches() {
        caches.allTopics.ensureSetUp({ Region region -> retrieveTopics(region) })
    }

    // Topics

    private List<TopicData> retrieveTopics(Region region) {
        List<Topic> topics = topicRetriever.retrieve(region, new ListTopicsRequest())
        topics.collect { new TopicData(it.topicArn) }
    }

    Collection<TopicData> getTopics(UserContext userContext) {
        caches.allTopics.by(userContext.region).list()
    }

    TopicData getTopic(UserContext userContext, String topicName, From from = From.AWS) {
        if (!topicName) { return null }
        Region region = userContext.region
        if (from == From.CACHE) {
            return caches.allTopics.by(region).get(topicName)
        }
        TopicData existingTopic = caches.allTopics.by(region).get(topicName)
        TopicData topic = existingTopic ?: new TopicData(region, accountNumber, topicName)
        String arn = topic.arn
        try {
            GetTopicAttributesRequest attributesRequest = new GetTopicAttributesRequest(arn)
            topic.attributes = awsClient.by(region).getTopicAttributes(attributesRequest).attributes
            return caches.allTopics.by(region).put(topicName, topic)
        } catch (AmazonServiceException ignored) {
            return null
        }
    }

    List<SubscriptionData> getSubscriptionsForTopic(UserContext userContext, TopicData topic) {
        ListSubscriptionsByTopicRequest request = new ListSubscriptionsByTopicRequest().withTopicArn(topic.arn)
        List<Subscription> subscriptions = subscriptionRetriever.retrieve(userContext.region, request)
        subscriptions.collect { SubscriptionData.fromSubscription(topic, it) }
    }

    void createTopic(UserContext userContext, String topicName, Task existingTask = null) {
        CreateTopicRequest request = new CreateTopicRequest(name: topicName)
        String topicArn = null
        taskService.runTask(userContext, "Create Topic '${topicName}'", { Task task ->
            CreateTopicResult result = awsClient.by(userContext.region).createTopic(request)
            topicArn = result.topicArn
        }, Link.to(EntityType.topic, topicName), existingTask)
        caches.allTopics.by(userContext.region).put(topicName, new TopicData(topicArn))
    }

    void deleteTopic(UserContext userContext, TopicData topic, Task existingTask = null) {
        DeleteTopicRequest request = new DeleteTopicRequest(topicArn: topic.arn)
        taskService.runTask(userContext, "Delete Topic '${topic.name}'", { Task task ->
            awsClient.by(userContext.region).deleteTopic(request)
        }, Link.to(EntityType.topic, topic.name), existingTask)
        caches.allTopics.by(userContext.region).put(topic.name, null)
    }

    /**
     * Creates a subscription to the SNS topic.
     *
     * @param userContext who, where, why
     * @param subscription information
     * @param existingTask will be used to do work if it is provided
     */
    void createSubscription(UserContext userContext, SubscriptionData subscription, Task existingTask = null) {
        taskService.runTask(userContext, "Create Subscription ${subscription}", { Task task ->
            awsClient.by(userContext.region).subscribe(subscription.toSubscribeRequest())
            if (subscription.protocol == SubscriptionData.Protocol.SQS.value) {
                SqsPolicyToSendMessageFromTopic policy = new SqsPolicyToSendMessageFromTopic(
                        topicArn: subscription.topicArn, queueArn: subscription.endpoint)
                awsSqsService.addSnsToSqsPolicy(userContext, policy, existingTask)
            }
        }, Link.to(EntityType.topic, subscription.topicName), existingTask)
    }

    void deleteSubscription(UserContext userContext, String topicName, String subscriptionArn, Task existingTask = null) {
        taskService.runTask(userContext, "Delete Subscription '${subscriptionArn}'", { Task task ->
            awsClient.by(userContext.region).unsubscribe(new UnsubscribeRequest(subscriptionArn: subscriptionArn))
        }, Link.to(EntityType.topic, topicName), existingTask)
    }

    void publishToTopic(UserContext userContext, TopicData topic, String subject, String message) {
        PublishRequest request = new PublishRequest(topicArn: topic.arn, subject: subject, message: message)
        awsClient.by(userContext.region).publish(request)
    }
}
