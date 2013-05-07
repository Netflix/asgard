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

import com.google.common.collect.Lists
import com.netflix.asgard.cache.CacheInitializer
import com.netflix.asgard.model.ApplicationInstance
import groovy.util.slurpersupport.GPathResult
import org.joda.time.Duration

class DiscoveryService implements CacheInitializer {

    static transactional = false

    def grailsApplication  // injected after construction
    Caches caches
    def configService
    def emailerService
    def eurekaAddressCollectorService
    def restClientService
    def taskService

    /** Discovery isn't yet strong enough to handle sustained rapid fire requests. */
    final Integer MILLIS_DELAY_BETWEEN_DISCOVERY_CALLS = 700

    void initializeCaches() {
        caches.allApplicationInstances.ensureSetUp({ Region region -> retrieveInstances(region) }, {},
                { Region region -> caches.allEurekaAddresses.by(region).filled }
        )
    }

    /**
     * Allow 30 seconds for client to update its server cache from Discovery, another 30 seconds for the platform jar's
     * cache to expire, and another 30 seconds for GC pauses and unpleasant surprises. Total: 90 seconds.
     */
    final Duration timeToWaitAfterDiscoveryChange = Duration.standardSeconds(90)

    /**
     * Cached addresses were the best choices when the Eureka address cache loaded recently. Find one that is currently
     * healthy, if possible. Then create a base URL for that
     *
     * @param region the region for which to look for Eureka nodes
     * @return the base URL of a healthy Eureka node
     */
    String findBaseUrl(Region region) {
        List<String> eurekaAddresses = Lists.newArrayList(caches.allEurekaAddresses.by(region).list())
        Collections.shuffle(eurekaAddresses)
        String hostName = eurekaAddressCollectorService.chooseBestEurekaNode(eurekaAddresses)
        hostName ? "http://${hostName}:${configService.eurekaPort}/${configService.eurekaUrlContext}" : null
    }

    String findBaseApiUrl(Region region) {
        String baseUrl = findBaseUrl(region)
        baseUrl ? "$baseUrl/v2" : null
    }

    private void handleEurekaConnectionError(Exception e, Region region) {
        log.warn("Refreshing Eureka addresses after failure to connect to Eureka in ${region}: ${e}")
        eurekaAddressCollectorService.fillCache(region)
    }

    // Discovery methods

    private List<ApplicationInstance> retrieveInstances(Region region) {
        new Retriable<List<ApplicationInstance>>(
                work: {
                    List instances = []
                    String baseApiUrl = findBaseApiUrl(region)
                    if (baseApiUrl) {
                        def xml = restClientService.getAsXml("$baseApiUrl/apps", 30 * 1000)
                        xml?.application?.each {
                            instances += extractApplicationInstances(it)
                        }
                    }
                    instances
                },
                handleException: { Exception e, int numberOfFailedAttemptsSoFar ->
                    handleEurekaConnectionError(e, region)
                }
        ).performWithRetries()
    }

    private Collection<ApplicationInstance> extractApplicationInstances(GPathResult xml) {
        //noinspection GroovyAccessibility
        xml?.name ? xml.instance?.collect { new ApplicationInstance(it) } : []
    }

    Collection<ApplicationInstance> getAppInstances(UserContext userContext) {
        caches.allApplicationInstances.by(userContext.region)?.list() ?: []
    }

    /** Retrieves a list of 0 or more application instance objects that optionally match an app name. */
    List<ApplicationInstance> getAppInstances(UserContext userContext, String appName) {
        getAppInstances(userContext).findAll { it.appName == appName }
    }

    /** Retrieves a list of 0 or more application instance objects that have an instance id from the specified list. */
    List<ApplicationInstance> getAppInstancesByIds(UserContext userContext, List<String> instanceIds) {
        getAppInstances(userContext).findAll { it.instanceId && instanceIds.contains(it.instanceId) }
    }

    /** Retrieves a single application instance object with a given app and host name. */
    ApplicationInstance getAppInstance(UserContext userContext, String appName, String hostName) {
        Region region = userContext.region
        new Retriable<ApplicationInstance>(
                work: {
                    ApplicationInstance appInst = null
                    String baseUrl = findBaseApiUrl(region)
                    if (baseUrl) {
                        String url = "$baseUrl/apps/${appName.toUpperCase()}/${hostName}"
                        log.debug(url)
                        def xml = restClientService.getAsXml(url)
                        if (xml) {
                            appInst = new ApplicationInstance(xml)
                        }
                        if (appInst) {
                            caches.allApplicationInstances.by(userContext.region).put(appInst.hostName, appInst)
                        }
                    }
                    appInst
                },
                handleException: { Exception e, int numberOfFailedAttemptsSoFar ->
                    handleEurekaConnectionError(e, region)
                }
        ).performWithRetries()
    }

    /** Retrieves & returns the application instance object with a given ec2 instanceId. */
    ApplicationInstance getAppInstance(UserContext userContext, String instanceId) {
        Region region = userContext.region
        new Retriable<ApplicationInstance>(
                work: {
                    ApplicationInstance appInst = null
                    String baseUrl = findBaseApiUrl(region)
                    if (baseUrl) {
                        String url = "$baseUrl/instances/${instanceId}"
                        log.debug(url)
                        def xml = restClientService.getAsXml(url)
                        appInst = xml ? new ApplicationInstance(xml) : null
                        if (appInst) {
                            caches.allApplicationInstances.by(userContext.region).put(appInst.hostName, appInst)
                        }
                    }
                    appInst
                },
                handleException: { Exception e, int numberOfFailedAttemptsSoFar ->
                    handleEurekaConnectionError(e, region)
                }
        ).performWithRetries()
    }

    /** Disables one or more application instances with a given app and host names by setting status to OUT_OF_SERVICE */
    void disableAppInstances(UserContext userContext, String appName, List<String> hostNames,
                             Task existingTask = null) {
        taskService.runTask(userContext, "Disable '${hostNames}' in Eureka", { task ->
            for (int i = 0 ; i < hostNames.size(); i++) {
                if (i > 0) { Time.sleepCancellably(150) } // Avoid denial of service attack on Discovery
                changeAppInstanceStatus(userContext, appName, hostNames[i], "OUT_OF_SERVICE")
                task.log("Disabled '${hostNames[i]}' in Eureka")
            }
        }, null, existingTask)
    }

    /** Enables one or more application instances with a given app and host names by setting status to UP */
    void enableAppInstances(UserContext userContext, String appName, List<String> hostNames, Task existingTask = null) {
        taskService.runTask(userContext, "Enable '${hostNames}' in Eureka", { task ->
            for (int i = 0 ; i < hostNames.size(); i++) {
                if (i > 0) { Time.sleepCancellably(150) } // Avoid denial of service attack on Discovery
                changeAppInstanceStatus(userContext, appName, hostNames[i], "UP")
                task.log("Enabled '${hostNames[i]}' in Eureka")
            }
        }, null, existingTask)
    }

    private void changeAppInstanceStatus(UserContext userContext, String appName, String hostName, String status) {
        Region region = userContext.region
        new Retriable<ApplicationInstance>(
                work: {
                    String baseUrl = findBaseApiUrl(region)
                    if (baseUrl) {
                        String url = "$baseUrl/apps/${appName.toUpperCase()}/${hostName}/status?value=${status}"
                        log.debug("PUT ${url}")
                        restClientService.put(url)
                    }
                },
                handleException: { Exception e, int numberOfFailedAttemptsSoFar ->
                    handleEurekaConnectionError(e, region)
                }
        ).performWithRetries()
    }
}
