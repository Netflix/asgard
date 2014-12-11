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
package com.netflix.asgard

import com.netflix.asgard.collections.GroupedAppRegistrationSet
import com.netflix.asgard.model.MonitorBucketType

interface ApplicationService {
    List<AppRegistration> getRegisteredApplications(UserContext userContext)

    List<AppRegistration> getRegisteredApplicationsForLoadBalancer(UserContext userContext)

    GroupedAppRegistrationSet getGroupedRegisteredApplications(UserContext ctx)

    AppRegistration getRegisteredApplication(UserContext userContext, String nameInput)

    AppRegistration getRegisteredApplication(UserContext userContext, String nameInput, From from)

    AppRegistration getRegisteredApplicationForLoadBalancer(UserContext userContext, String name)

    ApplicationModificationResult createRegisteredApplication(UserContext userContext, String name, String group,
                                                              String type, String description, String owner,
                                                              String email, MonitorBucketType monitorBucketType,
                                                              String tags)

    ApplicationModificationResult updateRegisteredApplication(UserContext userContext, String name, String group,
                                                              String type, String desc, String owner,
                                                              String email, MonitorBucketType bucketType,
                                                              String tags)

    void deleteRegisteredApplication(UserContext userContext, String name)

    /**
     * Get the email address of the relevant app, or empty string if no email address can be found for the specified
     * app name.
     *
     * @param appName the name of the app that has the email address
     * @return the email address associated with the app, or empty string if no email address can be found
     */
    String getEmailFromApp(UserContext userContext, String appName)

    /**
     * Provides a string to use for monitoring bucket, either provided an empty string, cluster name or app name based
     * on the application settings.
     *
     * @param userContext who, where, why
     * @param appName application name to look up, and the value to return if the bucket type is 'application'
     * @param clusterName value to return if the application's monitor bucket type is 'cluster'
     * @return appName or clusterName or empty string, based on the application's monitorBucketType
     */
    String getMonitorBucket(UserContext userContext, String appName, String clusterName)
}

/**
 * Records the results of trying to modify an Application.
 */
class ApplicationModificationResult {
    boolean successful
    String message

    String toString() {
        return message
    }

    Boolean succeeded() {
        return successful
    }
}

