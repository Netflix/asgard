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
import com.netflix.asgard.model.ApplicationInstance

/**
 * This service is separated from MergedInstanceGroupingService in order to avoid a deadlock when initializing
 * interdependent services.
 * MergedInstanceService must finish initializing before AwsAutoScalingService can start to initialize.
 * AwsAutoScalingService must finish initializing before MergedInstanceGroupingService can start to initialize.
 */
class MergedInstanceService {

    static transactional = false

    def awsEc2Service
    def discoveryService

    /**
     * Returns the merged instances for a given list of instance ids
     */
    List<MergedInstance> getMergedInstancesByIds(UserContext userContext, List<String> instanceIds) {
        Collection<Instance> ec2List = awsEc2Service.getInstancesByIds(userContext, instanceIds)
        List<ApplicationInstance> discList = discoveryService.getAppInstancesByIds(userContext, instanceIds)
        List<MergedInstance> instances = ec2List.collect { Instance ec2Inst ->
            ApplicationInstance discInst = discList.find { ApplicationInstance appInst ->
                appInst.instanceId == ec2Inst.instanceId
            }
            new MergedInstance(ec2Inst, discInst)
        }
        instances
    }

    /**
     * Returns a single healthy (running in AWS and UP in Eureka) instance from the specified instanceIds.
     *
     * @param userContext who, where, why
     * @param instanceIds that we will search for health
     * @return a healthy instance or null if none are found
     */
    MergedInstance findHealthyInstance(UserContext userContext, List<String> instanceIds) {
        if (!instanceIds) { return null }
        List<String> runningInstanceIds = awsEc2Service.getInstancesByIds(userContext, instanceIds).
                findAll { it.state.name == 'running' }*.instanceId
        List<MergedInstance> mergedInstances = getMergedInstancesByIds(userContext, runningInstanceIds)
        List<MergedInstance> upMergedInstances = mergedInstances.findAll { it.status == 'UP' }
        upMergedInstances ? upMergedInstances[0] : mergedInstances[0]
    }
}
