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
package com.netflix.asgard.userdata

import com.netflix.asgard.ApplicationService
import com.netflix.asgard.ConfigService
import com.netflix.asgard.Region
import com.netflix.asgard.UserContext
import javax.xml.bind.DatatypeConverter
import spock.lang.Specification

@SuppressWarnings("GroovyAssignabilityCheck")
class DefaultUserDataProviderSpec extends Specification {

    ConfigService configService
    ApplicationService applicationService
    DefaultUserDataProvider provider
    UserContext userContext = UserContext.auto(Region.SA_EAST_1)

    void setup() {
        configService = Mock(ConfigService) {
            getUserDataVarPrefix() >> ''
        }
        applicationService = Mock(ApplicationService) {
            getMonitorBucket(_, _, _) >> { it[1] }
        }
        provider = new DefaultUserDataProvider(configService: configService, applicationService: applicationService)
    }

    def 'should generate user data in the default format'() {

        String expected = 'export ENVIRONMENT=\n' +
                'export MONITOR_BUCKET=helloworld\n' +
                'export APP=helloworld\n' +
                'export APP_GROUP=\n' +
                'export STACK=example\n' +
                'export CLUSTER=helloworld-example\n' +
                'export AUTO_SCALE_GROUP=helloworld-example-v345\n' +
                'export LAUNCH_CONFIG=helloworld-example-v345-1234567890\n' +
                'export EC2_REGION=sa-east-1\n'

        when:
        String userDataEncoded = provider.buildUserDataForVariables(userContext, 'helloworld',
                'helloworld-example-v345', 'helloworld-example-v345-1234567890')

        then:
        decode(userDataEncoded) == expected
    }

    def 'should generate user data with blanks for null values'() {

        String expected = 'export ENVIRONMENT=\n' +
                'export MONITOR_BUCKET=helloworld\n' +
                'export APP=helloworld\n' +
                'export APP_GROUP=\n' +
                'export STACK=\n' +
                'export CLUSTER=\n' +
                'export AUTO_SCALE_GROUP=\n' +
                'export LAUNCH_CONFIG=\n' +
                'export EC2_REGION=sa-east-1\n'

        when:
        String userDataEncoded = provider.buildUserDataForVariables(userContext, 'helloworld', null, null)

        then:
        decode(userDataEncoded) == expected
    }

    private String decode(String encoded) {
        new String(DatatypeConverter.parseBase64Binary(encoded))
    }
}
