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

import com.netflix.asgard.model.ApplicationInstance
import grails.converters.XML
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings("GroovyAssignabilityCheck")
class EurekaServiceUnitSpec extends Specification {

    // DiscoveryService needs a complete name change, but it will be big, so not right now.
    DiscoveryService eurekaService
    ConfigService configService
    EurekaAddressCollectorService eurekaAddressCollectorService
    RestClientService restClientService
    CachedMap cachedMap
    Map<Region, String> regionsToEurekaServerNames = [(Region.US_WEST_2): 'oregon-eureka']
    UserContext userContext

    void setup() {
        eurekaService = new DiscoveryService()
        configService = Mock(ConfigService)
        eurekaService.configService = configService
        eurekaAddressCollectorService = Mock(EurekaAddressCollectorService)
        eurekaService.eurekaAddressCollectorService = eurekaAddressCollectorService
        restClientService = Mock(RestClientService)
        eurekaService.restClientService = restClientService
        eurekaService.taskService = new TaskService() {
            def runTask(UserContext context, String name, Closure work, Link link = null, Task existingTask = null) {
                work(new Task())
            }
        }
        cachedMap = Mock(CachedMap)
        eurekaService.caches = new Caches(new MockCachedMapBuilder([
                (EntityType.eurekaAddress): cachedMap,
        ]))
        regionsToEurekaServerNames = [(Region.US_WEST_2): 'oregon-eureka']
        userContext = UserContext.auto(Region.US_WEST_2)
    }

    @SuppressWarnings("GroovyAccessibility")
    @Unroll("for host name #hostName, constructs a base URL #url")
    void 'constructs a base URL'() {

        when:
        String result = eurekaService.constructBaseUrl(hostName)

        then:
        configService.getEurekaPort() >> { '7001' }
        configService.getEurekaUrlContext() >> { 'eur' }
        result == url

        where:
        hostName  | url
        '1.1.1.1' | 'http://1.1.1.1:7001/eur'
        null      | null

    }

    @SuppressWarnings("GroovyAccessibility")
    @Unroll("for host name #hostName, constructs a base API URL #url")
    void 'constructs a base API URL'() {

        when:
        String result = eurekaService.constructBaseApiUrl(hostName)

        then:
        configService.getEurekaPort() >> { '7001' }
        configService.getEurekaUrlContext() >> { 'eur' }
        result == url

        where:
        hostName  | url
        '1.1.1.1' | 'http://1.1.1.1:7001/eur/v2'
        null      | null
    }

    @Unroll("finds the Eureka canonical base URL #url for region #region")
    void 'finds the Eureka canonical base URL for the region or null if none exists'() {

        when:
        String result = eurekaService.findCanonicalBaseUrl(region)

        then:
        1 * configService.getRegionalDiscoveryServer(_) >> { Region region -> regionsToEurekaServerNames[region] }
        configService.getEurekaPort() >> { '7001' }
        configService.getEurekaUrlContext() >> { 'eur' }
        result == url

        where:
        region                | url
        Region.US_WEST_2      | 'http://oregon-eureka:7001/eur'
        Region.AP_NORTHEAST_1 | null
    }

    @Unroll("finds the Eureka canonical base API URL #url for region #region")
    void 'finds the Eureka canonical base API URL for the region or null if none exists'() {

        when:
        String result = eurekaService.findCanonicalBaseApiUrl(region)

        then:
        1 * configService.getRegionalDiscoveryServer(_) >> { Region region -> regionsToEurekaServerNames[region] }
        configService.getEurekaPort() >> { '7001' }
        configService.getEurekaUrlContext() >> { 'eur' }
        result == url

        where:
        region                | url
        Region.US_WEST_2      | 'http://oregon-eureka:7001/eur/v2'
        Region.AP_NORTHEAST_1 | null
    }

    void 'finds the base URL for a specific Eureka instance'() {

        when:
        String url = eurekaService.findSpecificBaseUrl(Region.US_WEST_2)

        then:
        configService.getEurekaPort() >> { '7001' }
        configService.getEurekaUrlContext() >> { 'eur' }
        cachedMap.list() >> { ['1.1.1.1', '2.2.2.2', '3.3.3.3'] }
        eurekaAddressCollectorService.chooseBestEurekaNode(_) >> { '3.3.3.3' }
        url == 'http://3.3.3.3:7001/eur'
    }

    void 'finds the base API URL for a specific Eureka instance'() {

        when:
        String url = eurekaService.findSpecificBaseApiUrl(Region.US_WEST_2)

        then:
        configService.getEurekaPort() >> { '7001' }
        configService.getEurekaUrlContext() >> { 'eur' }
        cachedMap.list() >> { ['1.1.1.1', '2.2.2.2', '3.3.3.3'] }
        eurekaAddressCollectorService.chooseBestEurekaNode(_) >> { '3.3.3.3' }
        url == 'http://3.3.3.3:7001/eur/v2'
    }

    void 'retrieving all app instances should retry and reload eureka node cache on failure'() {

        boolean firstCall = true

        //noinspection GroovyAccessibility
        when:
        List<ApplicationInstance> instances = eurekaService.retrieveInstances(Region.US_WEST_2)

        then:
        2 * restClientService.getAsXml(_, _) >> {
            if (firstCall) {
                firstCall = false
                throw new IOException("Can't reach Eureka server")
            }
            XML.parse(APP_INSTANCES_LIST_XML)
        }
        1 * eurekaAddressCollectorService.fillCache(Region.US_WEST_2) >> { }
        2 * eurekaAddressCollectorService.chooseBestEurekaNode(_) >> { '1.1.1.1' }
        2 * cachedMap.list() >> { ['1.1.1.1', '2.2.2.2', '3.3.3.3'] }
        instances.size() == 2
    }

    void 'getting one app instance by instance ID should retry and reload eureka node cache on failure'() {

        boolean firstCall = true

        when:
        ApplicationInstance instance = eurekaService.getAppInstance(userContext, 'i-deadbeef')

        then:
        2 * restClientService.getAsXml(_) >> {
            if (firstCall) {
                firstCall = false
                throw new IOException("Can't reach Eureka server")
            }
            XML.parse(APP_INSTANCE_XML)
        }
        1 * eurekaAddressCollectorService.fillCache(Region.US_WEST_2) >> { }
        2 * eurekaAddressCollectorService.chooseBestEurekaNode(_) >> { '1.1.1.1' }
        2 * cachedMap.list() >> { ['1.1.1.1', '2.2.2.2', '3.3.3.3'] }
        instance.instanceId == 'i-deadbeef'
    }

    void 'getting one app instance by app name and host name should retry and reload eureka node cache on failure'() {

        boolean firstCall = true

        when:
        ApplicationInstance instance = eurekaService.getAppInstance(userContext, 'fishfood', 'dev-jsmith-mac')

        then:
        2 * restClientService.getAsXml(_) >> {
            if (firstCall) {
                firstCall = false
                throw new IOException("Can't reach Eureka server")
            }
            XML.parse(APP_INSTANCE_XML)
        }
        1 * eurekaAddressCollectorService.fillCache(Region.US_WEST_2) >> { }
        2 * eurekaAddressCollectorService.chooseBestEurekaNode(_) >> { '1.1.1.1' }
        2 * cachedMap.list() >> { ['1.1.1.1', '2.2.2.2', '3.3.3.3'] }
        instance.appName == 'fishfood'
    }

    void 'disabling an instance should retry and reload eureka node cache on failure'() {

        boolean firstCall = true
        boolean instanceIsEnabledInEureka = true

        when:
        eurekaService.disableAppInstances(userContext, 'fishfood', ['i-deadbeef'])

        then:
        2 * restClientService.put(_) >> {
            if (firstCall) {
                firstCall = false
                throw new IOException("Can't reach Eureka server")
            }
            instanceIsEnabledInEureka = false
            200
        }
        !instanceIsEnabledInEureka
        1 * eurekaAddressCollectorService.fillCache(Region.US_WEST_2) >> { }
        2 * eurekaAddressCollectorService.chooseBestEurekaNode(_) >> { '1.1.1.1' }
        2 * cachedMap.list() >> { ['1.1.1.1', '2.2.2.2', '3.3.3.3'] }
    }

    void 'enabling an instance should retry and reload eureka node cache on failure'() {

        boolean firstCall = true
        boolean instanceIsEnabledInEureka = false

        when:
        eurekaService.enableAppInstances(userContext, 'fishfood', ['i-deadbeef'])

        then:
        2 * restClientService.put(_) >> {
            if (firstCall) {
                firstCall = false
                throw new IOException("Can't reach Eureka server")
            }
            instanceIsEnabledInEureka = true
            200
        }
        instanceIsEnabledInEureka
        1 * eurekaAddressCollectorService.fillCache(Region.US_WEST_2) >> { }
        2 * eurekaAddressCollectorService.chooseBestEurekaNode(_) >> { '1.1.1.1' }
        2 * cachedMap.list() >> { ['1.1.1.1', '2.2.2.2', '3.3.3.3'] }
    }

    private String APP_INSTANCES_LIST_XML = """
<applications>
  <application>
    <name>EMP_UI</name>
    <instance>
      <hostName>10.261.13.199</hostName>
      <app>EMP_UI</app>
      <ipAddr>10.261.13.199</ipAddr>
      <status>UP</status>
      <overriddenstatus>UNKNOWN</overriddenstatus>
      <port enabled="true">7001</port>
      <securePort enabled="false">7002</securePort>
      <countryId>1</countryId>
      <dataCenterInfo class="com.netflix.appinfo.AmazonInfo">
        <name>Amazon</name>
        <metadata>
          <availability-zone>us-east-1e</availability-zone>
          <instance-id>i-89e4a4c1</instance-id>
          <public-hostname>10.261.13.199</public-hostname>
          <local-ipv4>10.261.13.199</local-ipv4>
          <ami-id>ami-1b8ee372</ami-id>
          <instance-type>m1.large</instance-type>
        </metadata>
      </dataCenterInfo>
      <leaseInfo>
        <renewalIntervalInSecs>30</renewalIntervalInSecs>
        <durationInSecs>90</durationInSecs>
        <registrationTimestamp>1368473413809</registrationTimestamp>
        <lastRenewalTimestamp>1368486286156</lastRenewalTimestamp>
        <evictionTimestamp>0</evictionTimestamp>
      </leaseInfo>
      <metadata>
        <enableRoute53>true</enableRoute53>
        <route53ttl>60</route53ttl>
        <route53Type>A</route53Type>
        <route53NamePrefix>empui.us-east-1</route53NamePrefix>
      </metadata>
      <homePageUrl>http://10.261.13.199:7001/</homePageUrl>
      <statusPageUrl>http://10.261.13.199:7001/Status</statusPageUrl>
      <healthCheckUrl>http://10.261.13.199:7001/healthcheck</healthCheckUrl>
      <vipAddress>10.261.13.199:7001</vipAddress>
      <isCoordinatingDiscoveryServer>false</isCoordinatingDiscoveryServer>
      <lastUpdatedTimestamp>1368473413809</lastUpdatedTimestamp>
      <lastDirtyTimestamp>1368473393882</lastDirtyTimestamp>
      <actionType>ADDED</actionType>
      <asgName>emp_ui-v017</asgName>
    </instance>
  </application>
  <application>
    <name>EXAMPLECONTENTSERVER</name>
    <instance>
      <hostName>ec2-68-173-221-105.compute-1.amazonaws.com</hostName>
      <app>EXAMPLECONTENTSERVER</app>
      <ipAddr>10.218.31.148</ipAddr>
      <sid>builds</sid>
      <status>UP</status>
      <overriddenstatus>UNKNOWN</overriddenstatus>
      <port enabled="true">7001</port>
      <securePort enabled="false">7002</securePort>
      <countryId>1</countryId>
      <dataCenterInfo class="com.netflix.appinfo.AmazonInfo">
        <name>Amazon</name>
        <metadata>
          <availability-zone>us-east-1d</availability-zone>
          <instance-id>i-9856bcf4</instance-id>
          <public-ipv4>68.173.221.105</public-ipv4>
          <public-hostname>ec2-68-173-221-105.compute-1.amazonaws.com</public-hostname>
          <local-ipv4>10.218.31.148</local-ipv4>
          <ami-id>ami-db2447b1</ami-id>
          <instance-type>m2.2xlarge</instance-type>
        </metadata>
      </dataCenterInfo>
      <leaseInfo>
        <renewalIntervalInSecs>30</renewalIntervalInSecs>
        <durationInSecs>90</durationInSecs>
        <registrationTimestamp>1368382975624</registrationTimestamp>
        <lastRenewalTimestamp>1368486272657</lastRenewalTimestamp>
        <evictionTimestamp>0</evictionTimestamp>
      </leaseInfo>
      <metadata class="java.util.Collections\$EmptyMap"/>
      <homePageUrl>http://ec2-68-173-221-105.compute-1.amazonaws.com:7001/</homePageUrl>
      <statusPageUrl>http://ec2-68-173-221-105.compute-1.amazonaws.com:7001/Status</statusPageUrl>
      <healthCheckUrl>http://ec2-68-173-221-105.compute-1.amazonaws.com:7001/healthcheck</healthCheckUrl>
      <vipAddress>ec2-68-173-221-105.compute-1.amazonaws.com:7001</vipAddress>
      <isCoordinatingDiscoveryServer>false</isCoordinatingDiscoveryServer>
      <lastUpdatedTimestamp>1368382975624</lastUpdatedTimestamp>
      <lastDirtyTimestamp>1368382953816</lastDirtyTimestamp>
      <actionType>ADDED</actionType>
      <asgName>examplecontentserver-test-v001</asgName>
    </instance>
  </application>
</applications>
"""

    private String APP_INSTANCE_XML = """
<instance>
  <hostName>ec2-68-201-34-107.us-west-2.compute.amazonaws.com</hostName>
  <app>FISHFOOD</app>
  <ipAddr>10.268.79.93</ipAddr>
  <status>UP</status>
  <overriddenstatus>UNKNOWN</overriddenstatus>
  <port enabled="true">7001</port>
  <securePort enabled="false">7002</securePort>
  <countryId>1</countryId>
  <dataCenterInfo class="com.netflix.appinfo.AmazonInfo">
    <name>Amazon</name>
    <metadata>
      <availability-zone>us-west-2b</availability-zone>
      <public-ipv4>68.201.34.107</public-ipv4>
      <instance-id>i-deadbeef</instance-id>
      <public-hostname>ec2-68-201-34-107.us-west-2.compute.amazonaws.com</public-hostname>
      <local-ipv4>10.268.79.93</local-ipv4>
      <ami-id>ami-5938d123</ami-id>
      <instance-type>m2.4xlarge</instance-type>
    </metadata>
  </dataCenterInfo>
  <leaseInfo>
    <renewalIntervalInSecs>30</renewalIntervalInSecs>
    <durationInSecs>90</durationInSecs>
    <registrationTimestamp>1368734751061</registrationTimestamp>
    <lastRenewalTimestamp>1368734870644</lastRenewalTimestamp>
    <evictionTimestamp>0</evictionTimestamp>
  </leaseInfo>
  <metadata class="java.util.Collections\$EmptyMap"/>
  <homePageUrl>http://ec2-68-201-34-107.us-west-2.compute.amazonaws.com:7001/</homePageUrl>
  <statusPageUrl>http://ec2-68-201-34-107.us-west-2.compute.amazonaws.com:7001/Status</statusPageUrl>
  <healthCheckUrl>http://ec2-68-201-34-107.us-west-2.compute.amazonaws.com:7001/healthcheck</healthCheckUrl>
  <vipAddress>fishfood:7001</vipAddress>
  <isCoordinatingDiscoveryServer>false</isCoordinatingDiscoveryServer>
  <lastUpdatedTimestamp>1368734751061</lastUpdatedTimestamp>
  <lastDirtyTimestamp>1368734730577</lastDirtyTimestamp>
  <actionType>ADDED</actionType>
  <asgName>fishfood-v000</asgName>
</instance>
"""

}
