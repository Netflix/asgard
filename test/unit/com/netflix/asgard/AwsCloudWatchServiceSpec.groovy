package com.netflix.asgard

import com.amazonaws.services.cloudwatch.model.ListMetricsRequest
import com.amazonaws.services.cloudwatch.model.Metric
import com.netflix.asgard.model.MetricId
import com.netflix.asgard.retriever.AwsResultsRetriever
import spock.lang.Specification

class AwsCloudWatchServiceSpec extends Specification {

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
