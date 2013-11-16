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

import com.amazonaws.services.ec2.model.Instance
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.ApplicationInstance
import spock.lang.Specification

class MergedInstanceGroupingServiceSpec extends Specification {

    static final String INSTANCE_ID = 'instanceId'
    static final String INSTANCE_ID2 = 'instanceId2'

    DiscoveryService discoveryService = Mock(DiscoveryService)
    AwsEc2Service awsEc2Service = Mock(AwsEc2Service)
    AwsAutoScalingService awsAutoScalingService = Mock(AwsAutoScalingService)
    MergedInstanceGroupingService mergedInstanceGroupingService = new MergedInstanceGroupingService(
        discoveryService: discoveryService,
        awsEc2Service: awsEc2Service,
        awsAutoScalingService: awsAutoScalingService)

    Instance instance = new Instance(instanceId: INSTANCE_ID)
    Instance instance2 = new Instance(instanceId: INSTANCE_ID2)
    ApplicationInstance appInstance = new ApplicationInstance()
    ApplicationInstance appInstance2 = new ApplicationInstance()
    UserContext userContext = Mocks.userContext()

    def 'should create merged instances with ec2 and discovery instance'() {
        appInstance.instanceId = INSTANCE_ID
        appInstance2.instanceId = INSTANCE_ID2
        awsEc2Service.getInstances(userContext) >> [instance, instance2]
        discoveryService.getAppInstances(userContext) >> [appInstance, appInstance2]

        when:
        Collection<MergedInstance> mergedInstances = mergedInstanceGroupingService.getMergedInstances(userContext)

        then:
        mergedInstances?.size() == 2
        mergedInstances[0].ec2Instance == instance
        mergedInstances[0].appInstance == appInstance
        mergedInstances[1].ec2Instance == instance2
        mergedInstances[1].appInstance == appInstance2
    }

    def 'should create merged instances with ec2 instance only'() {
        awsEc2Service.getInstances(userContext) >> [instance, instance2]

        when:
        Collection<MergedInstance> mergedInstances = mergedInstanceGroupingService.getMergedInstances(userContext)

        then:
        mergedInstances?.size() == 2
        mergedInstances[0].ec2Instance == instance
        mergedInstances[0].appInstance == null
        mergedInstances[1].ec2Instance == instance2
        mergedInstances[1].appInstance == null
    }

    def 'should create merged instances with discovery instance only'() {
        discoveryService.getAppInstances(userContext) >> [appInstance, appInstance2]

        when:
        Collection<MergedInstance> mergedInstances = mergedInstanceGroupingService.getMergedInstances(userContext)

        then:
        mergedInstances?.size() == 2
        mergedInstances[0].ec2Instance == null
        mergedInstances[0].appInstance == appInstance
        mergedInstances[1].ec2Instance == null
        mergedInstances[1].appInstance == appInstance2
    }

    def 'should create merged instance when app specified'() {
        appInstance.instanceId = INSTANCE_ID
        discoveryService.getAppInstances(userContext, 'appName') >> [appInstance]
        awsEc2Service.getInstance(userContext, INSTANCE_ID, From.CACHE) >> instance

        when:
        Collection<MergedInstance> mergedInstances = mergedInstanceGroupingService
                .getMergedInstances(userContext, 'appName')

        then:
        mergedInstances?.size() == 1
        mergedInstances[0].ec2Instance == instance
        mergedInstances[0].appInstance == appInstance
    }

}
