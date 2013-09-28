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

import com.amazonaws.services.simpledb.AmazonSimpleDB
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.MonitorBucketType
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
class ApplicationServiceUnitSpec extends Specification {

    final Collection<String> APP_NAMES = ['aws_stats', 'api', 'cryptex', 'helloworld', 'abcache']

    def allApplications = Mock(CachedMap)
    def caches = new Caches(new MockCachedMapBuilder([
            (EntityType.application): allApplications,
    ]))
    ApplicationService applicationService

    void setup() {
        applicationService = Spy(ApplicationService)
        applicationService.caches = caches
        applicationService.taskService = new TaskService() {
            def runTask(UserContext context, String name, Closure work, Link link = null,
                        Task existingTask = null) {
                work(new Task())
            }
        }
        applicationService.simpleDbClient = Mock(AmazonSimpleDB)
        applicationService.cloudReadyService = Mock(CloudReadyService)
    }

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
        allApplications.list() >> APP_NAMES.collect { new AppRegistration(name: it) }
    }

    def 'application group should be optional when creating an app'() {

        applicationService.getRegisteredApplication(_, _) >> null

        when:
        CreateApplicationResult result = applicationService.createRegisteredApplication(
                UserContext.auto(Region.US_EAST_1), 'helloworld', null,
                'Web Application', 'Say hello', 'jsmith', 'jsmith@example.com', MonitorBucketType.application, true)

        then:
        1 * applicationService.simpleDbClient.putAttributes(_)
        notThrown(NullPointerException)
    }
}
