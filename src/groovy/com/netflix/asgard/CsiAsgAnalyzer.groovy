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
import com.netflix.asgard.model.ScheduledAsgAnalysis
import com.netflix.asgard.plugin.AsgAnalyzer
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired

/**
 * An AsgAnalyzer that interacts with the Critical Systems Inspector (CSI).
 */
class CsiAsgAnalyzer implements AsgAnalyzer {

    @Autowired
    ApplicationService applicationService

    @Autowired
    ConfigService configService

    @Autowired
    RestClientService restClientService

    @Autowired
    CsiScheduledAnalysisFactory csiScheduledAnalysisFactory

    @Override
    ScheduledAsgAnalysis startAnalysis(String clusterName, String notificationDestination) {
        String email = notificationDestination // TODO: Currently we only support e-mail, but will soon handle SNS too.
        String username = email.substring(0, email.indexOf('@'))
        Map<String, String> data = [
                type: 'asgard',
                cluster: clusterName,
                redblack: 'true',
                username: username,
                schedule: 'true'
        ]
        String url = "${configService.asgAnalyzerBaseUrl}/jr/csi/json_canaries/canary-launcher?cache=false"
        RestResponse response = restClientService.postAsNameValuePairs(url, data)
        if (response?.statusCode != HttpStatus.SC_OK) {
            throw new ServiceUnavailableException('CSI', "Failed to start CSI analysis. \
Got response ${response.statusCode} - ${response.content} from ${url}.")
        }
        csiScheduledAnalysisFactory.fromJson(response.content)
    }

    @Override
    void stopAnalysis(String name) {
        toggleState(name, 'paused')
    }

    private toggleState(String name, String state) {
        String baseUrl = configService.asgAnalyzerBaseUrl
        String encodedName = URLEncoder.encode(name, 'UTF-8')
        String url = "${baseUrl}/jr/csi/json__csi/scheduler?action=state&name=${encodedName}&state=${state}"
        def response = restClientService.getJsonAsText(url)
        if (response == null) {
            throw new ServiceUnavailableException("Failed to modify CSI analysis '${name}'. ${url}")
        }
    }
}
