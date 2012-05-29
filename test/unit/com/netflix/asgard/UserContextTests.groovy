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

import com.netflix.asgard.mock.Mocks

class UserContextTests extends GroovyTestCase {

    void testWithRegion() {
        UserContext userContext = Mocks.userContext()
        assert Region.US_EAST_1 == userContext.region
        assert 'localhost' == userContext.clientHostName
        assert 'localhost' == userContext.clientIpAddress
        assert null == userContext.ticket

        UserContext singaporeContext = userContext.withRegion(Region.AP_SOUTHEAST_1)
        assert Region.AP_SOUTHEAST_1 == singaporeContext.region
        assert 'localhost' == singaporeContext.clientHostName
        assert 'localhost' == singaporeContext.clientIpAddress
        assert null == singaporeContext.ticket

        // Original is unchanged
        assert Region.US_EAST_1 == userContext.region
    }
}
