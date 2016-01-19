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

import com.amazonaws.ClientConfiguration
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
     * @return return the Simple Mail Transport Protocol (SMTP) port for connecting to the mail server
     */
    int getSmtpPort() {
        grailsApplication.config.email.smtpPort ?: 25
    }

    /**
     * @return the Simple Mail Transport Protocol (SMTP) username that should be used for authenticating with the server
     */
    String getSmtpUsername() {
        grailsApplication.config.email.smtpUsername ?: null
    }

    /**
     * @return the Simple Mail Transport Protocol (SMTP) password that should be used for authenticating with the server
     */
    String getSmtpPassword() {
        grailsApplication.config.email.smtpPassword ?: null
    }

    /**
     * @return JavaMail properties required for enabling SMTP over SSL
     */
    Properties getJavaMailProperties() {

        Properties javaMailProperties = new Properties()

        if (grailsApplication.config.email.smtpSslEnabled) {
            javaMailProperties.put("mail.smtps.auth", "true")
            javaMailProperties.put("mail.smtp.ssl.enable", "true")
            javaMailProperties.put("mail.transport.protocol", "smtps")
        }

        return javaMailProperties
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

    /**
     * Gets the port on which an Asgard instance is serving traffic directly.
     * <p>
     * This is useful for cases like constructing a health check URL to report to Eureka Server, because checking the
     * health of a server is a case where the specific instance needs to accessed directly instead of through an
     * external router or load balancer.
     *
     * @return the port that other systems should use when calling an Asgard instance directly, defaulting to 80
     */
    Integer getLocalInstancePort() {
        grailsApplication.config.server?.localInstancePort ?: 80
    }

    /**
     * Gets the virtual host name, or "virtual IP" or "VIP", that the system should use to override the default virtual
     * host name in Eureka client when registering with Eureka service.
     *
     * It's common for the VIP to be in the form <name>:<port> such as
     * asgard-prod:7001
     *
     * @return the virtual host name override to use, or null if not configured
     * @see com.netflix.asgard.eureka.EurekaClientHolder#createEurekaInstanceConfig()
     * @see com.netflix.appinfo.EurekaInstanceConfig#getVirtualHostName()
     */
    String getLocalVirtualHostName() {
        grailsApplication.config.eureka?.localVirtualHostName ?: null
    }

    /**
     * Gets the URL to use for Eureka Client to register with Eureka Service if the availability zone where the system
     * is currently running does not have a zone entry anywhere in the values of regionToEurekaServiceAvailabilityZones.
     *
     * Example output:
     * http://us-west-1.discoverytest.example.com:7001/eureka/v2/
     *
     * @return the default Eureka Service URL for Eureka Client to register with if no zone-specific matches exist
     */
    String getEurekaDefaultRegistrationUrl() {
        grailsApplication.config.eureka?.defaultRegistrationUrl ?: null
    }

    /**
     * Example output:
     * <pre>
     * {@code
     * [
     *     'us-east-1': ['us-east-1a', 'us-east-1c', 'us-east-1d', 'us-east-1e'],
     *     'us-west-1': ['us-west-1a', 'us-west-1b', 'us-west-1c'],
     *     'us-west-2': ['us-west-2a', 'us-west-2b', 'us-west-2c'],
     *     'eu-west-1': ['eu-west-1a', 'eu-west-1b', 'us-west-1c'],
     * ]
     * }
     * </pre>
     *
     * @return a map of region names like 'us-west-1' to list of availability zones in that region where Eureka service
     *          is running, such as ['us-west-1a', 'us-west-1b']
     */
    Map<String, List<String>> getEurekaZoneListsByRegion() {
        grailsApplication.config.eureka?.zoneListsByRegion ?: [:]
    }

    /**
     * Gets the template string for constructing a URL to access Eureka Server from Eureka Client. If the template
     * contains any of the following strings they will be replaced by the availability zone, region, or environment name
     * of a Eureka Server node.
     * <p>
     * <pre>
     * {@code
     *
     * ${zone} - the availability zone listed as a value for a region in the {@link #getEurekaZoneListsByRegion} map
     * ${region} - the region listed as a key for the zone in the {@link #getEurekaZoneListsByRegion} map
     * ${env} - the environment name from {@link #getAccountName}
     *
     * Examples:
     * http://${zone}.${region}.eureka{env}.example:7001/eureka/v2/
     * http://${region}.eureka{env}.example:7001/eureka/v2/
     * http://eureka.example:7001/eureka/v2/
     *
     * }
     * </pre>
     * <p>
     *
     * @see com.netflix.asgard.eureka.AsgardEurekaClientConfig#getEurekaServerServiceUrls
     * @return the configured template for constructing a Eureka Server endpoint
     */
    String getEurekaUrlTemplateForZoneRegionEnv() {
        grailsApplication.config.eureka?.urlTemplateForZoneRegionEnv ?: null
    }

    /**
     * @return the short label that should be displayed in reference to the change control ticket for cloud changes
     */
    String getTicketLabel() {
        grailsApplication.config.ticket?.label ?: 'Ticket'
    }

    /**
     * @return the verbose label for documentation that defines the change control ticket for cloud changes
     */
    String getFullTicketLabel() {
        grailsApplication.config.ticket?.fullLabel ?: 'Ticket'
    }

    /**
     * Gets the list of AWS account numbers which, if an AMI has launch permissions to launch into that account, we
     * should assume that the AMI is in use by that account and therefore the AMI should not be automatically deleted
     * even if it satisfies all other rules that define an old, unused AMI.
     *
     * @return account numbers of the AWS accounts that cannot be inspected for AMI usage, meaning that the AMIs that
     *          can launch into those accounts should never be deleted through mass deletion
     */
    Collection<String> getExcludedLaunchPermissionsForMassDelete() {
        List<String> excludedLaunchPermissions = grailsApplication.config.cloud?.massDeleteExcludedLaunchPermissions
        Map matches = awsAccountNames.findAll { k, v ->
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

    /**
     * @return a list of maps each describing one offsite link, with url, text, and image keys, such as
     *          [url: 'mailto:help@example.com', text: 'Email Support',
     *              image: '/images/tango/16/actions/mail-message-new.png']
     */
    List<Map<String, String>> getExternalLinks() {
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
     * @return The SSL certificate id to use when creating HTTPS listener for an elastic load balancer
     */
    String getDefaultElbSslCertificateId() {
        grailsApplication.config.cloud?.defaultElbSslCertificateId ?: ""
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

    /**
     * @return map of section names to list of templates for creating links to other applications
     */
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
     * @return true if the current server is meant to be running online to interact with the cloud, false if working
     *          in offline development mode
     */
    boolean isOnline() {
        grailsApplication.config.server.online
    }

    /**
     * @return a map of deprecated server names to new canonical server names, or an empty map if not configured
     */
    Map<String, String> getDeprecatedServerNamesToReplacements() {
        grailsApplication.config.server?.deprecatedServerNamesToReplacements ?: [:]
    }

    /**
     * @return the AWS account access ID stored in the local configuration file, or null if not configured
     */
    String getAccessId() {
        grailsApplication.config.secret?.accessId ?: null
    }

    /**
     * @return the AWS secret key stored in the local configuration file, or null if not configured
     */
    String getSecretKey() {
        grailsApplication.config.secret?.secretKey ?: null
    }

    /**
     * @return the name of the local or remote file containing the AWS access ID, or null if not configured
     */
    String getAccessIdFileName() {
        grailsApplication.config.secret?.accessIdFileName ?: null
    }

    /**
     * @return the name of the local or remote file containing the AWS secret key, or null if not configured
     */
    String getSecretKeyFileName() {
        grailsApplication.config.secret?.secretKeyFileName ?: null
    }

    /**
     * @return the name of the file that contains the username needed for accessing a load balancer over SSH, for
     *      switching traffic between different Asgard servers
     */
    String getLoadBalancerUsernameFile() {
        grailsApplication.config.secret?.loadBalancerUsernameFileName ?: null
    }

    /**
     * @return the name of the file that contains the password needed for accessing a load balancer over SSH, for
     *      switching traffic between different Asgard servers
     */
    String getLoadBalancerPasswordFile() {
        grailsApplication.config.secret?.loadBalancerPasswordFileName ?: null
    }

    /**
     * @return the path to the local machine's directory that contains files with sensitive credentials
     */
    String getSecretLocalDirectory() {
        grailsApplication.config.secret?.localDirectory ?: null
    }

    /**
     * @return the username for SSH access to the remote server that contains secret files such as credentials
     */
    String getSecretRemoteUser() {
        grailsApplication.config.secret?.remoteUser ?: null
    }

    /**
     * @return the name of the remote server containing secret files such as credentials to be accessed over SSH
     */
    String getSecretRemoteServer() {
        grailsApplication.config.secret?.remoteServer ?: null
    }

    /**
     * @return the path to the directory containing secret files on a remote server to be accessed over SSH
     */
    String getSecretRemoteDirectory() {
        grailsApplication.config.secret?.remoteDirectory ?: null
    }

    /**
     * @return the Amazon Resource Name (ARN) of the IAM role that Asgard should assume when getting credentials
     */
    String getAssumeRoleArn() {
        grailsApplication.config.secret?.assumeRole?.roleArn ?: null
    }

    /**
     * @return the name to call the IAM session that is fetching credentials to AssumeRole for a different AWS account
     */
    String getAssumeRoleSessionName() {
        grailsApplication.config.secret?.assumeRole?.roleSessionName ?: null
    }

    /**
     * @return the endpoint to call for fetching secret keys for AWS access, or null by default
     */
    String getKeyManagementServiceEndpoint() {
        grailsApplication.config.secret?.keyManagement?.endpoint ?: null
    }

    /**
     * @return the port to use when establishing a secure SSL connection to a key management service, or null by default
     */
    Integer getKeyManagementServicePort() {
        grailsApplication.config.secret?.keyManagement?.servicePort ?: null
    }

    /**
     * @return the path to the local keystore file to use for SSL connections to a key management service
     */
    String getKeyManagementSslKeyStoreFilePath() {
        grailsApplication.config.secret?.keyManagement?.keyStoreFilePath ?: null
    }

    /**
     * @return the password for the keystore file used for SSL connections to a key management service
     */
    String getKeyManagementSslKeystorePassword() {
        grailsApplication.config.secret?.keyManagement?.password ?: 'changeit'
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
     * Only used by the example {@link com.netflix.asgard.userdata.NetflixAdvancedUserDataProvider}.
     *
     * @return true if {@link com.netflix.asgard.userdata.NetflixAdvancedUserDataProvider} should use property file
     *          formatted user data for deploying Windows images
     */
    boolean getUsePropertyFileUserDataForWindowsImages() {
        grailsApplication.config.cloud?.usePropertyFileUserDataForWindowsImages ?: false
    }

    /**
     * @return the list of server root URLs for copying data such as image tags from a source account to target accounts
     */
    List<String> getPromotionTargetServerRootUrls() {
        grailsApplication.config.promote?.targetServerRootUrls ?: []
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
     * @return number of milliseconds
     */
    int getSocketTimeout() {
        grailsApplication.config.cloud?.socketTimeout ?: ClientConfiguration.DEFAULT_SOCKET_TIMEOUT
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

    /**
     * @return true if api token based authentication is active, false otherwise
     */
    boolean isApiTokenEnabled() {
        grailsApplication.config.security?.apiToken?.enabled ?: false
    }

    /**
     * @return true if deletes (via the UI) should be disabled
     */
    boolean getDisableUIDeletes() {
        grailsApplication.config.disableUIDeletes ?: false
    }

    /**
     * @return true if updates (via the UI) should be disabled
     */
    boolean getDisableUIUpdates()  {
        grailsApplication.config.disableUIUpdates ?: false
    }

    /**
     * Gets a list of encryption keys for hashing api keys. The first item is used as the current key for new requests.
     * The remaining keys in the list are used to validate tokens that are already in circulation. This provides a way
     * to gracefully retire keys.
     *
     * If no encryption keys are configured for use, then a default key is used because this isn't meant to be a
     * serious security roadblock by any means. It's just a minor hoop to jump through in order to spoof a different
     * user. We'll replace this lame auth system with a better one later.
     *
     * @return list of encryption keys, starting with the current key for new API tokens
     */
    List<String> getApiEncryptionKeys() {
        grailsApplication.config.security?.apiToken?.encryptionKeys ?: []
    }

    /**
     * @return the current in use encryption key for Asgard API token generation
     */
    String getCurrentApiEncryptionKey() {
        apiEncryptionKeys[0]
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
     * @return the server names and ports of the other Asgard instances that should share in-memory task lists
     */
    List<String> getOtherServerNamePortCombos() {
        grailsApplication.config.server?.otherServerNamePortCombos ?: []
    }

    /**
     * @return port to use when calling other Asgard instances in the same cluster, if ports are unavailable from Eureka
     */
    String getPortForOtherServersInCluster() {
        grailsApplication.config.server?.portForOtherServersInCluster ?: null
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
     * @return device name - virtual name mapping for custom volumes added to launch configurations for specific
     * instance types
     */
    Map<String, String> getDeviceNameVirtualNameMapping() {
        grailsApplication.config.cloud?.launchConfig?.customVolumes?.deviceNameVirtualNameMapping ?:
                ['/dev/sdb': 'ephemeral0', '/dev/sdc': 'ephemeral1']
    }
   /**
    * @return a Closure that determines if EBS volumes are needed for launch configurations based on instance type
    */
    Closure<Boolean> getInstanceTypeNeedsEbsVolumes() {
        grailsApplication.config.cloud?.launchConfig?.ebsVolumes?.instanceTypeNeeds ?: { String instanceType ->
            false
        }
    }
    /**
     * @return a Closure that determines if custom volumes are needed for launch configurations based on instance type
    */
    Closure<Boolean> getInstanceTypeNeedsCustomVolumes() {
        grailsApplication.config.cloud?.launchConfig?.customVolumes?.instanceTypeNeeds ?: { String instanceType ->
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
     * @return URL with info about configuring Fast Properties
     */
    String getFastPropertyInfoUrl() {
        grailsApplication.config.platform?.fastPropertyInfoUrl ?: ''
    }

    /**
     * @return boolean representing enabled instance monitoring. By default, AWS enables detailed instance
     * monitoring; you must explicitly turn it off
     */
    boolean getEnableInstanceMonitoring() {
        grailsApplication.config.cloud?.launchConfig?.enableInstanceMonitoring ?: false
    }

    /**
     * @return List of filters to apply to instance reservations (i.e. Light Utilization, etc). By providing
     * values, these reservation types will be removed from the total count of reservations within the
     * AwsEc2Service. Otherwise, all reservation types will be returned.
     */
    List<String> getReservationOfferingTypeFilters(){
        grailsApplication.config.cloud?.reservationOfferingTypeFilters ?: []
    }

    /**
     * Get the Monkey Commander URL for editing Chaos Monkey settings for an application.
     *
     * @param applicationName name of application
     * @return link to Cloud Ready
     */
    String getMonkeyCommanderEditLink(String applicationName) {
        "http://monkey-commander.${accountName}.netflix.net/monkey-commander/chaos/${applicationName}/edit"
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
     * @return fast property console url based on accountName
     */
    String getFastPropertiesConsoleUrl() {
        grailsApplication.config.platform?.fastPropertyConsoleUrls?."${accountName}" ?: ""
    }

    /**
     * @return the ASG analyzer base url
     */
    String getAsgAnalyzerBaseUrl() {
        grailsApplication.config.cloud?.asgAnalyzerBaseUrl ?: ''
    }

    /**
     * The maximum number of auto scaling groups allowed in a cluster spanning multiple push sequence numbers.
     *
     * @return the configured maximum number of ASGs in a Cluster, or 3 if not configured
     */
    Integer getClusterMaxGroups() {
        grailsApplication.config.cluster?.maxGroups ?: 3
    }
}
