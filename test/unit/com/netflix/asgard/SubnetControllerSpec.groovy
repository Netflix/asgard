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

import com.netflix.asgard.model.SubnetData
import com.netflix.asgard.model.Subnets
import grails.plugin.spock.ControllerSpec

import static com.netflix.asgard.model.SubnetData.Target.ec2
import static com.netflix.asgard.model.SubnetData.Target.elb
import static com.netflix.asgard.model.SubnetsSpec.subnet

@SuppressWarnings("GroovyPointlessArithmetic")
class SubnetControllerSpec extends ControllerSpec {

    void setup() {
        TestUtils.setUpMockRequest()
        controller.awsEc2Service = Mock(AwsEc2Service)
    }

    def 'list should display subnets'() {
        controller.awsEc2Service.getSubnets(_) >> new Subnets(allSubnets: [
                subnet('subnet-e9b0a3a1', 'us-east-1a', 'internal', ec2),
                subnet('subnet-e9b0a3a1', 'us-east-1b', 'internal', ec2),
                subnet('subnet-e9b0a3a1', 'us-east-1a', 'internal', null),
                subnet('subnet-e9b0a3a1', 'us-east-1a', 'internal', elb),
                subnet('subnet-e9b0a3a1', 'us-east-1a', 'external', ec2),
        ])
        List<SubnetData> expectedSortedSubnets = [
                subnet('subnet-e9b0a3a1', 'us-east-1a', 'external', ec2),
                subnet('subnet-e9b0a3a1', 'us-east-1a', 'internal', null),
                subnet('subnet-e9b0a3a1', 'us-east-1a', 'internal', ec2),
                subnet('subnet-e9b0a3a1', 'us-east-1a', 'internal', elb),
                subnet('subnet-e9b0a3a1', 'us-east-1b', 'internal', ec2),
        ]

        when:
        final attrs = controller.list()

        then:
        attrs.subnets == expectedSortedSubnets
    }
}
