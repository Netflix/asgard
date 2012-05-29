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

class RegionTests extends GroovyTestCase {

    void testWithCode() {
        assert Region.US_EAST_1 == Region.withCode('us-east-1')
        assert Region.US_WEST_1 == Region.withCode('us-west-1')
        assert 'us-west-1' == Region.withCode('us-west-1').code
        assert 'eu-west-1' == Region.withCode('eu-west-1').code
        assertNull Region.withCode('us-east')
        assertNull Region.withCode('blah')
        assertNull Region.withCode('')
        assertNull Region.withCode(null)
        assertNull Region.withCode('  us-east-1  ')
    }

    void testWithPricingJsonCode() {
        assert Region.US_EAST_1 == Region.withPricingJsonCode('us-east')
        assert Region.US_WEST_1 == Region.withPricingJsonCode('us-west')
        assert Region.EU_WEST_1 == Region.withPricingJsonCode('eu-ireland')
        assert Region.AP_NORTHEAST_1 == Region.withPricingJsonCode('apac-tokyo')
        assert Region.AP_SOUTHEAST_1 == Region.withPricingJsonCode('apac-sin')
        assert 'us-west-1' == Region.withPricingJsonCode('us-west').code
        assert 'eu-west-1' == Region.withPricingJsonCode('eu-ireland').code
        assertNull Region.withPricingJsonCode('us-east-1')
        assertNull Region.withPricingJsonCode('blah')
        assertNull Region.withPricingJsonCode('')
        assertNull Region.withPricingJsonCode(null)
        assertNull Region.withPricingJsonCode('  us-east  ')
    }
}
