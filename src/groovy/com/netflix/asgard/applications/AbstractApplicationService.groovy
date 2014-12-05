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
package com.netflix.asgard.applications

import com.netflix.asgard.AppRegistration
import com.netflix.asgard.ApplicationService
import com.netflix.asgard.Relationships
import com.netflix.asgard.UserContext
import com.netflix.asgard.collections.GroupedAppRegistrationSet
import com.netflix.asgard.model.MonitorBucketType

abstract class AbstractApplicationService implements ApplicationService {
    @Override
    final List<AppRegistration> getRegisteredApplicationsForLoadBalancer(UserContext userContext) {
        new ArrayList<AppRegistration>(getRegisteredApplications(userContext).findAll {
            Relationships.checkAppNameForLoadBalancer(it.name)
        })
    }

    @Override
    final GroupedAppRegistrationSet getGroupedRegisteredApplications(UserContext ctx) {
        new GroupedAppRegistrationSet(getRegisteredApplications(ctx))
    }

    @Override
    final AppRegistration getRegisteredApplicationForLoadBalancer(UserContext userContext, String name) {
        Relationships.checkAppNameForLoadBalancer(name) ? getRegisteredApplication(userContext, name) : null
    }

    @Override
    final String getEmailFromApp(UserContext userContext, String appName) {
        getRegisteredApplication(userContext, appName)?.email ?: ''
    }

    @Override
    final String getMonitorBucket(UserContext userContext, String appName, String clusterName) {
        MonitorBucketType type = getRegisteredApplication(userContext, appName)?.monitorBucketType
        type == MonitorBucketType.application ? appName : type == MonitorBucketType.cluster ? clusterName : ''
    }
}
