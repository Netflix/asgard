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
import com.amazonaws.services.ec2.model.Image

class LaunchTemplateService {

    static transactional = false

    def applicationService
    def configService
    def grailsApplication
    def pluginService

    List<String> includeDefaultSecurityGroups(List<String> securityGroupNames) {
        Set<String> uniqueSecurityGroupNames = securityGroupNames as Set
        uniqueSecurityGroupNames.addAll(grailsApplication.config.cloud.defaultSecurityGroups ?: [])
        uniqueSecurityGroupNames as List
    }

    String buildUserDataForImage(UserContext userContext, Image image) {
        String appName = image?.packageName ?: ''
        pluginService.userDataProvider.buildUserDataForVariables(userContext, appName, '', '')
    }

    String buildUserData(UserContext userContext, AutoScalingGroup autoScalingGroup) {
        String appName = Check.notEmpty(autoScalingGroup.appName as String, 'appName')
        String autoScalingGroupName = Check.notEmpty(autoScalingGroup.autoScalingGroupName, 'autoScalingGroupName')
        String launchConfigName = Check.notEmpty(autoScalingGroup.launchConfigurationName, 'launchConfigurationName')
        pluginService.userDataProvider.buildUserDataForVariables(userContext, appName, autoScalingGroupName,
                launchConfigName)
    }

}
