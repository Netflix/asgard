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
package com.netflix.asgard.userdata

import com.netflix.asgard.AppRegistration
import com.netflix.asgard.ApplicationService
import com.netflix.asgard.ConfigService
import com.netflix.asgard.UserContext
import javax.xml.bind.DatatypeConverter
import spock.lang.Specification

/**
 * Tests for PropertiesUserDataProvider.
 */
class PropertiesUserDataProviderSpec extends Specification {

    AppRegistration app = new AppRegistration(group: 'common')
    String clusterName = 'helloworld-example-c0asia-d0sony'
    String asgName = 'helloworld-example-c0asia-d0sony-v033'
    String launchConfigName = 'helloworld-example-c0asia-d0sony-v033-123456789'
    UserContext userContext = UserContext.auto()
    ConfigService configService = Mock(ConfigService)
    ApplicationService applicationService = Mock(ApplicationService)
    PropertiesUserDataProvider provider = new PropertiesUserDataProvider(configService: configService,
            applicationService: applicationService)

    void setup() {
        configService.userDataVarPrefix >> 'CLOUD_'
        configService.accountName >> 'prod'
        applicationService.getMonitorBucket(userContext, 'helloworld', clusterName) >> 'helloworld'
        applicationService.getRegisteredApplication(userContext, 'helloworld') >> app
    }

    void 'should create a map of properties based on cloud objects'() {

        when:
        Map<String, String> props = provider.mapProperties(userContext, 'helloworld', asgName, launchConfigName)

        then:
        props == [
                CLOUD_APP: 'helloworld',
                CLOUD_APP_GROUP: 'common',
                CLOUD_AUTO_SCALE_GROUP: 'helloworld-example-c0asia-d0sony-v033',
                CLOUD_CLUSTER: 'helloworld-example-c0asia-d0sony',
                CLOUD_COUNTRIES: 'asia',
                CLOUD_DEV_PHASE: 'sony',
                CLOUD_ENVIRONMENT: 'prod',
                CLOUD_LAUNCH_CONFIG: 'helloworld-example-c0asia-d0sony-v033-123456789',
                CLOUD_MONITOR_BUCKET: 'helloworld',
                CLOUD_STACK: 'example',
                EC2_REGION: 'us-east-1'
        ]
    }

    def 'should generate user data in the format of a properties file'() {

        String expected = 'CLOUD_ENVIRONMENT=prod\n' +
                'CLOUD_MONITOR_BUCKET=helloworld\n' +
                'CLOUD_APP=helloworld\n' +
                'CLOUD_APP_GROUP=common\n' +
                'CLOUD_STACK=example\n' +
                'CLOUD_CLUSTER=helloworld-example-c0asia-d0sony\n' +
                'CLOUD_AUTO_SCALE_GROUP=helloworld-example-c0asia-d0sony-v033\n' +
                'CLOUD_LAUNCH_CONFIG=helloworld-example-c0asia-d0sony-v033-123456789\n' +
                'EC2_REGION=us-east-1\n' +
                'CLOUD_COUNTRIES=asia\n' +
                'CLOUD_DEV_PHASE=sony\n'

        when:
        String userDataEncoded = provider.buildUserDataForVariables(userContext, 'helloworld', asgName,
                launchConfigName)

        then:
        decode(userDataEncoded) == expected
    }

    private String decode(String encoded) {
        new String(DatatypeConverter.parseBase64Binary(encoded))
    }
}
