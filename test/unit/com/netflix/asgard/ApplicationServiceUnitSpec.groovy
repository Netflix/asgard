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
import com.netflix.asgard.model.MonitorBucketType
import spock.lang.Specification
import spock.lang.Unroll

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
        def appsForLoadBalancer = applicationService.getRegisteredApplicationsForLoadBalancer(UserContext.auto())

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
        def allApps = applicationService.getRegisteredApplications(UserContext.auto())

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
                UserContext.auto(), 'helloworld', null,
                'Web Application', 'Say hello', 'jsmith', 'jsmith@example.com', MonitorBucketType.application, true)

        then:
        1 * applicationService.simpleDbClient.putAttributes(_)
        notThrown(NullPointerException)
    }

    @Unroll('monitor bucket should be "#monitorBucket" if monitor bucket type is #type')
    def 'monitor bucket value should depend on monitor bucket type'() {

        AppRegistration app = new AppRegistration(name: 'hello', monitorBucketType: MonitorBucketType.byName(type))
        applicationService.getRegisteredApplication(_, _) >> app

        expect:
        monitorBucket == applicationService.getMonitorBucket(UserContext.auto(), 'hello', 'hello-there')

        where:
        type          | monitorBucket
        'none'        | ''
        'application' | 'hello'
        'cluster'     | 'hello-there'
    }
}
