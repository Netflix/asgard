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
package com.netflix.asgard.model

import grails.converters.XML
import spock.lang.Specification

class ApplicationInstanceSpec extends Specification {

    String instanceXml = '''<instance>
          <hostName>hostName</hostName>
          <app>APP</app>
          <ipAddr>255.255.255.0</ipAddr>
          <version>1</version>
          <sid>1</sid>
          <status>UP</status>
          <port enabled="true">8888</port>
          <securePort enabled="false">9999</securePort>
          <countryId>1</countryId>
          <identifyingAttribute>EC2_INSTANCE_ID</identifyingAttribute>
          <dataCenterInfo class="com.netflix.appinfo.AmazonInfo">
            <name>Amazon</name>
            <metadata>
              <availability-zone>us-east-1c</availability-zone>
              <public-ipv4>111.222.333.444</public-ipv4>
              <instance-id>instanceId</instance-id>
              <public-hostname>something.compute-1.amazonaws.com</public-hostname>
              <local-ipv4>1.1.1.1</local-ipv4>
              <ami-id>ami-1111111</ami-id>
              <instance-type>m2.2xlarge</instance-type>
            </metadata>
          </dataCenterInfo>
          <leaseInfo>
            <renewalIntervalInSecs>30</renewalIntervalInSecs>
            <durationInSecs>90</durationInSecs>
            <registrationTimestamp>1333504274856</registrationTimestamp>
            <lastRenewalTimestamp>1333575889624</lastRenewalTimestamp>
            <evictionTimestamp>0</evictionTimestamp>
            <clock>2395</clock>
          </leaseInfo>
          <metadata class="java.util.Collections$EmptyMap"/>
          <homePageUrl>http://something.compute-1.amazonaws.com:7001/</homePageUrl>
          <statusPageUrl>http://something.compute-1.amazonaws.com:7001/Status</statusPageUrl>
          <healthCheckUrl>http://something.compute-1.amazonaws.com:7001/healthcheck</healthCheckUrl>
          <vipAddress>something</vipAddress>
          <isCoordinatingDiscoveryServer>false</isCoordinatingDiscoveryServer>
          <lastUpdatedTimestamp>1333504274856</lastUpdatedTimestamp>
          <actionType>ADDED</actionType>
        </instance>'''

    def 'should parse xml and read instance id'() {
        when:
        ApplicationInstance instance = ApplicationInstance.fromXml(XML.parse(instanceXml))

        then:
        'instanceId' == instance.instanceId
    }

}
