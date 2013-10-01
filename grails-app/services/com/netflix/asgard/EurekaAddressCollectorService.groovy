/*
 * Copyright 2013 Netflix, Inc.
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
import org.apache.http.HttpStatus

/**
 * Service for collecting all the addresses of the most useful Eureka instances.
 */
class EurekaAddressCollectorService implements CacheInitializer {

    Caches caches
    def configService
    def dnsService
    def restClientService

    void initializeCaches() {
        caches.allEurekaAddresses.ensureSetUp({ Region region -> lookUpBestEurekaAddresses(region) })
    }

    /**
     * Invokes the cache-filling process for a specific region on demand, for times when the current cached set of
     * Eureka addresses in that region seems to be having problems.
     *
     * @param region the region whose cached Eureka addresses should be replaced
     */
    void fillCache(Region region) {
        caches.allEurekaAddresses.by(region).fill()
    }

    /**
     * Looks up all the Eureka addresses for the current region from DNS, then determines the best current nodes to use,
     * preferring healthy nodes if present, or responsive nodes if none are healthy, or all nodes if none are
     * responsive.
     *
     * @param region the cloud region for which to find Eureka nodes
     * @return set of the current best unique addresses for Eureka nodes
     */
    Collection<String> lookUpBestEurekaAddresses(Region region) {
        chooseBestEurekaNodes(lookUpEurekaAddresses(region))
    }

    /**
     * Gets the canonical server name for Eureka in the specified region.
     *
     * @param region the cloud region for which to find Eureka nodes
     * @return the canonical server name for Eureka in the specified region, or null if none is configured
     */
    String getEurekaCanonicalServerName(Region region) {
        configService.getRegionalDiscoveryServer(region)
    }

    /**
     * Repeatedly looks up the addresses for the Eureka nodes for the specified region. Assumes DNS is configured to
     * return only a random subset of suitable Eureka nodes on each call.
     *
     * @param region the cloud region for which to find Eureka nodes
     * @return set of unique addresses for Eureka nodes
     */
    Collection<String> lookUpEurekaAddresses(Region region) {
        String hostName = getEurekaCanonicalServerName(region)
        if (!hostName) {
            return []
        }
        Integer maxConsecutiveDnsLookupsWithoutNewResult = configService.maxConsecutiveDnsLookupsWithoutNewResult
        Integer dnsThrottleMillis = configService.dnsThrottleMillis
        Set<String> allAddressesSoFar = [] as Set
        Integer lookupsSinceLastUniqueResult = 0
        Boolean firstTime = true
        while (lookupsSinceLastUniqueResult <= maxConsecutiveDnsLookupsWithoutNewResult) {
            if (!firstTime) {
                Time.sleepCancellably(dnsThrottleMillis)
            }
            firstTime = false
            Collection<String> lastResults = dnsService.getCanonicalHostNamesForDnsName(hostName)

            // If the result set contains any new results, reset the counter so we'll keep trying longer.
            Collection<String> newAddresses = lastResults - allAddressesSoFar
            if (newAddresses) {
                lookupsSinceLastUniqueResult = 0
            } else {
                lookupsSinceLastUniqueResult++
            }

            allAddressesSoFar.addAll(lastResults)
        }
        allAddressesSoFar
    }

    /**
     * This method attempts to omit unhealthy Eureka from the set provided by DNS, without accidentally reducing the set
     * of usable addresses to zero. It's better to be able to call some intermittently problematic Eureka nodes than to
     * call none at all, but it's better to focus on the healthy nodes if only a subset are having problems.
     *
     * @param allAddresses all the addresses of Eureka nodes gathered from DNS
     * @return a subset of addresses that are the healthiest or most responsive of the addresses passed in
     */
    Collection<String> chooseBestEurekaNodes(Collection<String> allAddresses) {

        // Check health of Eureka nodes
        Map<String, Integer> addressesToResponseCodes = allAddresses.collectEntries {
            [(it): restClientService.getRepeatedResponseCode(buildHealthCheckUrl(it))]
        }

        // If there are any healthy nodes, use them.
        Set<String> healthyAddresses = addressesToResponseCodes.findAll { a, code -> code == HttpStatus.SC_OK }.keySet()
        if (healthyAddresses) {
            return healthyAddresses
        }

        // If there are no healthy nodes, but there are responsive yet "unhealthy" nodes, settle for those.
        Set<String> responsiveAddresses = addressesToResponseCodes.findAll { a, code -> code != null }.keySet()
        if (responsiveAddresses) {
            return responsiveAddresses
        }

        // If there are no responsive nodes, then just return all the nodes.
        allAddresses
    }

    /**
     * Finds the address of any one of the "best" Eureka nodes, preferring a healthy node, or a responsive node, or any
     * listed node, in that order of preference.
     *
     * @param allAddresses the addresses to look through for a single address to use
     * @return the address of the Eureka node to use
     */
    String chooseBestEurekaNode(Collection<String> allAddresses) {
        if (!allAddresses) {
            return null
        }
        List<String> addressesCopy = Lists.newArrayList(allAddresses)
        // Eureka is designed to run with very few nodes, so shuffling is a reasonable way to randomize and iterate.
        Collections.shuffle(addressesCopy)
        List<String> unhealthyButResponsiveAddresses = []
        for (String address in addressesCopy) {
            Integer responseCode = restClientService.getRepeatedResponseCode(buildHealthCheckUrl(address))
            if (restClientService.checkOkayResponseCode(responseCode)) {
                return address
            } else if (responseCode != null) {
                unhealthyButResponsiveAddresses << address
            }
        }
        // If we got here, no addresses were healthy. Use one that was responsive, if available.
        if (unhealthyButResponsiveAddresses) {
            return unhealthyButResponsiveAddresses[1]
        }
        addressesCopy[1]
    }

    /**
     * Constructs a base URL for a Eureka node, including scheme, server, port, and root context
     *
     * @param serverAddress the specific address of a Eureka server, possibly an IP address (must not be empty)
     * @return the URL of the root context of the specified Eureka server
     */
    String buildBaseUrl(String serverAddress) {
        Check.notEmpty(serverAddress, 'Eureka server address')
        "http://${serverAddress}:${configService.eurekaPort}/${configService.eurekaUrlContext}"
    }

    private String buildHealthCheckUrl(String serverAddress) {
        Check.notEmpty(serverAddress, 'Eureka server address')
        "${buildBaseUrl(serverAddress)}/healthcheck"
    }
}

