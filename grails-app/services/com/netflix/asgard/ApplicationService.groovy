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
import com.amazonaws.services.simpledb.model.Attribute
import com.amazonaws.services.simpledb.model.Item
import com.amazonaws.services.simpledb.model.ReplaceableAttribute
import com.netflix.asgard.cache.CacheInitializer
import com.netflix.asgard.collections.GroupedAppRegistrationSet
import com.netflix.asgard.model.MonitorBucketType
import org.joda.time.DateTime
import org.springframework.beans.factory.InitializingBean

class ApplicationService implements CacheInitializer, InitializingBean {

    static transactional = false

    /** The name of SimpleDB domain that stores the cloud application registry. */
    String domainName

    def grailsApplication  // injected after construction
    def awsAutoScalingService
    def awsClientService
    def awsEc2Service
    def awsLoadBalancerService
    def awsSimpleDbService
    Caches caches
    def cloudReadyService
    def configService
    def fastPropertyService
    def mergedInstanceGroupingService
    def taskService

    void afterPropertiesSet() {
        domainName = configService.applicationsDomain
    }

    void initializeCaches() {
        caches.allApplications.ensureSetUp({ retrieveApplications() })
    }

    private Collection<AppRegistration> retrieveApplications() {
        List<Item> items = awsSimpleDbService.selectAll(domainName).sort { it.name.toLowerCase() }
        items.collect { AppRegistration.from(it) }
    }

    List<AppRegistration> getRegisteredApplications(UserContext userContext) {
        caches.allApplications.list().sort { it.name }
    }

    List<AppRegistration> getRegisteredApplicationsForLoadBalancer(UserContext userContext) {
        new ArrayList<AppRegistration>(getRegisteredApplications(userContext).findAll {
            Relationships.checkAppNameForLoadBalancer(it.name)
        })
    }

    GroupedAppRegistrationSet getGroupedRegisteredApplications(UserContext ctx) {
        new GroupedAppRegistrationSet(getRegisteredApplications(ctx))
    }

    AppRegistration getRegisteredApplication(UserContext userContext, String nameInput, From from = From.AWS) {
        if (!nameInput) { return null }
        String name = nameInput.toLowerCase()
        if (from == From.CACHE) {
            return caches.allApplications.get(name)
        }
        Item item = awsSimpleDbService.selectOne(domainName, name.toUpperCase())
        AppRegistration appRegistration = AppRegistration.from(item)
        caches.allApplications.put(name, appRegistration)
        appRegistration
    }

    AppRegistration getRegisteredApplicationForLoadBalancer(UserContext userContext, String name) {
        Relationships.checkAppNameForLoadBalancer(name) ? getRegisteredApplication(userContext, name) : null
    }

    CreateApplicationResult createRegisteredApplication(UserContext userContext, String nameInput, String group,
            String type, String description, String owner, String email, MonitorBucketType monitorBucketType,
            String tags, boolean enableChaosMonkey) {
        String name = nameInput.toLowerCase()
        CreateApplicationResult result = new CreateApplicationResult()
        result.appName = name
        if (getRegisteredApplication(userContext, name)) {
            result.appCreateException = new IllegalStateException("Can't add Application ${name}. It already exists.")
            return result
        }
        String nowEpoch = new DateTime().millis as String
        Collection<ReplaceableAttribute> attributes = buildAttributesList(group, type, description, owner, email,
                monitorBucketType, tags, false)
        attributes << new ReplaceableAttribute('createTs', nowEpoch, false)
        String creationLogMessage = "Create registered app ${name}, type ${type}, owner ${owner}, email ${email}"
        taskService.runTask(userContext, creationLogMessage, { task ->
            try {
                awsSimpleDbService.save(domainName, name.toUpperCase(), attributes)
                result.appCreated = true
            } catch (AmazonServiceException e) {
                result.appCreateException = e
            }
            if (enableChaosMonkey) {
                task.log("Enabling Chaos Monkey for ${name}.")
                result.cloudReadyUnavailable = !cloudReadyService.enableChaosMonkeyForApplication(name)
            }
        }, Link.to(EntityType.application, name))
        getRegisteredApplication(userContext, name)
        result
    }

    private static Collection<ReplaceableAttribute> buildAttributesList(String group, String type, String description,
            String owner, String email, MonitorBucketType monitorBucketType, String tags,
            Boolean replaceExistingValues) {

        Check.notNull(monitorBucketType, MonitorBucketType, 'monitorBucketType')
        String nowEpoch = new DateTime().millis as String
        Collection<ReplaceableAttribute> attributes = []
        attributes << new ReplaceableAttribute('group', group ?: '', replaceExistingValues)
        attributes << new ReplaceableAttribute('type', Check.notEmpty(type), replaceExistingValues)
        attributes << new ReplaceableAttribute('description', Check.notEmpty(description), replaceExistingValues)
        attributes << new ReplaceableAttribute('owner', Check.notEmpty(owner), replaceExistingValues)
        attributes << new ReplaceableAttribute('email', Check.notEmpty(email), replaceExistingValues)
        attributes << new ReplaceableAttribute('monitorBucketType', monitorBucketType.name(), replaceExistingValues)
        attributes << new ReplaceableAttribute('updateTs', nowEpoch, replaceExistingValues)
        if (tags) {
            attributes << new ReplaceableAttribute('tags', tags, replaceExistingValues)
        }
        return attributes
    }

    void updateRegisteredApplication(UserContext userContext, String name, String group, String type, String desc,
                                     String owner, String email, String tags, MonitorBucketType bucketType) {
        Collection<ReplaceableAttribute> attributes = buildAttributesList(group, type, desc, owner, email,
                bucketType, tags, true)
        taskService.runTask(userContext,
                "Update registered app ${name}, type ${type}, owner ${owner}, email ${email}", { task ->
            awsSimpleDbService.save(domainName, name.toUpperCase(), attributes)
            if (!tags) {
                awsSimpleDbService.delete(domainName, name.toUpperCase(), [new Attribute().withName('tags')])
            }
        }, Link.to(EntityType.application, name))
        getRegisteredApplication(userContext, name)
    }

    void deleteRegisteredApplication(UserContext userContext, String name) {
        Check.notEmpty(name, "name")
        validateDelete(userContext, name)
        taskService.runTask(userContext, "Delete registered app ${name}", { task ->
            awsSimpleDbService.delete(domainName, name.toUpperCase())
        }, Link.to(EntityType.application, name))
        getRegisteredApplication(userContext, name)
    }

    private void validateDelete(UserContext userContext, String name) {
        List<String> objectsWithEntities = []
        if (awsAutoScalingService.getAutoScalingGroupsForApp(userContext, name)) {
            objectsWithEntities.add('Auto Scaling Groups')
        }
        if (awsLoadBalancerService.getLoadBalancersForApp(userContext, name)) {
            objectsWithEntities.add('Load Balancers')
        }
        if (awsEc2Service.getSecurityGroupsForApp(userContext, name)) {
            objectsWithEntities.add('Security Groups')
        }
        if (mergedInstanceGroupingService.getMergedInstances(userContext, name)) {
            objectsWithEntities.add('Instances')
        }
        if (fastPropertyService.getFastPropertiesByAppName(userContext, name)) {
            objectsWithEntities.add('Fast Properties')
        }

        if (objectsWithEntities) {
            String referencesString = objectsWithEntities.join(', ')
            String message = "${name} ineligible for delete because it still references ${referencesString}"
            throw new ValidationException(message)
        }
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
     * Provides a string to use for monitoring bucket, either provided an empty string, cluster name or app name based
     * on the application settings.
     *
     * @param userContext who, where, why
     * @param appName application name to look up, and the value to return if the bucket type is 'application'
     * @param clusterName value to return if the application's monitor bucket type is 'cluster'
     * @return appName or clusterName or empty string, based on the application's monitorBucketType
     */
    String getMonitorBucket(UserContext userContext, String appName, String clusterName) {
        MonitorBucketType type = getRegisteredApplication(userContext, appName)?.monitorBucketType
        type == MonitorBucketType.application ? appName : type == MonitorBucketType.cluster ? clusterName : ''
    }
}

/**
 * Records the results of trying to create an Application.
 */
class CreateApplicationResult {
    String appName
    Boolean appCreated
    Exception appCreateException
    Boolean cloudReadyUnavailable // Just a warning, does not affect success.

    String toString() {
        StringBuilder output = new StringBuilder()
        if (appCreated) {
            output.append("Application '${appName}' has been created. ")
        }
        if (appCreateException) {
            output.append("Could not create Application '${appName}': ${appCreateException}. ")
        }
        if (cloudReadyUnavailable) {
            output.append('Chaos Monkey was not enabled because Cloudready is currently unavailable. ')
        }
        output.toString()
    }

    Boolean succeeded() {
        appCreated && !appCreateException
    }
}

