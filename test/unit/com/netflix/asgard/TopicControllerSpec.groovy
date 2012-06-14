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
import com.amazonaws.services.sns.model.GetTopicAttributesResult
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult
import com.amazonaws.services.sns.model.ListTopicsResult
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.services.sns.model.SubscribeRequest
import com.amazonaws.services.sns.model.UnsubscribeRequest
import com.netflix.asgard.mock.Mocks
import grails.plugin.spock.ControllerSpec
import grails.test.MockUtils

@SuppressWarnings("GroovyPointlessArithmetic")
class TopicControllerSpec extends ControllerSpec {
    AmazonSNS mockAmazonSNS = Mock(AmazonSNS)

    void setup() {
        TestUtils.setUpMockRequest()
        mockRequest.region = Region.defaultRegion()
        MockUtils.prepareForConstraintsTests(SaveTopicCommand)
        MockUtils.prepareForConstraintsTests(SubscribeCommand)
        MockUtils.prepareForConstraintsTests(UnsubscribeCommand)
        MockUtils.prepareForConstraintsTests(PublishCommand)
        mockAmazonSNS.listTopics(_) >> { new ListTopicsResult(topics: []) }
        controller.awsSnsService = Mocks.newAwsSnsService(mockAmazonSNS)
        controller.configService = Mocks.configService()
        Mocks.waitForFill(controller.awsSnsService.caches.allTopics)
    }

    def 'show should display Topic'() {
        mockParams.id = 'abadmin-testConformity-Report'

        when:
        Map model = controller.show()

        then:
        'abadmin-testConformity-Report' == model.topic.name
        1 * mockAmazonSNS.getTopicAttributes(_) >> {
            new GetTopicAttributesResult(attributes: [:])
        }
        1 * mockAmazonSNS.listSubscriptionsByTopic(_) >> {
            new ListSubscriptionsByTopicResult(subscriptions: [])
        }
        0 * _._
    }

    def 'show should indicate Topic does not exist'() {
        mockParams.id = 'doesntexist'

        when:
        controller.show()

        then:
        '/error/missing' == controller.renderArgs.view
        "Topic 'doesntexist' not found in us-east-1 test" == controller.flash.message
        1 * mockAmazonSNS.getTopicAttributes(_) >> {
            throw new AmazonServiceException("No attributes for this one.")
        }
        0 * _._
    }

    def 'save should fail without topic'() {
        final cmd = new SaveTopicCommand(appName: '')
        cmd.validate()

        when:
        controller.save(cmd)

        then:
        chainArgs.action == 'create'
    }

    def 'save should fail with error'() {
        final cmd = new SaveTopicCommand(appName: 'app', detail: 'test')
        cmd.validate()
        mockAmazonSNS.createTopic(new CreateTopicRequest(name: 'app-test')) >> {
            throw new IllegalArgumentException("SNS service problems!")
        }

        when:
        controller.save(cmd)

        then:
        chainArgs.action == 'create'
        controller.flash.message == "Could not create Topic 'app-test': java.lang.IllegalArgumentException: SNS service problems!"
    }

    def 'save should create Topic'() {
        final cmd = new SaveTopicCommand(appName: 'app', detail: 'test')
        cmd.validate()

        when:
        controller.save(cmd)

        then:
        redirectArgs.action == 'show'
        redirectArgs.params == [id: "app-test"]
        controller.flash.message == "Topic 'app-test' has been created."
        1 * mockAmazonSNS.createTopic(new CreateTopicRequest(name: 'app-test')) >> {
            new CreateTopicResult()
        }
        0 * _._
    }

    def 'delete should delete Topic'() {
        mockParams.id = 'arn:aws:sns:us-east-1:179000000000:a_test'

        when:
        controller.delete()

        then:
        redirectArgs.action == 'result'
        controller.flash.message == "Topic 'a_test' has been deleted."
        1 * mockAmazonSNS.deleteTopic(new DeleteTopicRequest(topicArn: 'arn:aws:sns:us-east-1:179000000000:a_test'))
        0 * _._
    }

    def 'subscribe should create Subscription'() {
        final cmd = new SubscribeCommand(topic: 'a_test', endpoint: 'rick@grimes.com', protocol: 'email')
        cmd.validate()

        when:
        controller.subscribe(cmd)

        then:
        redirectArgs.action == 'show'
        controller.flash.message == "Subscribed 'rick@grimes.com' to Topic 'a_test'."
        1 * mockAmazonSNS.subscribe(new SubscribeRequest(topicArn: 'arn:aws:sns:us-east-1:179000000000:a_test',
                endpoint: 'rick@grimes.com', protocol: 'email'))
        0 * _._
    }

    def 'unsubscribe should delete Subscription'() {
        final cmd = new UnsubscribeCommand(topic: 'a_test', subscriptionArn: 'arn:subscriptionArn')
        cmd.validate()

        when:
        controller.unsubscribe(cmd)

        then:
        redirectArgs.action == 'show'
        controller.flash.message == "Unsubscribed from Topic 'a_test'."
        1 * mockAmazonSNS.unsubscribe(new UnsubscribeRequest(subscriptionArn: 'arn:subscriptionArn'))
        0 * _._
    }

    def 'publish should send message to Topic'() {
        final cmd = new PublishCommand(topic: 'a_test', subject: 'Regarding the unfortunate incident',
                message: 'Sorry about your carpet. K thanx, bye!')
        cmd.validate()

        when:
        controller.publish(cmd)

        then:
        redirectArgs.action == 'show'
        controller.flash.message == "Published to Topic 'a_test'."
        1 * mockAmazonSNS.publish(new PublishRequest(topicArn: 'arn:aws:sns:us-east-1:179000000000:a_test',
                subject: 'Regarding the unfortunate incident', message: 'Sorry about your carpet. K thanx, bye!'))
        0 * _._
    }
}
