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
package com.netflix.asgard.model

import com.amazonaws.services.sqs.model.QueueAttributeName
import com.netflix.asgard.Meta
import com.netflix.asgard.Time
import groovy.transform.Canonical
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.joda.time.DateTime
import org.joda.time.Duration

/**
 * Representation of a Simple Queue Service (SQS) object.
 * Not named Queue, because that would collide with java.util.Queue in Groovy default imports.
 */
@Canonical class SimpleQueue {
    String region
    String accountNumber
    String name
    Map<String, String> attributes = [:]

    private static Map<QueueAttributeName, Closure<String>> ATTR_NAMES_TO_HUMAN_READABILITY_METHODS = [
            (QueueAttributeName.VisibilityTimeout): { "${it} seconds" },
            (QueueAttributeName.MaximumMessageSize): { "${it} bytes" },
            (QueueAttributeName.MessageRetentionPeriod): { Time.format(Duration.standardSeconds(it as Long)) },
            (QueueAttributeName.CreatedTimestamp): { Time.format(new DateTime((it as Long) * 1000)) },
            (QueueAttributeName.LastModifiedTimestamp): { Time.format(new DateTime((it as Long) * 1000)) }
    ]

    private String humanReadableValue(String attrKey, String attrValue) {
        QueueAttributeName queueAttributeName
        try {
            queueAttributeName = QueueAttributeName.fromValue(attrKey)
        } catch (IllegalArgumentException ignore) {
            return attrValue
        }
        Closure<String> method = ATTR_NAMES_TO_HUMAN_READABILITY_METHODS[queueAttributeName]
        if (method) {
            return method(attrValue)
        }
        attrValue
    }

    private static String PRE_REGION = 'https://sqs.'
    private static String REGION = '[-a-z0-9]+'
    private static String POST_REGION = '.amazonaws.com/'
    private static String ACCOUNT_NUM_DIR = '[0-9]+'
    private static Pattern URL_PATTERN = ~/${PRE_REGION}(${REGION})${POST_REGION}(${ACCOUNT_NUM_DIR})\/(.*?)/
    private static Pattern ARN_PATTERN = ~/arn:aws:sqs:(${REGION}):(${ACCOUNT_NUM_DIR}):(.*?)/

    static SimpleQueue fromUrl(String url) {
        fromPattern(URL_PATTERN, url)
    }

    static SimpleQueue fromArn(String arn) {
        fromPattern(ARN_PATTERN, arn)
    }

    private static SimpleQueue fromPattern(Pattern pattern, String arn) {
        Matcher matcher = (arn =~ pattern)
        if (matcher.matches()) {
            return new SimpleQueue(region: matcher.group(1), accountNumber: matcher.group(2), name: matcher.group(3))
        }
        null
    }

    /** @return constructed URL for queue */
    String getUrl() {
        "${PRE_REGION}${region}${POST_REGION}${accountNumber}/${name}"
    }

    /** @return constructed Amazon Resource Name for queue */
    String getArn() {
        "arn:aws:sqs:${region}:${accountNumber}:${name}"
    }

    Map<String, String> getHumanReadableAttributes() {
        Map<String, String> humanReadableMap = [:]
        attributes.each { String key, String value ->
            humanReadableMap[Meta.splitCamelCase(key)] = humanReadableValue(key, value)
        }
        humanReadableMap
    }

    SimpleQueue withAttributes(Map<String, String> attributes) {
        this.attributes = attributes
        this
    }
}
