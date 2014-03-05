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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.ec2.model.GroupIdentifier
import com.amazonaws.services.ec2.model.Image
import grails.test.mixin.TestFor
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
@TestFor(LaunchConfigurationController)
class LaunchConfigurationControllerSpec extends Specification {

    AwsLoadBalancerService awsLoadBalancerService = Mock(AwsLoadBalancerService)
    AwsEc2Service awsEc2Service = Mock(AwsEc2Service)
    AwsAutoScalingService awsAutoScalingService = Mock(AwsAutoScalingService)
    ApplicationService applicationService = Mock(ApplicationService)

    void setup() {
        TestUtils.setUpMockRequest()
        request.region = Region.defaultRegion()
        controller.awsEc2Service = awsEc2Service
        controller.awsAutoScalingService = awsAutoScalingService
        controller.applicationService = applicationService
    }

    def 'should show info about a launch configuration'() {

        String name = 'helloworld-1234567890'
        String imageId = 'ami-deadfeed'
        String sgName = 'helloworld'
        LaunchConfiguration launchConfig = new LaunchConfiguration(launchConfigurationName: name, imageId: imageId,
                securityGroups: [sgName])
        AutoScalingGroup asg = new AutoScalingGroup(autoScalingGroupName: 'helloworld', launchConfigurationName: name)
        AppRegistration app = new AppRegistration(name: 'helloworld')
        Image image = new Image(imageId: imageId)
        List<GroupIdentifier> securityGroupIdObjects = [new GroupIdentifier(groupId: 'sg-123', groupName: sgName)]
        params.id = name

        when:
        Map attrs = controller.show()

        then:
        1 * awsAutoScalingService.getLaunchConfiguration(_, name) >> launchConfig
        1 * awsAutoScalingService.getAutoScalingGroupForLaunchConfig(_, name) >> asg
        1 * awsEc2Service.getSecurityGroupNameIdPairsByNamesOrIds(_, [sgName]) >> securityGroupIdObjects
        1 * awsEc2Service.getImage(_, imageId) >> image
        1 * applicationService.getRegisteredApplication(_, 'helloworld') >> app
        0 * _
        attrs == [
                app: app,
                cluster: 'helloworld',
                group: asg,
                image: image,
                lc: launchConfig,
                securityGroups: securityGroupIdObjects,
        ]
    }
}
