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

class RegionSpec extends Specification {

    void 'should get the Region for the specified code'() {
        expect:
        Region.US_EAST_1 == Region.withCode('us-east-1')
        Region.US_WEST_1 == Region.withCode('us-west-1')
        'us-west-1' == Region.withCode('us-west-1').code
        'eu-west-1' == Region.withCode('eu-west-1').code
        Region.withCode('us-east') == null
        Region.withCode('blah') == null
        Region.withCode('') == null
        Region.withCode(null) == null
        Region.withCode('  us-east-1  ') == null
    }

    void 'should get the Region for the specified code used by ec2 pricing json files'() {
        expect:
        Region.US_EAST_1 == Region.withPricingJsonCode('us-east')
        Region.US_WEST_1 == Region.withPricingJsonCode('us-west')
        Region.EU_WEST_1 == Region.withPricingJsonCode('eu-ireland')
        Region.AP_NORTHEAST_1 == Region.withPricingJsonCode('apac-tokyo')
        Region.AP_SOUTHEAST_1 == Region.withPricingJsonCode('apac-sin')
        'us-west-1' == Region.withPricingJsonCode('us-west').code
        'eu-west-1' == Region.withPricingJsonCode('eu-ireland').code
        Region.withPricingJsonCode('us-east-1') == null
        Region.withPricingJsonCode('blah') == null
        Region.withPricingJsonCode('') == null
        Region.withPricingJsonCode(null) == null
        Region.withPricingJsonCode('  us-east  ') == null
    }
}
