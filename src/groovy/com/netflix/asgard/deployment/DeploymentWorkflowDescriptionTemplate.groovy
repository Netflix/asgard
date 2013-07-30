/*
 * Copyright 2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.netflix.asgard.deployment

import com.netflix.asgard.UserContext
import com.netflix.asgard.flow.WorkflowDescriptionTemplate

/**
 * Construct the description for a specific deployment workflow execution.
 */
class DeploymentWorkflowDescriptionTemplate extends WorkflowDescriptionTemplate implements DeploymentWorkflow {

    @Override
    void deploy(UserContext userContext, DeploymentOptions deploymentOptions,
                LaunchConfigurationOptions lcOverrides, AutoScalingGroupOptions asgOverrides) {
        description = "Deploying new ASG to cluster '${deploymentOptions.clusterName}'"
    }
}
