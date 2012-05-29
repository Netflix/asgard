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

import com.amazonaws.services.cloudwatch.model.Dimension
import com.amazonaws.services.cloudwatch.model.Metric
import spock.lang.Specification

class ApplicationMetricsSpec extends Specification {

    Metric metricOf(String group, String namespace, String metricName) {
        new Metric(namespace: namespace, metricName: metricName,
                dimensions: [new Dimension(name: AlarmData.DIMENSION_NAME_FOR_ASG, value: group)])
    }

    def 'should load unique metrics per application'() {

        List<Metric> metrics = [
                metricOf('nccp-cbp-v009', 'AWS/EC2', 'CPUUtilization'),
                metricOf('nccp-cbp-v009', 'NFLX/EPIC', 'request.per-min'),
                metricOf('nccp-cbp-v009', 'NFLX/EPIC', 'request.per-sec'),
                metricOf('nccp-cbp-v008', 'AWS/EC2', 'CPUUtilization'),
                metricOf('nccp-cbp-v008', 'NFLX/EPIC', 'request.per-min'),
                metricOf('nccp-cbp', 'AWS/EC2', 'CPUUtilization'),
                metricOf('nccp-cbp', 'NFLX/EPIC', 'request.per-hour'),
                metricOf('nccp-cbp', 'NFLX/EPIC', 'CPUUtilization'),
                metricOf('device_wii-v008', 'NFLX/EPIC', 'request.per-min'),
                metricOf('device_ios', 'NFLX/EPIC', 'request.per-min'),
        ]

        when:
        Set<ApplicationMetrics> actualApplicationMetrics = ApplicationMetrics.load(metrics)

        then:
        actualApplicationMetrics == [
                new ApplicationMetrics(application: 'nccp', metrics: [
                        new MetricId(namespace: 'AWS/EC2', metricName: 'CPUUtilization'),
                        new MetricId(namespace: 'NFLX/EPIC', metricName: 'CPUUtilization'),
                        new MetricId(namespace: 'NFLX/EPIC', metricName: 'request.per-sec'),
                        new MetricId(namespace: 'NFLX/EPIC', metricName: 'request.per-min'),
                        new MetricId(namespace: 'NFLX/EPIC', metricName: 'request.per-hour'),
                ]),
                new ApplicationMetrics(application: 'device_wii', metrics: [
                        new MetricId(namespace: 'NFLX/EPIC', metricName: 'request.per-min'),
                ]),
                new ApplicationMetrics(application: 'device_ios', metrics: [
                        new MetricId(namespace: 'NFLX/EPIC', metricName: 'request.per-min'),
                ]),
        ] as Set
    }

}
