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

import javax.xml.bind.DatatypeConverter
import spock.lang.Specification

class DefaultUserDataProviderSpec extends Specification {

    def 'should generate user data with blanks for null values'() {

        ConfigService configService = Mock(ConfigService) {
            getUserDataVarPrefix() >> ''
        }
        ApplicationService applicationService = Mock(ApplicationService)
        DefaultUserDataProvider provider = new DefaultUserDataProvider(configService: configService,
                applicationService: applicationService)
        UserContext userContext = UserContext.auto(Region.SA_EAST_1)

        when:
        String userDataEncoded = provider.buildUserDataForVariables(userContext, 'helloworld', 'helloworld',
                'helloworld-1234567890')

        then:
        decode(userDataEncoded) == """export ENVIRONMENT=
export MONITOR_BUCKET=
export APP=helloworld
export STACK=
export CLUSTER=helloworld
export AUTO_SCALE_GROUP=helloworld
export LAUNCH_CONFIG=helloworld-1234567890
export EC2_REGION=sa-east-1
"""
    }

    private String decode(String encoded) {
        new String(DatatypeConverter.parseBase64Binary(encoded))
    }
}
