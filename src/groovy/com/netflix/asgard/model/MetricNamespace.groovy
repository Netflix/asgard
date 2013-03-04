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
package com.netflix.asgard.model

import com.google.common.collect.ImmutableSet
import groovy.transform.Canonical

/**
 * Holds AWS CloudWatch metrics and dimensions for a namespace.
 * http://docs.amazonwebservices.com/AmazonCloudWatch/latest/DeveloperGuide/CW_Support_For_AWS.html
 *
 * @see com.netflix.asgard.model.MetricNamespaces
 */
@Canonical
class MetricNamespace {

    /**
     * The AWS Cloudwatch namespace. It is the name of a service that metrics apply to.
     */
    final String namespace

    /**
     * Names of AWS Cloudwatch metrics contained in this namespace. Metrics are monitoring data points provided by a
     * service.
     */
    final ImmutableSet<String> metrics

    /**
     * Names of AWS Cloudwatch dimensions available to metrics in this namespace. Dimensions filter the metric data to
     * a specified subset of resources.
     */
    final ImmutableSet<String> dimensions

    static MetricNamespace of(String namespace, Collection<String> metrics, Collection<String> dimensions) {
        new MetricNamespace(namespace, ImmutableSet.copyOf(metrics), ImmutableSet.copyOf(dimensions))
    }

    /**
     * @returns this namespaces metrics as MetricIds with namespace populated
     */
    Set<MetricId> getMetricIds() {
        metrics.collect { MetricId.from(namespace, it) }
    }
}
