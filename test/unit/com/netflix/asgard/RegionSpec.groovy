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

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

class RegionSpec extends Specification {

    void 'should get the Region for the specified code or enum name'() {
        expect:
        Region.withCode(code) == region

        where:
        code            | region
        'us-east-1'     | Region.US_EAST_1
        'US_EAST_1'     | Region.US_EAST_1
        'us-west-1'     | Region.US_WEST_1
        'US_WEST_1'     | Region.US_WEST_1
        'us-east'       | null
        'blah'          | null
        ''              | null
        null            | null
        '  us-east-1  ' | null
        '  US_EAST_1  ' | null
    }

    void 'should get the Region for the specified code used by ec2 pricing json files'() {
        expect:
        Region.withPricingJsonCode(pricingJsonCode) == region

        where:
        pricingJsonCode | region
        'us-east'       | Region.US_EAST_1
        'us-west'       | Region.US_WEST_1
        'eu-ireland'    | Region.EU_WEST_1
        'apac-tokyo'    | Region.AP_NORTHEAST_1
        'apac-sin'      | Region.AP_SOUTHEAST_1
        'us-east-1'     | null
        'blah'          | null
        ''              | null
        null            | null
        '  us-east  '   | null
    }

    void 'Jackson should be able to get a Region from a JSON string using Jackson'() {

        expect:
        new ObjectMapper().readValue(json, Region) == region

        where:
        json          | region
        '"us-west-2"' | Region.US_WEST_2
        '"US_WEST_2"' | Region.US_WEST_2
        '"us-west"'   | null
    }
}
