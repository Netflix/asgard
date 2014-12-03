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
package com.netflix.asgard.applications

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.simpledb.model.Attribute
import com.amazonaws.services.simpledb.model.Item
import com.amazonaws.services.simpledb.model.ReplaceableAttribute
import com.netflix.asgard.AppRegistration
import com.netflix.asgard.AwsAutoScalingService
import com.netflix.asgard.AwsClientService
import com.netflix.asgard.AwsEc2Service
import com.netflix.asgard.AwsLoadBalancerService
import com.netflix.asgard.AwsSimpleDbService
import com.netflix.asgard.Caches
import com.netflix.asgard.Check
import com.netflix.asgard.ConfigService
import com.netflix.asgard.ApplicationModificationResult
import com.netflix.asgard.EntityType
import com.netflix.asgard.FastPropertyService
import com.netflix.asgard.From
import com.netflix.asgard.Link
import com.netflix.asgard.MergedInstanceGroupingService
import com.netflix.asgard.TaskService
import com.netflix.asgard.UserContext
import com.netflix.asgard.ValidationException
import com.netflix.asgard.cache.CacheInitializer
import com.netflix.asgard.model.MonitorBucketType
import org.joda.time.DateTime
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired

class SimpleDBApplicationService extends AbstractApplicationService implements CacheInitializer, InitializingBean {

    static transactional = false

    /** The name of SimpleDB domain that stores the cloud application registry. */
    String domainName

    @Autowired
    AwsAutoScalingService awsAutoScalingService

    @Autowired
    AwsClientService awsClientService

    @Autowired
    AwsEc2Service awsEc2Service

    @Autowired
    AwsLoadBalancerService awsLoadBalancerService

    @Autowired
    AwsSimpleDbService awsSimpleDbService

    @Autowired
    Caches caches

    @Autowired
    ConfigService configService

    @Autowired
    FastPropertyService fastPropertyService

    @Autowired
    MergedInstanceGroupingService mergedInstanceGroupingService

    @Autowired
    TaskService taskService

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

    @Override
    List<AppRegistration> getRegisteredApplications(UserContext userContext) {
        caches.allApplications.list().sort { it.name }
    }

    @Override
    AppRegistration getRegisteredApplication(UserContext userContext, String nameInput, From from = From.AWS) {
        if (!nameInput) {
            return null
        }
        String name = nameInput.toLowerCase()
        if (from == From.CACHE) {
            return caches.allApplications.get(name)
        }
        Item item = awsSimpleDbService.selectOne(domainName, name.toUpperCase())
        AppRegistration appRegistration = AppRegistration.from(item)
        caches.allApplications.put(name, appRegistration)
        appRegistration
    }

    @Override
    ApplicationModificationResult createRegisteredApplication(UserContext userContext, String nameInput, String group,
                                                              String type, String description, String owner,
                                                              String email, MonitorBucketType monitorBucketType,
                                                              String tags) {
        String name = nameInput.toLowerCase()

        if (getRegisteredApplication(userContext, name)) {
            return new ApplicationModificationResult(
                successful: false,
                message: "Can't add Application ${name}. It already exists."
            )
        }

        String message = "Application '${name}' has been created."
        boolean successful = true

        String nowEpoch = new DateTime().millis as String
        Collection<ReplaceableAttribute> attributes = buildAttributesList(group, type, description, owner, email,
            monitorBucketType, tags, false)
        attributes << new ReplaceableAttribute('createTs', nowEpoch, false)
        String creationLogMessage = "Create registered app ${name}, type ${type}, owner ${owner}, email ${email}"
        taskService.runTask(userContext, creationLogMessage, { task ->
            try {
                awsSimpleDbService.save(domainName, name.toUpperCase(), attributes)
                getRegisteredApplication(userContext, name)
            } catch (AmazonServiceException e) {
                message = "Could not create Application, reason: ${e}"
            }
        }, Link.to(EntityType.application, name))

        return new ApplicationModificationResult(successful: successful, message: message)
    }

    private static Collection<ReplaceableAttribute> buildAttributesList(String group, String type, String description,
                                                                        String owner, String email,
                                                                        MonitorBucketType monitorBucketType,
                                                                        String tags, Boolean replaceExistingValues) {

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

    @Override
    ApplicationModificationResult updateRegisteredApplication(UserContext userContext, String name, String group,
                                                              String type, String desc, String owner, String email,
                                                              MonitorBucketType bucketType, String tags) {
        Collection<ReplaceableAttribute> attributes = buildAttributesList(group, type, desc, owner, email,
            bucketType, tags, true)

        taskService.runTask(userContext,
            "Update registered app ${name}, type ${type}, owner ${owner}, email ${email}", { task ->
            awsSimpleDbService.save(domainName, name.toUpperCase(), attributes)
            if (!tags) {
                awsSimpleDbService.delete(domainName, name.toUpperCase(), [new Attribute().withName('tags')])
            }
            getRegisteredApplication(userContext, name)
        }, Link.to(EntityType.application, name))

        return new ApplicationModificationResult(successful: true, message: "Application '${name}' has been updated.")
    }

    @Override
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
}
