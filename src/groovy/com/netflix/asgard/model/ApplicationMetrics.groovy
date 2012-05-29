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
import com.netflix.asgard.Relationships

@Immutable final class ApplicationMetrics {

    String application
    Set<MetricId> metrics

    static Set<ApplicationMetrics> load(Collection<Metric> allMetrics) {
        Map<Object, List<Metric>> appNameToMetrics = allMetrics.groupBy { Metric metric ->
            Dimension asgDimension = metric.dimensions.find { it.name == AlarmData.DIMENSION_NAME_FOR_ASG }
            Relationships.appNameFromGroupName(asgDimension.value)
        }

        Set<ApplicationMetrics> metricsForApps = [] as Set
        appNameToMetrics.each { appName, appMetrics ->
            List<MetricId> metricIds = appMetrics.collect { MetricId.fromMetric(it) }
            metricsForApps << new ApplicationMetrics(application: appName, metrics: metricIds)
        }
        metricsForApps
    }

}
