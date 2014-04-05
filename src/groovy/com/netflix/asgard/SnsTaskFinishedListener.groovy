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

import com.netflix.asgard.model.TopicData
import com.netflix.asgard.plugin.TaskFinishedListener
import grails.converters.JSON
import grails.web.JSONBuilder
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired

/**
 * (@link TaskFinishedListener) implementation for publishing to an SNS topic.
 */
class SnsTaskFinishedListener implements TaskFinishedListener {

    static final List<String> EXCLUDED_PROPS = ['log', 'logAsString', 'thread']

    @Autowired
    AwsSnsService awsSnsService

    @Autowired
    ConfigService configService

    /**
     * Publishes to the SNS topic specified in Config.groovy under sns/taskFinished. If the topic is not specified,
     * this method does nothing.
     *
     * @param task The finished task (can be completed or failed)
     */
    void taskFinished(Task task) {
        Region region = configService.taskFinishedSnsTopicRegion
        String topicName = configService.taskFinishedSnsTopicName
        if (!region || !topicName || !configService.online) {
            return // Sns notifications are not configured
        }
        UserContext userContext = UserContext.auto(region)
        TopicData topicData = awsSnsService.getTopic(userContext, topicName, From.CACHE)
        String subject = StringUtils.substring(task.name, 0, 100).trim()
        // Idea from http://stackoverflow.com/questions/5936300/grails-converters-json-except-few-properties
        // Also this can be changed to the Groovy version of JSONBuilder when Grails 2.x upgrade is complete
        JSON builder = new JSONBuilder().build {
            task.properties.each { String propName, propValue ->
                if (!EXCLUDED_PROPS.contains(propName)) { // Keep under the 8k SNS limit
                    setProperty(propName, propValue)
                }
            }
        }
        awsSnsService.publishToTopic(userContext, topicData, subject, builder.toString())
    }

}
