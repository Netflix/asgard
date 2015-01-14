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
package com.netflix.asgard.applications

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
import com.netflix.spinnaker.client.Spinnaker
import com.netflix.spinnaker.client.model.Application
import com.netflix.spinnaker.client.model.TaskExecutionException
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired

class SpinnakerApplicationService extends AbstractApplicationService implements CacheInitializer {
    private static final log = LogFactory.getLog(this)

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

    Spinnaker spinnaker
    String accountName

    SpinnakerApplicationService(String spinnakerUrl, String accountName) {
        this(Spinnaker.using(spinnakerUrl), accountName)
    }

    private SpinnakerApplicationService(Spinnaker spinnaker, String accountName) {
        log.info("Initializing SpinnakerApplicationService (account: ${accountName})")

        this.spinnaker = spinnaker
        this.accountName = accountName
    }

    void initializeCaches() {
        caches.allApplications.ensureSetUp({ retrieveApplications() })
    }

    private Collection<AppRegistration> retrieveApplications() {
        log.info("Retrieving all applications for account '${accountName}'")
        spinnaker.applications().collect { convertToAppRegistration(it) }.findAll { it != null }
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

        AppRegistration appRegistration = convertToAppRegistration(spinnaker.application(name.toUpperCase()))
        if (appRegistration) {
            caches.allApplications.put(name, appRegistration)
        } else {
            caches.allApplications.remove(name)
        }

        appRegistration
    }

    @Override
    ApplicationModificationResult createRegisteredApplication(UserContext userContext, String nameInput, String group,
                                                              String type, String description, String applicationOwner,
                                                              String email, MonitorBucketType monitorBucketType,
                                                              String tags) {
        String name = nameInput.toLowerCase()
        log.info("Creating Application (name: '${name}')")

        if (getRegisteredApplication(userContext, name)) {
            return new ApplicationModificationResult(
                successful: false,
                message: "Can't add Application ${name}. It already exists."
            )
        }

        String message = "Application '${name}' has been created."
        boolean successful = true

        taskService.runTask(
            userContext,
            "Create registered app ${name}, type ${type}, owner ${applicationOwner}, email ${email}",
            { task ->
                try {
                    def tagSet = (tags ?: "").split(",").collect { it.trim().toLowerCase() } as Set
                    spinnaker.operations().application()
                        .withAccount(accountName)
                        .withName(name)
                        .withGroup(group)
                        .withType(type)
                        .withDescription(description)
                        .withOwner(applicationOwner)
                        .withEmail(email)
                        .withMonitorBucketType(monitorBucketType.name())
                        .withTags(tagSet)
                        .saveAndGet()
                    getRegisteredApplication(userContext, name)
                } catch (TaskExecutionException e) {
                    successful = false
                    message = e.getMessage()
                }
            },
            Link.to(EntityType.application, name)
        )

        return new ApplicationModificationResult(successful: successful, message: message)
    }

    @Override
    ApplicationModificationResult updateRegisteredApplication(UserContext userContext, String name, String group,
                                                              String type, String desc, String applicationOwner,
                                                              String email, MonitorBucketType bucketType,
                                                              String tags) {
        String message = "Application '${name}' has been updated."
        boolean successful = true

        taskService.runTask(
            userContext,
            "Update registered app ${name}, type ${type}, owner ${applicationOwner}, email ${email}",
            { task ->
                try {
                    def tagSet = (tags ?: "").split(",").collect { it.trim().toLowerCase() } as Set
                    spinnaker.application(name.toUpperCase())
                        .withAccount(accountName)
                        .withName(name)
                        .withGroup(group)
                        .withType(type)
                        .withDescription(desc)
                        .withOwner(applicationOwner)
                        .withEmail(email)
                        .withMonitorBucketType(bucketType.name())
                        .withTags(tagSet)
                        .saveAndGet()
                    getRegisteredApplication(userContext, name)
                } catch (TaskExecutionException e) {
                    successful = false
                    message = e.getMessage()
                }
            },
            Link.to(EntityType.application, name)
        )

        return new ApplicationModificationResult(successful: successful, message: message)
    }

    @Override
    void deleteRegisteredApplication(UserContext userContext, String name) {
        Check.notEmpty(name, "name")
        validateDelete(userContext, name)
        taskService.runTask(userContext, "Delete registered app ${name}", { task ->
            spinnaker.application(name)
                .withAccount(accountName)
                .deleteAndGet()
            getRegisteredApplication(userContext, name)
        }, Link.to(EntityType.application, name))
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

    private AppRegistration convertToAppRegistration(Application application) {
        if (application?.metadata) {
            def metadata = application.metadata
            if (metadata.accounts.contains(accountName)) {
                def monitorBucketType = MonitorBucketType.byName(metadata.monitorBucketType)
                Map<String, String> additionalAttributes = [:]
                if (metadata.pagerDuty) {
                    additionalAttributes.pdApiKey = metadata.pagerDuty
                }
                return new AppRegistration(
                    name: metadata.name?.toLowerCase(),
                    group: metadata.group,
                    type: metadata.type,
                    description: metadata.description,
                    owner: metadata.owner,
                    email: metadata.email,
                    createTime: metadata.created,
                    updateTime: metadata.updated,
                    monitorBucketType: monitorBucketType ?: MonitorBucketType.defaultForOldApps,
                    tags: metadata.tags as List,
                    additionalAttributes: additionalAttributes
                )
            }
        }

        return null
    }
}
