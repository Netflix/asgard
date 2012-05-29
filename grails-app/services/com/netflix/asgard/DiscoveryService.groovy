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
    def restClientService
    def taskService

    /** Discovery isn't yet strong enough to handle sustained rapid fire requests. */
    final Integer MILLIS_DELAY_BETWEEN_DISCOVERY_CALLS = 700

    void initializeCaches() {
        caches.allApplicationInstances.ensureSetUp({ Region region -> retrieveInstances(region) })
    }

    /**
     * Allow 30 seconds for client to update its server cache from Discovery, another 30 seconds for the platform jar's
     * cache to expire, and another 30 seconds for GC pauses and unpleasant surprises. Total: 90 seconds.
     */
    final Duration timeToWaitAfterDiscoveryChange = Duration.standardSeconds(90)

    private String chooseHealthyDiscoveryServer(String hostName) {
        if (grailsApplication.config.server.online) {
            // Pick the first Discovery server that is healthy.
            InetAddress[] addresses = []
            try {
                addresses = InetAddress.getAllByName(hostName)
            } catch (UnknownHostException uhe) {
                emailerService.sendExceptionEmail("Unknown host ${hostName}", uhe)
            }
            for (InetAddress address in addresses) {
                // Check health of Discovery host
                String healthcheckUrl = "http://${address.canonicalHostName}:7001/discovery/healthcheck"
                if (restClientService.getResponseCode(healthcheckUrl) == 200) {
                    return address.canonicalHostName
                }
            }
        }
        null
    }

    String findBaseUrl(Region region, Boolean dynamic) {
        String hostName = configService.getRegionalDiscoveryServer(region)
        String serverName = dynamic ? chooseHealthyDiscoveryServer(hostName) : hostName
        serverName ? "http://${serverName}:7001/discovery" : null
    }

    String findBaseApiUrl(Region region, Boolean dynamic = true) {
        String baseUrl = findBaseUrl(region, dynamic)
        baseUrl ? "$baseUrl/v2" : null
    }

    private void handleConnectionError(Exception e, Region region, String url) {
        log.warn(e)
        String discoveryHostName = configService.getRegionalDiscoveryServer(region)
        String msg
        try {
            InetAddress address = InetAddress.getByName(discoveryHostName)
            InetAddress[] addresses = InetAddress.getAllByName(discoveryHostName)
            msg = "Can't connect to Discovery $region at $url using ${address.hostAddress} " +
                    "IP address. Found ${addresses.size()} address${addresses.size() == 1 ? '' : 'es'} for " +
                    "$discoveryHostName: "
            addresses.each { InetAddress addr ->
                msg += "{ ${addr.hostAddress} ${addr.canonicalHostName} } "
            }
        } catch (UnknownHostException uhe) {
            msg = "Unknown host ${discoveryHostName}: ${uhe}"
            log.warn(uhe)
        } catch (Exception errorLookingUpDiscovery) {
            msg = "Error looking up Discovery: ${errorLookingUpDiscovery}"
            log.warn(errorLookingUpDiscovery)
        }
        emailerService.sendExceptionEmail(msg, e)
    }

    // Discovery methods

    private List<ApplicationInstance> retrieveInstances(Region region) {
        List instances = []
        String baseApiUrl = findBaseApiUrl(region)
        if (baseApiUrl) {
            def xml = null
            String url = "$baseApiUrl/apps"
            try {
                xml = restClientService.getAsXml(url, 30 * 1000)
            } catch (Exception e) {
                handleConnectionError(e, region, url)
            }
            xml?.application?.each {
                instances += extractApplicationInstances(it)
            }
        }
        instances
    }

    private Collection<ApplicationInstance> extractApplicationInstances(GPathResult xml) {
        xml?.name ? xml.instance?.collect { new ApplicationInstance(it) } : []
    }

    Collection<ApplicationInstance> getAppInstances(UserContext userContext) {
        caches.allApplicationInstances.by(userContext.region)?.list() ?: []
    }

    /** Retrieves a list of 0 or more application instance objects that optionally match an app name. */
    List<ApplicationInstance> getAppInstances(UserContext userContext, String appName, From from = From.CACHE) {
        if (from == From.CACHE) {
            return getAppInstances(userContext).findAll { it.appName == appName }
        }
        List<ApplicationInstance> instances = []
        String baseUrl = findBaseApiUrl(userContext.region)
        if (baseUrl) {
            GPathResult xml = null
            String url = "$baseUrl/apps/${appName.toUpperCase()}"
            try {
                xml = restClientService.getAsXml(url)
            } catch (Exception e) {
                handleConnectionError(e, userContext.region, url)
            }
            if (xml) {
                instances = xml.instance.collect { new ApplicationInstance(it) }
                Map<String, ApplicationInstance> forCache = mapIdsToAppInstances(instances)
                caches.allApplicationInstances.by(userContext.region)?.putAll(forCache)
            }
        }
        instances
    }

    private Map<String, ApplicationInstance> mapIdsToAppInstances(Collection<ApplicationInstance> instances) {
        instances.inject([:]) { Map map, ApplicationInstance instance -> map << [(instance.hostName): instance] } as Map
    }

    /** Retrieves a list of 0 or more application instance objects that have an instance id from the specified list. */
    List<ApplicationInstance> getAppInstancesByIds(UserContext userContext, List<String> instanceIds) {
        getAppInstances(userContext).findAll { it.instanceId && instanceIds.contains(it.instanceId) }
    }

    /** Retrieves a single application instance object with a given app and host name. */
    ApplicationInstance getAppInstance(UserContext userContext, String appName, String hostName) {
        //long start = System.currentTimeMillis()
        ApplicationInstance appInst = null
        String baseUrl = findBaseApiUrl(userContext.region)
        if (baseUrl) {
            def xml = null
            String url = "$baseUrl/apps/${appName.toUpperCase()}/${hostName}"
            try {
                xml = restClientService.getAsXml(url)
            } catch (Exception e) {
                handleConnectionError(e, userContext.region, url)
            }
            if (xml) {
                appInst = new ApplicationInstance(xml)
            }
            if (appInst) {
                caches.allApplicationInstances.by(userContext.region).put(appInst.hostName, appInst)
            }
        }
        appInst
    }

    /** Retrieves & returns the application instance object with a given ec2 instanceId. */
    ApplicationInstance getAppInstance(UserContext userContext, String instanceId) {
        //long start = System.currentTimeMillis()
        ApplicationInstance appInst = null
        String baseUrl = findBaseApiUrl(userContext.region)
        if (baseUrl) {
            def xml = null
            String url = "$baseUrl/instances/${instanceId}"
            try {
                xml = restClientService.getAsXml(url)
            } catch (Exception e) {
                handleConnectionError(e, userContext.region, url)
            }
            appInst = xml ? new ApplicationInstance(xml) : null
            if (appInst) {
                caches.allApplicationInstances.by(userContext.region).put(appInst.hostName, appInst)
            }
        }
        appInst
    }

    /** Disables one or more application instances with a given app and host names by setting status to OUT_OF_SERVICE */
    void disableAppInstances(UserContext userContext, String appName, List<String> hostNames,
                             Task existingTask = null) {
        taskService.runTask(userContext, "Disable '${hostNames}' in Discovery", { task ->
            for (int i = 0 ; i < hostNames.size(); i++) {
                if (i > 0) { Time.sleepCancellably(150) } // Avoid denial of service attack on Discovery
                changeAppInstanceStatus(userContext, appName, hostNames[i], "OUT_OF_SERVICE")
                task.log("Disabled '${hostNames[i]}' in Discovery")
            }
        }, null, existingTask)
    }

    /** Enables one or more application instances with a given app and host names by setting status to UP */
    void enableAppInstances(UserContext userContext, String appName, List<String> hostNames, Task existingTask = null) {
        taskService.runTask(userContext, "Enable '${hostNames}' in Discovery", { task ->
            for (int i = 0 ; i < hostNames.size(); i++) {
                if (i > 0) { Time.sleepCancellably(150) } // Avoid denial of service attack on Discovery
                changeAppInstanceStatus(userContext, appName, hostNames[i], "UP")
                task.log("Enabled '${hostNames[i]}' in Discovery")
            }
        }, null, existingTask)
    }

    private void changeAppInstanceStatus(UserContext userContext, String appName, String hostName, String status) {
        String baseUrl = findBaseApiUrl(userContext.region)
        if (baseUrl) {
            String url = "$baseUrl/apps/${appName.toUpperCase()}/${hostName}/status?value=${status}"
            log.debug(url)
            restClientService.put(url)
        }
    }
}
