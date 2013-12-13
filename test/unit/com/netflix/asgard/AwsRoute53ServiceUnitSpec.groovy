/*
 * Copyright 2013 Netflix, Inc.
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

import com.amazonaws.services.route53.AmazonRoute53
import com.amazonaws.services.route53.model.Change
import com.amazonaws.services.route53.model.ChangeAction
import com.amazonaws.services.route53.model.ChangeBatch
import com.amazonaws.services.route53.model.ChangeInfo
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult
import com.amazonaws.services.route53.model.CreateHostedZoneRequest
import com.amazonaws.services.route53.model.CreateHostedZoneResult
import com.amazonaws.services.route53.model.DeleteHostedZoneRequest
import com.amazonaws.services.route53.model.DeleteHostedZoneResult
import com.amazonaws.services.route53.model.GetHostedZoneRequest
import com.amazonaws.services.route53.model.GetHostedZoneResult
import com.amazonaws.services.route53.model.HostedZone
import com.amazonaws.services.route53.model.HostedZoneConfig
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult
import com.amazonaws.services.route53.model.NoSuchHostedZoneException
import com.amazonaws.services.route53.model.ResourceRecordSet
import com.netflix.asgard.retriever.AwsResultsRetriever
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings(["GroovyAssignabilityCheck", "GroovyAccessibility"])
class AwsRoute53ServiceUnitSpec extends Specification {

    UserContext userContext
    AwsRoute53Service awsRoute53Service = new AwsRoute53Service()

    def setup() {
        userContext = UserContext.auto()
        awsRoute53Service = new AwsRoute53Service(
                awsClient: Mock(AmazonRoute53),
                caches: new Caches(new MockCachedMapBuilder([(EntityType.hostedZone): Mock(CachedMap)])),
                taskService: new TaskService() {
                    def runTask(UserContext userContext, String name, Closure work, Link link = null) {
                        work(new Task())
                    }
                }
        )
    }

    def 'should retrieve hosted zones'() {
        awsRoute53Service.@hostedZoneRetriever = Mock(AwsResultsRetriever) {
            retrieve(_, _) >> [new HostedZone(id: 'ZATANNA')]
        }

        when:
        List<HostedZone> hostedZones = awsRoute53Service.retrieveHostedZones()

        then:
        hostedZones == [new HostedZone(id: 'ZATANNA')]
    }

    def 'should get hosted zone by id or name'() {

        HostedZone hostedZone = new HostedZone(id: 'ZATANNA', name: 'test.example.com.')

        when:
        HostedZone result = awsRoute53Service.getHostedZone(userContext, idOrName)

        then:
        1 * awsRoute53Service.awsClient.getHostedZone(new GetHostedZoneRequest(id: 'ZATANNA')) >>
                new GetHostedZoneResult(hostedZone: hostedZone)
        1 * awsRoute53Service.caches.allHostedZones.put('ZATANNA', hostedZone) >> hostedZone
        (0..2) * awsRoute53Service.caches.allHostedZones.list() >> [hostedZone]
        result.id == expectedId

        where:
        idOrName            | expectedId
        'ZATANNA'           | 'ZATANNA'
        'test.example.com.' | 'ZATANNA'
        'test.example.com'  | 'ZATANNA'
    }

    def 'should return null if hosted zone id not found'() {

        when:
        HostedZone result = awsRoute53Service.getHostedZone(userContext, 'ZZZZZZ')

        then:
        result == null
        1 * awsRoute53Service.awsClient.getHostedZone(new GetHostedZoneRequest(id: 'ZZZZZZ')) >>
                { throw new NoSuchHostedZoneException() }
        noExceptionThrown()
        0 * _
    }

    @Unroll('should return #expected if hosted zone id is #id')
    def 'should return null if hosted zone id is null or empty'() {

        when:
        HostedZone hostedZone = awsRoute53Service.getHostedZone(userContext, id)

        then:
        hostedZone == expected
        noExceptionThrown()
        0 * _

        where:
        id   | expected
        null | null
        ''   | null
    }

    def 'should create a hosted zone'() {

        HostedZone hostedZone = new HostedZone(id: 'ZATANNA', name: 'test.example.com.')
        CreateHostedZoneRequest createHostedZoneRequest = new CreateHostedZoneRequest(name: 'test.example.com.',
                callerReference: 'abc', hostedZoneConfig: new HostedZoneConfig(comment: 'unit test'))

        when:
        HostedZone result = awsRoute53Service.createHostedZone(userContext, 'test.example.com.', 'unit test', 'abc')

        then:
        1 * awsRoute53Service.awsClient.createHostedZone(createHostedZoneRequest) >>
                new CreateHostedZoneResult(hostedZone: hostedZone)
        1 * awsRoute53Service.awsClient.getHostedZone(new GetHostedZoneRequest(id: 'ZATANNA')) >>
                new GetHostedZoneResult(hostedZone: hostedZone)
        1 * awsRoute53Service.caches.allHostedZones.put('ZATANNA', hostedZone) >> hostedZone
        0 * _
        result == hostedZone
    }

    def 'should delete a hosted zone'() {

        when:
        ChangeInfo result = awsRoute53Service.deleteHostedZone(userContext, 'ZATANNA')

        then:
        1 * awsRoute53Service.awsClient.deleteHostedZone(new DeleteHostedZoneRequest(id: 'ZATANNA')) >>
                new DeleteHostedZoneResult(changeInfo: new ChangeInfo(id: '1234'))
        1 * awsRoute53Service.caches.allHostedZones.remove('ZATANNA')
        result == new ChangeInfo(id: '1234')
        0 * _
    }

    def 'should get resource record sets'() {

        when:
        List<ResourceRecordSet> resourceRecordSets = awsRoute53Service.getResourceRecordSets(userContext, 'ZATANNA')

        then:
        1 * awsRoute53Service.awsClient.listResourceRecordSets(
                new ListResourceRecordSetsRequest(hostedZoneId: 'ZATANNA')) >>
                new ListResourceRecordSetsResult(resourceRecordSets: [new ResourceRecordSet(name: 'test.example.com.')])
        resourceRecordSets == [new ResourceRecordSet(name: 'test.example.com.')]
    }

    def 'should create a resource record set'() {

        ResourceRecordSet resourceRecordSet = new ResourceRecordSet(name: 'test.example.com.')
        Change change = new Change(action: ChangeAction.CREATE, resourceRecordSet: resourceRecordSet)
        ChangeInfo changeInfo = new ChangeInfo(id: 'blah', status: 'PENDING', comment: 'The comment')

        when:
        ChangeInfo result = awsRoute53Service.createResourceRecordSet(userContext, 'ZATANNA', resourceRecordSet,
                'The comment')

        then:
        result == changeInfo
        1 * awsRoute53Service.awsClient.changeResourceRecordSets(new ChangeResourceRecordSetsRequest(
                hostedZoneId: 'ZATANNA', changeBatch: new ChangeBatch(comment: 'The comment', changes: [change]))) >>
                new ChangeResourceRecordSetsResult(changeInfo: changeInfo)
    }

    def 'should delete a resource record set'() {
        ResourceRecordSet resourceRecordSet = new ResourceRecordSet(name: 'test.example.com.')
        Change change = new Change(action: ChangeAction.DELETE, resourceRecordSet: resourceRecordSet)
        ChangeInfo changeInfo = new ChangeInfo(id: 'blah', status: 'PENDING', comment: 'The comment')

        when:
        ChangeInfo result = awsRoute53Service.deleteResourceRecordSet(userContext, 'ZATANNA', resourceRecordSet,
                'The comment')

        then:
        result == changeInfo
        1 * awsRoute53Service.awsClient.changeResourceRecordSets(new ChangeResourceRecordSetsRequest(
                hostedZoneId: 'ZATANNA', changeBatch: new ChangeBatch(comment: 'The comment', changes: [change]))) >>
                new ChangeResourceRecordSetsResult(changeInfo: changeInfo)
    }
}
