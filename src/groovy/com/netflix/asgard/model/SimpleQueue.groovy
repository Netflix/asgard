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

import com.netflix.asgard.Meta
import com.netflix.asgard.Region
import com.netflix.asgard.Time
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import java.util.regex.Matcher
import org.joda.time.DateTime
import org.joda.time.Duration

/**
 * Representation of a Simple Queue Service (SQS) object.
 * Not named Queue, because that would collide with java.util.Queue in Groovy default imports.
 */
@EqualsAndHashCode
@ToString
class SimpleQueue {
    String url
    String name
    Map<String, String> attributes = [:]

    static final String VISIBILITY_TIMEOUT_ATTR_NAME = 'VisibilityTimeout'
    static final String DELAY_SECONDS_ATTR_NAME = 'DelaySeconds'

    private static Map<String, Closure> ATTR_NAMES_TO_HUMAN_READABILITY_METHODS = [
            'VisibilityTimeout': { "${it} seconds"},
            'MaximumMessageSize': { "${it} bytes"},
            'MessageRetentionPeriod': { Time.format(Duration.standardSeconds(it as Long)) },
            'CreatedTimestamp': { Time.format(new DateTime((it as Long) * 1000)) },
            'LastModifiedTimestamp': { Time.format(new DateTime((it as Long) * 1000)) }
    ]

    private String humanReadableValue(String attrKey, String attrValue) {
        Closure method = SimpleQueue.ATTR_NAMES_TO_HUMAN_READABILITY_METHODS[attrKey]
        if (method) {
            return method.call(attrValue)
        }
        attrValue
    }

    private static String PRE_REGION = 'https://sqs.'
    private static String REGION = '[-a-z0-9]+'
    private static String POST_REGION = '.amazonaws.com/'
    private static String ACCOUNT_NUM_DIR = '[0-9]+/'
    private static String URL_PATTERN = /${PRE_REGION}${REGION}${POST_REGION}${ACCOUNT_NUM_DIR}(.*?)/

    SimpleQueue(Region region, String accountNumber, String name) {
        this.name = name
        this.url = "${PRE_REGION}${region}${POST_REGION}${accountNumber}/${name}"
    }

    SimpleQueue(String url) {
        this.url = url
        this.name = nameFromUrl(url)
    }

    private String nameFromUrl(String url) {
        Matcher matcher = (url =~ URL_PATTERN)
        if (matcher.matches()) {
            String capturedName = matcher.group(1)
            return capturedName
        }
        null
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
