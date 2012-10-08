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

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.slurpersupport.GPathResult
import org.springframework.web.util.HtmlUtils

@EqualsAndHashCode
@ToString
class FastProperty {

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
    Date timestamp

    private FastProperty(GPathResult xml) {
        ['key', 'value', 'env', 'appId', 'countries', 'serverId', 'updatedBy', 'stack', 'region', 'sourceOfUpdate',
                'cmcTicket', 'ts'].each {
            String value = xml[it]?.toString()
            this[it] = value ? HtmlUtils.htmlUnescape(value) : ''
        }
        this.timestamp = Time.parse(this.ts)?.toDate()
        this.id = generateId(key, appId, env, region, serverId, stack, countries)
    }

    static FastProperty fromXml(GPathResult xml) {
        xml ? new FastProperty(xml) : null
    }

    private String generateId(String key, String appId, String env, String region, String serverId, String stack,
                      String countries) {
        "${key ?: ''}|${appId ?: ''}|${env ?: ''}|${region ?: ''}|${serverId ?: ''}|${stack ?: ''}|${countries ?: ''}"
    }
}
