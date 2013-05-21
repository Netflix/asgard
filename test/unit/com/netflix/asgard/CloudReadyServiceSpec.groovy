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

class CloudReadyServiceSpec extends Specification {

    CloudReadyService cloudReadyService = new CloudReadyService()
    ConfigService mockConfigService = Mock(ConfigService)
    RestClientService mockRestClientService = Mock(RestClientService)

    String applicationSettingsUrl = 'http://cloudready.com/chaosmonkey/app/helloworld?format=json'
    String clusterSettingsUrl = 'http://cloudready.com/chaosmonkey/cluster/helloworld?format=json&region=us-east-1'

    void setup() {
        cloudReadyService.with {
            configService = mockConfigService
            restClientService = mockRestClientService
        }
        mockConfigService.getCloudReadyUrl() >> 'http://cloudready.com'
        mockConfigService.getChaosMonkeyRegions() >> [Region.US_EAST_1, Region.EU_WEST_1]
    }

    void 'should construct Chaos Monkey edit link'() {
        when:
        String link = cloudReadyService.constructChaosMonkeyEditLink(Region.EU_WEST_1, 'helloworld')

        then:
        link == 'http://cloudready.com/chaosmonkey/edit/helloworld?region=eu-west-1'
    }

    void 'should determine Chaos Monkey status for enabled application'() {
        when:
        String chaosMonkeyStatus = cloudReadyService.chaosMonkeyStatusForApplication('helloworld')

        then:
        1 * mockRestClientService.getAsJson(applicationSettingsUrl) >> [
            enabled: 'true',
            optLevel: 'application'
        ]
        0 * mockRestClientService._
        chaosMonkeyStatus == 'enabled'
    }

    void 'should determine Chaos Monkey status for disabled application'() {
        when:
        String chaosMonkeyStatus = cloudReadyService.chaosMonkeyStatusForApplication('helloworld')

        then:
        1 * mockRestClientService.getAsJson(applicationSettingsUrl) >> [
            enabled: 'false',
            optLevel: 'cluster'
        ]
        0 * mockRestClientService._
        chaosMonkeyStatus == 'disabled'
    }

    void 'should not determine Chaos Monkey status for application when Cloudready is unavailable'() {
        when:
        String chaosMonkeyStatus = cloudReadyService.chaosMonkeyStatusForApplication('helloworld')

        then:
        1 * mockRestClientService.getAsJson(applicationSettingsUrl)
        0 * mockRestClientService._
        chaosMonkeyStatus == 'unknown'
    }

    void 'should enable Chaos Monkey for application'() {
        when:
        cloudReadyService.enableChaosMonkeyForApplication('helloworld')

        then:
        1 * mockRestClientService.getAsJson('http://cloudready.com/chaosmonkey/enableapp/helloworld?format=json')
        0 * mockRestClientService._
    }

    void 'should disable Chaos Monkey for application'() {
        when:
        cloudReadyService.disableChaosMonkeyForApplication('helloworld')

        then:
        1 * mockRestClientService.getAsJson('http://cloudready.com/chaosmonkey/disableapp/helloworld?format=json')
        0 * mockRestClientService._
    }

    void 'should determine Chaos Monkey status for enabled cluster'() {
        when:
        String chaosMonkeyStatus = cloudReadyService.chaosMonkeyStatusForCluster(Region.US_EAST_1, 'helloworld')

        then:
        1 * mockRestClientService.getAsJson(clusterSettingsUrl) >> [
                enabled: 'true',
                optLevel: 'application'
        ]
        0 * mockRestClientService._
        chaosMonkeyStatus == 'enabled'
    }

    void 'should determine Chaos Monkey status for disabled cluster'() {
        when:
        String chaosMonkeyStatus = cloudReadyService.chaosMonkeyStatusForCluster(Region.US_EAST_1, 'helloworld')

        then:
        1 * mockRestClientService.getAsJson(clusterSettingsUrl) >> [
                enabled: 'false',
                optLevel: 'cluster'
        ]
        0 * mockRestClientService._
        chaosMonkeyStatus == 'disabled'
    }

    void 'should not determine Chaos Monkey status for cluster when Cloudready is unavailable'() {
        when:
        String chaosMonkeyStatus = cloudReadyService.chaosMonkeyStatusForCluster(Region.US_EAST_1, 'helloworld')

        then:
        1 * mockRestClientService.getAsJson(clusterSettingsUrl)
        0 * mockRestClientService._
        chaosMonkeyStatus == 'unknown'
    }

    void 'should enable Chaos Monkey for cluster'() {
        String expectedUrl = 'http://cloudready.com/chaosmonkey/enablecluster/helloworld?format=json&region=us-east-1'

        when:
        cloudReadyService.enableChaosMonkeyForCluster(Region.US_EAST_1, 'helloworld')

        then:
        1 * mockRestClientService.getAsJson(expectedUrl)
        0 * mockRestClientService._
    }

    void 'should disable Chaos Monkey for cluster'() {
        String expectUrl = 'http://cloudready.com/chaosmonkey/disablecluster/helloworld?format=json&region=us-east-1'

        when:
        cloudReadyService.disableChaosMonkeyForCluster(Region.US_EAST_1, 'helloworld')

        then:
        1 * mockRestClientService.getAsJson(expectUrl)
        0 * mockRestClientService._
    }

    void 'should retrieve all application names with specified opt level'() {
        String expectUrl = 'http://cloudready.com/chaosmonkey/listApps?format=json&optLevel=cluster'

        when:
        Set<String> appsWithClusterOptLevel = cloudReadyService.applicationsWithOptLevel('cluster')

        then:
        1 * mockRestClientService.getAsJson(expectUrl) >> [ 'applications': [
                [name: 'helloworld1'],
                [name: 'helloworld2']
        ]]
        0 * mockRestClientService._
        appsWithClusterOptLevel == ['helloworld1', 'helloworld2'] as Set
    }

    void 'should blow up if cloudready cannot be reached'() {
        String expectUrl = 'http://cloudready.com/chaosmonkey/listApps?format=json&optLevel=cluster'

        when:
        Set<String> appsWithClusterOptLevel = cloudReadyService.applicationsWithOptLevel('cluster')

        then:
        ServiceUnavailableException e = thrown(ServiceUnavailableException)
        e.message == 'Cloudready could not be contacted.'
        1 * mockRestClientService.getAsJson(expectUrl)
        0 * mockRestClientService._
        appsWithClusterOptLevel == null
    }

    void 'Chaos Monkey is active if URL is configured'() {
        expect: cloudReadyService.isChaosMonkeyActive()
    }

    void 'Chaos Monkey is not active if no URL is configured'() {
        ConfigService mockConfigService = Mock(ConfigService)
        cloudReadyService.configService = mockConfigService

        expect: !cloudReadyService.isChaosMonkeyActive()
    }

    void 'Chaos Monkey is active for region if URL and region are configured'() {
        expect: cloudReadyService.isChaosMonkeyActive(Region.US_EAST_1)
    }

    void 'Chaos Monkey is not active for region if no URL is configured'() {
        ConfigService mockConfigService = Mock(ConfigService)
        cloudReadyService.configService = mockConfigService

        expect: !cloudReadyService.isChaosMonkeyActive(Region.US_EAST_1)
    }

    void 'Chaos Monkey is not active if region is not configured'() {
        expect: !cloudReadyService.isChaosMonkeyActive(Region.US_WEST_1)
    }

    void 'Chaos Monkey is not active if no region is configured'() {
        ConfigService mockConfigService = Mock(ConfigService)
        cloudReadyService.configService = mockConfigService
        mockConfigService.getCloudReadyUrl() >> 'http://cloudready.com'

        expect: !cloudReadyService.isChaosMonkeyActive(Region.US_EAST_1)
    }

}
