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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.ec2.model.Image
import com.netflix.asgard.model.LaunchContext

class LaunchTemplateService {

    static transactional = false

    def applicationService
    def configService
    def grailsApplication
    def pluginService
    Caches caches

    /**
     * There are Security Groups that should always be included and they are added here.
     *
     * @param securityGroups Security Group IDs or names before the defaults are added (VPC needs IDs rather than names)
     * @param vpcZoneIdentifier VPC Zone Identifier denotes what VPC these securityGroups are in
     * @param region the default Security Group IDs will be looked up by name for this region (required for VPC)
     * @return new Collection of Security Group names or IDs without duplicates
     */
    Collection<String> includeDefaultSecurityGroups(List<String> securityGroups, String vpcZoneIdentifier = null,
            Region region = null) {
        List<String> defaultSecurityGroupNames =
            vpcZoneIdentifier ? configService.defaultVpcSecurityGroupNames : configService.defaultSecurityGroups
        List<String> defaultSecurityGroups = defaultSecurityGroupNames
        // Use IDs rather than names if VPC or ids were specified
        if (vpcZoneIdentifier || (securityGroups && securityGroups[0].startsWith('sg-')) ) {
            defaultSecurityGroups = defaultSecurityGroupNames.collect {
                caches.allSecurityGroups.by(region).get(it)?.groupId
            }.findAll { it }
        }
        (securityGroups + defaultSecurityGroups) as Set
    }

    String buildUserDataForImage(UserContext userContext, Image image) {

        Check.notNull(image, Image)
        LaunchContext launchContext = new LaunchContext(userContext: userContext, image: image)
        pluginService.advancedUserDataProvider.buildUserDataForCloudObjects(launchContext)
    }

    String buildUserData(UserContext userContext, AppRegistration application, Image image,
                         AutoScalingGroup groupTemplate, LaunchConfiguration launchConfigTemplate) {

        Check.notNull(image, Image)
        Check.notEmpty(groupTemplate.appName as String, 'appName')
        Check.notEmpty(groupTemplate.autoScalingGroupName, 'autoScalingGroupName')
        Check.notEmpty(groupTemplate.launchConfigurationName, 'launchConfigurationName')

        // Wrap all the inputs in a single class so we can add more inputs later without changing the plugin interface.
        LaunchContext launchContext = new LaunchContext(userContext, image, application, groupTemplate,
                launchConfigTemplate)
        pluginService.advancedUserDataProvider.buildUserDataForCloudObjects(launchContext)
    }

}
