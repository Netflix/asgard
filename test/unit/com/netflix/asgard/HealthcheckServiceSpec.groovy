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

import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class HealthcheckServiceSpec extends Specification {

    ServerService serverService = Mock(ServerService)
    HealthcheckService healthcheckService = new HealthcheckService(serverService: serverService)

    void 'should be unhealthy if server is not ready for traffic'() {

        when:
        healthcheckService.checkHealthAndInvokeCallbacks()

        then:
        !healthcheckService.readyForTraffic
        serverService.shouldCacheLoadingBlockUserRequests() >> true
    }

    void 'should be healthy if server is ready for traffic'() {

        when:
        healthcheckService.checkHealthAndInvokeCallbacks()

        then:
        healthcheckService.readyForTraffic
        serverService.shouldCacheLoadingBlockUserRequests() >> false
    }

    void 'should run any registered callbacks when performing health check'() {

        Boolean wasItHealthy = null
        Boolean isItHealthy = null

        Closure callback = { Boolean wasHealthyBefore, Boolean isHealthyNow ->
            wasItHealthy = wasHealthyBefore
            isItHealthy = isHealthyNow
        }

        when:
        healthcheckService.registerCallback('testing', callback)
        healthcheckService.checkHealthAndInvokeCallbacks()

        then:
        healthcheckService.readyForTraffic
        serverService.shouldCacheLoadingBlockUserRequests() >> false
        wasItHealthy == Boolean.FALSE
        isItHealthy == Boolean.TRUE
    }
}
