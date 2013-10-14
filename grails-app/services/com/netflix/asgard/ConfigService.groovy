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
import com.netflix.asgard.server.Environment
import com.netflix.asgard.text.TextLinkTemplate

/**
 * Type-checked configuration access with intelligent defaults.
 */
class ConfigService {

    static transactional = false

    def grailsApplication
    def flagService

    /**
     * Gets the most commonly used namespace for Amazon CloudWatch metrics used for auto scaling policies. If not
     * configured, this method returns the AWS standard namespace "AWS/EC2".
     *
     * @return the default namespace for choosing a CloudWatch metric for a scaling policy
     */
    String getDefaultMetricNamespace() {
        grailsApplication.config.cloud?.defaultMetricNamespace ?: 'AWS/EC2'
    }

    /**
     * @return custom (non AWS) metric namespaces mapped to the dimensions they support
     */
    Map<String, Collection<String>> customMetricNamespacesToDimensions() {
        grailsApplication.config.cloud?.customMetricNamespacesToDimensions ?: [:]
    }

    /**
     * @return true if emails are enabled for sending system errors to Asgard admins, false otherwise
     */
    boolean isSystemEmailEnabled() {
        grailsApplication.config.email.systemEnabled
    }

    /**
     * @return true if emails are enabled for sending notifications to app owners about cloud changes, false otherwise
     */
    boolean isUserEmailEnabled() {
        grailsApplication.config.email.userEnabled
    }

    /**
     * @return the Simple Mail Transport Protocol (SMTP) host that should be used for sending emails
     */
    String getSmtpHost() {
        grailsApplication.config.email.smtpHost ?: null
    }

    /**
     * @return the "from" address for sending user emails
     */
    String getFromAddressForEmail() {
        grailsApplication.config.email.fromAddress ?: null
    }

    /**
     * @return the email address that should receive system-level error email alerts
     */
    String getSystemEmailAddress() {
        grailsApplication.config.email.systemEmailAddress ?: null
    }

    /**
     * @return the common beginning of all system error email subjects
     */
    String getErrorEmailSubjectStart() {
        grailsApplication.config.email.errorSubjectStart ?: null
    }

    /**
     * Gets the Amazon Web Services account number for the current environment. This must be the first account number
     * string in the awsAccounts list in Config.groovy.
     *
     * @return the AWS account number for the current environment
     */
    String getAwsAccountNumber() {
        grailsApplication.config.grails?.awsAccounts[0]
    }

    /**
     * Gets the mapping of all relevant account numbers to account names.
     *
     * @return Map <String, String> account numbers to account names
     */
    Map<String, String> getAwsAccountNames() {
        grailsApplication.config.grails?.awsAccountNames ?: [:]
    }

    /**
     * Gets the maximum number of times to perform a DNS lookup without receiving a new result. This is useful to tune
     * when there is a service dependency like Eureka that may have DNS configuration only returning one random IP
     * address for each individual DNS lookup. Depending on the number of expected IP addresses, some Asgard
     * installations may be better off with a higher or lower number of DNS attempts before giving up and accepting
     * the currently gathered set of IP addresses.
     *
     * @return the maximum number of times to perform a DNS lookup without receiving a new result
     */
    Integer getMaxConsecutiveDnsLookupsWithoutNewResult() {
        grailsApplication.config.dns?.maxConsecutiveDnsLookupsWithoutNewResult ?: 10
    }

    /**
     * @return number of milliseconds to wait between DNS calls
     */
    int getDnsThrottleMillis() {
        grailsApplication.config.dns?.throttleMillis ?: 50
    }

    /**
     * Finds the Discovery server URL for the specified region, or null if there isn't one
     *
     * @param region the region in which to look for a Discovery URL
     * @return the Discovery server URL for the specified region, or null if there isn't one
     */
    String getRegionalDiscoveryServer(Region region) {
        if (isOnline()) {
            Map<Region, String> regionsToDiscoveryServers = grailsApplication.config.eureka?.regionsToServers
            return regionsToDiscoveryServers ? regionsToDiscoveryServers[region] : null
        }
        null
    }

    /**
     * Finds the PlatformService server URL for the specified region, or null if there isn't one
     *
     * @param region the region in which to look for a PlatformService URL
     * @return the PlatformService server URL for the specified region, or null if there isn't one
     */
    String getRegionalPlatformServiceServer(Region region) {
        if (isOnline()) {
            Map<Region, String> regionsToPlatformServiceServers = grailsApplication.config.platform?.regionsToServers
            return regionsToPlatformServiceServers ? regionsToPlatformServiceServers[region] : null
        }
        null
    }

    /**
     * Checks whether a Discovery URL is known for a specified region
     *
     * @param region the region in which to check for the existence of a Discovery URL
     * @return true if there is a Discovery URL in the specified region, false otherwise
     */
    boolean doesRegionalDiscoveryExist(Region region) {
        getRegionalDiscoveryServer(region) ? true : false
    }

    String getTicketLabel() {
        grailsApplication.config.ticket?.label ?: 'Ticket'
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

    /**
     * @return the full local system path to the directory where Asgard stores its configuration files
     */
    String getAsgardHome() {
        grailsApplication.config.asgardHome
    }

    /**
     * @return true if cloud keys and other minimum configuration has been provided, false otherwise
     */
    boolean isAppConfigured() {
        grailsApplication.config.appConfigured
    }

    List<Map<String,String>> getExternalLinks() {
        grailsApplication.config.link?.externalLinks?.sort { it.text } ?: []
    }

    /**
     * @return URL to link to so users can configure alerting for their applications, with a default of null
     */
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

    /**
     * @return the list of regions in which platformservice is available for fast property reading and writing
     */
    List<Region> getPlatformServiceRegions() {
        List<Region> activeRegions = Region.limitedRegions ?: Region.values()
        List<Region> platformServiceRegions = grailsApplication.config.cloud?.platformserviceRegions ?: []
        platformServiceRegions.intersect(activeRegions)
    }

    /**
     * @return true if the initial timing of cache-loading threads should be delayed by small random amounts in order
     *         to reduce the number of large, simultaneous data retrieval calls to cloud APIs
     */
    boolean getUseJitter() {
        grailsApplication.config.thread?.useJitter
    }

    /**
     * Gets the instance types that Asgard needs to use that are not included in the AWS Java SDK enum
     *
     * @return List <InstanceTypeData> the custom instance types, or an empty list
     */
    List<InstanceTypeData> getCustomInstanceTypes() {
        grailsApplication.config.cloud?.customInstanceTypes ?: []
    }

    /**
     * @return the default auto scaling termination policy name to suggest when creating new auto scaling groups
     */
    String getDefaultTerminationPolicy() {
        grailsApplication.config.cloud?.defaultAutoScalingTerminationPolicy ?: 'Default'
    }

    /**
     * Returns the list of relevant Amazon Web Services account numbers as strings, starting with the account primarily
     * used by this Asgard instance. All other accounts in the list are candidates for cross-account sharing of
     * resources such as Amazon Machine Images (AMIs).
     *
     * @return list of relevant AWS account numbers, starting with the current account of the current environment
     */
    List<String> getAwsAccounts() {
        grailsApplication.config.grails?.awsAccounts ?: []
    }

    /**
     * Gets the optional list of accounts whose public resources such as AMIs should be used.
     *
     * @return List < String > account numbers/names, or empty list if not configured
     */
    List<String> getPublicResourceAccounts() {
        grailsApplication.config.cloud?.publicResourceAccounts ?: []
    }

    /**
     * @return all the names of the availability zones that should not be recommended for use in the current account
     */
    List<String> getDiscouragedAvailabilityZones() {
        grailsApplication.config.cloud?.discouragedAvailabilityZones ?: []
    }

    Map<String, List<TextLinkTemplate>> getInstanceLinkGroupingsToLinkTemplateLists() {
        grailsApplication.config.link?.instanceLinkGroupingsToLinkTemplateLists ?: [:]
    }

    /**
     * @return the name of the account's recommended SSH key registered in the AWS EC2 API
     */
    String getDefaultKeyName() {
        grailsApplication.config.cloud?.defaultKeyName ?: ''
    }

    /**
     * @return the name of the Amazon Simple Workflow domain that should be used for automation
     */
    String getSimpleWorkflowDomain() {
        grailsApplication.config?.workflow?.domain ?: 'asgard'
    }

    /**
     * @return the name of the Amazon Simple Workflow task list that should be used for automation
     */
    String getSimpleWorkflowTaskList() {
        grailsApplication.config?.workflow?.taskList ?: 'primary'
    }

    /**
     * @return the number of days to retain Amazon Simple Workflow closed executions
     */
    Integer getWorkflowExecutionRetentionPeriodInDays() {
        grailsApplication.config?.workflow?.workflowExecutionRetentionPeriodInDays ?: 90
    }

    /**
     * @return Map of a property name in {@link Caches} to minimum size for that cache to be considered 'healthy'
     */
    Map<String, Integer> getHealthCheckMinimumCounts() {
        grailsApplication.config.healthCheck?.minimumCounts ?: [:]
    }

    /**
     * @return true if the current server is meant to be running online to interact with the cloud, false if working
     *          in offline development mode
     */
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
        grailsApplication.config.secret?.loadBalancerUsernameFileName ?: null
    }

    String getLoadBalancerPasswordFile() {
        grailsApplication.config.secret?.loadBalancerPasswordFileName ?: null
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

    /**
     * @return name of the current cloud account, such as "test" or "prod", with a default of null
     */
    String getAccountName() {
        grailsApplication.config.cloud?.accountName ?: null
    }

    /**
     * @return CSS class name of the current environment such as test, staging, or prod, with a default of empty string
     */
    String getEnvStyle() {
        grailsApplication.config.cloud?.envStyle ?: ''
    }

    /**
     * @return name of the database domain for storing application metadata, with a default of "CLOUD_APPLICATIONS"
     */
    String getApplicationsDomain() {
        grailsApplication.config.cloud?.applicationsDomain ?: 'CLOUD_APPLICATIONS'
    }

    /**
     * @return the first part of all the environment variable names to be inserted into user data
     */
    String getUserDataVarPrefix() {
        grailsApplication.config.cloud?.userDataVarPrefix ?: 'CLOUD_'
    }

    /**
     * @return the base server URL for generating links to the current Asgard instance in outgoing emails
     */
    String getLinkCanonicalServerUrl() {
        grailsApplication.config.link?.canonicalServerUrl ?: grailsApplication.config.grails.serverURL ?:
                'http://localhost:8080'
    }

    /**
     * @param the plugin name
     * @return the bean names used for this plugin implementation, null if none configured. This can be either a single
     *          string or a list of strings depending on the plugin
     */
    Object getBeanNamesForPlugin(String pluginName) {
        Object beanNames = grailsApplication.config.plugin[pluginName]
        beanNames ?: null
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
     * @return maximum time in milliseconds for remote REST calls to wait before timing out
     */
    int getRestClientTimeoutMillis() {
        grailsApplication.config.rest?.timeoutMillis ?: 2 * 1000
    }

    /**
     * @return maximum time in milliseconds for threads to wait for a connection from the http connection pool
     */
    long getHttpConnPoolTimeout() {
        grailsApplication.config.httpConnPool?.timeout ?: 50 * 1000
    }

    /**
     * @return maximum size of the http connection pool
     */
    int getHttpConnPoolMaxSize() {
        grailsApplication.config.httpConnPool?.maxSize ?: 50
    }

    /**
     * @return maximum number of connections in the connection pool per host
     */
    int getHttpConnPoolMaxForRoute() {
        grailsApplication.config.httpConnPool?.maxSize ?: 5
    }

    /**
     * @return HTTP proxy host name, or null if unspecified
     */
    String getProxyHost() {
        grailsApplication.config.proxy?.host ?: null
    }

    /**
     * @return port number for the proxy to use, or -1 if not specified because HTTP client will treat -1 as default
     */
    int getProxyPort() {
        grailsApplication.config.proxy?.port ?: -1
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
     * is configured.
     *
     * @return the port for constructing URLs to make eureka calls
     */
    String getEurekaPort() {
        grailsApplication.config.eureka?.port ?: '80'
    }

    /**
     * Gets the port number (as a String) used in constructing PlatformService URLs. The port varies depending on how
     * PlatformService is configured.
     *
     * @return the port for constructing URLs to make PlatformService calls
     */
    String getPlatformServicePort() {
        grailsApplication.config.platform?.port ?: '80'
    }

    /**
     * @return the URL of the JavaScript file to import when integrating with the Blesk notification system
     */
    String getBleskJavaScriptUrl() {
        grailsApplication.config.blesk?.javaScriptUrl ?: null
    }

    /**
     * @return the URL from which Blesk should pull notification data
     */
    String getBleskDataUrl() {
        grailsApplication.config.blesk?.dataUrl ?: null
    }

    /**
     * @return the names of the security groups that should be applied to all non-VPC deployments
     */
    List<String> getDefaultSecurityGroups() {
        grailsApplication.config.cloud?.defaultSecurityGroups ?: []
    }

    /**
     * @return the names of the security groups that should be applied to all VPC deployments
     */
    List<String> getDefaultVpcSecurityGroupNames() {
        grailsApplication.config.cloud?.defaultVpcSecurityGroupNames ?: []
    }

    /**
     * @return the maximum number of characters from the user data string to be stores in the launch configuration
     *          cache, with a minimum of 0 and a default of {@code Integer.MAX_VALUE}
     */
    int getCachedUserDataMaxLength() {
        int maxLength = grailsApplication.config.cloud?.cachedUserDataMaxLength ?: Integer.MAX_VALUE
        Math.max(0, maxLength)
    }

    /*
     * @return true if api token based authentication is active, false otherwise
     */
    boolean isApiTokenEnabled() {
        grailsApplication.config.security?.apiToken?.enabled ?: false
    }

    /**
     * @return List of encryption keys for hashing api keys. The first item is used as the current key for new requests.
     *         The remaining keys in the list are used to validate tokens that are already in circulation. This provides
     *         a way to gracefully retire keys.
     */
    List<String> getApiEncryptionKeys() {
        grailsApplication.config.security?.apiToken?.encryptionKeys ?: []
    }

    /**
     * @return file name containing a list of keys to use for hashing api keys
     */
    String getApiEncryptionKeyFileName() {
        grailsApplication.config.secret?.apiEncryptionKeyFileName ?: null
    }

    /**
     * @return number of days a newly generated api key will be active for
     */
    int getApiTokenExpirationDays() {
        grailsApplication.config.security?.apiToken?.expirationDays ?: 90
    }

    /**
     * @return number of days before API key expiration to send an email warning
     */
    int getApiTokenExpiryWarningThresholdDays() {
        grailsApplication.config.security?.apiToken?.expiryWarningThresholdDays ?: 7
    }

    /**
     * @return number of minutes between sending warnings about a specific API key expiring
     */
    int getApiTokenExpiryWarningIntervalMinutes() {
        grailsApplication.config.security?.apiToken?.expiryWarningIntervalMinutes ?: 360
    }

    /**
     * @return application specific URL from OneLogin to redirect Single Sign-On (SSO) requests to
     */
    String getOneLoginUrl() {
        grailsApplication.config.security?.onelogin?.url ?: null
    }

    /**
     * @return URL to redirect user to on logout to terminate OneLogin session
     */
    String getOneLoginLogoutUrl() {
        grailsApplication.config.security?.onelogin?.logoutUrl ?: null
    }

    /**
     * @return Certificate provided by OneLogin used to validate SAML tokens
     */
    String getOneLoginCertificate() {
        grailsApplication.config.security?.onelogin?.certificate ?: null
    }

    /**
     * @return common suffix to truncate off usernames returned by OneLogin. For example '@netflix.com'
     */
    String getOneLoginUsernameSuffix() {
        grailsApplication.config.security?.onelogin?.usernameSuffix ?: null
    }

    /**
     * @return details of server configurations.
     */
    List<Environment> getServerEnvironments() {
        grailsApplication.config.server?.environments ?: []
    }

    /**
     * @return identifying name for servers that service this AWS account
     */
    String getCanonicalServerName() {
        Environment currentEnvironment = serverEnvironments.find { it.name == accountName }
        currentEnvironment?.canonicalDnsName ?: "asgard ${accountName}"
    }
    /**
     * @return subnet purposes that should have an internal scheme for ELBs
     */
    List<String> getInternalSubnetPurposes() {
        grailsApplication.config.cloud?.internalSubnetPurposes ?: ['internal']
    }

    /**
     * @return number of milliseconds to wait between AWS calls
     */
    int getCloudThrottle() {
        grailsApplication.config.cloud?.throttleMillis ?: 250
    }

    /**
     * @return the AWS Identity and Access Management (IAM) role that will be used by default. http://aws.amazon.com/iam
     */
    String getDefaultIamRole() {
        grailsApplication.config.cloud?.defaultIamRole ?: null
    }

    /**
     * @return true if edit links should be hidden for unauthenticated users, false to show edit links to all users
     */
    boolean isAuthenticationRequiredForEdit() {
        if (flagService.isOn(Flag.SUSPEND_AUTHENTICATION_REQUIREMENT)) {
            return false
        }
        grailsApplication.config.security?.authenticationRequiredForEdit ?: false
    }

    /**
     * @return URL with content that people should grok in order to make educated decisions about using Spot Instances
     */
    String getSpotUrl() {
        grailsApplication.config.cloud?.spot?.infoUrl ?: ''
    }

    /**
     * @return URL with info about configuring Fast Properties
     */
    String getFastPropertyInfoUrl() {
        grailsApplication.config.platform?.fastPropertyInfoUrl ?: ''
    }

    /**
     * @return URL for Cloud Ready REST calls
     */
    String getCloudReadyUrl() {
        grailsApplication.config.cloud?.cloudReady?.url ?: null
    }

    /**
     * @return Regions where Chaos Monkey is indigenous
     */
    Collection<Region> getChaosMonkeyRegions() {
        grailsApplication.config.cloud?.cloudReady?.chaosMonkey?.regions ?: []
    }

    /**
     * @return base URL of the build server (Jenkins) where applications get built
     */
    String getBuildServerUrl() {
        grailsApplication.config.cloud?.buildServer ?: ''
    }

    /**
     * @return for stack names that are revered, we check the health of all the ASG instances
     */
    Collection<String> getSignificantStacks() {
        grailsApplication.config.cloud?.significantStacks ?: []
    }

    /**
     * @return Regions other than the standard AWS ones (a cloud-like data center for example).
     */
    Map<String, String> getSpecialCaseRegions() {
        grailsApplication.config.cloud?.specialCaseRegions ?: [:]
    }

    /**
     * @return a Closure that determines if EBS volumes are needed for launch configurations based on instance type
     */
    Closure<Boolean> getInstanceTypeNeedsEbsVolumes() {
        grailsApplication.config.cloud?.launchConfig?.ebsVolumes?.instanceTypeNeeds ?: { String instanceType ->
            instanceType.startsWith('m3.')
        }
    }

    /**
     * @return the size of EBS volumes added to launch configurations for specific instance types
     */
    int getSizeOfEbsVolumesAddedToLaunchConfigs() {
        grailsApplication.config.cloud?.launchConfig?.ebsVolumes?.size ?: 125
    }

    /**
     * @return device names for the EBS volumes added to launch configurations for specific instance types
     */
    List<String> getEbsVolumeDeviceNamesForLaunchConfigs() {
        grailsApplication.config.cloud?.launchConfig?.ebsVolumes?.deviceNames ?: ['/dev/sdb', '/dev/sdc']
    }

    /**
     * @return fast property console url based on accountName
     */
    String getFastPropertiesConsoleUrl() {
        grailsApplication.config.platform?.fastPropertyConsoleUrls?."${accountName}" ?: ""
    }
}
