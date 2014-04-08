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

import com.netflix.appinfo.ApplicationInfoManager
import com.netflix.appinfo.EurekaInstanceConfig
import com.netflix.appinfo.InstanceInfo.InstanceStatus
import com.netflix.asgard.ConfigService
import com.netflix.asgard.EnvironmentService
import com.netflix.asgard.HealthcheckService
import com.netflix.discovery.DiscoveryManager
import com.netflix.discovery.EurekaClientConfig
import org.springframework.beans.factory.InitializingBean

/**
 * Initializes Eureka Client (if configured) so that Asgard can register with Eureka Service.
 */
class EurekaClientHolder implements InitializingBean {

    ConfigService configService
    EnvironmentService environmentService
    DiscoveryManager discoveryManager
    HealthcheckService healthcheckService

    @Override
    void afterPropertiesSet() throws Exception {
        if (configService.eurekaDefaultRegistrationUrl && configService.eurekaZoneListsByRegion &&
                configService.eurekaUrlTemplateForZoneRegionEnv) {
            initDiscoveryManager()
            registerHealthcheckCallback()
        }
    }

    /**
     * The name under which to register the health check callback that will update the Eureka status when system is
     * ready for traffic.
     */
    final String healthcheckCallbackName = 'EurekaStatusUpdate'

    /**
     * The callback that should run after each time the local health check runs, to change the Eureka status from
     * STARTING to UP as soon as the system is ready for traffic.
     */
    final Closure healthcheckCallback = { Boolean wasHealthyLastTime, Boolean isHealthyNow ->
        if (isHealthyNow && ApplicationInfoManager.instance.info.status == InstanceStatus.STARTING) {
            ApplicationInfoManager.instance.instanceStatus = InstanceStatus.UP
        }
    }

    /**
     * Ensures that the DiscoveryManager singleton exists and is stored in holder, then initializes it with Asgard's
     * implementations of the Eureka config objects.
     */
    void initDiscoveryManager() {
        discoveryManager = DiscoveryManager.instance
        discoveryManager.initComponent(createEurekaInstanceConfig(), createEurekaClientConfig())
    }

    /**
     * When Asgard becomes healthy during Eureka instance status STARTING, change instance status to UP.
     */
    void registerHealthcheckCallback() {
        healthcheckService.registerCallback(healthcheckCallbackName, healthcheckCallback)
    }

    /**
     * @return a configured EurekaClientConfig implementation for Asgard to use when initializing
     *          com.netflix.discovery.DiscoveryManager
     */
    EurekaClientConfig createEurekaClientConfig() {
        new AsgardEurekaClientConfig(
                env: configService.accountName,
                region: environmentService.region,
                eurekaDefaultRegistrationUrl: configService.eurekaDefaultRegistrationUrl,
                eurekaZoneListsByRegion: configService.eurekaZoneListsByRegion,
                eurekaUrlTemplate: configService.eurekaUrlTemplateForZoneRegionEnv
        )
    }

    /**
     * @return a configured EurekaInstanceConfig implementation for Asgard to use when initializing
     *          com.netflix.discovery.DiscoveryManager
     */
    EurekaInstanceConfig createEurekaInstanceConfig() {
        if (environmentService.instanceId) {
            String virtualHostName = configService.localVirtualHostName
            Integer nonSecurePort = configService.localInstancePort
            return new AsgardCloudEurekaInstanceConfig(nonSecurePort: nonSecurePort, virtualHostName: virtualHostName)
        } else {
            return new AsgardDataCenterEurekaInstanceConfig()
        }
    }
}
