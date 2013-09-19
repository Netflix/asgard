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

import com.netflix.asgard.model.LaunchContext
import com.netflix.asgard.plugin.AdvancedUserDataProvider
import org.springframework.beans.factory.annotation.Autowired

/**
 * Default implementation of a system for generating a user data string for use in LaunchConfiguration and
 * LaunchSpecification objects. This can be overwritten by writing a custom user data plugin.
 */
class DefaultAdvancedUserDataProvider implements AdvancedUserDataProvider {

    @Autowired
    PluginService pluginService

    @Override
    String buildUserData(LaunchContext launchContext) {

        // Call the legacy plugin by default, because that is what many users have already overwritten.
        UserContext userContext = launchContext.userContext
        String groupName = launchContext.autoScalingGroup?.autoScalingGroupName
        String launchConfigName = launchContext.launchConfiguration?.launchConfigurationName
        String appName = Relationships.appNameFromGroupName(groupName)
        pluginService.userDataProvider.buildUserDataForVariables(userContext, appName, groupName, launchConfigName)
    }
}
