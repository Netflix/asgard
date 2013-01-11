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

import org.joda.time.DateTime
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Discovery API ApplicationInstance encapsulation contained within an Application.
 */
@EqualsAndHashCode
@ToString
class ApplicationInstance {
    String appName
    String hostName  // Always accessible hostname: ec2 public, or dc/intra private
    String ipAddr    // local ip
    String version
    String status
    String port
    String securePort
    //String identifyingAttribute  // HOSTNAME / EC2_INSTANCE_ID
    Map dataCenterInfo
    Map leaseInfo
    Map metadata
    String statusPageUrl
    String healthCheckUrl
    String vipAddress
    String instanceId
    //Boolean isCoordinatingDiscoveryServer  // needed only by Discovery internally

    ApplicationInstance() {
        super()
    }

    ApplicationInstance(xml) {
        if (xml.hostName) {
            appName = xml.app.toString().toLowerCase()
            hostName = xml.hostName
            //println "    appName/hostName: ${appName}/${hostName}"
            ipAddr = xml.ipAddr
            version = xml.version
            status = xml.status
            port = xml.port.text()
            securePort = xml.securePort.text()
            statusPageUrl = xml.statusPageUrl.text()
            healthCheckUrl = xml.healthCheckUrl.text()
            vipAddress = xml.vipAddress.text()
            xml.dataCenterInfo.metadata?.each {
                dataCenterInfo = parseKeyValue(it)
            }
            xml.leaseInfo.each {
                leaseInfo = parseKeyValue(it)
            }
            xml.metadata.each {
                metadata = parseKeyValue(it)
            }
            instanceId = dataCenterInfo?.'instance-id'
        }
    }

    private static final Long SO_BIG_IT_MUST_BE_AN_EPOCH_TIMESTAMP = 1000L * 1000 * 1000 * 1000
    static def parseKeyValue(xml) {
        def map = [:]
        xml.'*'.each {
            def key = it.name().trim()
            if (!(key in ["metadata", "dataCenterInfo", "leaseInfo"])) {
                def value = it.text()
                if (value.isLong() && value.toLong() > SO_BIG_IT_MUST_BE_AN_EPOCH_TIMESTAMP) {
                    value = new DateTime(value.toLong()).toString()
                }
                map[key] = value
                //println "      " + key + ": " + value
            }
        }
        map
    }
}
