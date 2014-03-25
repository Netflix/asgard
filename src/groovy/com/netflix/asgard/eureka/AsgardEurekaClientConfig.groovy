/*
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.asgard.eureka

import com.netflix.discovery.DefaultEurekaClientConfig
import groovy.transform.Canonical
import org.apache.commons.logging.LogFactory

/**
 * Enables Eureka Client configuration values to originate from ConfigService instead of from properties files on the
 * classpath.
 */
@Canonical class AsgardEurekaClientConfig extends DefaultEurekaClientConfig {

    private static final log = LogFactory.getLog(this)

    String env
    String region
    String eurekaDefaultRegistrationUrl
    Map<String, List<String>> eurekaZoneListsByRegion
    String eurekaUrlTemplate

    @Override String[] getAvailabilityZones(String region) {
        eurekaZoneListsByRegion[region] as String[]
    }

    @Override List<String> getEurekaServerServiceUrls(String myZone) {
        log.debug "Looking up Eureka server service URL for availability zone ${myZone}"
        List<String> urls
        Map.Entry<String, List<String>> regionToZones = eurekaZoneListsByRegion.find { k, v -> v.contains myZone }
        if (regionToZones) {
            String region = regionToZones.key
            urls = [eurekaUrlTemplate.replace('${zone}', myZone).replace('${region}', region).replace('${env}', env)]
        } else {
            urls = eurekaDefaultRegistrationUrl ? [eurekaDefaultRegistrationUrl] : null
        }
        log.debug "Using Eureka server service URLs ${urls}"
        urls
    }

    @Override String getRegion() { region }
}
