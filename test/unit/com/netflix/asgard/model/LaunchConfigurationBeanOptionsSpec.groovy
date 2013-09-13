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
package com.netflix.asgard.model

import com.amazonaws.services.autoscaling.model.BlockDeviceMapping
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest
import com.amazonaws.services.autoscaling.model.Ebs
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.netflix.asgard.SpotInstanceRequestService
import spock.lang.Specification

class LaunchConfigurationBeanOptionsSpec extends Specification {

    LaunchConfigurationBeanOptions lcOptions = new LaunchConfigurationBeanOptions(
            launchConfigurationName: 'launchConfigurationName1',
            imageId: 'imageId1',
            keyName: 'keyName1',
            securityGroups: ['sg-1'],
            userData: 'userData1',
            instanceType: 'instanceType1',
            kernelId: 'kernelId1',
            ramdiskId: 'ramdiskId1',
            blockDeviceMappings: [new BlockDeviceMapping(deviceName: 'deviceName1', ebs: new Ebs(volumeSize: 256))],
            instanceMonitoring: null,
            instancePriceType: InstancePriceType.ON_DEMAND,
            iamInstanceProfile: 'iamInstanceProfile1',
            ebsOptimized: false
    )

    LaunchConfiguration awsLaunchConfiguration = new LaunchConfiguration(
            launchConfigurationName: 'launchConfigurationName1',
            imageId: 'imageId1',
            keyName: 'keyName1',
            securityGroups: ['sg-1'],
            userData: 'userData1',
            instanceType: 'instanceType1',
            kernelId: 'kernelId1',
            ramdiskId: 'ramdiskId1',
            blockDeviceMappings: [new BlockDeviceMapping(deviceName: 'deviceName1', ebs: new Ebs(volumeSize: 256))],
            instanceMonitoring: null,
            iamInstanceProfile: 'iamInstanceProfile1',
            ebsOptimized: false
    )
    CreateLaunchConfigurationRequest createLaunchConfigurationRequest = new CreateLaunchConfigurationRequest(
            launchConfigurationName: 'launchConfigurationName1',
            imageId: 'imageId1',
            keyName: 'keyName1',
            securityGroups: ['sg-1'],
            userData: 'userData1',
            instanceType: 'instanceType1',
            kernelId: 'kernelId1',
            ramdiskId: 'ramdiskId1',
            blockDeviceMappings: [new BlockDeviceMapping(deviceName: 'deviceName1', ebs: new Ebs(volumeSize: 256))],
            instanceMonitoring: null,
            iamInstanceProfile: 'iamInstanceProfile1',
            ebsOptimized: false
    )

    def 'should deep copy'() {
        when:
        LaunchConfigurationBeanOptions actualLc = LaunchConfigurationBeanOptions.from(lcOptions)

        then:
        lcOptions == actualLc

        when:
        actualLc.blockDeviceMappings.iterator().next().deviceName = 'deviceName2'

        then:
        lcOptions != actualLc
    }

    def 'should copy with null collection'() {
        lcOptions.securityGroups = null

        when:
        LaunchConfigurationBeanOptions actualLc = LaunchConfigurationBeanOptions.from(lcOptions)

        then:
        lcOptions == actualLc

        when:
        actualLc.blockDeviceMappings.iterator().next().deviceName = 'deviceName2'

        then:
        lcOptions != actualLc
    }

    def 'should create from AWS LaunchConfiguration'() {
        expect:
        LaunchConfigurationBeanOptions.from(awsLaunchConfiguration) == lcOptions
    }

    def 'should create from AWS LaunchConfiguration with spot price'() {
        awsLaunchConfiguration.spotPrice = '100'
        lcOptions.instancePriceType = InstancePriceType.SPOT

        expect:
        LaunchConfigurationBeanOptions.from(awsLaunchConfiguration) == lcOptions
    }

    def 'should create CreateLaunchConfigurationRequest'() {
        expect:
        lcOptions.getCreateLaunchConfigurationRequest(null, null) == createLaunchConfigurationRequest
    }

    def 'should create CreateLaunchConfigurationRequest with spot price'() {
        SpotInstanceRequestService mockSpotInstanceRequestService = Mock(SpotInstanceRequestService)
        lcOptions.instancePriceType = InstancePriceType.SPOT

        when:
        CreateLaunchConfigurationRequest request = lcOptions.getCreateLaunchConfigurationRequest(null,
                mockSpotInstanceRequestService)

        then:
        request == createLaunchConfigurationRequest.withSpotPrice('50')
        1 * mockSpotInstanceRequestService.recommendSpotPrice(null, 'instanceType1') >> '50'
    }
}
