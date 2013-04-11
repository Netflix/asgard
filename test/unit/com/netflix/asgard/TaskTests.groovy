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

class TaskTests extends GroovyTestCase {

    void testGetDurationString() {
        Task task = new Task(startTime: new Date(1290061831444), updateTime: new Date(1290066308555), status: "completed")
        assert "1h 14m 37s" == task.durationString

        task = new Task(startTime: new Date(1290066320888), updateTime: new Date(1290066324475), status: "completed")
        assert "3s" == task.durationString

        task = new Task(startTime: new Date(1290066324444), updateTime: new Date(1290239124555), status: "completed")
        assert "2d" == task.durationString

        task = new Task(startTime: new Date(1290329325444), updateTime: new Date(1290329325444), status: "completed")
        assert "0s" == task.durationString

        task = new Task(startTime: new Date(1290329325444), updateTime: new Date(1290329325555), status: "completed")
        assert "0s" == task.durationString

        task = new Task(startTime: new Date(1290239124333), updateTime: new Date(1290239315222), status: "completed")
        assert "3m 10s" == task.durationString

        task = new Task(startTime: new Date(1290239315666), updateTime: new Date(1290242925333), status: "completed")
        assert "1h 9s" == task.durationString

        task = new Task(startTime: new Date(1290239315666), updateTime: new Date(1290329325555), status: "completed")
        assert "1d 1h 9s" == task.durationString
    }

    void testGetSummary() {
        assert new Task(name: 'Create ASG helloworld-v001', env: 'prod',
                status: 'completed', userContext: new UserContext(region: Region.US_WEST_2, username: 'hsimpson',
                clientHostName: 'laptop-hsimpson')).summary ==
                'Asgard task completed in prod us-west-2 by hsimpson: Create ASG helloworld-v001'

        assert new Task(name: 'Create ASG helloworld-v001', env: 'prod',
                status: 'completed', userContext: new UserContext(region: Region.US_WEST_2, username: null,
                clientHostName: 'laptop-hsimpson')).summary ==
                'Asgard task completed in prod us-west-2 by laptop-hsimpson: Create ASG helloworld-v001'
    }
}
