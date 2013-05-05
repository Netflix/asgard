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

class FastPropsTagLib {

    def writeScope = { attrs ->
        def instanceId = attrs.instanceId ? attrs.remove('instanceId') : null
        def asg = attrs.asg ? attrs.remove('asg') : null
        def ami = attrs.ami ? attrs.remove('ami') : null
        def cluster = attrs.cluster ? attrs.remove('cluster') : null
        def countries = attrs.countries ? attrs.remove('countries') : null
        def stack = attrs.stack ? attrs.remove('stack') : null
        def zone = attrs.zone ? attrs.remove('zone') : null
        def ttl = attrs.ttl ? attrs.remove('ttl') : null

        if (instanceId || asg || ami || cluster || countries || stack || zone || ttl) {
            out << '<table class="scopeAttribs">'

            // in the order or priority
            if (instanceId) {
                out << "<tr><td>Instance</td><td>${instanceId?.encodeAsHTML()}</td></tr>"
            }

            if (asg) {
                out << "<tr><td>ASG</td><td>${asg?.encodeAsHTML()}</td></tr>"
            }

            if (ami) {
                out << "<tr><td>AMI</td><td>${ami?.encodeAsHTML()}</td></tr>"
            }

            if (cluster) {
                out << "<tr><td>Cluster</td><td>${cluster?.encodeAsHTML()}</td></tr>"
            }

            if (countries) {
                out << "<tr><td>Countries</td><td>${countries?.encodeAsHTML()}</td></tr>"
            }

            if (stack) {
                out << "<tr><td>Stack</td><td>${stack?.encodeAsHTML()}</td></tr>"
            }

            if (zone) {
                out << "<tr><td>Zone</td><td>${zone?.encodeAsHTML()}</td></tr>"
            }

            if (ttl) {
                out << "<tr><td>TTL</td><td>${ttl?.encodeAsHTML()}</td></tr>"
            }
            out << '</table>'
        }
    }

}
