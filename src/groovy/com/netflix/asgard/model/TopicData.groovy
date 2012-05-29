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

import com.netflix.asgard.Region
import java.util.regex.Matcher

class TopicData {

    /**
     * Amazon Resource Locator such as
     * arn:aws:sns:us-east-1:179000000000:abadmin-testConformity-Report
     */
    final String arn

    /** Distinctive part of the topic ARN, like abadmin-testConformity-Report */
    final String name

    Map<String,String> attributes = [:]

    private static String PRE_REGION = 'arn:aws:sns:'
    private static String REGION = '[-a-z0-9]+'
    private static String ACCOUNT_NUMBER = ':[0-9]+:'
    private static String ARN_PATTERN = /${PRE_REGION}${REGION}${ACCOUNT_NUMBER}(.*?)/

    TopicData(String arn) {
        this.arn = arn
        this.name = nameFromArn(arn)
    }

    TopicData(Region region, String accountNumber, String name) {
        this.name = name
        this.arn = "${PRE_REGION}${region}:${accountNumber}:${name}"
    }

    private String nameFromArn(String arn) {
        Matcher matcher = (arn =~ ARN_PATTERN)
        if (matcher.matches()) {
            String capturedName = matcher.group(1)
            return capturedName
        }
        null
    }
}
