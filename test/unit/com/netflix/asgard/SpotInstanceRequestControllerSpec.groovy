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
package com.netflix.asgard

import com.amazonaws.services.ec2.model.GroupIdentifier
import com.amazonaws.services.ec2.model.LaunchSpecification
import com.amazonaws.services.ec2.model.SpotInstanceRequest
import grails.test.mixin.TestFor
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
@TestFor(SpotInstanceRequestController)
class SpotInstanceRequestControllerSpec extends Specification {

    AwsEc2Service awsEc2Service = Mock(AwsEc2Service)
    SpotInstanceRequestService spotInstanceRequestService = Mock(SpotInstanceRequestService)

    void setup() {
        controller.awsEc2Service = awsEc2Service
        controller.spotInstanceRequestService = spotInstanceRequestService
    }

    def 'should show info about a spot instance request'() {

        String id = 'sir-1234'
        List<String> securityGroupIdStrings = ['sg-1234', 'vamp']
        List<GroupIdentifier> securityGroupIdObjects = [
                new GroupIdentifier(groupId: 'sg-1234', groupName: 'wolf'),
                new GroupIdentifier(groupId: 'sg-5678', groupName: 'vamp'),
        ]
        SpotInstanceRequest sir = new SpotInstanceRequest(spotInstanceRequestId: id, launchSpecification:
                new LaunchSpecification(securityGroups: securityGroupIdStrings))
        params.id = id

        when:
        Map attrs = controller.show()

        then:
        1 * spotInstanceRequestService.getSpotInstanceRequest(_, id) >> sir
        1 * awsEc2Service.getSecurityGroupNameIdPairsByNamesOrIds(_, securityGroupIdStrings) >> securityGroupIdObjects
        0 * _
        attrs == [spotInstanceRequest: sir, securityGroups: securityGroupIdObjects]
    }
}
