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

import com.amazonaws.AmazonServiceException
import com.amazonaws.AmazonServiceException.ErrorType
import com.amazonaws.services.route53.model.AliasTarget
import com.amazonaws.services.route53.model.ChangeInfo
import com.amazonaws.services.route53.model.HostedZone
import com.amazonaws.services.route53.model.InvalidInputException
import com.amazonaws.services.route53.model.ResourceRecord
import com.amazonaws.services.route53.model.ResourceRecordSet
import grails.test.mixin.TestFor
import org.apache.http.HttpStatus
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
@TestFor(HostedZoneController)
class HostedZoneControllerSpec extends Specification {

    void setup() {
        TestUtils.setUpMockRequest()
        controller.awsRoute53Service = Mock(AwsRoute53Service)
    }

    def 'should list all hosted zones'() {

        List<HostedZone> hostedZones = [new HostedZone(id: 'a'), new HostedZone(id: 'b')]

        when:
        Map attrs = controller.list()

        then:
        attrs.hostedZones == hostedZones
        1 * controller.awsRoute53Service.getHostedZones() >> hostedZones
        0 * _
    }

    def 'should show details of a hosted zone and its resource record sets sorted by name'() {

        HostedZone hostedZone = new HostedZone(id: 'ZATANNA')
        List<ResourceRecordSet> recordSets = ['c', 'a', 'b'].collect { new ResourceRecordSet(name: "${it}.com") }
        controller.params.id = 'ZATANNA'

        when:
        Map attrs = controller.show()

        then:
        attrs.hostedZone == hostedZone
        attrs.resourceRecordSets*.name == ['a.com', 'b.com', 'c.com']
        1 * controller.awsRoute53Service.getHostedZone(_, 'ZATANNA') >> hostedZone
        1 * controller.awsRoute53Service.getResourceRecordSets(_, 'ZATANNA') >> recordSets
        0 * _
    }

    def 'attempt to show a hosted zone should render a not found page if id has no result'() {

        when:
        controller.params.id = 'nosuchluck'
        controller.show()

        then:
        view == '/error/missing'
        response.status == HttpStatus.SC_NOT_FOUND
        controller.flash.message == "Hosted Zone 'nosuchluck' not found in us-east-1 test"
    }

    def 'should create a new hosted zone'() {

        HostedZoneSaveCommand cmd = new HostedZoneSaveCommand(name: 'test.example.com', comment: 'Cuz why not?')
        HostedZone hostedZone = new HostedZone(id: 'ZATANNA', name: 'test.example.com')

        when:
        controller.save(cmd)

        then:
        controller.flash.message == "Hosted Zone 'ZATANNA' with name 'test.example.com' has been created."
        response.redirectUrl == '/hostedZone/show/ZATANNA'
        1 * controller.awsRoute53Service.createHostedZone(_, 'test.example.com', 'Cuz why not?') >> hostedZone
        0 * _
    }

    def 'should handle failed attempt to create a new hosted zone'() {

        HostedZoneSaveCommand cmd = new HostedZoneSaveCommand()
        cmd.name = controller.params.name = 'test.example.com'

        when:
        controller.save(cmd)

        then:
        1 * controller.awsRoute53Service.createHostedZone(_, 'test.example.com', null) >> {
            AmazonServiceException e = new AmazonServiceException('Sorry, pal.')
            e.errorCode = 'DontLikeYou'
            e.errorType = ErrorType.Service
            e.requestId = 'BOOGIEBOOGIE'
            e.serviceName = 'Route53'
            e.statusCode = 503
            throw e
        }
        controller.flash.message == 'Status Code: 503, AWS Service: Route53, AWS Request ID: BOOGIEBOOGIE, ' +
                'AWS Error Code: DontLikeYou, AWS Error Message: Sorry, pal.'
        response.redirectUrl == '/hostedZone/create?name=test.example.com'
        0 * _
    }

    def 'should delete a hosted zone if it exists'() {

        controller.params.id = 'ZATANNA'

        when:
        controller.delete()

        then:
        response.redirectUrl == '/hostedZone/result'
        1 * controller.awsRoute53Service.deleteHostedZone(_, 'ZATANNA')
        1 * controller.awsRoute53Service.getHostedZone(_, 'ZATANNA') >> new HostedZone(name: '.test.example.com')
        0 * _
    }

    def 'attempt to delete non-existent hosted zone should fail gracefully'() {

        controller.params.id = 'ZATANNA'

        when:
        controller.delete()

        then:
        !response.redirectUrl
        response.status == 404
        1 * controller.awsRoute53Service.getHostedZone(_, 'ZATANNA')
        0 * _
        noExceptionThrown()
    }

    def 'should add a resource record set to a hosted zone'() {

        ResourceRecordSetCommand cmd = new ResourceRecordSetCommand(hostedZoneId: 'ZATANNA',
                resourceRecordSetName: 'magic.example.com.', type: 'CNAME', setIdentifier: 'magic repo us-west-2 A',
                weight: 50, resourceRecordSetRegion: 'us-west-2', ttl: 300, resourceRecords: '''\
                        ns91.example.com
                        ns92.example.com
                        '''.stripIndent(), aliasTarget: 'brucewayne.elb.amazonaws.net',
                comment: 'Enchanted DNS entry')
        ResourceRecordSet recordSet = new ResourceRecordSet(name: 'magic.example.com.', type: 'CNAME',
                setIdentifier: 'magic repo us-west-2 A', weight: 50, region: 'us-west-2', tTL: 300,
                resourceRecords: ['ns91.example.com', 'ns92.example.com'].collect { new ResourceRecord(it) },
                aliasTarget: new AliasTarget('ZATANNA', 'brucewayne.elb.amazonaws.net'))
        String msg = 'DNS CREATE change submitted. ChangeInfo: {Id: ETATIVELREH,Status: PENDING,Comment: Good}'

        when:
        controller.addResourceRecordSet(cmd)

        then:
        1 * controller.awsRoute53Service.createResourceRecordSet(_, 'ZATANNA', recordSet, 'Enchanted DNS entry') >>
                new ChangeInfo(id: 'ETATIVELREH', status: 'PENDING', comment: 'Good')
        0 * _
        response.redirectUrl == '/hostedZone/show/ZATANNA'
        controller.flash.message == msg
    }

    def 'should handle failed attempt to create a new resource record set'() {

        ResourceRecordSetCommand cmd = new ResourceRecordSetCommand(hostedZoneId: 'ZATANNA',
                resourceRecordSetName: 'plain.example.com.')
        controller.params.hostedZoneId = 'ZATANNA'
        controller.params.resourceRecordSetName = 'plain.example.com.'
        ResourceRecordSet recordSet = new ResourceRecordSet(name: 'plain.example.com.')

        when:
        controller.addResourceRecordSet(cmd)

        then:
        1 * controller.awsRoute53Service.createResourceRecordSet(_, 'ZATANNA', recordSet, null) >> {
            InvalidInputException e = new InvalidInputException('Sorry, pal.')
            e.errorCode = 'DontLikeYou'
            e.errorType = ErrorType.Service
            e.requestId = 'BOOGIEBOOGIE'
            e.serviceName = 'Route53'
            e.statusCode = 503
            throw e
        }
        controller.flash.message == 'Could not add resource record set: ' +
                'com.amazonaws.services.route53.model.InvalidInputException: ' +
                'Status Code: 503, AWS Service: Route53, AWS Request ID: BOOGIEBOOGIE, ' +
                'AWS Error Code: DontLikeYou, AWS Error Message: Sorry, pal.'
        response.redirectUrl ==
                '/hostedZone/prepareResourceRecordSet?hostedZoneId=ZATANNA&resourceRecordSetName=plain.example.com.'
        0 * _
    }

    def 'should delete a resource record set'() {

        ResourceRecordSetCommand cmd = new ResourceRecordSetCommand(hostedZoneId: 'ZATANNA',
                resourceRecordSetName: 'magic.example.com.', type: 'CNAME', setIdentifier: 'magic repo us-west-2 A',
                weight: 50, resourceRecordSetRegion: 'us-west-2', ttl: 300, resourceRecords: '''\
                        ns91.example.com
                        ns92.example.com
                        '''.stripIndent()
        )
        ResourceRecordSet recordSet = new ResourceRecordSet(name: 'magic.example.com.', type: 'CNAME',
                setIdentifier: 'magic repo us-west-2 A', weight: 50, region: 'us-west-2', tTL: 300,
                resourceRecords: ['ns91.example.com', 'ns92.example.com'].collect { new ResourceRecord(it) }
        )
        String msg = 'DNS DELETE change submitted. ChangeInfo: {Id: EKOMSDNASRORRIM,Status: PENDING,Comment: Good}'

        when:
        controller.removeResourceRecordSet(cmd)

        then:
        response.redirectUrl == '/hostedZone/show/ZATANNA'
        controller.flash.message == msg
        1 * controller.awsRoute53Service.deleteResourceRecordSet(_, 'ZATANNA', recordSet, null) >>
                new ChangeInfo(id: 'EKOMSDNASRORRIM', status: 'PENDING', comment: 'Good')
        0 * _
    }

    def 'should handle failure to delete a resource record set'() {
        ResourceRecordSetCommand cmd = new ResourceRecordSetCommand(hostedZoneId: 'ZATANNA',
                resourceRecordSetName: 'magic.example.com.', type: 'CNAME', setIdentifier: 'magic repo us-west-2 A',
                weight: 50, resourceRecordSetRegion: 'us-west-2', ttl: 300, resourceRecords: '''\
                        ns91.example.com
                        ns92.example.com
                        '''.stripIndent()
        )
        ResourceRecordSet recordSet = new ResourceRecordSet(name: 'magic.example.com.', type: 'CNAME',
                setIdentifier: 'magic repo us-west-2 A', weight: 50, region: 'us-west-2', tTL: 300,
                resourceRecords: ['ns91.example.com', 'ns92.example.com'].collect { new ResourceRecord(it) }
        )

        when:
        controller.removeResourceRecordSet(cmd)

        then:
        1 * controller.awsRoute53Service.deleteResourceRecordSet(_, 'ZATANNA', recordSet, null) >> {
            InvalidInputException e = new InvalidInputException('Sorry, pal.')
            e.errorCode = 'DontLikeYou'
            e.errorType = ErrorType.Service
            e.requestId = 'BOOGIEBOOGIE'
            e.serviceName = 'Route53'
            e.statusCode = 503
            throw e
        }
        controller.flash.message == 'Could not delete resource record set: ' +
                'com.amazonaws.services.route53.model.InvalidInputException: ' +
                'Status Code: 503, AWS Service: Route53, AWS Request ID: BOOGIEBOOGIE, ' +
                'AWS Error Code: DontLikeYou, AWS Error Message: Sorry, pal.'
        response.redirectUrl == '/hostedZone/show/ZATANNA'
        0 * _
        noExceptionThrown()
    }
}
