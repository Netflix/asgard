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
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import com.netflix.asgard.model.LaunchContext

/**
 * Dynamically creates certain fields of a launch template (LaunchConfiguration/LaunchSpecification) based on the
 * context where instances will be launched.
 */
class LaunchTemplateService {

    static transactional = false

    def applicationService
    def awsEc2Service
    def configService
    def grailsApplication
    def pluginService
    Caches caches

    /**
     * There are Security Groups that should always be included and they are added here.
     *
     * @param securityGroups Security Group IDs or names before the defaults are added (VPC needs IDs rather than names)
     * @param isVPC indicates if security groups are in VPC
     * @param region the default Security Group IDs will be looked up by name for this region (required for VPC)
     * @return new Collection of Security Group names or IDs without duplicates
     */
    Collection<String> includeDefaultSecurityGroups(Collection<String> securityGroups, boolean isVPC = false,
            Region region = null) {
        List<String> defaultSecurityGroupNames =
            isVPC ? configService.defaultVpcSecurityGroupNames : configService.defaultSecurityGroups
        List<String> defaultSecurityGroups = defaultSecurityGroupNames
        // Use IDs rather than names if VPC or ids were specified
        if (isVPC || (securityGroups && securityGroups.iterator().next().startsWith('sg-')) ) {
            defaultSecurityGroups = defaultSecurityGroupNames.collect {
                caches.allSecurityGroups.by(region).get(it)?.groupId
            }.findAll { it }
        }
        (securityGroups + defaultSecurityGroups) as Set
    }

    /**
     * Builds the user data string for a single instance launch of a specified image.
     *
     * @param userContext who, where, why
     * @param image the image of which to launch an instance
     * @return the user data string that should be applied to the LaunchSpecification of an image launch request
     */
    String buildUserDataForImage(UserContext userContext, Image image) {
        Check.notNull(image, Image)
        LaunchContext launchContext = new LaunchContext(userContext: userContext, image: image)
        pluginService.advancedUserDataProvider.buildUserData(launchContext)
    }

    /**
     * Builds the user data string for an intended auto scaling group and launch configuration.
     *
     * @param userContext who, where, why
     * @param autoScalingGroup most of the fields of the intended AutoScalingGroup, including autoScalingGroupName
     * @param launchConfiguration most of the fields of the intended LaunchConfiguration, including imageId
     * @return the user data string that should be applied to the LaunchConfiguration of an auto scaling group
     */
    String buildUserData(UserContext userContext, AutoScalingGroupBeanOptions autoScalingGroup,
            LaunchConfigurationBeanOptions launchConfiguration) {

        String imageId = launchConfiguration.imageId
        Image image = awsEc2Service.getImage(userContext, imageId)
        String appName = Relationships.appNameFromGroupName(autoScalingGroup.autoScalingGroupName)
        AppRegistration app = applicationService.getRegisteredApplication(userContext, appName)

        // Wrap all the inputs in a single class so we can add more inputs later without changing the plugin interface.
        LaunchContext launchContext = new LaunchContext(userContext, image, app, autoScalingGroup, launchConfiguration)
        pluginService.advancedUserDataProvider.buildUserData(launchContext)
    }

}
