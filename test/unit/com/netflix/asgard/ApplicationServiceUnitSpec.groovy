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

import com.netflix.asgard.mock.Mocks
import spock.lang.Specification

class ApplicationServiceUnitSpec extends Specification {

    final Collection<String> APP_NAMES = ['aws_stats', 'api', 'cryptex','helloworld','abcache']

    def allApplications = Mock(CachedMap)
    def caches = new Caches(new MockCachedMapBuilder([
        (EntityType.application): allApplications,
    ]))
    ApplicationService applicationService = new ApplicationService(caches: caches)


    def 'should return correct apps for load balancer'() {
        mockApplications()

        when:
        def appsForLoadBalancer = applicationService.getRegisteredApplicationsForLoadBalancer(Mocks.userContext())

        then:
        4 == appsForLoadBalancer.size()
        'abcache' == appsForLoadBalancer[0].name
        'api' == appsForLoadBalancer[1].name
        'cryptex' == appsForLoadBalancer[2].name
        'helloworld' == appsForLoadBalancer[3].name
    }

    def 'should return correct registered applications'() {
        mockApplications()

        when:
        def allApps = applicationService.getRegisteredApplications(Mocks.userContext())

        then:
        5 == allApps.size()
        'aws_stats' == allApps[2].name
    }

    void mockApplications() {
        allApplications.list() >> APP_NAMES.collect { new AppRegistration(name: it)}
    }
}
