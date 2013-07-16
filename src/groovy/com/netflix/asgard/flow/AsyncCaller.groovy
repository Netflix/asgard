/*
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.asgard.flow

import com.amazonaws.services.simpleworkflow.flow.DynamicActivitiesClient
import com.amazonaws.services.simpleworkflow.flow.DynamicActivitiesClientImpl
import com.amazonaws.services.simpleworkflow.flow.annotations.Activities
import com.amazonaws.services.simpleworkflow.flow.annotations.Activity
import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.model.ActivityType
import groovy.transform.Canonical
import java.lang.reflect.Method

/**
 * Use this to schedule SWF activities using the methods on your activities interface.
 */
@Canonical
class AsyncCaller<T> {

    final Class<T> type
    final DynamicActivitiesClientFactory dynamicActivitiesClientFactory

    static <T> T of(Class<T> type, DynamicActivitiesClientFactory dynamicActivitiesClientFactory =
            new DynamicActivitiesClientFactory(DynamicActivitiesClientImpl)) {
        new AsyncCaller(type, dynamicActivitiesClientFactory) as T
    }

    def methodMissing(String name, args) {
        ReflectionHelper reflectionHelper = new ReflectionHelper(type)
        Activities activities = reflectionHelper.findAnnotationOnClass(Activities)
        Method method = reflectionHelper.findMethodForNameAndArgsOrFail(name, args as List)
        Activity activity = reflectionHelper.findAnnotationOnMethod(Activity, method)
        String version = activity?.version() ?: activities?.version()
        ActivityType activityType = new ActivityType(name: activity?.name(), version: version)
        if (!activity) {
            activityType.name = "${type.simpleName}.${method.name}"
        }
        Class returnType = method.returnType
        if (method.returnType == Void.TYPE) {
            returnType = Void
        }
        List<Promise<?>> promises = args.collect { Promise.asPromise(it) }
        DynamicActivitiesClient dynamicActivitiesClient = dynamicActivitiesClientFactory.getInstance()
        return dynamicActivitiesClient.scheduleActivity(activityType, promises as Promise[], null, returnType, null)
    }

    @Canonical
    static class DynamicActivitiesClientFactory {
        Class<? extends DynamicActivitiesClient> dynamicActivitiesClientType

        DynamicActivitiesClient getInstance() {
            dynamicActivitiesClientType.newInstance()
        }
    }

}

