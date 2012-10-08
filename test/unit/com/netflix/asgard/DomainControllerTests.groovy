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

import com.amazonaws.services.simpledb.model.DomainMetadataResult
import com.netflix.asgard.mock.Mocks
import org.junit.Before

class DomainControllerTests {

    @Before
    void setUp() {
        TestUtils.setUpMockRequest()
        controller.simpleDbDomainService = Mocks.simpleDbDomainService()
    }

    void testShow() {
        def params = controller.params
        params.id = 'RESOURCE_REGISTRY'
        def attrs = controller.show()
        assert 'RESOURCE_REGISTRY' == attrs.domainName
        DomainMetadataResult actualMetadata = attrs.domainMetadata
        assert 2042 == actualMetadata.itemCount
        assert 114217 == actualMetadata.itemNamesSizeBytes
        assert 18 == actualMetadata.attributeNameCount
        assert 124 == actualMetadata.attributeNamesSizeBytes
        assert 12586 == actualMetadata.attributeValueCount
        assert 186995 == actualMetadata.attributeValuesSizeBytes
        assert 1319646219 == actualMetadata.timestamp
    }

    void testShowNonExistent() {
        def p = controller.params
        p.id = 'doesntexist'
        controller.show()
        assert '/error/missing' == view
        assert "SimpleDB Domain 'doesntexist' not found in us-east-1 test" == controller.flash.message
    }
}
