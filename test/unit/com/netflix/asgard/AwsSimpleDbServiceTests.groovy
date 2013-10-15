/*
 * Copyright 2012 Netflix, Inc.
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

import com.netflix.asgard.mock.Mocks
import grails.test.GrailsUnitTestCase
import com.amazonaws.services.simpledb.model.DomainMetadataResult
import com.netflix.asgard.model.SimpleDbSequenceLocator

class AwsSimpleDbServiceTests extends GrailsUnitTestCase {

    AwsSimpleDbService awsSimpleDbService
    UserContext userContext

    void setUp() {
        awsSimpleDbService = Mocks.awsSimpleDbService()
        userContext = Mocks.userContext()
    }

    void testGetDomainMetadata() {
        DomainMetadataResult result = awsSimpleDbService.getDomainMetadata(userContext, 'CLOUD_TASK_SEQUENCE')
        assert 1 == result.itemCount
        assert 7 == result.attributeNamesSizeBytes
    }

    void testCreateAndDeleteDomain() {
        assertNull awsSimpleDbService.getDomainMetadata(userContext, 'MONKEY_BUSINESS')
        awsSimpleDbService.createDomain(userContext, 'MONKEY_BUSINESS')
        DomainMetadataResult result = awsSimpleDbService.getDomainMetadata(userContext, 'MONKEY_BUSINESS')
        assert 0 == result.itemCount
        assert 0 == result.attributeNamesSizeBytes
        awsSimpleDbService.deleteDomain(userContext, 'MONKEY_BUSINESS')
        assertNull awsSimpleDbService.getDomainMetadata(userContext, 'MONKEY_BUSINESS')
    }

    void testListDomains() {
        List<String> domains = awsSimpleDbService.listDomains(userContext.region)
        assert ['SIMPLEDB_PROPERTIES', 'RESOURCE_REGISTRY', 'CLOUD_TASK_SEQUENCE'] == domains
    }

    void testIncrementAndGetSequenceNumber() {

        SimpleDbSequenceLocator locator = SimpleDbSequenceLocator.Task

        Long seqA = awsSimpleDbService.incrementAndGetSequenceNumber(userContext, locator).toLong()
        Long seqB = awsSimpleDbService.incrementAndGetSequenceNumber(userContext, locator).toLong()
        Long seqC = awsSimpleDbService.incrementAndGetSequenceNumber(userContext, locator).toLong()

        assert seqA + 1 == seqB
        assert seqB + 1 == seqC
    }

}
