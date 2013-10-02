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

import com.netflix.asgard.model.SubscriptionData
import com.netflix.asgard.model.TopicData
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class TopicController {

    def awsSnsService
    def configService
    def applicationService

    def allowedMethods = [save: 'POST', update: 'POST', delete: 'POST', subscribe: 'POST', unsubscribe: 'POST', publish: 'POST']

    static editActions = ['prepareSubscribe', 'preparePublish']

    def index = { redirect(action: 'list', params:params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        List<TopicData> topics = (awsSnsService.getTopics(userContext) as List).sort { it.name?.toLowerCase() }
        Map details = ['topics': topics]
        withFormat {
            html { details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    def show = {
        UserContext userContext = UserContext.of(request)
        String topicName = params.id
        TopicData topic = awsSnsService.getTopic(userContext, topicName)
        if (!topic) {
            Requests.renderNotFound('Topic', topicName, this)
        } else {
            List<SubscriptionData> subscriptions = awsSnsService.getSubscriptionsForTopic(userContext, topic).sort()
            boolean hasConfirmedSubscriptions = subscriptions.find { it.isConfirmed() } != null
            Map result = [topic: topic, subscriptions: subscriptions,
                    hasConfirmedSubscriptions: hasConfirmedSubscriptions]
            withFormat {
                html { return result }
                xml { new XML(result).render(response) }
                json { new JSON(result).render(response) }
            }
        }
    }

    def create = {
        String appName = params.appName
        String detail = params.detail
        [
                applications : applicationService.getRegisteredApplications(UserContext.of(request)),
                appName: appName, detail: detail
        ]
    }

    def save = { SaveTopicCommand cmd ->
        if (cmd.hasErrors()) {
            chain(action: 'create', model: [cmd: cmd], params: params)
            return
        }
        String topicName = Relationships.buildAppDetailName(cmd.appName, cmd.detail)
        try {
            awsSnsService.createTopic(UserContext.of(request), topicName)
            flash.message = "Topic '${topicName}' has been created."
            redirect(action: 'show', params: [id: topicName])
        } catch (Exception e) {
            flash.message = "Could not create Topic '${topicName}': ${e}"
            chain(action: 'create', model: [cmd: cmd], params: params)
        }
    }

    def delete = {
        UserContext userContext = UserContext.of(request)
        String topicArn = params.id
        TopicData topic = new TopicData(topicArn)
        awsSnsService.deleteTopic(userContext, topic)
        flash.message = "Topic '${topic.name}' has been deleted."
        redirect(action: 'result')
    }

    def prepareSubscribe = {
        String topic = params.id ?: params.topic
        String endpoint = params.endpoint
        String protocol = params.protocol ?: SubscriptionData.Protocol.default.value
        [topic: topic, endpoint: endpoint, protocol: protocol, protocols: SubscriptionData.Protocol.values()]
    }

    def subscribe = { SubscribeCommand cmd ->
        if (cmd.hasErrors()) {
            chain(action: 'prepareSubscribe', model: [cmd: cmd], params: params)
            return
        }
        UserContext userContext = UserContext.of(request)
        TopicData topic = new TopicData(userContext.region, configService.awsAccountNumber, cmd.topic)
        SubscriptionData subscription = new SubscriptionData(topicName: topic.name, topicArn: topic.arn,
                protocol: cmd.protocol, endpoint: cmd.endpoint)
        try {
            awsSnsService.createSubscription(userContext, subscription)
            flash.message = "Subscribed '${cmd.endpoint}' to Topic '${topic.name}'."
            redirect(action: 'show', params: [id: topic.name])
        } catch (Exception e) {
            flash.message = "Could not subscribe '${cmd.endpoint}' to Topic '${topic.name}': ${e}"
            chain(action: 'prepareSubscribe', model: [cmd: cmd], params: params)
        }
    }

    def unsubscribe = { UnsubscribeCommand cmd ->
        if (cmd.hasErrors()) {
            chain(action: 'show', model: [cmd: cmd], params: params)
            return
        }
        UserContext userContext = UserContext.of(request)
        try {
            awsSnsService.deleteSubscription(userContext, cmd.topic, cmd.subscriptionArn)
            flash.message = "Unsubscribed from Topic '${cmd.topic}'."
        } catch (Exception e) {
            flash.message = "Could not unsubscribe '${cmd.subscriptionArn}': ${e}"
        }
        redirect(action: 'show', params: [id: cmd.topic])
    }

    def preparePublish = {
        String topic = params.id ?: params.topic
        String subject = params.subject
        String message = params.message
        [topic: topic, subject: subject, message: message]
    }

    def publish = { PublishCommand cmd ->
        if (cmd.hasErrors()) {
            chain(action: 'preparePublish', model: [cmd: cmd], params: params)
            return
        }
        UserContext userContext = UserContext.of(request)
        TopicData topic = new TopicData(userContext.region, configService.awsAccountNumber, cmd.topic)
        try {
            awsSnsService.publishToTopic(userContext, topic, cmd.subject, cmd.message)
            flash.message = "Published to Topic '${topic.name}'."
            redirect(action: 'show', params: [id: topic.name])
        } catch (Exception e) {
            flash.message = "Could not publish to Topic '${topic.name}': ${e}"
            chain(action: 'preparePublish', model: [cmd: cmd], params: params)
        }
    }

    def result = { render view: '/common/result' }
}

class SaveTopicCommand {
    String appName
    String detail
    static constraints = {
        appName(nullable: false, blank: false)
    }
}

class SubscribeCommand {
    String topic
    String endpoint
    String protocol
    static constraints = {
        topic(nullable: false, blank: false)
        endpoint(nullable: false, blank: false)
        protocol(nullable: false, blank: false)
    }
}

class UnsubscribeCommand {
    String topic
    String subscriptionArn
    static constraints = {
        topic(nullable: false, blank: false)
        subscriptionArn(nullable: false, blank: false)
    }
}

class PublishCommand {
    String topic
    String subject
    String message
    static constraints = {
        topic(nullable: false, blank: false)
        subject(nullable: false, blank: false)
        message(nullable: false, blank: false)
    }
}
