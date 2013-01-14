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
import com.netflix.frigga.NameValidation
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder
import org.springframework.web.util.HtmlUtils

@EqualsAndHashCode
@ToString
class FastProperty {

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
    final Date timestamp

    private static final ImmutableList<String> PROPERTIES_THAT_FORM_ID = ImmutableList.of('key', 'appId', 'env',
            'region', 'serverId', 'stack', 'countries')

    FastProperty() {
        this.timestamp = Time.parse(this.ts)?.toDate()
    }

    static FastProperty fromXml(GPathResult xml) {
        if (!xml) { return null }
        FastProperty fastProperty = new FastProperty()
        ['key', 'value', 'env', 'appId', 'countries', 'serverId', 'updatedBy', 'stack', 'region', 'sourceOfUpdate',
                'cmcTicket', 'ts'].each {
            String value = xml[it]?.toString()
            fastProperty[it] = value ? HtmlUtils.htmlUnescape(value) : ''
        }
        fastProperty
    }

    /**
     * Generates the id based on current field values.
     *
     * @return id of a fast property with these field values
     */
    String getId() {
        PROPERTIES_THAT_FORM_ID.collect { this[it] ?: '' }.join('|')
    }

    /**
     * Ensures the properties that make up an ID are valid.
     *
     * @throws IllegalStateException describing invalid properties
     */
    void validateId() {
        Map<String, String> propertiesToValues = PROPERTIES_THAT_FORM_ID.collectEntries { [(it) : this[it]] }
        Map<String, String> invalidPropertiesToValues = propertiesToValues.
                findAll { String name, String value -> value && !NameValidation.checkDetail(value) }
        if (!invalidPropertiesToValues) { return }
        String invalidValues = invalidPropertiesToValues.
                collect { String name, String value -> "${name} = '${value}'" }.join(', ')
        String msg = "Attributes that form a Fast Property ID can only include letters, numbers, dots, underscores, " +
                "and hyphens. The following values are not allowed: ${invalidValues}"
        throw new IllegalStateException(msg)
    }

    String toXml() {
        StringWriter writer = new StringWriter()
        final MarkupBuilder builder = new MarkupBuilder(writer)
        builder.property {
            builder.key(key)
            builder.value(value)
            builder.env(env)
            builder.appId(appId)
            builder.region(region)
            builder.stack(stack)
            builder.countries(countries)
            builder.updatedBy(updatedBy)
            builder.sourceOfUpdate(sourceOfUpdate)
            builder.cmcTicket(cmcTicket)
        }
        writer.toString()
    }
}
