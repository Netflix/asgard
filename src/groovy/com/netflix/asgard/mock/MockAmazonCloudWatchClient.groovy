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

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest
import com.amazonaws.services.cloudwatch.model.ListMetricsResult
import com.amazonaws.services.cloudwatch.model.MetricAlarm

class MockAmazonCloudWatchClient extends AmazonCloudWatchClient {

    private Collection<MetricAlarm> mockAlarms

    private Collection<MetricAlarm> loadMockAlarms() {
        [new MetricAlarm().withAlarmName('goofy')]
    }

    MockAmazonCloudWatchClient(AWSCredentialsProvider credentialsProvider, ClientConfiguration clientConfiguration) {
        super(credentialsProvider, clientConfiguration)
        mockAlarms = loadMockAlarms()
    }

    void setEndpoint(String endpoint) {

    }

    DescribeAlarmsResult describeAlarms(DescribeAlarmsRequest request) {
        List<String> requestedNames = request.alarmNames
        Collection<MetricAlarm> foundAlarms = requestedNames ? findMatches(requestedNames) : mockAlarms
        new DescribeAlarmsResult().withMetricAlarms(foundAlarms)
    }

    private Collection<MetricAlarm> findMatches(List<String> requestedNames) {
        mockAlarms.findAll { it.alarmName in requestedNames }
    }

    @Override
    public ListMetricsResult listMetrics(ListMetricsRequest listMetricsRequest) {
        new ListMetricsResult()
    }
}
