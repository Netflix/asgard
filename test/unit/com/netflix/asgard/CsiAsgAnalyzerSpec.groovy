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

import com.netflix.asgard.model.CsiScheduledAnalysisFactory
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
class CsiAsgAnalyzerSpec extends Specification {

    RestClientService mockRestClientService = Mock(RestClientService)
    ConfigService mockConfigService = Mock(ConfigService)
    ApplicationService mockApplicationService = Mock(ApplicationService)
    CsiScheduledAnalysisFactory mockCsiScheduledAnalysisFactory = Mock(CsiScheduledAnalysisFactory)
    CsiAsgAnalyzer csiAsgAnalyzer = new CsiAsgAnalyzer(restClientService: mockRestClientService,
            configService: mockConfigService, applicationService: mockApplicationService,
            csiScheduledAnalysisFactory: mockCsiScheduledAnalysisFactory)

    void 'should start analysis'() {
        when:
        csiAsgAnalyzer.startAnalysis('fakeblock', 'gmaharis@faceblock.com')

        then:
        1 * mockConfigService.getAsgAnalyzerBaseUrl() >> 'http://csi.netflix.com'
        1 * mockRestClientService.postAsNameValuePairs(
                'http://csi.netflix.com/jr/csi/json_canaries/canary-launcher?cache=false', [
                type: 'asgard',
                cluster: 'fakeblock',
                redblack: 'true',
                username: 'gmaharis',
                schedule: 'true'
        ]) >> new RestResponse(200, '{ "csi": "There is always a clue." }')
        1 * mockCsiScheduledAnalysisFactory.fromJson('{ "csi": "There is always a clue." }')
        0 * _
    }

    void 'should error on start if CSI is unavailable'() {
        when:
        csiAsgAnalyzer.startAnalysis('fakeblock', 'gmaharis@faceblock.com')

        then:
        1 * mockConfigService.getAsgAnalyzerBaseUrl() >> 'http://csi.netflix.com'
        1 * mockRestClientService.postAsNameValuePairs(
                'http://csi.netflix.com/jr/csi/json_canaries/canary-launcher?cache=false', [
                type: 'asgard',
                cluster: 'fakeblock',
                redblack: 'true',
                username: 'gmaharis',
                schedule: 'true'
        ]) >> new RestResponse(404, '{ "csi": "No victim can ever say we didn\'t try." }')
        0 * _

        and:
        ServiceUnavailableException error = thrown()
        error.message == 'Failed to start CSI analysis. Got response 404 - \
{ "csi": "No victim can ever say we didn\'t try." } from http://csi.netflix.com/jr/csi/json_canaries/\
canary-launcher?cache=false.'
    }

    void 'should error propagate error'() {
        when:
        csiAsgAnalyzer.startAnalysis('fakeblock', 'gmaharis@faceblock.com')

        then:
        1 * mockConfigService.getAsgAnalyzerBaseUrl() >> 'http://csi.netflix.com'
        1 * mockRestClientService.postAsNameValuePairs(
                'http://csi.netflix.com/jr/csi/json_canaries/canary-launcher?cache=false', [
                type: 'asgard',
                cluster: 'fakeblock',
                redblack: 'true',
                username: 'gmaharis',
                schedule: 'true'
        ]) >> {
            throw new IllegalStateException('Uh oh!')
        }
        0 * _

        and:
        IllegalStateException error = thrown()
        error.message == 'Uh oh!'
    }

    void 'should stop analysis'() {
        when:
        csiAsgAnalyzer.stopAnalysis('fakeblock_analysis')

        then:
        1 * mockConfigService.getAsgAnalyzerBaseUrl() >> 'http://csi.netflix.com'
        1 * mockRestClientService.getJsonAsText('http://csi.netflix.com/jr/csi/json__csi/scheduler?\
action=state&name=fakeblock_analysis&state=paused') >> 'okay'
        0 * _
    }

    void 'should error on stop if CSI is unavailable'() {

        when:
        csiAsgAnalyzer.stopAnalysis('fakeblock_analysis')

        then:
        1 * mockConfigService.getAsgAnalyzerBaseUrl() >> 'http://csi.netflix.com'
        1 * mockRestClientService.getJsonAsText('http://csi.netflix.com/jr/csi/json__csi/scheduler?\
action=state&name=fakeblock_analysis&state=paused')
        0 * _

        and:
        ServiceUnavailableException error = thrown()
        error.message == "Failed to modify CSI analysis 'fakeblock_analysis'. http://csi.netflix.com/jr/csi/json__csi\
/scheduler?action=state&name=fakeblock_analysis&state=paused could not be contacted."
    }
}
