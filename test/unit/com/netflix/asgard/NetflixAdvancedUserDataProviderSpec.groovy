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

import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.Tag
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.LaunchContext
import com.netflix.asgard.plugin.UserDataProvider
import javax.xml.bind.DatatypeConverter
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings("GroovyAssignabilityCheck")
class NetflixAdvancedUserDataProviderSpec extends Specification {

    ConfigService configService
    ApplicationService applicationService
    NetflixAdvancedUserDataProvider netflixAdvancedUserDataProvider
    UserContext userContext
    PluginService pluginService
    LaunchContext launchContext

    void setup() {
        new MonkeyPatcherService().createDynamicMethods()
        userContext = UserContext.auto(Region.US_WEST_2)
        configService = Mock(ConfigService) {
            getUserDataVarPrefix() >> 'NETFLIX_'
            getAccountName() >> 'test'
        }
        applicationService = Mock(ApplicationService) {
            getMonitorBucket(_, _, _) >> { it[1] }
        }
        pluginService = Mock(PluginService) {
            getUserDataProvider() >> new UserDataProvider() {
                @Override
                String buildUserDataForVariables(UserContext userContext, String appName, String autoScalingGroupName,
                                                 String launchConfigName) {
                    String userData = "No soup for you. " +
                            "region=${userContext.region ?: ''} app=${appName ?: ''} asg=${autoScalingGroupName ?: ''}"
                    DatatypeConverter.printBase64Binary(userData.bytes)
                }
            }
        }
        netflixAdvancedUserDataProvider = new NetflixAdvancedUserDataProvider(configService: configService,
                applicationService: applicationService, pluginService: pluginService)
        launchContext = new LaunchContext(userContext: userContext)
    }

    final static String helloStandardUserData = """\
            export NETFLIX_ENVIRONMENT=test
            export NETFLIX_MONITOR_BUCKET=hello
            export NETFLIX_APP=hello
            export NETFLIX_STACK=dev
            export NETFLIX_CLUSTER=hello-dev
            export NETFLIX_AUTO_SCALE_GROUP=hello-dev-v001
            export NETFLIX_LAUNCH_CONFIG=hello-dev-v001-1234567
            export EC2_REGION=us-west-2
            """.stripIndent()
    final static String helloCustomUserData = "No soup for you. region=us-west-2 app=hello asg=hello-dev-v001"

    def 'should choose user data format based on nflx-base version in AMI description'() {

        String description = "blah blah blah, ancestor_version=nflx-base-${baseVersion}-12345-h24"
        launchContext.image = new Image(description: description)
        launchContext.application = new AppRegistration(name: 'hello')
        launchContext.autoScalingGroup = new AutoScalingGroupBeanOptions(autoScalingGroupName: 'hello-dev-v001')
        launchContext.launchConfiguration = new LaunchConfigurationBeanOptions(
                launchConfigurationName: 'hello-dev-v001-1234567')

        expect:
        userData == decode(netflixAdvancedUserDataProvider.buildUserData(launchContext))

        where:
        baseVersion | userData
        1           | helloCustomUserData
        1.0         | helloCustomUserData
        1.3         | helloCustomUserData
        2.0         | helloStandardUserData
        3           | helloStandardUserData
        4.1         | helloStandardUserData
        10.0        | helloStandardUserData
        10.1        | helloStandardUserData
        10          | helloStandardUserData
        111.1       | helloStandardUserData
    }

    def 'should build user data with legacy format if AMI description does not match the standard pattern'() {

        launchContext.image = new Image(description: 'blah blah blah')
        launchContext.application = new AppRegistration(name: 'hello')
        launchContext.autoScalingGroup = new AutoScalingGroupBeanOptions(autoScalingGroupName: 'hello-dev-v001')
        launchContext.launchConfiguration = new LaunchConfigurationBeanOptions(
                launchConfigurationName: 'hello-dev-v001-1234567')
        String expected = "No soup for you. region=us-west-2 app=hello asg=hello-dev-v001"

        expect:
        expected == decode(netflixAdvancedUserDataProvider.buildUserData(launchContext))
    }

    def 'should build user data for image launch when there is no ASG and no launch config'() {

        launchContext.image = new Image(description: 'blah blah blah')
        String expected = "No soup for you. region=us-west-2 app= asg="

        expect:
        expected == decode(netflixAdvancedUserDataProvider.buildUserData(launchContext))
    }

    @Unroll('app env var should be #appEnvVar if app is #app, asg is #asg, package is #packageName')
    def 'app name should come from application or ASG or appversion image tag, as available, in that order'() {
        Tag tag = new Tag(key: 'appversion', value: "${packageName}-1.4.0-1140443.h420/build-huxtable/420")
        String description = 'blah blah blah, ancestor_version=nflx-base-2.0-12345-h24'
        launchContext.image = new Image(description: description, tags: [tag])
        launchContext.application = app
        launchContext.autoScalingGroup = asg
        launchContext.launchConfiguration = new LaunchConfigurationBeanOptions(
                launchConfigurationName: 'robot-123456')

        when:
        String userData = decode(netflixAdvancedUserDataProvider.buildUserData(launchContext))

        then:
        userData == """\
                export NETFLIX_ENVIRONMENT=test
                export NETFLIX_MONITOR_BUCKET=${appEnvVar}
                export NETFLIX_APP=${appEnvVar}
                export NETFLIX_STACK=
                export NETFLIX_CLUSTER=${asg?.autoScalingGroupName ?: ''}
                export NETFLIX_AUTO_SCALE_GROUP=${asg?.autoScalingGroupName ?: ''}
                export NETFLIX_LAUNCH_CONFIG=robot-123456
                export EC2_REGION=us-west-2
                """.stripIndent()

        where:
        appEnvVar  | app                                  | asg                   | packageName
        'voltron'  | new AppRegistration(name: 'voltron') | asg('blazing--sword') | 'lionhead'
        'blazing'  | null                                 | asg('blazing--sword') | 'lionhead'
        'lionhead' | null                                 | null                  | 'lionhead'
    }

    def 'launching an image without the name of a application should not send nulls to user data provider'() {

        UserDataProvider userDataProvider = Mock(UserDataProvider)
        pluginService = Mock(PluginService) { getUserDataProvider() >> userDataProvider }
        netflixAdvancedUserDataProvider.pluginService = pluginService
        launchContext.image = new Image()

        when:
        netflixAdvancedUserDataProvider.buildUserData(launchContext)

        then: "no null string values sent to plugin"
        1 * userDataProvider.buildUserDataForVariables(userContext, '', '', '')
    }

    private AutoScalingGroupBeanOptions asg(String name) {
        new AutoScalingGroupBeanOptions(autoScalingGroupName: name)
    }

    private String decode(String encoded) {
        new String(DatatypeConverter.parseBase64Binary(encoded))
    }
}
