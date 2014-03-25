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
package com.netflix.asgard

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.ec2.model.Instance
import com.netflix.asgard.push.AsgDeletionMode
import java.util.concurrent.CountDownLatch
import spock.lang.Specification

/**
 * Tests for WitherService.
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class WitherServiceSpec extends Specification {

    AwsAutoScalingService awsAutoScalingService = Mock(AwsAutoScalingService)
    ConfigService configService = Mock(ConfigService)
    EnvironmentService environmentService = Mock(EnvironmentService)
    TaskService taskService = Mock(TaskService)
    WitherService witherService = new WitherService(awsAutoScalingService: awsAutoScalingService,
            configService: configService, environmentService: environmentService, taskService: taskService)

    void 'cancelling wither process should do nothing if not withering'() {

        expect:
        witherService.cancelWither() == ['Wither process was not running']
    }

    void 'should cancel running wither process'() {

        CountDownLatch workFinished = new CountDownLatch(1)
        Thread thread = Thread.start {
            try {
                workFinished.await()
            } catch (InterruptedException ignore) { }
        }
        witherService.withering = thread

        expect:
        thread.isAlive()

        when:
        witherService.cancelWither()

        then:
        thread.join(10)
        !thread.isAlive()
    }

    void 'starting wither should fail if wither is already running'() {

        CountDownLatch workFinished = new CountDownLatch(1)
        Thread thread = Thread.start {
            try {
                workFinished.await()
            } catch (InterruptedException ignore) { }
        }
        witherService.withering = thread

        when:
        witherService.startWither()

        then:
        thrown(IllegalStateException)

        cleanup:
        thread.interrupt()
    }

    void 'wither should delete current ASG if it has one instance'() {

        String asgName = 'helloworld-v002'
        AutoScalingGroup asg = new AutoScalingGroup(instances: [new Instance()])
        UserContext userContext = UserContext.auto(Region.US_WEST_1)

        when:
        witherService.startWither()
        witherService.withering.join()

        then:
        1 * configService.userDataVarPrefix >> 'CLOUD_'
        1 * environmentService.getEnvironmentVariable('CLOUD_AUTO_SCALE_GROUP') >> asgName
        1 * environmentService.getEnvironmentVariable('EC2_REGION') >> 'us-west-1'
        1 * taskService.localRunningInMemory >> []
        1 * awsAutoScalingService.getAutoScalingGroup(userContext, asgName) >> asg
        1 * awsAutoScalingService.deleteAutoScalingGroup(userContext, asgName, AsgDeletionMode.FORCE)
        0 * _
    }

    void 'wither should fail if ASG has many instances'() {

        String asgName = 'helloworld-v002'
        AutoScalingGroup asg = new AutoScalingGroup(instances: [new Instance(), new Instance()])
        UserContext userContext = UserContext.auto(Region.US_WEST_1)

        when:
        witherService.startWither()
        witherService.withering.join()

        then:
        1 * configService.userDataVarPrefix >> 'CLOUD_'
        1 * environmentService.getEnvironmentVariable('CLOUD_AUTO_SCALE_GROUP') >> asgName
        1 * environmentService.getEnvironmentVariable('EC2_REGION') >> 'us-west-1'
        1 * taskService.localRunningInMemory >> []
        1 * awsAutoScalingService.getAutoScalingGroup(userContext, asgName) >> asg
        0 * _
    }
}
