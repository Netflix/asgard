package com.netflix.asgard.plugin

import com.netflix.asgard.UserContext

interface UserDataProvider {

    String buildUserDataForVariables(UserContext userContext, String appName, String autoScalingGroupName,
            String launchConfigName)
}
