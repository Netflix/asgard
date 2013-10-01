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
package com.netflix.asgard.mock

import com.amazonaws.AmazonServiceException
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.simpledb.AmazonSimpleDBClient
import com.amazonaws.services.simpledb.model.Attribute
import com.amazonaws.services.simpledb.model.BatchDeleteAttributesRequest
import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest
import com.amazonaws.services.simpledb.model.CreateDomainRequest
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest
import com.amazonaws.services.simpledb.model.DeleteDomainRequest
import com.amazonaws.services.simpledb.model.DomainMetadataRequest
import com.amazonaws.services.simpledb.model.DomainMetadataResult
import com.amazonaws.services.simpledb.model.GetAttributesRequest
import com.amazonaws.services.simpledb.model.GetAttributesResult
import com.amazonaws.services.simpledb.model.Item
import com.amazonaws.services.simpledb.model.ListDomainsRequest
import com.amazonaws.services.simpledb.model.ListDomainsResult
import com.amazonaws.services.simpledb.model.PutAttributesRequest
import com.amazonaws.services.simpledb.model.ReplaceableAttribute
import com.amazonaws.services.simpledb.model.SelectRequest
import com.amazonaws.services.simpledb.model.SelectResult
import com.amazonaws.services.simpledb.model.UpdateCondition
import org.codehaus.groovy.grails.web.json.JSONArray
import org.joda.time.format.ISODateTimeFormat

class MockAmazonSimpleDBClient extends AmazonSimpleDBClient {

    private Collection<Item> mockAppItems
    private Map<String, DomainMetadataResult> mockDomains
    private String task_id_value = 0

    private List<Item> loadMockAppItems() {
        JSONArray jsonArray = Mocks.parseJsonString(MockApplications.DATA)
        jsonArray.collect {
            new Item().withName(it.name).withAttributes(
                    new Attribute('description', it.description),
                    new Attribute('type', it.type), new Attribute('owner', it.owner),
                    new Attribute('email', it.email),
                    new Attribute('createTs',
                            ISODateTimeFormat.dateTimeParser().parseDateTime(it.createTime).millis.toString()),
                    new Attribute('updateTs',
                            ISODateTimeFormat.dateTimeParser().parseDateTime(it.updateTime).millis.toString())
            )
        }
    }

    private Map<String, DomainMetadataResult> loadMockDomains() {
        [SIMPLEDB_PROPERTIES: new DomainMetadataResult(
                attributeNameCount: 9,
                attributeNamesSizeBytes: 73,
                attributeValueCount: 3089,
                attributeValuesSizeBytes: 53513,
                itemCount: 478,
                itemNamesSizeBytes: 6124,
                timestamp: 1319646152
        ), RESOURCE_REGISTRY: new DomainMetadataResult(
                attributeNameCount: 18,
                attributeNamesSizeBytes: 124,
                attributeValueCount: 12586,
                attributeValuesSizeBytes: 186995,
                itemCount: 2042,
                itemNamesSizeBytes: 114217,
                timestamp: 1319646219
        ), CLOUD_TASK_SEQUENCE: new DomainMetadataResult(
                attributeNameCount: 1,
                attributeNamesSizeBytes: 7,
                attributeValueCount: 1,
                attributeValuesSizeBytes: 7,
                itemCount: 1,
                itemNamesSizeBytes: 7,
                timestamp: 1319646219
        )]
    }

    MockAmazonSimpleDBClient(BasicAWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        super(awsCredentials as BasicAWSCredentials, clientConfiguration)
        mockAppItems = loadMockAppItems()
        mockDomains = loadMockDomains()
    }

    void setEndpoint(String endpoint) {

    }

    SelectResult select(SelectRequest selectRequest) {
        String query = selectRequest.selectExpression
        if (query.contains(" where itemName()=")) {
            String appName = query.split('=')[1] - '\'' - '\''
            return new SelectResult().withItems(mockAppItems.find { it.name.equalsIgnoreCase(appName) })
        } else if (query.endsWith(' limit 2500')) {
            return new SelectResult().withItems(mockAppItems)
        }
        new SelectResult()
    }

    void batchDeleteAttributes(BatchDeleteAttributesRequest batchDeleteAttributesRequest) {

    }

    void deleteDomain(DeleteDomainRequest deleteDomainRequest) {
        mockDomains.remove(deleteDomainRequest.domainName)
    }

    void createDomain(CreateDomainRequest createDomainRequest) {
        String name = createDomainRequest.domainName
        mockDomains.put(name, new DomainMetadataResult(
                attributeNameCount: 0,
                attributeNamesSizeBytes: 0,
                attributeValueCount: 0,
                attributeValuesSizeBytes: 0,
                itemCount: 0,
                itemNamesSizeBytes: 0,
                timestamp: 1319646219))
    }

    void deleteAttributes(DeleteAttributesRequest deleteAttributesRequest) {

    }

    ListDomainsResult listDomains(ListDomainsRequest listDomainsRequest) {
        new ListDomainsResult().withDomainNames(mockDomains.keySet() as List)
    }

    void putAttributes(PutAttributesRequest putAttributesRequest) {
        String domainName = putAttributesRequest.domainName
        String itemName = putAttributesRequest.itemName
        List<ReplaceableAttribute> attributes = putAttributesRequest.attributes
        UpdateCondition updateCondition = putAttributesRequest.expected

        if (domainName == 'CLOUD_TASK_SEQUENCE' && itemName == 'task_id' &&
                attributes.size() == 1 && attributes[0].name == 'value' && attributes[0].replace &&
                updateCondition.name == 'value' && updateCondition.exists) {

            if (updateCondition.value == task_id_value) {
                task_id_value = (task_id_value.toLong() + 1).toString()
            } else {
                AmazonServiceException ase = new AmazonServiceException('Conditional check failed')
                ase.errorCode = 'ConditionalCheckFailed'
                throw ase
            }
        }
    }

    GetAttributesResult getAttributes(GetAttributesRequest getAttributesRequest) {
        String domainName = getAttributesRequest.domainName
        String itemName = getAttributesRequest.itemName
        List<String> attributeNames = getAttributesRequest.attributeNames

        if (domainName == 'CLOUD_TASK_SEQUENCE' && itemName == 'task_id' && attributeNames == ['value']) {
            return new GetAttributesResult().withAttributes(new Attribute('value', task_id_value))
        }
        null
    }

    void batchPutAttributes(BatchPutAttributesRequest batchPutAttributesRequest) {

    }

    DomainMetadataResult domainMetadata(DomainMetadataRequest domainMetadataRequest) {
        String name = domainMetadataRequest.domainName
        DomainMetadataResult domainMetadataResult = mockDomains.get(name)
        if (domainMetadataResult) {
            return domainMetadataResult
        } else {
            throw new AmazonServiceException("Status Code: 400, AWS Request ID: 123unittest, " +
                        "AWS Error Code: InvalidDomain.NotFound, AWS Error Message: " +
                        "The Domain '${name}' does not exist")
        }
    }

    ListDomainsResult listDomains() {
        new ListDomainsResult().withDomainNames(mockDomains.keySet() as List)
    }
}
