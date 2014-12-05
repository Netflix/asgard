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

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.simpledb.model.Item
import com.netflix.asgard.applications.SimpleDBApplicationService
import com.netflix.asgard.model.MonitorBucketType
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings("GroovyAssignabilityCheck")
class SimpleDBApplicationServiceUnitSpec extends Specification {

    static final DOMAIN_NAME = 'CLOUD_APPLICATIONS'

    final Collection<String> APP_NAMES = ['aws_stats', 'api', 'cryptex', 'helloworld', 'abcache']

    def allApplications = Mock(CachedMap)
    def caches = new Caches(new MockCachedMapBuilder([
            (EntityType.application): allApplications,
    ]))
    SimpleDBApplicationService applicationService

    void setup() {
        applicationService = Spy(SimpleDBApplicationService)
        applicationService.caches = caches
        applicationService.taskService = new TaskService() {
            def runTask(UserContext context, String name, Closure work, Link link = null,
                        Task existingTask = null) {
                work(new Task())
            }
        }
        applicationService.awsSimpleDbService = Mock(AwsSimpleDbService)
        applicationService.domainName = DOMAIN_NAME
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
        applicationService.createRegisteredApplication(UserContext.auto(), 'helloworld', null,
                'Web Application', 'Say hello', 'jsmith', 'jsmith@example.com',
                MonitorBucketType.application, 'a,b,c')

        then:
        1 * applicationService.awsSimpleDbService.save('CLOUD_APPLICATIONS', 'HELLOWORLD', _)
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

    def 'should retrieve applications'() {
        new MonkeyPatcherService().createDynamicMethods()
        Item item1 = new Item(name: 'Zebra')
        Item item2 = new Item(name: 'aardvark')

        when:
        Collection<AppRegistration> applications = applicationService.retrieveApplications()

        then:
        1 * applicationService.awsSimpleDbService.selectAll(DOMAIN_NAME) >> [item1, item2]
        applications == [AppRegistration.from(item2), AppRegistration.from(item1)]
    }

    def 'should bubble up AWS error'() {
        AmazonServiceException ase = new AmazonServiceException('other exception')

        when:
        applicationService.retrieveApplications()

        then:
        1 * applicationService.awsSimpleDbService.selectAll(DOMAIN_NAME) >> { throw ase }
        AmazonServiceException e = thrown()
        e == ase
    }
}
