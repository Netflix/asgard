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
package com.netflix.asgard

import spock.lang.Specification

class EurekaAddressCollectorServiceUnitSpec extends Specification {

    EurekaAddressCollectorService eurekaAddressCollectorService
    RestClientService restClientService
    ConfigService configService

    void setup() {
        eurekaAddressCollectorService = new EurekaAddressCollectorService()
        restClientService = Mock(RestClientService)
        configService = Mock(ConfigService)
        eurekaAddressCollectorService.restClientService = restClientService
        eurekaAddressCollectorService.configService = configService
    }

    def 'should get some ip addresses'() {

        DnsService dnsService = Mock(DnsService)
        eurekaAddressCollectorService.dnsService = dnsService
        Integer callCounter = 0
        String hostName = 'eu-west-1.eurekatest.company.com'

        when:
        Collection<String> addresses = eurekaAddressCollectorService.lookUpEurekaAddresses(Region.EU_WEST_1)

        then:
        1 * configService.getRegionalDiscoveryServer(_) >> { hostName }
        1 * configService.getMaxConsecutiveDnsLookupsWithoutNewResult() >> { 10 }

        19 * dnsService.getCanonicalHostNamesForDnsName(hostName) >> {
            // Return single, different IP addresses at different times, like the real Eureka DNS entry does.
            callCounter++
            if (callCounter <= 5) {
                return ['1.1.1.1']
            } else if (callCounter <= 7) {
                return ['2.2.2.2']
            } else if (callCounter <= 9) {
                return ['3.3.3.3']
            }
            return ['1.1.1.1'] // At beginning and end of cycle
        }
        ['1.1.1.1', '2.2.2.2', '3.3.3.3'] == addresses.sort()
    }

    def 'should choose healthy nodes if available'() {

        List<String> eurekaAddresses = ['1.1.1.1', '2.2.2.2', '3.3.3.3', '4.4.4.4']

        when:
        Collection<String> bestEurekaAddresses = eurekaAddressCollectorService.chooseBestEurekaNodes(eurekaAddresses)

        then:
        4 * configService.getEurekaPort() >> { 80 }
        4 * configService.getEurekaUrlContext() >> { 'eureka' }
        4 * restClientService.getRepeatedResponseCode(_) >> { String url ->
            Map<String, Integer> urlsToResponseCodes = [
                    'http://1.1.1.1:80/eureka/healthcheck': 200,
                    'http://3.3.3.3:80/eureka/healthcheck': 503,
                    'http://4.4.4.4:80/eureka/healthcheck': 200,
            ]
            urlsToResponseCodes[url]
        }
        ['1.1.1.1', '4.4.4.4'] == bestEurekaAddresses.sort()
    }

    def 'should choose responsive unhealthy nodes if available when none are healthy'() {

        List<String> eurekaAddresses = ['1.1.1.1', '2.2.2.2', '3.3.3.3', '4.4.4.4']

        when:
        Collection<String> bestEurekaAddresses = eurekaAddressCollectorService.chooseBestEurekaNodes(eurekaAddresses)

        then:
        4 * configService.getEurekaPort() >> { 80 }
        4 * configService.getEurekaUrlContext() >> { 'eureka' }
        4 * restClientService.getRepeatedResponseCode(_) >> { String url ->
            Map<String, Integer> urlsToResponseCodes = [
                    'http://1.1.1.1:80/eureka/healthcheck': 503,
                    'http://3.3.3.3:80/eureka/healthcheck': 503,
                    'http://4.4.4.4:80/eureka/healthcheck': 404,
            ]
            urlsToResponseCodes[url]
        }
        ['1.1.1.1', '3.3.3.3', '4.4.4.4'] == bestEurekaAddresses.sort()
    }

    def 'should choose all nodes if none are responsive'() {

        List<String> eurekaAddresses = ['1.1.1.1', '2.2.2.2', '3.3.3.3', '4.4.4.4']

        when:
        Collection<String> bestEurekaAddresses = eurekaAddressCollectorService.chooseBestEurekaNodes(eurekaAddresses)

        then:
        4 * configService.getEurekaPort() >> { 80 }
        4 * configService.getEurekaUrlContext() >> { 'eureka' }
        4 * restClientService.getRepeatedResponseCode(_) >> { null }
        ['1.1.1.1', '2.2.2.2', '3.3.3.3', '4.4.4.4'] == bestEurekaAddresses.sort()
    }

    def 'should always choose a healthy node from a set containing healthy and sick nodes'() {

        when:
        List<String> allAddresses = ['3.3.3.3', '5.5.5.5', '2.2.2.2', '4.4.4.4', '1.1.1.1']
        List<String> tenBestNodes = (1..10).collect { eurekaAddressCollectorService.chooseBestEurekaNode(allAddresses) }

        then:
        configService.getEurekaUrlContext() >> { 'eureka' }
        configService.getEurekaPort() >> { '8081' }
        restClientService.getRepeatedResponseCode(_) >> { String url ->
            Map<String, Integer> urlsToResponseCodes = [
                    'http://2.2.2.2:8081/eureka/healthcheck': 200,
                    'http://3.3.3.3:8081/eureka/healthcheck': 503,
                    'http://4.4.4.4:8081/eureka/healthcheck': 200
            ]
            urlsToResponseCodes[url]
        }
        restClientService.checkOkayResponseCode(_) >> { Integer code -> code == 200 }
        10 == tenBestNodes.findAll { it in ['2.2.2.2', '4.4.4.4'] }.size()
    }

    def 'should build base URL for Eureka'() {

        when:
        String baseUrl = eurekaAddressCollectorService.buildBaseUrl('3.4.5.6')

        then:
        1 * configService.getEurekaUrlContext() >> { 'eureka' }
        1 * configService.getEurekaPort() >> { '8081' }
        'http://3.4.5.6:8081/eureka' == baseUrl
    }
}
