/*
 * Copyright 2014 Netflix, Inc.
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

import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.Tag
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.netflix.asgard.MergedInstance
import com.netflix.asgard.MonkeyPatcherService
import com.netflix.frigga.ami.AppVersion
import spock.lang.Specification

class GroupedInstanceSpec extends Specification {

    void setup() {
        new MonkeyPatcherService().createDynamicMethods()
    }

    def 'should include fields from input objects including app instance from Eureka'() {
        Date launchTime = new Date(1394486241000)
        String publicDnsName = 'ec2-102-33-120-162.compute-1.amazonaws.com'
        String publicIpAddress = '102.33.120.162'
        String privateDnsName = 'ip-10-200-33-222.example.com'
        String privateIpAddress = '10.200.33.222'
        String instanceId = 'i-deadbeef'
        String appVersion = 'helloworld-1.4.0-1140443.h420/build-huxtable/420'
        String healthCheckUrl = 'http://amihealthy'
        String zone = 'us-west-1a'
        String launchConfigName = 'helloworld-v031-20131013103142'
        String imageId = 'ami-deadbeef'
        String lifeCycleState = 'InService'
        String port = '7001'
        Collection<LoadBalancerDescription> lbs = [new LoadBalancerDescription(loadBalancerName: 'hello-frontend')]
        Instance ec2Instance = new Instance(instanceId: instanceId, publicDnsName: publicDnsName,
                publicIpAddress: publicIpAddress, privateDnsName: privateDnsName, privateIpAddress: privateIpAddress,
                launchTime: launchTime)
        ApplicationInstance appInstance = new ApplicationInstance(hostName: publicDnsName, status: 'UP',
                healthCheckUrl: healthCheckUrl, port: port)
        MergedInstance mergedInstance = new MergedInstance(ec2Instance, appInstance)
        Image image = new Image(imageId: imageId, tags: [new Tag(key: 'appversion', value: appVersion)])
        def asgInstance = new com.amazonaws.services.autoscaling.model.Instance(instanceId: instanceId,
                availabilityZone: zone, lifecycleState: lifeCycleState, healthStatus: 'Healthy',
                launchConfigurationName: launchConfigName)

        when:
        GroupedInstance result = GroupedInstance.from(asgInstance, lbs, mergedInstance, image)

        then:
        result.appVersion == AppVersion.parseName(appVersion)
        result.availabilityZone == zone
        result.buildJobName == 'build-huxtable'
        result.buildNumber == '420'
        result.discoveryStatus == 'UP'
        result.healthCheckUrl == healthCheckUrl
        result.healthStatus == 'Healthy'
        result.hostName == publicDnsName
        result.imageId == imageId
        result.instanceId == instanceId
        result.launchConfigurationName == launchConfigName
        result.launchTime.time == launchTime.time
        result.lifecycleState == lifeCycleState
        result.loadBalancers == ['hello-frontend']
        result.port == port
        result.publicDnsName == publicDnsName
        result.publicIpAddress == publicIpAddress
        result.privateDnsName == privateDnsName
        result.privateIpAddress == privateIpAddress
    }
}
