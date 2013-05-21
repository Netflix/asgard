/*
 * Copyright 2013 Netflix, Inc.
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

import com.amazonaws.services.cloudwatch.model.ListMetricsRequest
import com.amazonaws.services.cloudwatch.model.Metric
import com.netflix.asgard.model.MetricId
import com.netflix.asgard.retriever.AwsResultsRetriever
import spock.lang.Specification

class AwsCloudWatchServiceUnitSpec extends Specification {

    def 'should retreive custom metrics'() {
        AwsCloudWatchService service = new AwsCloudWatchService()
        AwsResultsRetriever retriever = Mock(AwsResultsRetriever)
        service.configService = Mock(ConfigService) {
            customMetricNamespacesToDimensions() >> ['NFLX/EPIC': ['length', 'width']]
        }

        when:
        List<MetricId> customMetrics = service.retrieveCustomMetrics(retriever, [Region.US_WEST_1])

        then:
        1 * retriever.retrieve(Region.US_WEST_1, new ListMetricsRequest(namespace: 'NFLX/EPIC')) >> [
                new Metric(namespace: 'NFLX/EPIC', metricName: 'numberOfHatsWornAtOneTime'),
                new Metric(namespace: 'NFLX/EPIC', metricName: 'currentValueOfStarWarsActionFigures'),
                new Metric(namespace: 'NFLX/EPIC', metricName: 'amountOfJillbeesEatenYtdInMetricTons')
        ]
        customMetrics == [
                new MetricId(namespace: 'NFLX/EPIC', metricName: 'numberOfHatsWornAtOneTime'),
                new MetricId(namespace: 'NFLX/EPIC', metricName: 'currentValueOfStarWarsActionFigures'),
                new MetricId(namespace: 'NFLX/EPIC', metricName: 'amountOfJillbeesEatenYtdInMetricTons'),
        ]
    }
}
