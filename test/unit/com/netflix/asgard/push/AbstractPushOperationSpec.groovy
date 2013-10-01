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
package com.netflix.asgard.push

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.netflix.asgard.AwsAutoScalingService
import com.netflix.asgard.From
import com.netflix.asgard.UserContext
import com.netflix.asgard.mock.Mocks
import spock.lang.Specification

class AbstractPushOperationSpec extends Specification {

    def awsAutoScalingService = Mock(AwsAutoScalingService)
    AbstractPushOperation pushOperation = new AbstractPushOperation() { }
    UserContext userContext = Mocks.userContext()

    void setup() {
        pushOperation.awsAutoScalingService = awsAutoScalingService
    }

    def 'should return group if it exists'() {
        awsAutoScalingService.getAutoScalingGroup(userContext, 'asg', From.AWS) >> new AutoScalingGroup()

        when:
        AutoScalingGroup group = pushOperation.checkGroupStillExists(userContext, 'asg')

        then:
        group == new AutoScalingGroup()
    }

    def 'should throw PushException if group missing'() {
        awsAutoScalingService.getAutoScalingGroup(userContext, 'asg', From.AWS) >> null

        when:
        AutoScalingGroup group = pushOperation.checkGroupStillExists(userContext, 'asg')

        then:
        Exception e = thrown(PushException)
        e.message == "Group 'asg' can no longer be found."
    }
}
