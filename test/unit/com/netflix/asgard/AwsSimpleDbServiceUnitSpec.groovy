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

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.simpledb.AmazonSimpleDB
import com.amazonaws.services.simpledb.model.Attribute
import com.amazonaws.services.simpledb.model.CreateDomainRequest
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest
import com.amazonaws.services.simpledb.model.Item
import com.amazonaws.services.simpledb.model.PutAttributesRequest
import com.amazonaws.services.simpledb.model.ReplaceableAttribute
import com.amazonaws.services.simpledb.model.SelectRequest
import com.amazonaws.services.simpledb.model.SelectResult
import spock.lang.Specification

class AwsSimpleDbServiceUnitSpec extends Specification {

    static final DOMAIN_NAME = 'CLOUD_APPLICATIONS'
    static final SELECT_ALL_QUERY = "select * from ${DOMAIN_NAME} limit 2500"

    def simpleDbClient = Mock(AmazonSimpleDB)
    AwsSimpleDbService awsSimpleDbService = new AwsSimpleDbService(
            awsClient: new MultiRegionAwsClient({ simpleDbClient }))

    Collection<Item> items = [new Item(name: 'Zebra'), new Item(name: 'aardvark')]

    def 'should select all items'() {
        when:
        Collection<Item> actualItems = awsSimpleDbService.selectAll(DOMAIN_NAME)

        then:
        1 * simpleDbClient.select(new SelectRequest(SELECT_ALL_QUERY, true)) >> new SelectResult(items: items)
        actualItems == items
    }

    def 'should select one item'() {
        String query = "select * from CLOUD_APPLICATIONS where itemName()='beaver'"

        when:
        Item actualItem = awsSimpleDbService.selectOne(DOMAIN_NAME, 'beaver')

        then:
        1 * simpleDbClient.select(new SelectRequest(query, true)) >> new SelectResult(items: [new Item(name: 'beaver')])
        actualItem == new Item(name: 'beaver')
    }

    def 'should create domain if missing'() {
        AmazonServiceException ase = new AmazonServiceException('missing domain')
        ase.errorCode = 'NoSuchDomain'

        when:
        Collection<Item> items = awsSimpleDbService.selectAll(DOMAIN_NAME)

        then:
        1 * simpleDbClient.select(new SelectRequest(SELECT_ALL_QUERY, true)) >> { throw ase }
        1 * simpleDbClient.createDomain(new CreateDomainRequest(DOMAIN_NAME))
        items == []
    }

    def 'should save item'() {
        Collection<ReplaceableAttribute> attributes = [new ReplaceableAttribute(name: 'status', value: 'busy')]

        when:
        awsSimpleDbService.save(DOMAIN_NAME, 'beaver', attributes)

        then:
        1 * simpleDbClient.putAttributes(new PutAttributesRequest(domainName: DOMAIN_NAME, itemName: 'beaver',
                attributes: attributes))
    }

    def 'should save item after first attempt fails due to missing domain'() {
        Collection<ReplaceableAttribute> attributes = [new ReplaceableAttribute(name: 'status', value: 'busy')]

        when:
        awsSimpleDbService.save(DOMAIN_NAME, 'beaver', attributes)

        then:
        1 * simpleDbClient.putAttributes(new PutAttributesRequest(domainName: DOMAIN_NAME, itemName: 'beaver',
                attributes: attributes)) >> {
            AmazonServiceException amazonServiceException = new AmazonServiceException('')
            amazonServiceException.errorCode = 'NoSuchDomain'
            throw amazonServiceException
        }

        then:
        1 * simpleDbClient.createDomain(new CreateDomainRequest(DOMAIN_NAME))

        then:
        1 * simpleDbClient.putAttributes(new PutAttributesRequest(domainName: DOMAIN_NAME, itemName: 'beaver',
                attributes: attributes))
    }

    def 'should delete item'() {
        Collection<Attribute> attributes = [new Attribute(name: 'status', value: 'busy')]

        when:
        awsSimpleDbService.delete(DOMAIN_NAME, 'beaver', attributes)

        then:
        1 * simpleDbClient.deleteAttributes(new DeleteAttributesRequest(domainName: DOMAIN_NAME, itemName: 'beaver',
                attributes: attributes))
    }
}
