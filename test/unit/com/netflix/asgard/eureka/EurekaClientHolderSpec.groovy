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
import com.netflix.appinfo.InstanceInfo
import com.netflix.appinfo.InstanceInfo.InstanceStatus
import com.netflix.asgard.ConfigService
import com.netflix.asgard.EnvironmentService
import com.netflix.asgard.HealthcheckService
import com.netflix.asgard.ServerService
import com.netflix.discovery.EurekaClientConfig
import spock.lang.Specification

/**
 * Tests for EurekaClientHolder.
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class EurekaClientHolderSpec extends Specification {

    ConfigService configService = Mock(ConfigService)
    EnvironmentService environmentService = Mock(EnvironmentService)
    ServerService serverService = Mock(ServerService)
    HealthcheckService healthcheckService = new HealthcheckService(serverService: serverService)
    EurekaClientHolder eurekaClientHolder = new EurekaClientHolder(configService: configService,
            environmentService: environmentService, healthcheckService: healthcheckService)

    void 'should initialize Eureka Client only if fully configured'() {
        EurekaClientHolder eurekaClientHolder = Spy(EurekaClientHolder)
        eurekaClientHolder.configService = configService
        eurekaClientHolder.healthcheckService = healthcheckService
        eurekaClientHolder.createEurekaClientConfig() >> null
        eurekaClientHolder.createEurekaInstanceConfig() >> null

        when:
        eurekaClientHolder.afterPropertiesSet()

        then:
        configService.eurekaDefaultRegistrationUrl >> defaultUrl
        configService.eurekaZoneListsByRegion >> zonesByRegion
        configService.eurekaUrlTemplateForZoneRegionEnv >> template
        count * eurekaClientHolder.initDiscoveryManager() >> { }

        where:
        count | defaultUrl             | zonesByRegion                 | template
        1     | 'http://eurekadefault' | ['us-west-1': ['us-west-1a']] | 'http://${zone}.eureka.example.com/eureka/v2'
        0     | ''                     | ['us-west-1': ['us-west-1a']] | 'http://${zone}.eureka.example.com/eureka/v2'
        0     | null                   | ['us-west-1': ['us-west-1a']] | 'http://${zone}.eureka.example.com/eureka/v2'
        0     | 'http://eurekadefault' | [:]                           | 'http://${zone}.eureka.example.com/eureka/v2'
        0     | 'http://eurekadefault' | null                          | 'http://${zone}.eureka.example.com/eureka/v2'
        0     | 'http://eurekadefault' | ['us-west-1': ['us-west-1a']] | ''
        0     | 'http://eurekadefault' | ['us-west-1': ['us-west-1a']] | null
    }

    void 'should create a EurekaClientConfig'() {

        Map<String, ArrayList<String>> zoneListsByRegion = [
                'us-east-1': ['us-east-1a', 'us-east-1c', 'us-east-1d', 'us-east-1e'],
                'us-west-1': ['us-west-1a', 'us-west-1b', 'us-west-1c'],
                'us-west-2': ['us-west-2a', 'us-west-2b', 'us-west-2c'],
                'eu-west-1': ['eu-west-1a', 'eu-west-1b', 'us-west-1c'],
        ]

        when:
        EurekaClientConfig eurekaClientConfig = eurekaClientHolder.createEurekaClientConfig()

        then:
        1 * configService.accountName >> 'test'
        1 * environmentService.region >> 'us-west-1'
        1 * configService.eurekaDefaultRegistrationUrl >> 'http://example.com'
        1 * configService.eurekaZoneListsByRegion >> zoneListsByRegion
        1 * configService.eurekaUrlTemplateForZoneRegionEnv >> 'http://${zone}.${region}.${env}.example.com'
        0 * _
        eurekaClientConfig == new AsgardEurekaClientConfig(
                env: 'test',
                region: 'us-west-1',
                eurekaDefaultRegistrationUrl: 'http://example.com',
                eurekaZoneListsByRegion: zoneListsByRegion,
                eurekaUrlTemplate: 'http://${zone}.${region}.${env}.example.com')
    }

    void 'should create a EurekaInstanceConfig outside the cloud'() {

        when:
        EurekaInstanceConfig eurekaInstanceConfig = eurekaClientHolder.createEurekaInstanceConfig()

        then:
        1 * environmentService.instanceId >> null
        0 * _
        eurekaInstanceConfig instanceof AsgardDataCenterEurekaInstanceConfig
        eurekaInstanceConfig.appname == 'asgard'
    }

    void 'should change instance status from STARTING to UP when health check starts to pass'() {

        InstanceInfo instanceInfo = Mock(InstanceInfo)
        ApplicationInfoManager applicationInfoManager = Mock(ApplicationInfoManager)
        ApplicationInfoManager.metaClass.static.getInstance = { applicationInfoManager }

        when: 'health check occurs before callback has been registered'
        healthcheckService.checkHealthAndInvokeCallbacks()

        then: 'health gets checked but no callbacks are registered so no callbacks execute'
        1 * serverService.shouldCacheLoadingBlockUserRequests() >> true
        healthcheckService.callbackNamesToCallbacks == [:]
        0 * _

        when: 'health check callback gets registered and the health check runs and callbacks get invoked'
        eurekaClientHolder.registerHealthcheckCallback()
        healthcheckService.checkHealthAndInvokeCallbacks()

        then: 'callback is registered but if health check fails then callback does nothing'
        healthcheckService.callbackNamesToCallbacks == ['EurekaStatusUpdate': eurekaClientHolder.healthcheckCallback]
        1 * serverService.shouldCacheLoadingBlockUserRequests() >> true
        !healthcheckService.readyForTraffic
        0 * _

        when: 'health check runs again but this time it passes and Eureka status is STARTING'
        healthcheckService.checkHealthAndInvokeCallbacks()

        then: 'Eureka status changes to UP'
        1 * serverService.shouldCacheLoadingBlockUserRequests() >> false
        healthcheckService.readyForTraffic
        1 * applicationInfoManager.info >> instanceInfo
        1 * instanceInfo.status >> 'STARTING'
        1 * applicationInfoManager.setInstanceStatus(InstanceStatus.UP)
        0 * _
    }
}
