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

import com.amazonaws.services.ec2.model.GroupIdentifier
import com.amazonaws.services.ec2.model.IpPermission
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.ec2.model.UserIdGroupPair
import com.netflix.asgard.mock.Mocks
import grails.test.MockUtils
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
@TestFor(SecurityController)
class SecurityControllerSpec extends Specification {

    ApplicationService applicationService
    AwsAutoScalingService awsAutoScalingService
    AwsEc2Service awsEc2Service
    AwsLoadBalancerService awsLoadBalancerService

    void setup() {
        new MonkeyPatcherService(grailsApplication: new DefaultGrailsApplication()).createDynamicMethods()
        TestUtils.setUpMockRequest()
        MockUtils.prepareForConstraintsTests(SecurityCreateCommand)
        awsEc2Service = Mock(AwsEc2Service) { isSecurityGroupEditable(_) >> true }
        controller.awsEc2Service = awsEc2Service
        applicationService = Mock(ApplicationService) {
            getRegisteredApplication(_, _) >> new AppRegistration(name: 'helloworld')
        }
        controller.applicationService = applicationService
        controller.configService = Mock(ConfigService) { getAwsAccountNames() >> ['179000000000': 'test'] }
        awsAutoScalingService = Mock(AwsAutoScalingService)
        controller.awsAutoScalingService = awsAutoScalingService
        awsLoadBalancerService = Mock(AwsLoadBalancerService)
        controller.awsLoadBalancerService = awsLoadBalancerService
    }

    def 'show should display details for name'() {
        controller.params.name = 'helloworld'

        when:
        def attrs = controller.show()

        then:
        'helloworld' == attrs['app'].name
        'helloworld' == attrs['group'].groupName
        'test' == attrs['accountNames']['179000000000']
        null != attrs['editable']
        1 * awsEc2Service.getSecurityGroup(_, 'helloworld') >> new SecurityGroup(groupName: 'helloworld')
        1 * awsEc2Service.getInstancesWithSecurityGroup(_, _) >> []
        1 * awsAutoScalingService.getLaunchConfigurationsForSecurityGroup(_, _) >> []
        1 * awsLoadBalancerService.getLoadBalancersWithSecurityGroup(_, _) >> []
    }

    def 'show should show details for id and populate and sort missing security group names in permissions'() {

        SecurityGroup securityGroup = new SecurityGroup(groupName: 'helloworld',
                groupId: 'sg-1337', ipPermissions: [
                new IpPermission(userIdGroupPairs: ['sg-1111', 'sg-3333'].collect { new UserIdGroupPair(groupId: it) }),
                new IpPermission(userIdGroupPairs: ['sg-2222', 'sg-4444'].collect { new UserIdGroupPair(groupId: it) }),
        ])
        List<GroupIdentifier> groupIdentifierObjects1 = [
                new GroupIdentifier(groupId: 'sg-1111', groupName: 'carol'),
                new GroupIdentifier(groupId: 'sg-3333', groupName: 'barry'),
        ]
        List<GroupIdentifier> groupIdentifierObjects2 = [
                new GroupIdentifier(groupId: 'sg-2222', groupName: 'donna'),
                new GroupIdentifier(groupId: 'sg-4444', groupName: 'alice'),
        ]

        controller.params.name = 'sg-1337'

        when:
        def attrs = controller.show()

        then: 'basic results should be returned'
        attrs['app'].name == 'helloworld'
        attrs['group'].groupName == 'helloworld'
        attrs['group'].groupId == 'sg-1337'
        attrs['accountNames']['179000000000'] == 'test'
        attrs['editable'] != null

        and: 'ip permissions with group IDs but no group names should get names added and then sorted by name'
        attrs['group'].ipPermissions == [
                new IpPermission(userIdGroupPairs: [
                        new UserIdGroupPair(groupId: 'sg-4444', groupName: 'alice'),
                        new UserIdGroupPair(groupId: 'sg-2222', groupName: 'donna')]),
                new IpPermission(userIdGroupPairs: [
                        new UserIdGroupPair(groupId: 'sg-3333', groupName: 'barry'),
                        new UserIdGroupPair(groupId: 'sg-1111', groupName: 'carol')]),
        ]

        1 * awsEc2Service.getSecurityGroup(_, 'sg-1337') >> securityGroup
        1 * awsEc2Service.getSecurityGroupNameIdPairsByNamesOrIds(_, ['sg-1111', 'sg-3333']) >> groupIdentifierObjects1
        1 * awsEc2Service.getSecurityGroupNameIdPairsByNamesOrIds(_, ['sg-2222', 'sg-4444']) >> groupIdentifierObjects2
        1 * awsEc2Service.getInstancesWithSecurityGroup(_, _) >> []
        1 * awsAutoScalingService.getLaunchConfigurationsForSecurityGroup(_, _) >> []
        1 * awsLoadBalancerService.getLoadBalancersWithSecurityGroup(_, _) >> []
    }

    def 'show should not find missing security group'() {
        def p = controller.params
        p.name = 'doesntexist'

        when: controller.show()

        then:
        '/error/missing' == view
        "Security Group 'doesntexist' not found in us-east-1 test" == controller.flash.message
        1 * awsEc2Service.getSecurityGroup(_, 'doesntexist') >> null
        0 * _._
    }

    def 'save should fail without app'() {
        request.method = 'POST'
        def p = controller.params
        p.wrongParam = 'helloworld'
        def cmd = new SecurityCreateCommand()
        cmd.applicationService = Mocks.applicationService()

        when: cmd.validate()
        then: cmd.hasErrors()

        when: controller.save(cmd)

        then:
        '/security/create?wrongParam=helloworld' == response.redirectUrl
        0 * _
    }

    def 'save should create security group'() {
        request.method = 'POST'
        def p = controller.params
        p.appName = 'helloworld'
        p.detail = 'indiana'
        p.description = 'Only accessible by Indiana Jones'
        SecurityCreateCommand cmd = new SecurityCreateCommand(appName: p.appName, detail: p.detail)
        cmd.applicationService = Mocks.applicationService()

        when: cmd.validate()
        then: !cmd.hasErrors()

        when:
        controller.save(cmd)

        then:
        '/security/show/sg-123' == response.redirectUrl
        controller.flash.message == "Security Group 'helloworld-indiana' has been created."
        1 * awsEc2Service.getSecurityGroup(_, 'helloworld-indiana')
        1 * awsEc2Service.createSecurityGroup(_, 'helloworld-indiana', 'Only accessible by Indiana Jones', null) >>
                new SecurityGroup(groupName: 'helloworld-indiana', groupId: 'sg-123')
        0 * _
    }
}
