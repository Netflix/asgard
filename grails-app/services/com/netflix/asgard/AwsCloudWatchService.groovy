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

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult
import com.amazonaws.services.cloudwatch.model.DimensionFilter
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest
import com.amazonaws.services.cloudwatch.model.ListMetricsResult
import com.amazonaws.services.cloudwatch.model.Metric
import com.amazonaws.services.cloudwatch.model.MetricAlarm
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest
import com.amazonaws.services.cloudwatch.model.SetAlarmStateRequest
import com.netflix.asgard.cache.CacheInitializer
import com.netflix.asgard.model.AlarmData
import com.netflix.asgard.model.MetricId
import com.netflix.asgard.model.MetricNamespaces
import com.netflix.asgard.model.SimpleDbSequenceLocator
import com.netflix.asgard.retriever.AwsResultsRetriever
import org.springframework.beans.factory.InitializingBean

class AwsCloudWatchService implements CacheInitializer, InitializingBean {

    static transactional = false

    MultiRegionAwsClient<AmazonCloudWatch> awsClient
    def awsClientService
    def awsSnsService
    Caches caches
    def configService
    def idService
    def taskService

    void afterPropertiesSet() {
        awsClient = new MultiRegionAwsClient<AmazonCloudWatch>( { Region region ->
            AmazonCloudWatch client = awsClientService.create(AmazonCloudWatch)
            client.setEndpoint("monitoring.${region}.amazonaws.com")
            client
        })
    }

    void initializeCaches() {
        caches.allAlarms.ensureSetUp({ Region region -> retrieveAlarms(region) })
        caches.allCustomMetrics.ensureSetUp({ retrieveCustomMetrics() })
    }

    private List<MetricId> retrieveCustomMetrics() {
        AwsResultsRetriever retriever = new AwsResultsRetriever<Metric, ListMetricsRequest, ListMetricsResult>() {
            ListMetricsResult makeRequest(Region region, ListMetricsRequest request) {
                awsClient.by(region).listMetrics(request)
            }
            List<Metric> accessResult(ListMetricsResult result) {
                result.metrics
            }
        }
        retrieveCustomMetrics(retriever)
    }

    private List<MetricId> retrieveCustomMetrics(AwsResultsRetriever retriever,
            Collection<Region> regions = Region.values()) {
        List<Metric> allMetrics = []
        configService.customMetricNamespacesToDimensions().keySet().each { String namespace ->
            regions.each { Region region ->
                allMetrics = retriever.retrieve(region, new ListMetricsRequest(namespace: namespace))
            }
        }
        allMetrics.collect { MetricId.fromMetric(it) }
    }

    private List<MetricAlarm> retrieveAlarms(Region region) {
        List<MetricAlarm> alarms = []
        DescribeAlarmsResult result = retrieveAlarms(region, null)
        while (true) {
            alarms.addAll(result.metricAlarms)
            if (result.getNextToken() == null) {
                break
            }
            result = retrieveAlarms(region, result.getNextToken())
        }
        alarms
    }

    private DescribeAlarmsResult retrieveAlarms(Region region, String nextToken) {
        awsClient.by(region).describeAlarms(new DescribeAlarmsRequest().withNextToken(nextToken))
    }

    Collection<MetricAlarm> getAllAlarms(UserContext userContext) {
        caches.allAlarms.by(userContext.region).list()
    }

    List<MetricAlarm> getAlarms(UserContext userContext, Collection<String> alarmNames, From from = From.AWS) {
        if (!alarmNames) { return [] }
        if (from == From.CACHE) {
            return alarmNames.inject([]) { List<MetricAlarm> list, String name ->
                list << caches.allAlarms.by(userContext.region).get(name)
            } as List<MetricAlarm>
        }
        DescribeAlarmsResult result = awsClient.by(userContext.region).describeAlarms(
                new DescribeAlarmsRequest(alarmNames: alarmNames))
        final List<MetricAlarm> alarms = result.metricAlarms

        // Fix cache to update found alarms and remove missing alarms
        caches.allAlarms.by(userContext.region).putAllAndRemoveMissing(alarmNames, alarms)

        alarms
    }

    void setAlarmState(UserContext userContext, String alarm, String state, Task existingTask = null) {
        SetAlarmStateRequest request = new SetAlarmStateRequest(alarmName: alarm, stateValue: state,
                stateReason: "Alarm was manually set to ${state} by ${userContext}")
        taskService.runTask(userContext, "Set state of alarm '${alarm}' to ${state}", { Task task ->
            awsClient.by(userContext.region).setAlarmState(request)
        }, Link.to(EntityType.alarm, request.alarmName), existingTask)
    }

    MetricAlarm getAlarm(UserContext userContext, String alarmName, From from = From.AWS) {
        if (!alarmName) { return null }
        Check.loneOrNone(getAlarms(userContext, [alarmName], from), MetricAlarm)
    }

    String createAlarm(UserContext userContext, AlarmData alarmData, String policyArn, Task existingTask = null) {
        String id = idService.nextId(userContext, SimpleDbSequenceLocator.Alarm)
        PutMetricAlarmRequest request = alarmData.toPutMetricAlarmRequest(policyArn, id)
        taskService.runTask(userContext, "Create Alarm '${request.alarmName}'", { Task task ->
            awsClient.by(userContext.region).putMetricAlarm(request)
        }, Link.to(EntityType.alarm, request.alarmName), existingTask)
        request.alarmName
    }

    String updateAlarm(UserContext userContext, AlarmData alarmData, Task existingTask = null) {
        PutMetricAlarmRequest request = alarmData.toPutMetricAlarmRequest()
        taskService.runTask(userContext, "Update Alarm '${request.alarmName}'", { Task task ->
            awsClient.by(userContext.region).putMetricAlarm(request)
        }, Link.to(EntityType.alarm, request.alarmName), existingTask)
        request.alarmName
    }

    void deleteAlarms(UserContext userContext, Collection<String> alarmNames, Task existingTask = null) {
        if (!alarmNames) { return }
        taskService.runTask(userContext, "Delete Alarms '${alarmNames}'", { Task task ->
            final DeleteAlarmsRequest request = new DeleteAlarmsRequest(alarmNames: alarmNames)
            awsClient.by(userContext.region).deleteAlarms(request)
        }, null, existingTask)
        alarmNames.each {
            caches.allAlarms.by(userContext.region).put(it, null)
        }
    }

    List<Metric> getMetricsAppliedToGroups(Collection<Region> regions = Region.values()) {
        DimensionFilter dimensionFilter = new DimensionFilter(name: AlarmData.DIMENSION_NAME_FOR_ASG)
        AwsResultsRetriever retriever = new AwsResultsRetriever<Metric, ListMetricsRequest, ListMetricsResult>() {
            ListMetricsResult makeRequest(Region region, ListMetricsRequest request) {
                awsClient.by(region).listMetrics(request)
            }
            List<Metric> accessResult(ListMetricsResult result) {
                result.metrics
            }
        }
        List<Metric> allMetrics = []
        regions.each { Region region ->
            allMetrics.addAll(retriever.retrieve(region, new ListMetricsRequest(dimensions: [dimensionFilter])))
        }
        allMetrics
    }

    private MetricNamespaces getMetricNamespaces() {
        new MetricNamespaces(configService.customMetricNamespacesToDimensions(), caches.allCustomMetrics.list())
    }

    /**
     * Gets the dimensions available for a metric namespace.
     *
     * @param namespace
     * @return dimension names
     */
    List<String> getDimensionsForNamespace(String namespace) {
        getMetricNamespaces().getDimensionsForNamespace(namespace)
    }

    Map<String, ?> prepareForAlarmCreation(UserContext userContext, Map<String, String> params,
            AlarmData alarmData = null) {
        Collection<String> topicNames = awsSnsService.getTopics(userContext)*.name.sort()
        String description = params.description ?: alarmData?.description
        String statistic = chooseStatistic(params, alarmData)
        boolean useExistingMetric = !params.namespace && !params.metric
        String existingMetric = params.existingMetric
        MetricNamespaces namespaces = getMetricNamespaces()
        Set<MetricId> metrics = namespaces.allMetricIds
        MetricId currentMetric = null
        if (alarmData) {
            currentMetric = new MetricId(namespace: alarmData.namespace, metricName: alarmData.metricName)
            existingMetric = existingMetric ?: currentMetric?.toJson()
            metrics << currentMetric
        }
        List<MetricId> sortedMetrics = metrics?.sort()
        String namespace = chooseNamespace(params, alarmData)
        List<String> dimensions = namespaces.getDimensionsForNamespace(namespace)
        String metric = params.metric ?: alarmData?.metricName
        String comparisonOperator = params.comparisonOperator ?: alarmData?.comparisonOperator
        String threshold = params.threshold ?: alarmData?.threshold
        String period = choosePeriod(params, alarmData)
        String evaluationPeriods = chooseEvaluationPeriods(params, alarmData)
        String topic = params.topic ?: Check.loneOrNone(alarmData?.topicNames ?: [], String)
        [
                comparisonOperators: AlarmData.ComparisonOperator.values(), statistics: AlarmData.Statistic.values(),
                topics: topicNames, metrics: sortedMetrics, description: description, currentMetric: currentMetric,
                statistic: statistic, useExistingMetric: useExistingMetric, existingMetric: existingMetric,
                namespace: namespace, metric: metric, comparisonOperator: comparisonOperator,
                threshold: threshold, period: period, evaluationPeriods: evaluationPeriods, topic: topic,
                dimensions: dimensions, dimensionValues: alarmData?.dimensions
        ]
    }

    private String chooseNamespace(Map<String, String> params, AlarmData alarmData) {
        params.namespace ?: alarmData?.namespace ?: configService.defaultMetricNamespace
    }

    private Serializable choosePeriod(Map<String, String> params, AlarmData alarmData) {
        params.period ?: alarmData?.period ?: '60'
    }

    private Serializable chooseEvaluationPeriods(Map<String, String> params, AlarmData alarmData) {
        params.evaluationPeriods ?: alarmData?.evaluationPeriods ?: '5'
    }

    private Serializable chooseStatistic(Map<String, String> params, AlarmData alarmData) {
        params.statistic ?: alarmData?.statistic ?: AlarmData.Statistic.default.name()
    }
}
