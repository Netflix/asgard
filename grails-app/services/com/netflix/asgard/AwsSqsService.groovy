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
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.CreateQueueRequest
import com.amazonaws.services.sqs.model.DeleteQueueRequest
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest
import com.amazonaws.services.sqs.model.ListQueuesRequest
import com.amazonaws.services.sqs.model.QueueAttributeName
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest
import com.netflix.asgard.cache.CacheInitializer
import com.netflix.asgard.model.SimpleQueue
import org.springframework.beans.factory.InitializingBean

class AwsSqsService implements CacheInitializer, InitializingBean {

    static transactional = false

    private String accountNumber
    MultiRegionAwsClient<AmazonSQS> awsClient
    def grailsApplication
    def awsClientService
    Caches caches
    def taskService

    void afterPropertiesSet() {
        accountNumber = grailsApplication.config.grails.awsAccounts[0]

        awsClient = new MultiRegionAwsClient<AmazonSQS>( { Region region ->
            AmazonSQS client = awsClientService.create(AmazonSQS)
            client.setEndpoint("sqs.${region}.amazonaws.com")
            client
        })
    }

    void initializeCaches() {
        caches.allQueues.ensureSetUp({ Region region -> retrieveQueues(region) })
    }

    // Queues

    private List<SimpleQueue> retrieveQueues(Region region) {
        awsClient.by(region).listQueues(new ListQueuesRequest()).queueUrls.collect { new SimpleQueue(it) }
    }

    Collection<SimpleQueue> getQueues(UserContext userContext) {
        caches.allQueues.by(userContext.region).list()
    }

    SimpleQueue getQueue(UserContext userContext, String queueName, From from = From.AWS) {
        Check.notEmpty(queueName, 'queue name')
        if (from == From.CACHE) {
            return caches.allQueues.by(userContext.region).get(queueName)
        }
        Map<String, String> attributes = getQueueAttributes(userContext, queueName)
        SimpleQueue queue = null
        CachedMap<SimpleQueue> queues = caches.allQueues.by(userContext.region)
        if (attributes == null) {
            queues.remove(queueName)
        } else {
            SimpleQueue existingQueue = queues.get(queueName)
            queue = existingQueue ?: new SimpleQueue(userContext.region, accountNumber, queueName)
            queue.attributes = attributes
            queues.put(queueName, queue)
        }
        queue
    }

    private Map<String, String> getQueueAttributes(UserContext userContext, String queueName) {
        Check.notEmpty(queueName, 'queue name')
        Region region = userContext.region
        SimpleQueue existingQueue = caches.allQueues.by(region).get(queueName)
        SimpleQueue queue = existingQueue ?: new SimpleQueue(region, accountNumber, queueName)
        String url = queue.url
        try {
            GetQueueAttributesRequest attrRequest = new GetQueueAttributesRequest(url).withAttributeNames('All')
            return awsClient.by(userContext.region).getQueueAttributes(attrRequest).attributes.sort()
        } catch (AmazonServiceException ase) {
            return null
        }
    }

    void createQueue(UserContext userContext, String queueName, Integer timeout) {
        Check.notEmpty(queueName, 'queue name')
        String taskMessage = "Creating queue '${queueName}' with visibility timeout ${timeout}s"
        taskService.runTask(userContext, taskMessage, { task ->
            SimpleQueue queue = getQueue(userContext, queueName)
            if (queue == null) {
                Map<String, String> attributes = [(QueueAttributeName.VisibilityTimeout.toString()): timeout.toString()]
                CreateQueueRequest request = new CreateQueueRequest(queueName).withAttributes(attributes)
                awsClient.by(userContext.region).createQueue(request)
            } else {
                throw new IllegalStateException("Queue '${queueName}' already exists")
            }
            getQueue(userContext, queueName)
        }, Link.to(EntityType.queue, queueName))
    }

    void deleteQueue(UserContext userContext, String queueName) {
        Check.notEmpty(queueName, 'queue name')
        taskService.runTask(userContext, "Deleting queue '${queueName}'", { task ->
            SimpleQueue queue = getQueue(userContext, queueName)
            if (queue) {
                awsClient.by(userContext.region).deleteQueue(new DeleteQueueRequest(queue.url))
                caches.allQueues.by(userContext.region).remove(queueName)
            } else {
                throw new IllegalStateException("Queue '${queueName}' not found")
            }
        }, Link.to(EntityType.queue, queueName))
    }

    SimpleQueue updateQueue(UserContext userContext, String queueName, Integer timeout) {
        Check.notEmpty(queueName, 'queue name')
        SimpleQueue queue = null
        taskService.runTask(userContext, "Update Queue '${queueName}' with timeout ${timeout}", { task ->
            queue = getQueue(userContext, queueName)
            Map<String, String> attributes = [:]
            attributes.put(SimpleQueue.VISIBILITY_TIMEOUT_ATTR_NAME, timeout.toString())
            SetQueueAttributesRequest request = new SetQueueAttributesRequest(queue.url, attributes)
            awsClient.by(userContext.region).setQueueAttributes(request)
            queue = getQueue(userContext, queueName)
        }, Link.to(EntityType.queue, queueName))
        queue
    }
}
