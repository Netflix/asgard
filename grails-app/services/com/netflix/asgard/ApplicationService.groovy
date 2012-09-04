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
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest
import com.amazonaws.services.simpledb.model.Item
import com.amazonaws.services.simpledb.model.PutAttributesRequest
import com.amazonaws.services.simpledb.model.ReplaceableAttribute
import com.amazonaws.services.simpledb.model.SelectRequest
import com.amazonaws.services.simpledb.model.SelectResult
import com.netflix.asgard.cache.CacheInitializer
import com.netflix.asgard.model.MonitorBucketType
import org.joda.time.DateTime
import org.springframework.beans.factory.InitializingBean

class ApplicationService implements CacheInitializer, InitializingBean {

    static transactional = false

    /** The name of SimpleDB domain that stores the cloud application registry. */
    private String domainName

    def grailsApplication  // injected after construction
    def awsClientService
    Caches caches
    def configService
    def taskService

    AmazonSimpleDB simpleDbClient

    void afterPropertiesSet() {
        domainName = configService.applicationsDomain

        // Applications are stored only in the default region, so no multi region support needed here.
        simpleDbClient = awsClientService.create(AmazonSimpleDB)
    }

    void initializeCaches() {
        caches.allApplications.ensureSetUp({ retrieveApplications() })
    }

    String selectAllStatement() {
        "select * from ${domainName} limit 2500"
    }

    String selectOneStatement() {
        "select * from ${domainName} where itemName()="
    }

    private Collection<AppRegistration> retrieveApplications() {
        List<Item> items = runQuery(selectAllStatement()).sort { it.name.toLowerCase() }
        items.collect { AppRegistration.from(it) }
    }

    private List<Item> runQuery(String queryString) {
        List<Item> appItems = []
        try {
            SelectResult result = simpleDbClient.select(new SelectRequest(queryString, true))
            while (true) {
                appItems.addAll(result.items)
                String nextToken = result.nextToken
                if (nextToken) {
                    sleep 500
                    result = simpleDbClient.select(new SelectRequest(queryString, true).withNextToken(nextToken))
                } else {
                    break
                }
            }
        } catch (AmazonServiceException ase) {
            if (ase.errorCode == 'NoSuchDomain') {
                simpleDbClient.createDomain(new CreateDomainRequest(domainName))
            } else {
                throw ase
            }
        }
        appItems
    }

    List<AppRegistration> getRegisteredApplications(UserContext userContext) {
        caches.allApplications.list().sort { it.name }
    }

    List<AppRegistration> getRegisteredApplicationsForLoadBalancer(UserContext userContext) {
        new ArrayList<AppRegistration>(getRegisteredApplications(userContext).findAll {
            Relationships.checkAppNameForLoadBalancer(it.name)
        })
    }

    AppRegistration getRegisteredApplication(UserContext userContext, String name, From from = From.AWS) {
        if (!name) { return null }
        if (from == From.CACHE) {
            return caches.allApplications.get(name)
        }
        assert !(name.contains("'")) // Simple way to avoid SQL injection
        String queryString = "${selectOneStatement()}'${name.toUpperCase()}'"
        List<Item> items = runQuery(queryString)
        AppRegistration appRegistration = AppRegistration.from(Check.loneOrNone(items, 'items'))
        caches.allApplications.put(name, appRegistration)
        appRegistration
    }

    AppRegistration getRegisteredApplicationForLoadBalancer(UserContext userContext, String name) {
        Relationships.checkAppNameForLoadBalancer(name) ? getRegisteredApplication(userContext, name) : null
    }

    void createRegisteredApplication(UserContext userContext, String name, String type, String description,
                                     String owner, String email,
                                     MonitorBucketType monitorBucketType) {
        name = name.toLowerCase()
        if (getRegisteredApplication(userContext, name)) {
            throw new IllegalStateException("Can't add Application $name. It already exists.")
        }
        String nowEpoch = new DateTime().millis as String
        Collection<ReplaceableAttribute> attributes = buildAttributesList(type, description, owner, email,
                monitorBucketType, false)
        attributes << new ReplaceableAttribute('createTs', nowEpoch, false)

        String creationLogMessage = "Create registered app ${name}, type ${type}, owner ${owner}, email ${email}"
        taskService.runTask(userContext, creationLogMessage, { task ->
            simpleDbClient.putAttributes(new PutAttributesRequest().withDomainName(domainName).
                    withItemName(name.toUpperCase()).withAttributes(attributes))
        }, Link.to(EntityType.application, name))
        getRegisteredApplication(userContext, name)
    }

    private Collection<ReplaceableAttribute> buildAttributesList(String type, String description, String owner,
            String email, MonitorBucketType monitorBucketType,
            Boolean replaceExistingValues) {

        Check.notNull(monitorBucketType, MonitorBucketType, 'monitorBucketType')
        String nowEpoch = new DateTime().millis as String
        Collection<ReplaceableAttribute> attributes = []
        attributes << new ReplaceableAttribute('type', Check.notEmpty(type), replaceExistingValues)
        attributes << new ReplaceableAttribute('description', Check.notEmpty(description), replaceExistingValues)
        attributes << new ReplaceableAttribute('owner', Check.notEmpty(owner), replaceExistingValues)
        attributes << new ReplaceableAttribute('email', Check.notEmpty(email), replaceExistingValues)
        attributes << new ReplaceableAttribute('monitorBucketType', monitorBucketType.name(), replaceExistingValues)
        attributes << new ReplaceableAttribute('updateTs', nowEpoch, replaceExistingValues)
        return attributes
    }

    void updateRegisteredApplication(UserContext userContext, String name, String type, String desc, String owner,
                                     String email, MonitorBucketType bucketType) {
        Collection<ReplaceableAttribute> attributes = buildAttributesList(type, desc, owner, email,
                bucketType, true)
        taskService.runTask(userContext,
                "Update registered app ${name}, type ${type}, owner ${owner}, email ${email}", { task ->
            simpleDbClient.putAttributes(new PutAttributesRequest().withDomainName(domainName).
                    withItemName(name.toUpperCase()).withAttributes(attributes))
        }, Link.to(EntityType.application, name))
        getRegisteredApplication(userContext, name)
    }

    void deleteRegisteredApplication(UserContext userContext, String name) {
        Check.notEmpty(name, "name")
        taskService.runTask(userContext, "Delete registered app ${name}", { task ->
            simpleDbClient.deleteAttributes(new DeleteAttributesRequest(domainName, name.toUpperCase()))
        }, Link.to(EntityType.application, name))
        getRegisteredApplication(userContext, name)
    }

    /**
     * Get the email address of the relevant app, or empty string if no email address can be found for the specified
     * app name.
     *
     * @param appName the name of the app that has the email address
     * @return the email address associated with the app, or empty string if no email address can be found
     */
    String getEmailFromApp(UserContext userContext, String appName) {
        getRegisteredApplication(userContext, appName)?.email ?: ''
    }

    /**
     * Provides a string to use for monitoring bucket, either provided cluster name or app name based on the
     * application settings. If the application is not found, the app name is returned.
     *
     * @param userContext who, where, why
     * @param appName application name to lookup
     * @param clusterName value to return if the application's monitor bucket type is 'cluster'
     * @return appName or clusterName parameter, based on the application's monitorBucketType
     */
    String getMonitorBucket(UserContext userContext, String appName, String clusterName) {
        MonitorBucketType bucketType = getRegisteredApplication(userContext, appName)?.monitorBucketType
        (bucketType == MonitorBucketType.cluster) ? clusterName : appName
    }
}
