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

import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.SecurityGroup
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.LaunchContext
import com.netflix.asgard.model.MonitorBucketType
import com.netflix.asgard.plugin.AdvancedUserDataProvider
import com.netflix.asgard.plugin.UserDataProvider
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
class LaunchTemplateServiceSpec extends Specification {

    ApplicationService applicationService
    AwsEc2Service awsEc2Service
    ConfigService mockConfigService = Mock(ConfigService)
    CachedMap mockSecurityGroupCache = Mock(CachedMap)
    Caches caches
    LaunchTemplateService launchTemplateService
    PluginService pluginService
    AdvancedUserDataProvider advancedUserDataProvider
    UserDataProvider userDataProvider

    def setup() {
        applicationService = Mock(ApplicationService)
        awsEc2Service = Mock(AwsEc2Service)
        caches = new Caches(new MockCachedMapBuilder([(EntityType.security): mockSecurityGroupCache]))
        advancedUserDataProvider = Mock(DefaultAdvancedUserDataProvider)
        userDataProvider = Mock(UserDataProvider)
        pluginService = Mock(PluginService) {
            getAdvancedUserDataProvider() >> advancedUserDataProvider
        }
        advancedUserDataProvider.getPluginService() >> pluginService
        launchTemplateService = new LaunchTemplateService(configService: mockConfigService, caches: caches,
                pluginService: pluginService, applicationService: applicationService, awsEc2Service: awsEc2Service)
        new MonkeyPatcherService().createDynamicMethods()
    }

    def 'should include default security group names'() {
        when:
        Set<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups([])

        then:
        securityGroups == ['dsg1', 'dsg2'] as Set
        1 * mockConfigService.defaultSecurityGroups >> ['dsg1', 'dsg2']
    }

    def 'should not include duplicate security group names'() {
        when:
        Set<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups(['sg1', 'dsg2'])

        then:
        securityGroups == ['sg1', 'dsg1', 'dsg2'] as Set
        1 * mockConfigService.defaultSecurityGroups >> ['dsg1', 'dsg2']
    }

    def 'should include default VPC security group IDs'() {
        when:
        Set<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups([], true, Region.US_EAST_1)

        then:
        securityGroups == ['sg-101', 'sg-102'] as Set
        1 * mockConfigService.defaultVpcSecurityGroupNames >> ['dsg1', 'dsg2']
        1 * mockSecurityGroupCache.get('dsg1') >> new SecurityGroup(groupId: 'sg-101')
        1 * mockSecurityGroupCache.get('dsg2') >> new SecurityGroup(groupId: 'sg-102')
    }

    def 'should not include duplicate VPC security group IDs'() {
        when:
        Set<String> securityGroups = launchTemplateService.
                includeDefaultSecurityGroups(['sg1', 'sg-102'], true, Region.US_EAST_1)

        then:
        securityGroups == ['sg1', 'sg-101', 'sg-102'] as Set
        1 * mockConfigService.defaultVpcSecurityGroupNames >> ['dsg1', 'dsg2']
        1 * mockSecurityGroupCache.get('dsg1') >> new SecurityGroup(groupId: 'sg-101')
        1 * mockSecurityGroupCache.get('dsg2') >> new SecurityGroup(groupId: 'sg-102')
    }

    def 'should not include default VPC security group IDs not in cache'() {
        when:
        Set<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups(['sg1'], true,
                Region.US_EAST_1)

        then:
        securityGroups == ['sg1', 'sg-101'] as Set
        1 * mockConfigService.defaultVpcSecurityGroupNames >> ['dsg1', 'dsg2']
        1 * mockSecurityGroupCache.get('dsg1') >> new SecurityGroup(groupId: 'sg-101')
        1 * mockSecurityGroupCache.get('dsg2') >> null
    }

    def 'should consistently use id rather than name when ids are specified'() {
        mockConfigService.defaultSecurityGroups >> ['dsg1']
        mockSecurityGroupCache.get('dsg1') >> new SecurityGroup(groupId: 'sg-101')

        when:
        Set<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups(['sg-100'])

        then:
        securityGroups == ['sg-100', 'sg-101'] as Set
    }

    def 'should consistently use name rather than id when names are specified'() {
        mockConfigService.defaultSecurityGroups >> ['dsg1']
        mockSecurityGroupCache.get('dsg1') >> new SecurityGroup(groupId: 'sg-101')

        when:
        Set<String> securityGroups = launchTemplateService.includeDefaultSecurityGroups(['sg1'])

        then:
        securityGroups == ['sg1', 'dsg1'] as Set
    }

    def 'should build user data string for auto scaling group and launch config'() {

        UserContext userContext = UserContext.auto(Region.US_WEST_1)
        AppRegistration application = new AppRegistration(name: 'hello', monitorBucketType: MonitorBucketType.cluster)
        Image image = new Image(imageId: 'ami-deadfeed')
        AutoScalingGroupBeanOptions asg = new AutoScalingGroupBeanOptions(autoScalingGroupName: 'hello-wassup',
                launchConfigurationName: 'hello-wassup-987654321')
        LaunchConfigurationBeanOptions launchConfig = new LaunchConfigurationBeanOptions(
                launchConfigurationName: 'hello-wassup-987654321', imageId: image.imageId)
        launchTemplateService.applicationService = Mock(ApplicationService) {
            getRegisteredApplication(*_) >> application
        }
        launchTemplateService.awsEc2Service = Mock(AwsEc2Service) {
            getImage(*_) >> image
        }

        when:
        String userData = launchTemplateService.buildUserData(userContext, asg, launchConfig)

        then:
        1 * advancedUserDataProvider.buildUserData(
                new LaunchContext(userContext, image, application, asg, launchConfig)) >> 'ta da!'
        userData == 'ta da!'
    }

    def 'should build user data string for image launch alone without an ASG or launch config'() {

        UserContext userContext = UserContext.auto(Region.US_WEST_1)
        Image image = new Image(imageId: 'ami-deadfeed')

        when:
        String userData = launchTemplateService.buildUserDataForImage(userContext, image)

        then:
        1 * advancedUserDataProvider.buildUserData(new LaunchContext(userContext, image, null, null, null)) >> 'ta da!'
        userData == 'ta da!'
    }
}
