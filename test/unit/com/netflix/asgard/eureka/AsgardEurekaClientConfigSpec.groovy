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
package com.netflix.asgard.eureka

import spock.lang.Specification

/**
 * Tests for AsgardEurekaClientConfig.
 */
class AsgardEurekaClientConfigSpec extends Specification {

    AsgardEurekaClientConfig asgardEurekaClientConfig = new AsgardEurekaClientConfig(
            env: 'test',
            eurekaZoneListsByRegion: [
                    'us-east-1': ['us-east-1a', 'us-east-1c', 'us-east-1d', 'us-east-1e'],
                    'us-west-1': ['us-west-1a', 'us-west-1b', 'us-west-1c'],
                    'us-west-2': ['us-west-2a', 'us-west-2b', 'us-west-2c'],
                    'eu-west-1': ['eu-west-1a', 'eu-west-1b', 'us-west-1c'],
            ],
            eurekaUrlTemplate: 'http://${zone}.${region}.eureka${env}.example.com:9004/eureka/v2/',
    )

    void 'should get availability zones for region based on eurekaZoneListsByRegion'() {

        when:
        List<String> zones = asgardEurekaClientConfig.getAvailabilityZones('us-west-1') as List

        then:
        zones == ['us-west-1a', 'us-west-1b', 'us-west-1c']
    }

    void 'should get injected region'() {
        expect:
        new AsgardEurekaClientConfig(region: 'us-west-1').getRegion() == 'us-west-1'
    }

    void 'should get Eureka server service URLs for a zone'() {

        when:
        asgardEurekaClientConfig.eurekaDefaultRegistrationUrl = defaultUrl
        List<String> result = asgardEurekaClientConfig.getEurekaServerServiceUrls(zone)

        then:
        result == urls

        where:
        zone         | defaultUrl               | urls
        'us-west-1c' | 'http://example.com/v2/' | ['http://us-west-1c.us-west-1.eurekatest.example.com:9004/eureka/v2/']
        'us-west-1a' | 'http://example.com/v2/' | ['http://us-west-1a.us-west-1.eurekatest.example.com:9004/eureka/v2/']
        'sa-east-1a' | 'http://example.com/v2/' | ['http://example.com/v2/']
        'sa-east-1a' | null                     | null
    }
}
