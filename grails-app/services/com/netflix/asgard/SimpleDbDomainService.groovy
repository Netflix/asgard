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

import com.amazonaws.services.simpledb.AmazonSimpleDB
import com.amazonaws.services.simpledb.model.DomainMetadataResult
import com.netflix.asgard.cache.CacheInitializer

class SimpleDbDomainService implements CacheInitializer {

    static transactional = false

    MultiRegionAwsClient<AmazonSimpleDB> awsClient
    def awsSimpleDbService
    Caches caches
    def taskService

    void initializeCaches() {
        caches.allDomains.ensureSetUp({ Region region -> retrieveDomains(region) })
    }

    private List<String> retrieveDomains(Region region) {
        awsSimpleDbService.listDomains(region)
    }

    Collection<String> getDomains(UserContext userContext) {
        caches.allDomains.by(userContext.region).list()
    }

    String getDomainFromCache(UserContext userContext, String domainName) {
        caches.allDomains.by(userContext.region).get(domainName)
    }

    DomainMetadataResult getDomainMetadata(UserContext userContext, String domainName) {
        awsSimpleDbService.getDomainMetadata(userContext, domainName)
    }

    void createDomain(UserContext userContext, String domainName) {
        taskService.runTask(userContext, "Creating SimpleDB domain '${domainName}'", { task ->
            if (getDomainMetadata(userContext, domainName)) {
                throw new IllegalStateException("SimpleDB domain '${domainName}' already exists")
            } else {
                awsSimpleDbService.createDomain(userContext, domainName)
                caches.allDomains.by(userContext.region).put(domainName, domainName)
            }
        }, Link.to(EntityType.domain, domainName))
    }

    void deleteDomain(UserContext userContext, String domainName) {
        taskService.runTask(userContext, "Deleting SimpleDB domain '${domainName}'", { task ->
            if (!getDomainMetadata(userContext, domainName)) {
                throw new IllegalStateException("SimpleDB domain '${domainName}' not found")
            } else {
                awsSimpleDbService.deleteDomain(userContext, domainName)
                caches.allDomains.by(userContext.region).remove(domainName)
            }
        }, Link.to(EntityType.domain, domainName))
    }
}
