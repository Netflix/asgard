package com.netflix.asgard

import com.netflix.asgard.model.TopicData
import com.netflix.asgard.plugin.TaskFinishedListener
import grails.converters.JSON
import grails.web.JSONBuilder
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired

class SnsTaskFinishedListener implements TaskFinishedListener {

    static final List<String> EXCLUDED_PROPS = ['log', 'logAsString', 'thread']

    @Autowired
    AwsSnsService awsSnsService

    @Autowired
    ConfigService configService

    void taskFinished(Task task) {
        Region region = configService.taskFinishedSnsTopicRegion
        String topicName = configService.taskFinishedSnsTopicName
        if (!region || !topicName) {
            return // Sns notifications are not configured
        }
        UserContext userContext = UserContext.auto(region)
        TopicData topicData = awsSnsService.getTopic(userContext, topicName)
        String subject = StringUtils.substring(task.name, 0, 100).trim()
        // Idea from http://stackoverflow.com/questions/5936300/grails-converters-json-except-few-properties
        // Also this can be changed to the Groovy version of JSONBuilder when Grails 2.x upgrade is complete
        JSON builder = new JSONBuilder().build {
            task.properties.each { propName, propValue ->
                if (!EXCLUDED_PROPS.contains(propName)) { // Keep under the 8k SNS limit
                    setProperty(propName, propValue)
                }
            }
        }
        awsSnsService.publishToTopic(userContext, topicData, subject, builder.toString())
    }

}
