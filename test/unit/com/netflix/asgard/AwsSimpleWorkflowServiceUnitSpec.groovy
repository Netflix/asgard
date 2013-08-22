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

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow
import com.amazonaws.services.simpleworkflow.model.DomainInfo
import com.netflix.asgard.retriever.AwsResultsRetriever
import spock.lang.Specification

class AwsSimpleWorkflowServiceUnitSpec extends Specification {

    AwsSimpleWorkflowService awsSimpleWorkflowService = Spy(AwsSimpleWorkflowService)

    def 'should retrieve domains'() {
        awsSimpleWorkflowService.@domainFetcher = Mock(AwsResultsRetriever) {
            retrieve(_, _) >> [new DomainInfo(name: 'domain1')]
        }
        awsSimpleWorkflowService.configService = Mock(ConfigService) {
            getSimpleWorkflowDomain() >> 'domain1'
        }
        awsSimpleWorkflowService.simpleWorkflowClient = Mock(AmazonSimpleWorkflow)

        when:
        List<DomainInfo> domains = awsSimpleWorkflowService.retrieveDomains()

        then:
        domains == [new DomainInfo(name: 'domain1')]
        0 * awsSimpleWorkflowService.simpleWorkflowClient.registerDomain(_)
    }

    def 'should register domain if not found on retrieval'() {
        awsSimpleWorkflowService.@domainFetcher = Mock(AwsResultsRetriever) {
            retrieve(_, _) >> [new DomainInfo(name: 'domain1')]
        }
        awsSimpleWorkflowService.configService = Mock(ConfigService) {
            getSimpleWorkflowDomain() >> 'domain2'
        }
        awsSimpleWorkflowService.simpleWorkflowClient = Mock(AmazonSimpleWorkflow)

        when:
        awsSimpleWorkflowService.retrieveDomains()

        then:
        1 * awsSimpleWorkflowService.simpleWorkflowClient.registerDomain(_)
    }
}
