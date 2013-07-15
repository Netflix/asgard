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
import com.amazonaws.services.simpleworkflow.model.ActivityType
import com.netflix.asgard.flow.example.HelloWorldActivities
import spock.lang.Specification

class AsyncCallerSpec extends Specification {

    def 'AsyncCaller should schedule activity for method call'() {
        def mockDynamicActivitiesClient = Mock(DynamicActivitiesClient)
        def mockDynamicActivitiesClientFactory = Mock(AsyncCaller.DynamicActivitiesClientFactory) {
            getInstance() >> mockDynamicActivitiesClient
        }

        def activities = AsyncCaller.of(HelloWorldActivities, mockDynamicActivitiesClientFactory)

        when:
        activities.printHello('hi')

        then:
        1 * mockDynamicActivitiesClient.scheduleActivity(new ActivityType(name: 'HelloWorldActivities.printHello',
                version: '1.0'), { it.collect { it.get() } == ['hi'] }, null, Void, null)
    }
}
