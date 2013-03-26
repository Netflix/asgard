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

import spock.lang.Specification

class MetricNamespacesSpec extends Specification {

    def 'should return dimensions for namespace'() {
        MetricNamespaces namespaces = new MetricNamespaces()

        expect:
        namespaces.getDimensionsForNamespace('AWS/EC2') == ['AutoScalingGroupName', 'ImageId', 'InstanceId',
                'InstanceType']
    }

    def 'should return dimensions for custom namespace'() {
        MetricNamespaces namespaces = new MetricNamespaces(['NFLX/EPIC' : ['AutoScalingGroupName', 'InstanceId']])

        expect:
        namespaces.getDimensionsForNamespace('NFLX/EPIC') == ['AutoScalingGroupName', 'InstanceId']
    }

    def 'should return all metric Ids'() {
        Collection<MetricId> allCustomMetricIds = [MetricId.from('NFLX/EPIC', 'stat1'),
                MetricId.from('NFLX/EPIC', 'stat2')]
        MetricNamespaces namespaces = new MetricNamespaces(['NFLX/EPIC' : ['AutoScalingGroupName', 'InstanceId']],
                allCustomMetricIds)

        when:
        Set<MetricId> allMetricIds = namespaces.getAllMetricIds()

        then:
        allMetricIds.contains(MetricId.from('NFLX/EPIC', 'stat1'))
        allMetricIds.contains(MetricId.from('NFLX/EPIC', 'stat2'))
        allMetricIds.contains(MetricId.from('AWS/EC2', 'CPUUtilization'))
        allMetricIds.size() == 116

    }

    def 'should handle null'() {
        MetricNamespaces namespaces = new MetricNamespaces(null, null)

        expect:
        namespaces.getAllMetricIds().size() == 114
    }
}
