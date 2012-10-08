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

import com.amazonaws.services.cloudwatch.model.Metric
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import grails.converters.JSON
import groovy.transform.Immutable
import java.util.concurrent.TimeUnit
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONWriter

@Immutable final class MetricId implements Comparable<MetricId> {

    /**
     * The purpose of this cache is to limit the number of unique MetricIds in memory. MetricIds are grouped by
     * application name and the same MetricId can be referenced by many application names. In practice there are
     * actually relatively few unique MetricIds.
     */
    static LoadingCache<MetricId, MetricId> internIds = CacheBuilder.newBuilder().
            expireAfterWrite(20, TimeUnit.MINUTES).
            build(new CacheLoader<MetricId, MetricId>() {
                MetricId load(MetricId k) { k }
            })

    String namespace
    String metricName

    static MetricId fromMetric(Metric metric) {
        from(metric.namespace, metric.metricName)
    }

    static MetricId fromJson(String json) {
        JSONElement jsonElement = JSON.parse(json)
        from(jsonElement.namespace, jsonElement.metricName)
    }

    static MetricId from(String namespace, String metricName) {
        internIds.get(new MetricId(namespace: namespace.trim(), metricName: metricName.trim()))
    }

    @Override
    String toString() {
        toJson()
    }

    int compareTo(MetricId that) {
        namespace <=> that.namespace ?: metricName <=> that.metricName
    }

    String toJson() {
        StringWriter writer = new StringWriter()
        new JSONWriter(writer).object()
                .key('namespace').value(namespace)
                .key('metricName').value(metricName)
                .endObject()
        writer.toString()
    }

    String getDisplayText() {
        "${namespace} - ${metricName}"
    }

}
