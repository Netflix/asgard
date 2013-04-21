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

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.netflix.frigga.NameValidation
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.xml.MarkupBuilder
import org.joda.time.DateTime
import org.joda.time.Period
import org.springframework.web.util.HtmlUtils

@EqualsAndHashCode
@ToString
class FastProperty {

    static final String SOURCE_OF_UPDATE = 'asgard'
    static final ImmutableList<String> TTL_UNITS = ImmutableList.of('Weeks', 'Days', 'Hours', 'Minutes', 'Seconds')

    String id
    String key
    String value
    String env
    String appId
    String countries
    String serverId
    String updatedBy
    String stack
    String region
    String sourceOfUpdate
    String cmcTicket
    String ts
    String asg
    String cluster
    String ami
    String zone
    String ttl
    String constraints

    private static final ImmutableList<String> ATTRIBUTES_THAT_FORM_ID = ImmutableList.of('key', 'appId', 'env',
            'region', 'serverId', 'stack', 'countries', 'asg', 'cluster', 'ami', 'zone')

    private static final ImmutableList<String> ADVANCED_ATTRIBUTES = ImmutableList.of('ttl', 'serverId', 'asg',
            'cluster', 'ami', 'countries', 'zone', 'constraints')

    public static final ImmutableList<String> ALL_ATTRIBUTES = ImmutableList.copyOf((getMetaClass().
            properties*.name - ['SOURCE_OF_UPDATE', 'metaClass', 'class', 'TTL_UNITS', 'timestamp', 'expires']).sort())

    private static final ImmutableMap<String, String> ATTRIBUTE_TO_XML_NAME = ImmutableMap.copyOf(ALL_ATTRIBUTES.
            collectEntries { [it, it] } + ['id': 'propertyId'])

    /**
     * Constructs FastProperty from an XML representation.
     */
    static FastProperty fromXml(xml) {
        if (!xml) { return null }
        FastProperty fastProperty = new FastProperty()
        ATTRIBUTE_TO_XML_NAME.each { String attributeName, xmlName ->
            String value = xml[xmlName]?.toString()
            fastProperty[attributeName] = value ? HtmlUtils.htmlUnescape(value) : ''
        }
        fastProperty
    }

    /**
     * Ensures all attributes are valid.
     *
     * @throws IllegalStateException describing invalid properties
     */
    void validate() {
        if (!key) { throw new IllegalStateException('A Fast Property key is required.') }
        if (!value) { throw new IllegalStateException('A Fast Property value is required.') }
        if (!ttl && (serverId || asg || ami)) {
            throw new IllegalStateException('A TTL must be specified when transient scoping dimensions are used.')
        }
        Map<String, String> propertiesToValues = ATTRIBUTES_THAT_FORM_ID.collectEntries { [(it) : this[it]] }
        Map<String, String> invalidPropertiesToValues = propertiesToValues.
                findAll { String name, String value -> value && !NameValidation.checkDetail(value) }
        if (!invalidPropertiesToValues) { return }
        String invalidValues = invalidPropertiesToValues.
                collect { String name, String value -> "${name} = '${value}'" }.join(', ')
        String msg = "Attributes that form a Fast Property ID can only include letters, numbers, dots, underscores, " +
                "and hyphens. The following values are not allowed: ${invalidValues}"
        throw new IllegalStateException(msg)
    }

    /**
     * Constructs an XML representation from this FastProperty.
     */
    String toXml() {
        StringWriter writer = new StringWriter()
        final MarkupBuilder builder = new MarkupBuilder(writer)
        builder.property {
            ATTRIBUTE_TO_XML_NAME.each { String attributeName, xmlName ->
                String value = this[attributeName]
                if (value) {
                    builder."${xmlName}"(value)
                }
            }
        }
        writer.toString()
    }

    /**
     * Determines if FastProperty has more than just the basic attributes.
     */
    boolean hasAdvancedAttributes() {
        ADVANCED_ATTRIBUTES.find { this[it] }
    }

    /**
     * Converts ts to a DateTime.
     */
    DateTime getTimestamp() {
        new DateTime(ts)
    }

    /**
     * Calculates expiration time if there is a ttl.
     */
    DateTime getExpires() {
        if (!ttl) { return null }
        timestamp.plusSeconds(ttl as Integer)
    }

    /**
     * Set the ttl in seconds converted from a specified unit of measure. Seconds is assumed if no unit is provided.
     *
     * @param ttlInUnits amount of time
     * @param unitOfMeasurement unit of measurement for the ttlInUnits
     */
    void setTtlInUnits(int ttlInUnits, String unitOfMeasurement) {
        if (!unitOfMeasurement) {
            ttl = ttlInUnits
            return
        }
        if (!(unitOfMeasurement in TTL_UNITS)) {
            throw new IllegalArgumentException("TTL Unit must be one of these: ${TTL_UNITS}")
        }
        Period period = new Period()."with${unitOfMeasurement}"(ttlInUnits)
        ttl = period.toStandardSeconds().seconds
    }
}
