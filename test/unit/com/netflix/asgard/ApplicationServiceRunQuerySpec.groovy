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
import com.amazonaws.services.simpledb.model.CreateDomainRequest
import com.amazonaws.services.simpledb.model.Item
import com.amazonaws.services.simpledb.model.SelectRequest
import com.amazonaws.services.simpledb.model.SelectResult
import spock.lang.Specification

@SuppressWarnings("GroovyAccessibility")
class ApplicationServiceRunQuerySpec extends Specification {

    static final DOMAIN_NAME = 'CLOUD_APPLICATIONS'
    static final SELECT_ALL_QUERY = "select * from ${DOMAIN_NAME} limit 2500"

    def simpleDbClient = Mock(AmazonSimpleDB)
    ApplicationService applicationService = new ApplicationService(
        simpleDbClient: simpleDbClient,
        domainName: DOMAIN_NAME)

    def setup() {
        new MonkeyPatcherService().createDynamicMethods()
    }

    def 'should retrieve applications'() {
        Item item1 = new Item(name: 'Zebra')
        Item item2 = new Item(name: 'aardvark')
        SelectResult result = new SelectResult(items: [item1, item2])
        simpleDbClient.select(new SelectRequest(SELECT_ALL_QUERY, true)) >> result

        when:
        Collection<AppRegistration> applications = applicationService.retrieveApplications()

        then:
        applications == [AppRegistration.from(item2), AppRegistration.from(item1)]
    }

    def 'should create domain if missing'() {
        AmazonServiceException ase = new AmazonServiceException('missing domain')
        ase.errorCode = 'NoSuchDomain'
        simpleDbClient.select(new SelectRequest(SELECT_ALL_QUERY, true)) >> { throw ase }

        when:
        Collection<AppRegistration> applications = applicationService.retrieveApplications()

        then:
        1 * simpleDbClient.createDomain(new CreateDomainRequest(DOMAIN_NAME))
        applications == []
    }

    def 'should bubble up AWS error'() {
        AmazonServiceException ase = new AmazonServiceException('other exception')
        simpleDbClient.select(new SelectRequest(SELECT_ALL_QUERY, true)) >> { throw ase }

        when:
        applicationService.retrieveApplications()

        then:
        AmazonServiceException e = thrown()
        e == ase
    }

}
