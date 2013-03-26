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

import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.InstanceState
import spock.lang.Specification

class MergedInstanceServiceSpec extends Specification {

    def 'should return a running instance in the cluster, preferably UP'() {
        MergedInstanceService mergedInstanceService = Spy(MergedInstanceService) {
            1 * getMergedInstancesByIds(_, ['i-00000002', 'i-00000003']) >> [
                    new MergedInstance(instanceId: 'i-00000002', status: 'DOWN'),
                    new MergedInstance(instanceId: 'i-00000003', status: 'UP'),
            ]
        }
        AwsEc2Service awsEc2Service = Mock(AwsEc2Service) {
            1 * getInstancesByIds(_, ['i-00000001', 'i-00000002', 'i-00000003']) >> [
                    new Instance(instanceId: 'i-00000001', state: new InstanceState(name: 'stopping')),
                    new Instance(instanceId: 'i-00000002', state: new InstanceState(name: 'running')),
                    new Instance(instanceId: 'i-00000003', state: new InstanceState(name: 'running'))
            ]
        }
        mergedInstanceService.awsEc2Service = awsEc2Service

        expect:
        mergedInstanceService.findHealthyInstance(new UserContext(region: Region.US_EAST_1),
                ['i-00000001', 'i-00000002', 'i-00000003']).instanceId == 'i-00000003'
    }

    def 'should return a DOWN instances as last resort'() {
        MergedInstanceService mergedInstanceService = Spy(MergedInstanceService) {
            1 * getMergedInstancesByIds(_, ['i-00000002']) >> [
                    new MergedInstance(instanceId: 'i-00000002', status: 'DOWN')
            ]
        }
        mergedInstanceService.awsEc2Service = Mock(AwsEc2Service) {
            1 * getInstancesByIds(_, ['i-00000001', 'i-00000002']) >> [
                    new Instance(instanceId: 'i-00000001', state: new InstanceState(name: 'stopping')),
                    new Instance(instanceId: 'i-00000002', state: new InstanceState(name: 'running'))
            ]
        }

        expect:
        mergedInstanceService.findHealthyInstance(new UserContext(region: Region.US_EAST_1),
                ['i-00000001', 'i-00000002']).instanceId == 'i-00000002'
    }

    def 'should return null when there are not any instances in the cluster'() {
        MergedInstanceService mergedInstanceService = new MergedInstanceService()

        expect:
        mergedInstanceService.findHealthyInstance(new UserContext(region: Region.US_EAST_1), []) == null
    }
}
