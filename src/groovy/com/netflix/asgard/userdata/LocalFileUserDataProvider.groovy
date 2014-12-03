/*
 * Copyright 2014 Netflix, Inc.
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
import com.netflix.asgard.Check
import com.netflix.asgard.ConfigService
import com.netflix.asgard.Region
import com.netflix.asgard.Relationships
import com.netflix.asgard.UserContext
import com.netflix.asgard.plugin.UserDataProvider
import com.netflix.frigga.Names
import org.springframework.beans.factory.annotation.Autowired

class LocalFileUserDataProvider implements UserDataProvider {

  private static final INSERTION_MARKER = '\nexport EC2_REGION='

  @Autowired
  ConfigService configService

  @Autowired
  ApplicationService applicationService

  String buildUserDataForVariables(UserContext userContext, String appName,
                                   String autoScalingGroupName, String launchConfigName) {
    String env = configService.getAccountName()
    String rawUserData = assembleUserData(appName, autoScalingGroupName, userContext.region, env)
    replaceUserDataTokens(userContext, rawUserData, appName, env, autoScalingGroupName,
      launchConfigName)
  }

  private String assembleUserData(String app, String groupName, Region region, String env) {
    def udfRoot = System.getProperty('userDataRoot') ?: '/apps/nflx-udf'

    // If there is a matching Ruby file for Windows then it should be the entire User Data string, without any Unix
    String udfRuby = getContents("${udfRoot}/custom.d/${app}-${env}.rb")
    if (udfRuby) {
      return udfRuby
    }

    String cluster = Relationships.clusterFromGroupName(groupName)
    String stack = Relationships.stackNameFromGroupName(groupName)

    // If no Ruby file then get the component Unix shell template files into string lists including custom files for
    // the app and/or auto scaling group.
    // If app and group names are identical, only include their UDF file once.

    // LinkedHashSet ensures correct order and no duplicates when the app, cluster, and groupName are equal.
    Set<String> udfPaths = new LinkedHashSet<String>()
    udfPaths << "${udfRoot}/udf0"
    udfPaths << "${udfRoot}/udf-${env}"
    udfPaths << "${udfRoot}/udf-${region}-${env}"
    udfPaths << "${udfRoot}/udf1"
    udfPaths << "${udfRoot}/custom.d/${app}-${env}"
    udfPaths << "${udfRoot}/custom.d/${app}-${stack}-${env}"
    udfPaths << "${udfRoot}/custom.d/${cluster}-${env}"
    udfPaths << "${udfRoot}/custom.d/${groupName}-${env}"
    udfPaths << "${udfRoot}/custom.region.d/${region}/${app}-${env}"
    udfPaths << "${udfRoot}/custom.region.d/${region}/${app}-${stack}-${env}"
    udfPaths << "${udfRoot}/custom.region.d/${region}/${cluster}-${env}"
    udfPaths << "${udfRoot}/custom.region.d/${region}/${groupName}-${env}"
    udfPaths << "${udfRoot}/udf2"

    // Concat all the Unix shell templates into one string
    udfPaths.collect { String path -> getContents(path) }.join('')
  }

  private String getContents(String filePath) {
    try {
      File file = new File(filePath)
      String contents = file.getText('UTF-8')
      if (contents.length() && !contents.endsWith("\n")) { contents = contents + '\n' }
      return contents
    } catch (IOException ignore) {
      // This normal case happens if the requested file is not found.
      return ''
    }
  }

  private String replaceUserDataTokens(UserContext userContext, String rawUserData, String app, String env,
                                       String autoScalingGroupName, String launchConfigName) {
    Check.notNull(rawUserData, String, 'rawUserData')
    Check.notNull(app, String, 'app')
    Check.notNull(env, String, 'env')
    Check.notNull(autoScalingGroupName, String, 'autoScalingGroupName')
    Check.notNull(launchConfigName, String, 'launchConfigName')

    Names names = Relationships.dissectCompoundName(autoScalingGroupName)
    String stack = names.stack ?: ''
    String cluster = names.cluster ?: ''
    String region = userContext.region.code
    String tier = applicationService.getMonitorBucket(userContext, app, names.cluster)

    // Replace the tokens & return the result
    String result = rawUserData
      .replace('%%app%%', app)
      .replace('%%tier%%', tier)
      .replace('%%env%%', env)
      .replace('%%region%%', region)
      .replace('%%group%%', autoScalingGroupName)
      .replace('%%autogrp%%', autoScalingGroupName)
      .replace('%%cluster%%', cluster)
      .replace('%%stack%%', stack)
      .replace('%%launchconfig%%', launchConfigName)
      .replace('%%app_group%%', applicationService.getRegisteredApplication(userContext, app)?.group ?: '')

    List<String> additionalEnvVars = Relationships.labeledEnvironmentVariables(names,
      configService.userDataVarPrefix)
    if (additionalEnvVars) {
      String insertion = "\n${additionalEnvVars.join('\n')}"
      result = result.replace(INSERTION_MARKER, "\n${insertion}${INSERTION_MARKER}")
    }
    result
  }

}

