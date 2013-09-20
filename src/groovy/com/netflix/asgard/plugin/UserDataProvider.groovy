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
package com.netflix.asgard.plugin

import com.netflix.asgard.UserContext

/**
 * This is maintained only for backward compatibility with existing overrides of userDataProvider.
 *
 * Limited legacy interface for creating user data based solely on UserContext, ASG name, app name, and launch config
 * name. Implementing this interface as a plugin should be considered deprecated. Instead, implement
 * AdvancedUserDataProvider.
 */
interface UserDataProvider {

    String buildUserDataForVariables(UserContext userContext, String appName, String autoScalingGroupName,
            String launchConfigName)
}
