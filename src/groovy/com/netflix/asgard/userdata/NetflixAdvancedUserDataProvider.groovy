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
import com.netflix.asgard.ApplicationService
import com.netflix.asgard.ConfigService
import com.netflix.asgard.PluginService
import com.netflix.asgard.Relationships
import com.netflix.asgard.UserContext
import com.netflix.asgard.model.LaunchContext
import com.netflix.asgard.plugin.AdvancedUserDataProvider
import com.netflix.asgard.plugin.UserDataProvider
import java.util.regex.Matcher
import org.springframework.beans.factory.annotation.Autowired

/**
 * This Netflix-specific plugin is published in the public Asgard code base as an example of how to customize the
 * behavior of an AdvancedUserDataProvider conditionally based on the inputs in the LaunchContext.
 *
 * This user data creation plugin is used by Netflix, and not recommended for use by people outside Netflix.
 *
 * For the newer AMIs created by Aminator, this implementation creates a short, simple user data string consisting only
 * of name value pairs formatted like a properties file. For other AMIs, this plugin delegates to the complex, legacy,
 * closed-source UserDataProvider plugin used at Netflix for deployments of an older Base AMI that has different startup
 * behavior.
 */
class NetflixAdvancedUserDataProvider implements AdvancedUserDataProvider {

    @Autowired
    ApplicationService applicationService

    @Autowired
    ConfigService configService

    @Autowired
    PluginService pluginService

    @Override
    String buildUserData(LaunchContext launchContext) {

        UserContext userContext = launchContext.userContext
        String appNameFromApplication = launchContext.application?.name
        String groupName = launchContext.autoScalingGroup?.autoScalingGroupName ?: ''
        String launchConfigName = launchContext.launchConfiguration?.launchConfigurationName ?: ''
        Image image = launchContext.image
        String appName = appNameFromApplication ?: Relationships.appNameFromGroupName(groupName) ?:
            Relationships.packageFromAppVersion(image.appVersion) ?: ''

        if (shouldUsePropertiesUserData(image)) {
            UserDataProvider simpleProvider = new PropertiesUserDataProvider(configService: configService,
                    applicationService: applicationService)
            return simpleProvider.buildUserDataForVariables(userContext, appName, groupName, launchConfigName)
        }

        // If the AMI lacks a nflx-base version 2 or greater, use the complex legacy user data format.
        pluginService.userDataProvider.buildUserDataForVariables(userContext, appName, groupName, launchConfigName)
    }

    /**
     * Determines whether or not the deployment of the specified image should have user data in properties file format.
     *
     * @return true if Asgard's configuration and the image have the characteristics that indicate the need for a
     *          user data in a properties file format
     */
    boolean shouldUsePropertiesUserData(Image image) {
        if (configService.usePropertyFileUserDataForWindowsImages && image?.platform?.toLowerCase() == 'windows') {
            return true
        }
        // If the AMI's description shows a nflx-base version of 2 or greater, use the simple user data format.
        Matcher matcher = image?.description =~ /.*ancestor_version=nflx-base-([0-9]+)[^0-9].*/
        if (matcher.matches()) {
            Integer majorVersion = matcher.group(1) as Integer
            if (majorVersion >= 2) {
                return true
            }
        }
        false
    }
}
