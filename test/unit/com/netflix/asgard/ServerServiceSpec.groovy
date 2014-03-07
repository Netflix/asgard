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

import spock.lang.Specification
import spock.lang.Unroll

class ServerServiceSpec extends Specification {

    InitService initService = Mock(InitService)
    ServerService serverService = new ServerService(initService: initService)

    @Unroll("it's #blocked that traffic gets blocked iff caches are filled is #filled")
    void 'should indicate that user requests should be blocked only if caches are empty'() {

        when:
        Boolean shouldBlock = serverService.shouldCacheLoadingBlockUserRequests()

        then:
        initService.cachesFilled() >> filled
        shouldBlock == blocked

        where:
        filled | blocked
        true   | false
        false  | true
    }
}
