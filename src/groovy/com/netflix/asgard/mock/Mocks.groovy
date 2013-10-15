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
package com.netflix.asgard.mock

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing
import com.amazonaws.services.simpledb.model.Attribute
import com.amazonaws.services.simpledb.model.Item
import com.amazonaws.services.sns.AmazonSNS
import com.netflix.asgard.AppRegistration
import com.netflix.asgard.ApplicationService
import com.netflix.asgard.AwsAutoScalingService
import com.netflix.asgard.AwsClientService
import com.netflix.asgard.AwsCloudWatchService
import com.netflix.asgard.AwsEc2Service
import com.netflix.asgard.AwsLoadBalancerService
import com.netflix.asgard.AwsRdsService
import com.netflix.asgard.AwsSimpleDbService
import com.netflix.asgard.AwsSnsService
import com.netflix.asgard.AwsSqsService
import com.netflix.asgard.CachedMapBuilder
import com.netflix.asgard.Caches
import com.netflix.asgard.ConfigService
import com.netflix.asgard.DefaultUserDataProvider
import com.netflix.asgard.DiscoveryService
import com.netflix.asgard.DnsService
import com.netflix.asgard.EmailerService
import com.netflix.asgard.EurekaAddressCollectorService
import com.netflix.asgard.FastPropertyService
import com.netflix.asgard.FlagService
import com.netflix.asgard.IdService
import com.netflix.asgard.InstanceTypeService
import com.netflix.asgard.LaunchTemplateService
import com.netflix.asgard.Link
import com.netflix.asgard.MergedInstanceGroupingService
import com.netflix.asgard.MergedInstanceService
import com.netflix.asgard.MonkeyPatcherService
import com.netflix.asgard.MultiRegionAwsClient
import com.netflix.asgard.PushService
import com.netflix.asgard.Region
import com.netflix.asgard.RestClientService
import com.netflix.asgard.SecretService
import com.netflix.asgard.ServerService
import com.netflix.asgard.SimpleDbDomainService
import com.netflix.asgard.StackService
import com.netflix.asgard.Task
import com.netflix.asgard.TaskService
import com.netflix.asgard.ThreadScheduler
import com.netflix.asgard.Time
import com.netflix.asgard.UserContext
import com.netflix.asgard.cache.Fillable
import com.netflix.asgard.model.HardwareProfile
import com.netflix.asgard.model.InstanceTypeData
import com.netflix.asgard.model.SimpleDbSequenceLocator
import com.netflix.asgard.plugin.UserDataProvider
import grails.converters.JSON
import grails.converters.XML
import grails.test.MockUtils
import groovy.util.slurpersupport.GPathResult
import javax.servlet.http.HttpServletRequest
import org.codehaus.groovy.grails.web.json.JSONArray
import org.joda.time.format.ISODateTimeFormat
import org.springframework.mock.web.MockHttpServletRequest

class Mocks {

    static final String TEST_AWS_ACCOUNT_ID = '179000000000'
    static final String PROD_AWS_ACCOUNT_ID = '149000000000'
    static final String SEG_AWS_ACCOUNT_ID = '119000000000'

    static JSONArray parseJsonString(String jsonData) {
        JSON.parse(jsonData) as JSONArray
    }

    static String jsonNullable(def jsonValue) {
        jsonValue?.toString() == 'null' ? null : jsonValue?.toString()
    }

    /**
     * Parses an ISO formatted date string
     *
     * @param jsonValue date from JSON in ISO date format
     * @return Date the parsed date, or null if jsonValue was null
     */
    static Date parseJsonDate(String jsonValue) {
        jsonNullable(jsonValue) ? ISODateTimeFormat.dateTimeParser().parseDateTime(jsonValue).toDate() : null
    }

    static {
            // Add dynamic methods
            monkeyPatcherService()
    }

    private static grailsApplication
    static grailsApplication() {
        if (grailsApplication == null) {
            grailsApplication = [
                    config: [
                            online: false,
                            cloud: [
                                    accountName: 'test',
                                    customInstanceTypes: [
                                            new InstanceTypeData(linuxOnDemandPrice: 145.86, hardwareProfile:
                                                    new HardwareProfile(
                                                            instanceType: 'huge.mainframe',
                                                            architecture: '64-bit')
                                            )],
                                    defaultKeyName: 'nf-test-keypair-a',
                                    defaultSecurityGroups: ['nf-datacenter', 'nf-infrastructure'],
                                    discouragedAvailabilityZones: ['us-east-1b', 'us-west-1b'],
                                    discoveryServers: [(Region.US_EAST_1): 'discoveryinuseast.net'],
                                    imageTagMasterAccount: 'test',
                                    massDeleteExcludedLaunchPermissions: ['seg'],
                                    platformserviceRegions: [Region.US_EAST_1],
                                    specialCaseRegions: [
                                            code: 'us-nflx-1',
                                            description: 'us-nflx-1 (Netflix Data Center)'
                                    ]
                            ],
                            email: [:],
                            grails: [
                                    awsAccountNames: [(TEST_AWS_ACCOUNT_ID): 'test', (PROD_AWS_ACCOUNT_ID): 'prod', (SEG_AWS_ACCOUNT_ID): 'seg'],
                                    awsAccounts: [TEST_AWS_ACCOUNT_ID, PROD_AWS_ACCOUNT_ID]
                            ],
                            promote: [
                                    targetServer: 'http://prod',
                                    imageTags: true,
                                    canonicalServerForBakeEnvironment: 'http://test'
                            ],
                            server: [:],
                            thread: [useJitter: false]
                    ],
                    metadata: [:]
            ]
        }
        grailsApplication
    }

    private static Caches caches
    static Caches caches() {
        if (caches == null) {
            caches = new Caches(new CachedMapBuilder(new ThreadScheduler(configService())), configService())
        }
        caches
    }

    private static MonkeyPatcherService monkeyPatcherService
    static MonkeyPatcherService monkeyPatcherService() {
        if (monkeyPatcherService == null) {
            MockUtils.mockLogging(MonkeyPatcherService, false)
            monkeyPatcherService = new MonkeyPatcherService()
            monkeyPatcherService.grailsApplication = grailsApplication()
            monkeyPatcherService.afterPropertiesSet()
        }
        monkeyPatcherService
    }

    private static ApplicationService applicationService
    static ApplicationService applicationService() {
        if (applicationService == null) {
            MockUtils.mockLogging(ApplicationService, false)
            applicationService = new ApplicationService()
            applicationService.caches = caches()
            applicationService.grailsApplication = grailsApplication()
            applicationService.configService = configService()
            applicationService.awsClientService = awsClientService()
            applicationService.simpleDbClient = applicationService.awsClientService.createImpl(MockAmazonSimpleDBClient)

            List<String> names =
                    ['abcache', 'api', 'aws_stats', 'cryptex', 'helloworld', 'ntsuiboot', 'videometadata'].asImmutable()
            List<AppRegistration> apps = names.collect({ AppRegistration.from(item(it.toUpperCase())) }).asImmutable()

            // Populate map of names to apps
            Map<String, AppRegistration> namesToApps = [:]
            apps.eachWithIndex { app, index -> namesToApps[names[index]] = app }
            namesToApps = namesToApps.asImmutable()

            applicationService.metaClass.getRegisteredApplications = { UserContext userContext -> apps }
            applicationService.metaClass.getRegisteredApplication = { UserContext userContext, String name -> namesToApps[name] }

            applicationService.afterPropertiesSet()
            applicationService.initializeCaches()

            // Sanity check that we can get a registered application by name with test code.
            assert "cryptex" == applicationService.getRegisteredApplication(Mocks.userContext(), "cryptex").name
        }
        applicationService
    }

    private static Item item(String name) {
        new Item().withName(name).withAttributes(
                [new Attribute('createTs', '1279755598817'), new Attribute('updateTs', '1279755598817')])
    }

    static UserContext userContext() {
        HttpServletRequest request = new MockHttpServletRequest()
        request.setAttribute('region', Region.US_EAST_1)
        UserContext.of(request)
    }

    private static MergedInstanceService mergedInstanceService
    static MergedInstanceService mergedInstanceService() {
        if (mergedInstanceService == null) {
            MockUtils.mockLogging(MergedInstanceService, false)
            mergedInstanceService = new MergedInstanceService()
            mergedInstanceService.awsEc2Service = awsEc2Service()
            mergedInstanceService.discoveryService = discoveryService()
            mergedInstanceService
        }
        mergedInstanceService
    }

    private static MergedInstanceGroupingService mergedInstanceGroupingService
    static MergedInstanceGroupingService mergedInstanceGroupingService() {
        if (mergedInstanceGroupingService == null) {
            MockUtils.mockLogging(MergedInstanceGroupingService, false)
            mergedInstanceGroupingService = new MergedInstanceGroupingService()
            mergedInstanceGroupingService.awsAutoScalingService = awsAutoScalingService()
            mergedInstanceGroupingService.awsEc2Service = awsEc2Service()
            mergedInstanceGroupingService.discoveryService = discoveryService()
            mergedInstanceGroupingService.metaClass.getMergedInstances = { UserContext userContext -> [] }
        }
        mergedInstanceGroupingService
    }

    private static DnsService dnsService
    static DnsService dnsService() {
        if (dnsService == null) {
            MockUtils.mockLogging(DnsService, false)
            dnsService = new DnsService() {
                Collection<String> getCanonicalHostNamesForDnsName(String hostName) { ['localhost'] }
            }
        }
        dnsService
    }

    private static EurekaAddressCollectorService eurekaAddressCollectorService
    static EurekaAddressCollectorService eurekaAddressCollectorService() {
        if (eurekaAddressCollectorService == null) {
            MockUtils.mockLogging(EurekaAddressCollectorService, false)
            eurekaAddressCollectorService = new EurekaAddressCollectorService()
            eurekaAddressCollectorService.caches = caches()
            eurekaAddressCollectorService.configService = configService()
            eurekaAddressCollectorService.restClientService = restClientService()
            eurekaAddressCollectorService.dnsService = dnsService()
            eurekaAddressCollectorService.initializeCaches()
        }
        eurekaAddressCollectorService
    }

    private static DiscoveryService discoveryService
    static DiscoveryService discoveryService() {
        if (discoveryService == null) {
            MockUtils.mockLogging(DiscoveryService, false)
            discoveryService = new DiscoveryService()
            discoveryService.grailsApplication = grailsApplication()
            discoveryService.caches = caches()
            discoveryService.eurekaAddressCollectorService = eurekaAddressCollectorService()
            discoveryService.configService = configService()
            discoveryService.taskService = taskService()
            discoveryService.metaClass.getAppInstancesByIds = { UserContext userContext, List<String> instanceIds -> [] }
            discoveryService.initializeCaches()
        }
        discoveryService
    }

    private static InstanceTypeService instanceTypeService
    static InstanceTypeService instanceTypeService() {
        if (instanceTypeService == null) {
            MockUtils.mockLogging(InstanceTypeService, false)
            instanceTypeService = new InstanceTypeService()
            instanceTypeService.grailsApplication = grailsApplication()
            instanceTypeService.awsEc2Service = awsEc2Service()
            instanceTypeService.configService = configService()
            instanceTypeService.emailerService = emailerService()
            instanceTypeService.caches = caches()
            instanceTypeService.initializeCaches()
            waitForFill(caches.allInstanceTypes)
        }
        instanceTypeService
    }

    private static AwsLoadBalancerService awsLoadBalancerService
    static AwsLoadBalancerService awsLoadBalancerService() {
        if (awsLoadBalancerService == null) {
            awsLoadBalancerService = newAwsLoadBalancerService()
        }
        awsLoadBalancerService
    }

    static AwsLoadBalancerService newAwsLoadBalancerService(AmazonElasticLoadBalancing mockElb = null) {
        MockUtils.mockLogging(AwsLoadBalancerService, false)
        AwsLoadBalancerService newAwsLoadBalancerService = new AwsLoadBalancerService()
        newAwsLoadBalancerService.with {
            grailsApplication = grailsApplication()
            awsClientService = awsClientService()
            caches = caches()
            awsEc2Service = awsEc2Service()
            taskService = taskService()
        }
        if (mockElb) {
            newAwsLoadBalancerService.awsClient = new MultiRegionAwsClient({ mockElb })
        }
        newAwsLoadBalancerService.afterPropertiesSet()
        newAwsLoadBalancerService.initializeCaches()
        newAwsLoadBalancerService
    }

    private static AwsClientService awsClientService
    static AwsClientService awsClientService() {
        if (awsClientService == null) {
            MockUtils.mockLogging(AwsClientService, false)
            awsClientService = new AwsClientService()
            awsClientService.grailsApplication = grailsApplication()
            awsClientService.secretService = new SecretService()
            awsClientService.configService = configService()
            awsClientService.serverService = serverService()
            awsClientService.afterPropertiesSet()
        }
        awsClientService
    }

    private static PushService pushService
    static PushService pushService() {
        if (pushService == null) {
            MockUtils.mockLogging(PushService, false)
            pushService = new PushService()
            pushService.grailsApplication = grailsApplication()
            pushService.configService = configService()
            pushService.awsAutoScalingService = awsAutoScalingService()
            pushService.awsEc2Service = awsEc2Service()
            pushService.instanceTypeService = instanceTypeService()
            pushService.restClientService = restClientService()
        }
        pushService
    }

    private static TaskService taskService
    static TaskService taskService() {
        if (taskService == null) {
            MockUtils.mockLogging(TaskService, false)
            taskService = new TaskService() {
                // To run tasks synchronously for tests
                Task startTask(UserContext userContext, String name, Closure work, Link link = null) {
                    runTask(userContext, name, work, link)
                    null
                }
            }
            taskService.grailsApplication = grailsApplication()
            taskService.emailerService = emailerService()
            taskService.awsSimpleDbService = awsSimpleDbService()
            taskService.idService = new IdService() {
                String nextId(UserContext userContext, SimpleDbSequenceLocator sequenceLocator) {
                    '1'
                }
            }
        }
        taskService
    }

    private static EmailerService emailerService
    static EmailerService emailerService() {
        if (emailerService == null) {
            MockUtils.mockLogging(EmailerService, false)
            emailerService = new EmailerService()
            emailerService.configService = configService()
            emailerService.afterPropertiesSet()
        }
        emailerService
    }

    private static FlagService flagService
    static FlagService flagService() {
        if (flagService == null) {
            MockUtils.mockLogging(FlagService, false)
            flagService = new FlagService()
        }
        flagService
    }

    private static FastPropertyService fastPropertyService
    static FastPropertyService fastPropertyService() {
        if (fastPropertyService == null) {
            MockUtils.mockLogging(FastPropertyService, false)
            fastPropertyService = new FastPropertyService()
            fastPropertyService.metaClass.platformServiceHostAndPort = { UserContext userContext -> 'nowhere:80' }
            fastPropertyService.grailsApplication = grailsApplication()
            fastPropertyService.applicationService = applicationService()
            fastPropertyService.caches = caches()
            fastPropertyService.discoveryService = discoveryService()
            fastPropertyService.mergedInstanceGroupingService = mergedInstanceGroupingService()

            GPathResult listXml = XML.parse(MockFastProperties.DATA) as GPathResult
            fastPropertyService.restClientService = [
                    getAsXml: { String uri ->
                        if (uri.endsWith('allprops')) {
                            return listXml
                        }
                        String id = uri.substring(uri.lastIndexOf('/') + 1)
                        return listXml.properties.property.find { GPathResult fastPropertyData ->
                            fastPropertyData.propertyId.toString() == id
                        }
                    }
            ]
            fastPropertyService.taskService = taskService()
            fastPropertyService.initializeCaches()
            waitForFill(caches.allFastProperties)
        }
        fastPropertyService
    }

    private static RestClientService restClientService
    static RestClientService restClientService() {
        if (restClientService == null ) {
            restClientService = newRestClientService()
        }
        restClientService
    }

    static RestClientService newRestClientService() {
        MockUtils.mockLogging(RestClientService, false)
        final RestClientService newRestClientService = new RestClientService()
        newRestClientService
    }

    private static StackService stackService
    static StackService stackService() {
        if (stackService == null) {
            MockUtils.mockLogging(StackService, false)
            stackService = new StackService()
            stackService.awsAutoScalingService = awsAutoScalingService()
        }
        stackService
    }

    private static LaunchTemplateService launchTemplateService
    static LaunchTemplateService launchTemplateService() {
        if (launchTemplateService == null) {
            MockUtils.mockLogging(LaunchTemplateService, false)
            launchTemplateService = new LaunchTemplateService()
            launchTemplateService.grailsApplication = grailsApplication()
            launchTemplateService.applicationService = applicationService()
            launchTemplateService.configService = configService()
            launchTemplateService.pluginService = [ userDataProvider: userDataProvider() ]
        }
        launchTemplateService
    }

    private static UserDataProvider userDataProvider
    static UserDataProvider userDataProvider() {
        if (userDataProvider == null) {
            userDataProvider = new DefaultUserDataProvider()
            userDataProvider.applicationService = applicationService()
            userDataProvider.configService = configService()
        }
        userDataProvider
    }

    private static AwsAutoScalingService awsAutoScalingService
    static AwsAutoScalingService awsAutoScalingService() {
        if (awsAutoScalingService == null ) {
            awsAutoScalingService = newAwsAutoScalingService()
        }
        awsAutoScalingService
    }

    static AwsAutoScalingService newAwsAutoScalingService() {
        MockUtils.mockLogging(AwsAutoScalingService, false)
        final newAwsAutoScalingService = new AwsAutoScalingService()
        newAwsAutoScalingService.with {
            grailsApplication = grailsApplication()
            awsClientService = awsClientService()
            caches = caches()
            applicationService = applicationService()
            awsEc2Service = awsEc2Service()
            awsLoadBalancerService = awsLoadBalancerService()
            configService = configService()
            discoveryService = discoveryService()
            mergedInstanceService = mergedInstanceService()
            taskService = taskService()
            launchTemplateService = launchTemplateService()
            pushService = pushService()
            awsCloudWatchService = awsCloudWatchService()
            emailerService = emailerService()
            awsSimpleDbService = awsSimpleDbService()
            afterPropertiesSet()
            initializeCaches()
            waitForFill(caches.allAutoScalingGroups)
            waitForFill(caches.allClusters)
        }
        newAwsAutoScalingService
    }

    private static AwsEc2Service awsEc2Service
    static AwsEc2Service awsEc2Service() {
        if (awsEc2Service == null) {
            awsEc2Service = newAwsEc2Service()
        }
        awsEc2Service
    }

    static AwsEc2Service newAwsEc2Service(AmazonEC2 amazonEC2 = null) {
        MockUtils.mockLogging(AwsEc2Service, false)
        AwsEc2Service awsEc2Service = new AwsEc2Service()
        awsEc2Service.with() {
            configService = configService()
            awsClientService = awsClientService()
            caches = caches()
            taskService = taskService()
            if (amazonEC2) {
                awsClient = new MultiRegionAwsClient({ amazonEC2 })
            }
            afterPropertiesSet()
            initializeCaches()
        }
        awsEc2Service
    }

    private static AwsCloudWatchService awsCloudWatchService
    static AwsCloudWatchService awsCloudWatchService() {
        if (awsCloudWatchService == null) {
            awsCloudWatchService = newAwsCloudWatchService()
        }
        awsCloudWatchService
    }

    static AwsCloudWatchService newAwsCloudWatchService() {
        MockUtils.mockLogging(AwsCloudWatchService, false)
        AwsCloudWatchService newAwsCloudWatchService = new AwsCloudWatchService()
        newAwsCloudWatchService.with {
            awsClientService = awsClientService()
            caches = caches()
            configService = configService()
            taskService = taskService()
            afterPropertiesSet()
            initializeCaches()
        }
        newAwsCloudWatchService
    }

    private static AwsSnsService awsSnsService
    static AwsSnsService awsSnsService() {
        if (awsSnsService == null) {
            awsSnsService = newAwsSnsService()
        }
        awsSnsService
    }

    static AwsSnsService newAwsSnsService(AmazonSNS mockAmazonSNS = null) {
        MockUtils.mockLogging(AwsSnsService, false)
        AwsSnsService newAwsSnsService = new AwsSnsService()
        newAwsSnsService.with {
            grailsApplication = grailsApplication()
            awsClientService = awsClientService()
            caches = caches()
            configService = configService()
            taskService = taskService()
        }
        if (mockAmazonSNS) {
            newAwsSnsService.awsClient = new MultiRegionAwsClient({ mockAmazonSNS })
        }
        newAwsSnsService.afterPropertiesSet()
        newAwsSnsService.initializeCaches()
        newAwsSnsService
    }

    private static ConfigService configService
    static ConfigService configService() {
        if (configService == null) {
            configService = new ConfigService()
            configService.grailsApplication = grailsApplication()
        }
        configService
    }

    private static ServerService serverService
    static ServerService serverService() {
        if (serverService == null) {
            serverService = new ServerService()
            serverService.grailsApplication = grailsApplication()
        }
        serverService
    }

    private static AwsSqsService awsSqsService
    static AwsSqsService awsSqsService() {
        if (awsSqsService == null) {
            MockUtils.mockLogging(AwsSqsService, false)
            awsSqsService = new AwsSqsService()
            awsSqsService.grailsApplication = grailsApplication()
            awsSqsService.awsClientService = awsClientService()
            awsSqsService.caches = caches()
            awsSqsService.taskService = taskService()
            awsSqsService.afterPropertiesSet()
            awsSqsService.initializeCaches()
        }
        awsSqsService
    }

    private static SimpleDbDomainService simpleDbDomainService
    static SimpleDbDomainService simpleDbDomainService() {
        if (simpleDbDomainService == null) {
            MockUtils.mockLogging(SimpleDbDomainService, false)
            simpleDbDomainService = new SimpleDbDomainService()
            simpleDbDomainService.caches = caches()
            simpleDbDomainService.awsSimpleDbService = awsSimpleDbService()
            simpleDbDomainService.initializeCaches()
        }
        simpleDbDomainService
    }

    private static AwsSimpleDbService awsSimpleDbService
    static AwsSimpleDbService awsSimpleDbService() {
        if (awsSimpleDbService == null) {
            MockUtils.mockLogging(AwsSimpleDbService, false)
            awsSimpleDbService = new AwsSimpleDbService()
            awsSimpleDbService.awsClientService = awsClientService()
            awsSimpleDbService.afterPropertiesSet()
        }
        awsSimpleDbService
    }

    private static AwsRdsService awsRdsService
    static AwsRdsService awsRdsService() {
        if (awsRdsService == null) {
            MockUtils.mockLogging(AwsRdsService, false)
            awsRdsService = new AwsRdsService()
            awsRdsService.configService = configService()
            awsRdsService.taskService = taskService()
            awsRdsService.awsClientService = awsClientService()
            awsRdsService.caches = caches()
            awsRdsService.afterPropertiesSet()
            awsRdsService.initializeCaches()
        }
        awsRdsService
    }

    static AmazonServiceException makeAmazonServiceException(String message, int statusCode, String errorCode,
                                                             String requestId) {
        AmazonServiceException e = new AmazonServiceException(message)
        e.errorCode = statusCode
        e.requestId = requestId
        e.errorCode = errorCode
        e
    }

    static void waitForFill(Fillable cache) {
        while (!cache.filled) {
            Time.sleepCancellably(10)
        }
    }

    static void createDynamicMethods()  {
        monkeyPatcherService().createDynamicMethods()
    }
}
