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

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.ec2.model.Tag
import com.netflix.asgard.mock.Mocks
import com.amazonaws.services.ec2.model.Image

class MonkeyPatcherTests extends GroovyTestCase {

    void testAddClassNameToStringOutputForAmazonServiceException() {
        AmazonServiceException ase = new AmazonServiceException('Bad things happened')
        ase.errorCode = 'Throttling'
        ase.requestId = '45678'
        ase.statusCode = 400
        ase.serviceName = 'AutoScaling'
        Mocks.monkeyPatcherService()
        assert 'AmazonServiceException: Status Code: 400, AWS Service: AutoScaling, AWS Request ID: 45678, AWS Error Code: Throttling, AWS Error Message: Bad things happened' == ase.toString()
    }

    void testAsgInstanceCopy() {
        Mocks.monkeyPatcherService()
        com.amazonaws.services.autoscaling.model.Instance asgInstance =
            new com.amazonaws.services.autoscaling.model.Instance(
                    availabilityZone: 'us-east-1a',
                    instanceId: 'i-deadbeef',
                    lifecycleState: 'InService',
                    healthStatus: 'healthy',
                    launchConfigurationName: 'superterrifichappyhour'
            )

        com.amazonaws.services.autoscaling.model.Instance asgInstanceCopy = asgInstance.copy()

        assert !asgInstanceCopy.is(asgInstance)
        assert 'us-east-1a' == asgInstanceCopy.availabilityZone
        assert 'i-deadbeef' == asgInstanceCopy.instanceId
        assert 'InService' == asgInstanceCopy.lifecycleState
        assert 'healthy' == asgInstanceCopy.healthStatus
        assert 'superterrifichappyhour' == asgInstanceCopy.launchConfigurationName
    }

    void testEc2InstanceTagGetters() {
        Mocks.monkeyPatcherService()
        com.amazonaws.services.ec2.model.Instance ec2Instance =
            new com.amazonaws.services.ec2.model.Instance(
                    instanceId: 'i-deadbeef',
            ).withTags(new Tag('app', 'helloworld'), new Tag('owner', 'dboreanaz'))

        assert 'dboreanaz' == ec2Instance.owner
        assert 'helloworld' == ec2Instance.app
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    void testImageKeepForever() {
        Mocks.monkeyPatcherService()
        Image image = new Image()
        assertFalse image.keepForever
        image.tags = [new Tag('expiration_time', 'the future')]
        assertFalse image.keepForever
        image.tags = [new Tag('expiration_time', 'never')]
        assertTrue image.keepForever
    }
}
