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

import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.LaunchContext
import com.netflix.asgard.plugin.UserDataProvider
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
class DefaultAdvancedUserDataProviderSpec extends Specification {

    UserContext userContext = UserContext.auto(Region.US_WEST_2)
    PluginService pluginService
    LaunchContext launchContext
    UserDataProvider userDataProvider

    void setup() {
        userDataProvider = Mock(UserDataProvider)
        pluginService = Mock(PluginService) {
            getUserDataProvider() >> userDataProvider
        }
        launchContext = new LaunchContext(userContext: userContext)
    }

    def 'should delegate to original user data provider implementation registered as a plugin'() {
        DefaultAdvancedUserDataProvider provider = new DefaultAdvancedUserDataProvider(pluginService: pluginService)
        launchContext.autoScalingGroup = new AutoScalingGroupBeanOptions(autoScalingGroupName: 'go-ahead')
        launchContext.launchConfiguration = new LaunchConfigurationBeanOptions(launchConfigurationName: 'go-ahead-123')

        when:
        String userData = provider.buildUserData(launchContext)

        then:
        1 * userDataProvider.buildUserDataForVariables(userContext, 'go', 'go-ahead', 'go-ahead-123') >> { 'well done' }
        userData == 'well done'
    }
}
