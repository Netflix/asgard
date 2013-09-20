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

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.simpledb.AmazonSimpleDB
import com.amazonaws.services.simpledb.model.Attribute
import com.amazonaws.services.simpledb.model.AttributeDoesNotExistException
import com.amazonaws.services.simpledb.model.CreateDomainRequest
import com.amazonaws.services.simpledb.model.DeleteDomainRequest
import com.amazonaws.services.simpledb.model.DomainMetadataRequest
import com.amazonaws.services.simpledb.model.DomainMetadataResult
import com.amazonaws.services.simpledb.model.GetAttributesRequest
import com.amazonaws.services.simpledb.model.GetAttributesResult
import com.amazonaws.services.simpledb.model.ListDomainsRequest
import com.amazonaws.services.simpledb.model.ListDomainsResult
import com.amazonaws.services.simpledb.model.PutAttributesRequest
import com.amazonaws.services.simpledb.model.ReplaceableAttribute
import com.amazonaws.services.simpledb.model.RequestTimeoutException
import com.amazonaws.services.simpledb.model.UpdateCondition
import com.netflix.asgard.model.SimpleDbSequenceLocator
import org.springframework.beans.factory.InitializingBean

class AwsSimpleDbService implements InitializingBean {

    static transactional = false

    MultiRegionAwsClient<AmazonSimpleDB> awsClient
    def awsClientService

    void afterPropertiesSet() {
        awsClient = new MultiRegionAwsClient<AmazonSimpleDB>( { Region region ->
            AmazonSimpleDB client = awsClientService.create(AmazonSimpleDB)
            // Unconventional SDB endpoints. http://docs.amazonwebservices.com/general/latest/gr/index.html?rande.html
            if (region != Region.US_EAST_1) { client.setEndpoint("sdb.${region}.amazonaws.com") }
            client
        })
    }

    // Domains

    List<String> listDomains(Region region) {
        List<String> domains = []

        ListDomainsResult result = listDomainsWithToken(region, null)
        while (true) {
            domains.addAll(result.domainNames)
            if (result.getNextToken() == null) {
                break
            }
            result = listDomainsWithToken(region, result.getNextToken())
        }
        domains
    }

    private ListDomainsResult listDomainsWithToken(Region region, String nextToken) {
        awsClient.by(region).listDomains(new ListDomainsRequest().withNextToken(nextToken))
    }

    DomainMetadataResult getDomainMetadata(UserContext userContext, String domainName) {
        try {
            return awsClient.by(userContext.region).domainMetadata(new DomainMetadataRequest(domainName))
        } catch (AmazonServiceException ignored) {
            return null
        }
    }

    void createDomain(UserContext userContext, String domainName) {
        awsClient.by(userContext.region).createDomain(new CreateDomainRequest(domainName))
    }

    void deleteDomain(UserContext userContext, String domainName) {
        awsClient.by(userContext.region).deleteDomain(new DeleteDomainRequest(domainName))
    }

    String incrementAndGetSequenceNumber(UserContext userContext, SimpleDbSequenceLocator locator) {

        // Diagnostics in case of surprising failure modes
        Boolean triedToCreateDomain = false
        Boolean triedToResetAttribute = false
        Integer conditionalCheckFailureCount = 0
        Integer maxAttempts = 100

        for (int i = 0; i < maxAttempts; i++) {
            try {
                return nextTaskIdAttempt(locator)
            } catch (AttributeDoesNotExistException ignored) {
                triedToResetAttribute = true
                resetSequence(locator)
            } catch (AmazonServiceException ase) {
                if (ase.errorCode == 'ConditionalCheckFailed') {
                    conditionalCheckFailureCount++
                    // Race condition between concurrent threads trying to get next number. Try again. Avoid rate limit.
                    Time.sleepCancellably(50)
                } else if (ase.errorCode == 'NoSuchDomain') {
                    triedToCreateDomain = true
                    createDomain(userContext.withRegion(locator.region), locator.domainName)
                } else {
                    throw ase
                }
            }
        }
        throw new RequestTimeoutException("""Error getting next task id. maxAttempts=${maxAttempts},
 triedToCreateDomain=${triedToCreateDomain}, triedToResetAttribute=${triedToResetAttribute}
 conditionalCheckFailureCount=${conditionalCheckFailureCount}, SimpleDbSequenceLocator=${locator}"""
        )
    }

    private String nextTaskIdAttempt(SimpleDbSequenceLocator locator) {

        String itemName = locator.itemName
        String attributeName = locator.attributeName
        String domainName = locator.domainName
        Region region = locator.region
        GetAttributesResult result = awsClient.by(region).getAttributes(new GetAttributesRequest().
                withDomainName(domainName).withItemName(itemName).withAttributeNames(attributeName).
                withConsistentRead(true))
        Attribute attribute = result.attributes.find { it.name == attributeName }
        if (!attribute) {
            throw new AttributeDoesNotExistException("${locator} attribute does not exist")
        }

        String oldValue = attribute.value
        Long oldId = oldValue.toLong()
        Long newId = oldId + 1

        // In the distant future after the Sun has burned out we'll be done with positive Long values and newId will
        // overflow to become negative. At that point, reset the sequence to 1.
        if (newId < 0) { newId = 1 }
        String newValue = newId.toString()

        // Conditional put
        awsClient.by(region).putAttributes(new PutAttributesRequest().withDomainName(domainName).
                    withExpected(new UpdateCondition(attributeName, oldValue, true)).
                    withItemName(itemName).withAttributes(new ReplaceableAttribute(attributeName, newValue, true)))

        // If no exception was thrown during the conditional put operation then the new ID is written and valid
        newValue
    }

    void resetSequence(SimpleDbSequenceLocator locator) {
        PutAttributesRequest request = new PutAttributesRequest().withDomainName(locator.domainName).
                withItemName(locator.itemName).
                withAttributes(new ReplaceableAttribute(locator.attributeName, '0', true))
        awsClient.by(locator.region).putAttributes(request)
    }
}
