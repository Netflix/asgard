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

import org.joda.time.format.ISODateTimeFormat

/**
 * Tag library class for fast properties views.
 *
 * Implements custom tags which
 *    1. builds additional scope inline table
 *    2. calculates and prints expiration timestamp for a fast property
 */
class FastPropsTagLib {

    def configService

    /**
     * Builds additional scope attributes inline table for a fastProperty
     *
     * @attr instanceId ServerId corresponding to the current fastProperty's scope
     * @attr asg Auto Scaling Group corresponding to the current fastProperty's scope
     * @attr ami AMI corresponding to the current fastProperty's scope
     * @attr cluster Cluster corresponding to the current fastProperty's scope
     * @attr countries Countries corresponding to the current fastProperty's scope
     * @attr stack Stack corresponding to the current fastProperty's scope
     * @attr zone Zone corresponding to the current fastProperty's scope
     */
    def writeScope = { attrs ->
        def instanceId = attrs.instanceId ? attrs.remove('instanceId') : null
        def asg = attrs.asg ? attrs.remove('asg') : null
        def ami = attrs.ami ? attrs.remove('ami') : null
        def cluster = attrs.cluster ? attrs.remove('cluster') : null
        def countries = attrs.countries ? attrs.remove('countries') : null
        def stack = attrs.stack ? attrs.remove('stack') : null
        def zone = attrs.zone ? attrs.remove('zone') : null

        if (instanceId || asg || ami || cluster || countries || stack || zone) {
            out << '<table class="scopeAttribs">'

            // In the order of priority
            outputInstanceId(instanceId)
            outputAsg(asg)
            outputAmi(ami)
            outputCluster(cluster)
            outputCountries(countries)
            outputStack(stack)
            outputZone(zone)

            out << '</table>'
        }
    }

    private void outputZone(zone) {
        if (zone) {
            out << "<tr><td>Zone</td><td>${zone?.encodeAsHTML()}</td></tr>"
        }
    }

    private void outputStack(stack) {
        if (stack) {
            out << "<tr><td>Stack</td><td>${stack?.encodeAsHTML()}</td></tr>"
        }
    }

    private void outputCountries(countries) {
        if (countries) {
            out << "<tr><td>Countries</td><td>${countries?.encodeAsHTML()}</td></tr>"
        }
    }

    private void outputCluster(cluster) {
        if (cluster) {
            out << "<tr><td>Cluster</td><td>${cluster?.encodeAsHTML()}</td></tr>"
        }
    }

    private void outputAmi(ami) {
        if (ami) {
            out << "<tr><td>AMI</td><td>${ami?.encodeAsHTML()}</td></tr>"
        }
    }

    private void outputAsg(asg) {
        if (asg) {
            out << "<tr><td>ASG</td><td>${asg?.encodeAsHTML()}</td></tr>"
        }
    }

    private void outputInstanceId(instanceId) {
        if (instanceId) {
            out << "<tr><td>Instance</td><td>${instanceId?.encodeAsHTML()}</td></tr>"
        }
    }

    /**
     * Calculates and prints expiration timestamp for a fastProperty
     * The output format is an ISOi-8601. It does not include seconds, msecs or explicit
     * timezone information.
     *
     * @attr ttl TTL in seconds as stored for a fastProperty
     * @attr ts last update/created timestamp in ISO-8601 format for a fastProperty
     */
    def writeExpiration = { attrs ->
        def ttl = attrs.ttl ? attrs.remove('ttl') : null
        def creationTimeStamp = attrs.ts ? attrs.remove('ts') : null
        if (ttl && creationTimeStamp) {
            def dateTimeFormatter = ISODateTimeFormat.dateTimeParser()
            def outputFormatter = ISODateTimeFormat.dateHourMinute()
            def createdTime = dateTimeFormatter.parseDateTime(creationTimeStamp)
            def expTime = createdTime.plusSeconds(ttl?.toInteger())
            out << outputFormatter.print(expTime)
        }
    }

    /**
     * Builds fast property console url link if found in configuration
     * It does not create any output if configuration does not contain this information
     * Configuration needed for this tag:
     *  fastPropertyConsoleUrls = [
     *      test : '...'
     *      prod : '...'
     *  ]
     * This way you configure an external console url based on the account type (test/prod)
     */
    def extLinkToPropertiesConsole = {
        String propertiesConsoleUrl = configService.getFastPropertiesConsoleUrl()
        if (propertiesConsoleUrl) {
            out << '<li class="menuButton">'
            out << '<a href="' + propertiesConsoleUrl + '" target="_blank" class="fastProperties">Fast Properties</a>'
            out << '</li>'
        }
    }
}
