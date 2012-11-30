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

/**
 * Service for interacting with Cloudready and telling Chaos Monkey who she can kill.
 */
class CloudReadyService {

    static transactional = false

    def configService
    def restClientService

    /**
     * Constructs the Cloud Ready URL for editing the Chaos Monkey opt in for an application.
     *
     * @param region aws region
     * @param applicationName name of application
     * @return link to Cloud Ready
     */
    String constructChaosMonkeyEditLink(Region region, String applicationName) {
        "${configService.cloudReadyUrl}/chaosmonkey/edit/${applicationName}?region=${region.code}"
    }

    /**
     * Retrieves Chaos Monkey status for Application from Cloudready.
     *
     * @param applicationName name of application
     * @return 'enabled', 'disabled', or 'unknown' if Cloudready was unavailable
     */
    String chaosMonkeyStatusForApplication(String applicationName) {
        String url = "${configService.cloudReadyUrl}/chaosmonkey/app/${applicationName}?format=json"
        chaosMonkeyStatusForUrl(url)
    }

    /**
     * Retrieves Chaos Monkey status for Cluster from Cloudready.
     *
     * @param region aws region
     * @param clusterName name of cluster
     * @return 'enabled', 'disabled', or 'unknown' if Cloudready was unavailable
     */
    String chaosMonkeyStatusForCluster(Region region, String clusterName) {
        String cloudReadyUrl = configService.cloudReadyUrl
        String url = "${cloudReadyUrl}/chaosmonkey/cluster/${clusterName}?format=json&region=${region.code}"
        chaosMonkeyStatusForUrl(url)
    }

    private String chaosMonkeyStatusForUrl(String url) {
        def response = restClientService.getAsJson(url)
        if (response == null) {
            return 'unknown'
        }
        boolean enabled = Boolean.valueOf(response.enabled as String)
        enabled ? 'enabled' : 'disabled'
    }

    /**
     * Enables Chaos Monkey for application at an application opt level.
     *
     * @param applicationName name of application
     * @return request success indication
     */
    boolean enableChaosMonkeyForApplication(String applicationName) {
        String url = "${configService.cloudReadyUrl}/chaosmonkey/enableapp/${applicationName}?format=json"
        def response = restClientService.getAsJson(url)
        response != null
    }

    /**
     * Disables Chaos Monkey for application
     *
     * @param applicationName name of application
     * @return request success indication
     */
    boolean disableChaosMonkeyForApplication(String applicationName) {
        String url = "${configService.cloudReadyUrl}/chaosmonkey/disableapp/${applicationName}?format=json"
        def response = restClientService.getAsJson(url)
        response != null
    }

    /**
     * Enables Chaos Monkey for cluster at a cluster opt level.
     *
     * @param region aws region
     * @param clusterName name of cluster
     * @return request success indication
     */
    boolean enableChaosMonkeyForCluster(Region region, String clusterName) {
        String cloudReadyUrl = configService.cloudReadyUrl
        String url = "${cloudReadyUrl}/chaosmonkey/enablecluster/${clusterName}?format=json&region=${region.code}"
        def response = restClientService.getAsJson(url)
        response != null
    }

    /**
     * Disables Chaos Monkey for cluster
     *
     * @param region aws region
     * @param clusterName name of cluster
     * @return request success indication
     */
    boolean disableChaosMonkeyForCluster(Region region, String clusterName) {
        String cloudReadyUrl = configService.cloudReadyUrl
        String url = "${cloudReadyUrl}/chaosmonkey/disablecluster/${clusterName}?format=json&region=${region.code}"
        def response = restClientService.getAsJson(url)
        response != null
    }

    /**
     * Retrieves applications that have a specified opt level.
     *
     * @param optLevel retrieve application names with this optlevel
     * @return application names or null if Cloudready was unavailable
     * @throws ServiceUnavailableException if Cloudready could not be reached
     */
    Set<String> applicationsWithOptLevel(String optLevel) throws ServiceUnavailableException {
        String cloudReadyUrl = configService.cloudReadyUrl
        String url = "${cloudReadyUrl}/chaosmonkey/listApps?format=json&optLevel=${optLevel}"
        def response = restClientService.getAsJson(url)
        if (response == null) {
            throw new ServiceUnavailableException('Cloudready')
        }
        response.applications*.'name' as Set
    }

    /**
     * Determines if Chaos Monkey is used for a particular Asgard instance.
     *
     * @param region optionally check in a specific region (needed for Clusters)
     * @return indication that Chaos Monkey is in the area
     */
    boolean isChaosMonkeyActive(Region region = null) {
        configService.cloudReadyUrl && (!region || region in configService.chaosMonkeyRegions)
    }
}
