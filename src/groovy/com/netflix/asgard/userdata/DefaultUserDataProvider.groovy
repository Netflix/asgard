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

class DefaultUserDataProvider implements UserDataProvider {

    @Autowired
    ConfigService configService

    @Autowired
    ApplicationService applicationService

    String buildUserDataForVariables(UserContext userContext, String appName, String autoScalingGroupName,
            String launchConfigName) {
        Names names = Relationships.dissectCompoundName(autoScalingGroupName)
        String monitorBucket = applicationService.getMonitorBucket(userContext, appName, names.cluster)
        String appGroup = applicationService.getRegisteredApplication(userContext, appName)?.group
        String result = exportVar(UserDataPropertyKeys.ENVIRONMENT, configService.accountName) +
            exportVar(UserDataPropertyKeys.MONITOR_BUCKET, monitorBucket) +
            exportVar(UserDataPropertyKeys.APP, appName) +
            exportVar(UserDataPropertyKeys.APP_GROUP, appGroup) +
            exportVar(UserDataPropertyKeys.STACK, names.stack) +
            exportVar(UserDataPropertyKeys.CLUSTER, names.cluster) +
            exportVar(UserDataPropertyKeys.AUTO_SCALE_GROUP, autoScalingGroupName) +
            exportVar(UserDataPropertyKeys.LAUNCH_CONFIG, launchConfigName) +
            exportVar(UserDataPropertyKeys.EC2_REGION, userContext.region.code, false)
        List<String> additionalEnvVars = Relationships.labeledEnvironmentVariables(names,
                configService.userDataVarPrefix)
        result += additionalEnvVars ? additionalEnvVars.join('\n') : ''
        DatatypeConverter.printBase64Binary(result.bytes)
    }

    private String exportVar(String name, String val, boolean includePrefix = true) {
        "export ${includePrefix ? configService.userDataVarPrefix : ''}${name}=${val ?: ''}\n"
    }
}
