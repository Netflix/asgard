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

import grails.test.MockUtils
import org.joda.time.DateTime
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class HealthcheckServiceSpec extends Specification {

    HealthcheckService healthcheckService
    ConfigService configService = Mock(ConfigService)
    Caches caches = Mock(Caches)
    def initService = Mock(InitService)
    CachedMap asgCachedMap = new CachedMapBuilder(null).of(EntityType.autoScaling).buildCachedMap()
    MultiRegionCachedMap asgMultiRegionCachedMap = Mock(MultiRegionCachedMap)
    String asgCacheName = EntityType.autoScaling.displayName

    def setup() {
        MockUtils.mockLogging(HealthcheckService)
        healthcheckService = new HealthcheckService(
                configService: configService,
                caches: caches,
                initService: initService)
    }

    def 'should be unhealthy if caches empty'() {
        initializeMocks()

        when:
        healthcheckService.checkCaches()

        then:
        !healthcheckService.isHealthy
        healthcheckService.cacheNamesToProblems.size() == 1
        healthcheckService.cacheNamesToProblems[asgCacheName] == 'Cache size is 0 which is below minimum size 1'
    }

    def 'should recover from failure'() {
        initializeMocks()

        when:
        healthcheckService.checkCaches()

        then:
        !healthcheckService.isHealthy
        healthcheckService.cacheNamesToProblems.size() == 1
        healthcheckService.cacheNamesToProblems[asgCacheName] == 'Cache size is 0 which is below minimum size 1'

        when:
        asgCachedMap.map['id'] = new Object()
        healthcheckService.checkCaches()

        then:
        healthcheckService.isHealthy
        healthcheckService.cacheNamesToProblems.size() == 0
    }

    def 'should be unhealthy if cache is stale'() {
        initializeMocks()
        DateTime oneDayAgo = new DateTime().minusDays(1)
        asgCachedMap.active = true
        asgCachedMap.lastActiveTime = oneDayAgo
        asgCachedMap.lastFillTime = oneDayAgo

        when:
        healthcheckService.checkCaches()

        then:
        !healthcheckService.isHealthy
        healthcheckService.cacheNamesToProblems.size() == 1
        healthcheckService.cacheNamesToProblems[asgCacheName].startsWith("Cache is actively used but last " +
            "fill time is ${oneDayAgo} which is before 10 minutes ago")
    }

    def 'should fail on bad cache name'() {
        initService.cachesFilled() >> true
        configService.healthCheckMinimumCounts >> [blah: 1]
        caches.getProperty('blah') >> { throw new MissingPropertyException('') }

        when:
        healthcheckService.checkCaches()

        then:
        !healthcheckService.isHealthy
        healthcheckService.cacheNamesToProblems.size() == 1
        healthcheckService.cacheNamesToProblems['blah'].startsWith('Invalid cache name')
    }

    def 'should fail on error with checking cache'() {
        initService.cachesFilled() >> true
        // leaving this without data so it throws a NPE
        caches.getProperty('allAutoScalingGroups') >> asgMultiRegionCachedMap
        asgMultiRegionCachedMap.by(Region.defaultRegion()) >> { throw new IOException('This error') }
        configService.healthCheckMinimumCounts >> [allAutoScalingGroups: 1]

        when:
        healthcheckService.checkCaches()

        then:
        !healthcheckService.isHealthy
        healthcheckService.cacheNamesToProblems == [allAutoScalingGroups: 'This error']
    }

    def 'should fail on error with config service'() {
        initService.cachesFilled() >> true
        configService.healthCheckMinimumCounts >> { throw new NullPointerException() }

        when:
        healthcheckService.checkCaches()

        then:
        !healthcheckService.isHealthy
        healthcheckService.cacheNamesToProblems.size() == 0
    }

    private initializeMocks() {
        initService.cachesFilled() >> true
        asgMultiRegionCachedMap.by(Region.defaultRegion()) >> asgCachedMap
        caches.getProperty('allAutoScalingGroups') >> asgMultiRegionCachedMap
        configService.healthCheckMinimumCounts >> [allAutoScalingGroups: 1]
    }

}
