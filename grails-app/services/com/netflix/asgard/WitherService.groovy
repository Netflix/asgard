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
import com.netflix.asgard.push.AsgDeletionMode
import com.netflix.asgard.userdata.UserDataPropertyKeys

/**
 * Handles the work involved in making an Asgard instance "wither", that is, waiting for all in-memory tasks to end and
 * then trying to self-destruct the current Asgard instance. This is useful during deployments of new versions of
 * Asgard, where some long-running user tasks are still being performed by in-memory threads.
 *
 * When all in-memory tasks are rewritten to use Amazon Simple Workflow Service (SWF), we will no longer need this class
 * nor the concept of withering. And there will be much rejoicing.
 */
class WitherService {

    static transactional = false

    def configService
    def environmentService
    def taskService
    def awsAutoScalingService
    Thread withering

    /**
     * Aborts the current withering thread so the current server will not be shut down.
     */
    List<String> cancelWither() {
        if (withering?.isAlive()) {
            withering.interrupt()
            withering = null
            return ['Wither process cancelled']
        }
        ['Wither process was not running']
    }

    /**
     * Starts a thread that will wait until there are zero local in-memory tasks, and then will check that there is only
     * one instance in the current server's Auto Scaling Group. If so, then the withering thread will force delete the
     * ASG, indirectly causing the instance to terminate.
     */
    void startWither() {

        // If process is already running then give up
        if (withering?.isAlive()) {
            throw new IllegalStateException('Withering process is already running.')
        }

        withering = Thread.start('Wither process will drain local in-memory tasks then terminate server') {

            String prefix = configService.userDataVarPrefix
            String asgName = environmentService.getEnvironmentVariable("${prefix}AUTO_SCALE_GROUP")
            String regionCode = environmentService.getEnvironmentVariable(UserDataPropertyKeys.EC2_REGION)
            Region region = Region.withCode(regionCode)
            if (!asgName || !region) {
                throw new IllegalStateException("Cannot wither in ASG '${asgName}' in region '${regionCode}'")
            }
            while (taskService.localRunningInMemory.size() > 0) {
                Time.sleepCancellably 10
            }
            UserContext userContext = UserContext.auto(region)
            AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, asgName)
            if (!group) {
                throw new IllegalStateException("ASG '${asgName}' not found in region '${region}'")
            }
            if (group.instances.size() != 1) {
                String string = "ASG '${asgName}' in '${region}' has ${group.instances.size()} instances instead of 1"
                throw new IllegalStateException(string)
            }
            log.debug "Force delete ASG ASG ${asgName} in region ${region}"
            awsAutoScalingService.deleteAutoScalingGroup(userContext, asgName, AsgDeletionMode.FORCE)
        }
    }
}
