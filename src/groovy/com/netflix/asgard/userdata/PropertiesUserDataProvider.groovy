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
package com.netflix.asgard.userdata

import com.netflix.asgard.ApplicationService
import com.netflix.asgard.ConfigService
import com.netflix.asgard.Relationships
import com.netflix.asgard.UserContext
import com.netflix.asgard.plugin.UserDataProvider
import com.netflix.frigga.Names
import javax.xml.bind.DatatypeConverter
import org.springframework.beans.factory.annotation.Autowired

/**
 * Providers of user data strings in a format similar to a properties file.
 */
class PropertiesUserDataProvider implements UserDataProvider {

    @Autowired
    ConfigService configService

    @Autowired
    ApplicationService applicationService

    @Override
    String buildUserDataForVariables(UserContext userContext, String appName, String autoScalingGroupName,
                                     String launchConfigName) {

        Map<String, String> props = mapProperties(userContext, appName, autoScalingGroupName, launchConfigName)
        String result = props.collect { k, v -> "${k}=${v}" }.join('\n') + '\n'
        DatatypeConverter.printBase64Binary(result.bytes)
    }

    /**
     * Creates a map of environment keys to values for use in constructing user data strings, based on the specified
     * cloud objects associated with the current deployment.
     *
     * @param userContext who, where, why
     * @param appName the name of the application being deployed
     * @param autoScalingGroupName the name of the ASG which will launch and manage the instances
     * @param launchConfigName the name of the launch configuration for launching the instances
     * @return a map of keys to values for the deployment environment
     */
    Map<String, String> mapProperties(UserContext userContext, String appName, String autoScalingGroupName,
                                      String launchConfigName) {
        Names names = Names.parseName(autoScalingGroupName)
        String monitorBucket = applicationService.getMonitorBucket(userContext, appName, names.cluster)
        String appGroup = applicationService.getRegisteredApplication(userContext, appName)?.group
        [
                (prependNamespace(UserDataPropertyKeys.ENVIRONMENT)): configService.accountName ?: '',
                (prependNamespace(UserDataPropertyKeys.MONITOR_BUCKET)): monitorBucket ?: '',
                (prependNamespace(UserDataPropertyKeys.APP)): appName ?: '',
                (prependNamespace(UserDataPropertyKeys.APP_GROUP)): appGroup ?: '',
                (prependNamespace(UserDataPropertyKeys.STACK)): names.stack ?: '',
                (prependNamespace(UserDataPropertyKeys.CLUSTER)): names.cluster ?: '',
                (prependNamespace(UserDataPropertyKeys.AUTO_SCALE_GROUP)): autoScalingGroupName ?: '',
                (prependNamespace(UserDataPropertyKeys.LAUNCH_CONFIG)): launchConfigName ?: '',
                (UserDataPropertyKeys.EC2_REGION): userContext.region.code ?: '',
        ] + Relationships.labeledEnvVarsMap(names, configService.userDataVarPrefix)
    }

    private String prependNamespace(String key) {
        "${configService.userDataVarPrefix}${key}"
    }
}
