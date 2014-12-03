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
package com.netflix.asgard.userdata

import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.Tag
import com.netflix.asgard.AppRegistration
import com.netflix.asgard.ApplicationService
import com.netflix.asgard.ConfigService
import com.netflix.asgard.MonkeyPatcherService
import com.netflix.asgard.PluginService
import com.netflix.asgard.Region
import com.netflix.asgard.UserContext
import com.netflix.asgard.applications.SimpleDBApplicationService
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.LaunchContext
import com.netflix.asgard.model.MonitorBucketType
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
            NETFLIX_ENVIRONMENT=test
            NETFLIX_MONITOR_BUCKET=hello
            NETFLIX_APP=hello
            NETFLIX_APP_GROUP=
            NETFLIX_STACK=dev
            NETFLIX_CLUSTER=hello-dev
            NETFLIX_AUTO_SCALE_GROUP=hello-dev-v001
            NETFLIX_LAUNCH_CONFIG=hello-dev-v001-1234567
            EC2_REGION=us-west-2
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

    @Unroll('monitor bucket should be "#monitorBucket" if monitor bucket type is #type')
    def 'monitor bucket should be empty, cluster, or app name as requested'() {

        String description = "blah blah blah, ancestor_version=nflx-base-2.0-12345-h24"
        launchContext.image = new Image(description: description)
        AppRegistration app = new AppRegistration(name: 'hi', monitorBucketType: MonitorBucketType.byName(type),
                group: 'hi_group')
        launchContext.application = app
        launchContext.autoScalingGroup = new AutoScalingGroupBeanOptions(autoScalingGroupName: 'hi-dev-v001')
        launchContext.launchConfiguration = new LaunchConfigurationBeanOptions(
                launchConfigurationName: 'hi-dev-v001-1234567')
        netflixAdvancedUserDataProvider.applicationService = Spy(SimpleDBApplicationService) {
            getRegisteredApplication(_, _) >> app
        }

        when:
        String userData = decode(netflixAdvancedUserDataProvider.buildUserData(launchContext))

        then:
        userData == """\
                NETFLIX_ENVIRONMENT=test
                NETFLIX_MONITOR_BUCKET=${monitorBucket ?: ''}
                NETFLIX_APP=hi
                NETFLIX_APP_GROUP=hi_group
                NETFLIX_STACK=dev
                NETFLIX_CLUSTER=hi-dev
                NETFLIX_AUTO_SCALE_GROUP=hi-dev-v001
                NETFLIX_LAUNCH_CONFIG=hi-dev-v001-1234567
                EC2_REGION=us-west-2
                """.stripIndent()

        where:
        type          | monitorBucket
        'none'        | ''
        'cluster'     | 'hi-dev'
        'application' | 'hi'
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
                NETFLIX_ENVIRONMENT=test
                NETFLIX_MONITOR_BUCKET=${appEnvVar}
                NETFLIX_APP=${appEnvVar}
                NETFLIX_APP_GROUP=
                NETFLIX_STACK=
                NETFLIX_CLUSTER=${asg?.autoScalingGroupName ?: ''}
                NETFLIX_AUTO_SCALE_GROUP=${asg?.autoScalingGroupName ?: ''}
                NETFLIX_LAUNCH_CONFIG=robot-123456
                EC2_REGION=us-west-2
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

    void 'should use properties file format user data only if image and configuration indicate are set up for it'() {

        configService.usePropertyFileUserDataForWindowsImages >> propForWin
        Image image = new Image(platform: platform, description: description)

        expect:
        result == netflixAdvancedUserDataProvider.shouldUsePropertiesUserData(image)

        where:
        result | propForWin | platform  | description
        false  | false      | null      | null
        false  | false      | ''        | ''
        false  | true       | ''        | ''
        false  | false      | ''        | "blah blah blah, ancestor_version=nflx-base-1-12345-h24"
        false  | true       | null      | "blah blah blah, ancestor_version=nflx-base-1-12345-h24"
        false  | false      | null      | "blah blah blah, ancestor_version=nflx-base-1.0-12345-h24"
        false  | false      | null      | "blah blah blah, ancestor_version=nflx-base-1.3-12345-h24"
        true   | false      | null      | "blah blah blah, ancestor_version=nflx-base-2.0-12345-h24"
        true   | true       | null      | "blah blah blah, ancestor_version=nflx-base-2.0-12345-h24"
        true   | false      | null      | "blah blah blah, ancestor_version=nflx-base-3-12345-h24"
        true   | false      | null      | "blah blah blah, ancestor_version=nflx-base-10-12345-h24"
        true   | false      | null      | "blah blah blah, ancestor_version=nflx-base-10.0-12345-h24"
        true   | false      | null      | "blah blah blah, ancestor_version=nflx-base-10.1-12345-h24"
        true   | false      | null      | "blah blah blah, ancestor_version=nflx-base-11-12345-h24"
        false  | false      | null      | "blah blah blah"
        false  | false      | 'windows' | "blah blah blah"
        false  | false      | 'Windows' | "blah blah blah"
        true   | true       | 'windows' | "blah blah blah"
        true   | true       | 'windows' | "blah blah blah"
        true   | true       | 'Windows' | "blah blah blah"
        false  | true       | 'linux'   | "blah blah blah"
        true   | true       | 'windows' | "blah blah blah, ancestor_version=nflx-base-1-12345-h24"
        true   | true       | 'windows' | "blah blah blah, ancestor_version=nflx-base-2-12345-h24"
    }

    private AutoScalingGroupBeanOptions asg(String name) {
        new AutoScalingGroupBeanOptions(autoScalingGroupName: name)
    }

    private String decode(String encoded) {
        new String(DatatypeConverter.parseBase64Binary(encoded))
    }
}
