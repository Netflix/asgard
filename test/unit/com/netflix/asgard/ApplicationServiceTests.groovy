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

import grails.test.GrailsUnitTestCase
import com.netflix.asgard.mock.Mocks

class ApplicationServiceTests extends GrailsUnitTestCase {

    def appService

    void setUp() {
        super.setUp()
        appService = Mocks.applicationService()
    }

    void testGetRegisteredApplicationsForLoadBalancer() {
        def appsForLoadBalancer = appService.getRegisteredApplicationsForLoadBalancer(Mocks.userContext())
        assert 6 == appsForLoadBalancer.size()
        assert 'abcache' == appsForLoadBalancer[0].name
        assert 'api' == appsForLoadBalancer[1].name
        assert 'cryptex' == appsForLoadBalancer[2].name
        assert 'helloworld' == appsForLoadBalancer[3].name

        def allApps = appService.getRegisteredApplications(Mocks.userContext())
        assert 7 == allApps.size()
        assert 'aws_stats' == allApps[2].name
    }

}
