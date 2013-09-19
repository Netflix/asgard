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

        when:
        String userDataEncoded = provider.buildUserDataForVariables(userContext, 'helloworld',
                'helloworld-example-v345', 'helloworld-example-v345-1234567890')

        then:
        decode(userDataEncoded) == '''\
                export ENVIRONMENT=
                export MONITOR_BUCKET=helloworld
                export APP=helloworld
                export STACK=example
                export CLUSTER=helloworld-example
                export AUTO_SCALE_GROUP=helloworld-example-v345
                export LAUNCH_CONFIG=helloworld-example-v345-1234567890
                export EC2_REGION=sa-east-1
                '''.stripIndent()
    }

    def 'should generate user data with blanks for null values'() {

        when:
        String userDataEncoded = provider.buildUserDataForVariables(userContext, 'helloworld', null, null)

        then:
        decode(userDataEncoded) == '''\
                export ENVIRONMENT=
                export MONITOR_BUCKET=helloworld
                export APP=helloworld
                export STACK=
                export CLUSTER=
                export AUTO_SCALE_GROUP=
                export LAUNCH_CONFIG=
                export EC2_REGION=sa-east-1
                '''.stripIndent()
    }

    private String decode(String encoded) {
        new String(DatatypeConverter.parseBase64Binary(encoded))
    }
}
