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
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult
import com.amazonaws.services.ec2.model.SecurityGroup
import com.netflix.asgard.mock.Mocks
import grails.plugin.spock.ControllerSpec
import grails.test.MockUtils

class SecurityControllerSpec extends ControllerSpec {
    AmazonEC2 amazonEC2 = Mock(AmazonEC2)

    void setup() {
        TestUtils.setUpMockRequest()
        MockUtils.prepareForConstraintsTests(SecurityCreateCommand)
        controller.awsEc2Service = Mocks.newAwsEc2Service(amazonEC2)
        controller.applicationService = Mocks.applicationService()
        controller.metaClass.grailsApplication = Mocks.grailsApplication()
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
        1 * amazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupIds: ['helloworld'])) >> {
            throw new AmazonServiceException('Missing Security Group')
        }
        1 * amazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupNames: ['helloworld'])) >>
                new DescribeSecurityGroupsResult(securityGroups: [new SecurityGroup(groupName: 'helloworld')])
        0 * _._
    }

    def 'show should display details for id'() {
        controller.params.name = 'sg-1337'

        when:
        def attrs = controller.show()

        then:
        'helloworld' == attrs['app'].name
        'helloworld' == attrs['group'].groupName
        'test' == attrs['accountNames']['179000000000']
        null != attrs['editable']
        1 * amazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupIds: ['sg-1337'])) >>
                new DescribeSecurityGroupsResult(securityGroups: [new SecurityGroup(groupName: 'helloworld')])
        0 * _._
    }

    def 'show should not find missing security group'() {
        def p = controller.params
        p.name ='doesntexist'

        when: controller.show()

        then:
        '/error/missing' == controller.renderArgs.view
        "Security Group 'doesntexist' not found in us-east-1 test" == controller.flash.message
        1 * amazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupIds: ['doesntexist'])) >> {
            throw new AmazonServiceException('Missing Security Group')
        }
        1 * amazonEC2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupNames: ['doesntexist'])) >> {
            throw new AmazonServiceException('Missing Security Group')
        }
        0 * _._
    }

    def 'save should fail without app'() {
        mockRequest.method = 'POST'
        def p = controller.params
        p.wrongParam = 'helloworld'
        def cmd = new SecurityCreateCommand()
        cmd.applicationService = Mocks.applicationService()

        when: cmd.validate()
        then: cmd.hasErrors()

        when: controller.save(cmd)

        then:
        controller.create == controller.chainArgs.action
        p == controller.chainArgs.params
        cmd == controller.chainArgs.model['cmd']
    }

    void 'save should create security group'() {
        mockRequest.method = 'POST'
        def p = mockParams
        p.appName = 'helloworld'
        p.detail ='indiana'
        p.description = 'Only accessible by Indiana Jones'
        SecurityCreateCommand cmd = new SecurityCreateCommand(appName: p.appName, detail: p.detail)
        cmd.applicationService = Mocks.applicationService()
        amazonEC2.describeSecurityGroups(_) >> {
            throw new AmazonServiceException('Missing Security Group')
        }

        when: cmd.validate()
        then: !cmd.hasErrors()

        when:
        controller.save(cmd)

        then:
        controller.show == controller.redirectArgs.action
        'helloworld-indiana' == controller.redirectArgs.params.id
        controller.flash.message == "Security Group 'helloworld-indiana' has been created."
        1 * amazonEC2.createSecurityGroup(new CreateSecurityGroupRequest(groupName: 'helloworld-indiana',
                description: 'Only accessible by Indiana Jones'))
    }
}