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

import com.netflix.asgard.model.InstanceTypeData
import com.netflix.asgard.text.TextLinkTemplate

/**
 * Type-checked configuration access with intelligent defaults.
 */
class ConfigService {

    static transactional = false

    def grailsApplication

    String getDefaultMetricNamespace() {
        grailsApplication.config.cloud?.defaultMetricNamespace ?: 'AWS/EC2'
    }

    String getAwsAccountNumber() {
        grailsApplication.config?.grails?.awsAccounts[0]
    }

    /**
     * Gets the mapping of all relevant account numbers to account names.
     *
     * @return Map <String, String> account numbers to account names
     */
    Map<String, String> getAwsAccountNames() {
        grailsApplication.config?.grails?.awsAccountNames ?: [:]
    }

    String getRegionalDiscoveryServer(Region region) {
        Map<Region, String> regionsToDiscoveryServers = grailsApplication.config.eureka?.regionsToServers
        regionsToDiscoveryServers ? regionsToDiscoveryServers[region] : null
    }

    boolean doesRegionalDiscoveryExist(Region region) {
        getRegionalDiscoveryServer(region) ? true : false
    }

    String getTicketLabel() {
        grailsApplication.config?.ticket?.label ?: 'Ticket'
    }

    String getFullTicketLabel() {
        grailsApplication.config.ticket?.fullLabel ?: 'Ticket'
    }

    List<String> getPromotionTargetServers() {
        grailsApplication.config.promote?.targetServers ?: []
    }

    Collection<String> getExcludedLaunchPermissionsForMassDelete() {
        List<String> excludedLaunchPermissions = grailsApplication.config.cloud?.massDeleteExcludedLaunchPermissions
        Map matches = grailsApplication.config.grails.awsAccountNames.findAll { k, v ->
            excludedLaunchPermissions?.contains(v)
        } as Map
        matches.keySet()
    }

    String getAsgardHome() {
        grailsApplication.config?.asgardHome
    }

    boolean isAppConfigured() {
        grailsApplication.config.appConfigured
    }

    List<Map<String,String>> getExternalLinks() {
        grailsApplication.config.link?.externalLinks?.sort { it.text } ?: []
    }

    String getAlertingServiceConfigUrl() {
        grailsApplication.config.cloud?.alertingServiceConfigUrl ?: null
    }

    /**
     * @return the message to show users after they create an elastic load balancer
     */
    String getPostElbCreationMessage() {
        grailsApplication.config.cloud?.postElbCreateMessage ?:
                'Contact your cloud admin to enable security group ingress permissions from elastic load balancers.'
    }

    List<Region> getPlatformServiceRegions() {
        List<Region> activeRegions = Region.limitedRegions ?: Region.values()
        List<Region> platformServiceRegions = grailsApplication.config?.cloud?.platformserviceRegions ?: []
        platformServiceRegions.intersect(activeRegions)
    }

    boolean getUseJitter() {
        Boolean result = grailsApplication.config?.thread?.useJitter
        if (result == null) {
            return true
        }
        result
    }

    /**
     * Gets the instance types that Asgard needs to use that are not included in the AWS Java SDK enum
     *
     * @return List <InstanceTypeData> the custom instance types, or an empty list
     */
    List<InstanceTypeData> getCustomInstanceTypes() {
        grailsApplication.config?.cloud?.customIntanceTypes ?: []
    }

    List<String> getAwsAccounts() {
        grailsApplication.config?.grails?.awsAccounts ?: []
    }

    /**
     * Gets the optional list of accounts whose public resources such as AMIs should be used.
     *
     * @return List < String > account numbers/names, or empty list if not configured
     */
    List<String> getPublicResourceAccounts() {
        grailsApplication.config?.cloud?.publicResourceAccounts ?: []
    }

    List<String> getDiscouragedAvailabilityZones() {
        grailsApplication.config?.cloud?.discouragedAvailabilityZones ?: []
    }

    Map<String, List<TextLinkTemplate>> getInstanceLinkGroupingsToLinkTemplateLists() {
        grailsApplication.config.link?.instanceLinkGroupingsToLinkTemplateLists ?: [:]
    }

    String getDefaultKeyName() {
        grailsApplication.config?.cloud?.defaultKeyName ?: ''
    }

    /**
     * @return Map of a property name in {@link Caches} to minimum size for that cache to be considered 'healthy'
     */
    Map<String, Integer> getHealthCheckMinimumCounts() {
        grailsApplication.config.healthCheck?.minimumCounts ?: [:]
    }

    boolean isOnline() {
        grailsApplication.config.server.online
    }

    String getAccessId() {
        grailsApplication.config.secret?.accessId ?: null
    }

    String getSecretKey() {
        grailsApplication.config.secret?.secretKey ?: null
    }

    String getAccessIdFileName() {
        grailsApplication.config.secret?.accessIdFileName ?: null
    }

    String getSecretKeyFileName() {
        grailsApplication.config.secret?.secretKeyFileName ?: null
    }

    String getLoadBalancerUsernameFile() {
        grailsApplication.config.secret?.loadBalancerUsernameFile ?: null
    }

    String getLoadBalancerPasswordFile() {
        grailsApplication.config.secret?.loadBalancerPasswordFile ?: null
    }

    String getSecretLocalDirectory() {
        grailsApplication.config.secret?.localDirectory ?: null
    }

    String getSecretRemoteUser() {
        grailsApplication.config.secret?.remoteUser ?: null
    }

    String getSecretRemoteServer() {
        grailsApplication.config.secret?.remoteServer ?: null
    }

    String getSecretRemoteDirectory() {
        grailsApplication.config.secret?.remoteDirectory ?: null
    }

    String getAccountName() {
        grailsApplication.config.cloud?.accountName ?: null
    }

    String getEnvStyle() {
        grailsApplication.config.cloud?.envStyle ?: ''
    }

    String getApplicationsDomain() {
        grailsApplication.config.cloud?.applicationsDomain ?: 'CLOUD_APPLICATIONS'
    }

    String getUserDataVarPrefix() {
        grailsApplication.config.cloud?.userDataVarPrefix ?: 'CLOUD_'
    }

    /**
     * @return Name of the plugins to the implementing beans, ex. [userDataProvider: 'perforceUserDataProvider']
     */
    Map<String, Object> getPluginNamesToBeanNames() {
        grailsApplication.config.plugin ?: [:]
    }

    /**
     * @return Region indicating where the SNS topic for task finished notifications resides
     */
    Region getTaskFinishedSnsTopicRegion() {
        grailsApplication.config.sns?.taskFinished?.region ?: null
    }

    /**
     * @return SNS Topic name of where to send task finished notifications
     */
    String getTaskFinishedSnsTopicName() {
        grailsApplication.config.sns?.taskFinished?.topicName ?: null
    }

    /**
     * @return Maximum time in miliseconds for threads to wait for a connection from the http connection pool
     */
    long getHttpConnPoolTimeout() {
        grailsApplication.config.httpConnPool?.timeout ?: 50 * 1000
    }

    /**
     * @return Maximum size of the http connection pool
     */
    int getHttpConnPoolMaxSize() {
        grailsApplication.config.httpConnPool?.maxSize ?: 50
    }

    /**
     * @return Maximum number of connections in the connection pool per host.
     */
    int getHttpConnPoolMaxForRoute() {
        grailsApplication.config.httpConnPool?.maxSize ?: 5
    }

    /**
     * Gets the context string used in constructing Eureka URLs. The context varies depending on how Eureka is
     * configured.
     *
     * @return the context string for constructing URLs to make eureka calls
     */
    String getEurekaUrlContext() {
        grailsApplication.config.eureka?.urlContext ?: 'eureka'
    }

    /**
     * Gets the port number (as a String) used in constructing Eureka URLs. The port varies depending on how Eureka
     * or is configured.
     *
     * @return the port for constructing URLs to make eureka calls
     */
    String getEurekaPort() {
        grailsApplication.config.eureka?.port ?: '80'
    }

    /**
     * @return Default Security Groups.
     */
    List<String> getDefaultSecurityGroups() {
        grailsApplication.config.cloud?.defaultSecurityGroups ?: []
    }

    /**
     * @return Default VPC Security Groups.
     */
    List<String> getDefaultVpcSecurityGroupNames() {
        grailsApplication.config.cloud?.defaultVpcSecurityGroupNames ?: []
    }

    /**
     * @return subnet purposes that should have an internal scheme for ELBs.
     */
    List<String> getInternalSubnetPurposes() {
        grailsApplication.config.cloud?.internalSubnetPurposes ?: ['internal']
    }
    
    /**
     * @return Amount of time to wait between AWS calls.
     */
    int getCloudThrottle() {
        grailsApplication.config.cloud?.throttleMillis ?: 250
    }
}
