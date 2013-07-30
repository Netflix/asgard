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

import com.amazonaws.services.simpleworkflow.flow.annotations.Execute
import com.amazonaws.services.simpleworkflow.flow.annotations.GetState
import com.amazonaws.services.simpleworkflow.flow.annotations.Workflow
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions
import com.netflix.asgard.UserContext

/**
 * Method contracts and annotations used for the automatic deployment SWF workflow.
 */
@Workflow
@WorkflowRegistrationOptions(defaultExecutionStartToCloseTimeoutSeconds = 604800L)
interface DeploymentWorkflow {

    /**
     * Start the deployment of a new ASG in an existing cluster.
     *
     * @param userContext who, where, why
     * @param deploymentOptions dictate what the deployment will do
     * @param lcOverrides specify changes to the template launch configuration
     * @param asgOverrides specify changes to the template auto scaling group
     */
    @Execute(version = "1.2")
    void deploy(UserContext userContext, DeploymentOptions deploymentOptions,
                LaunchConfigurationOptions lcOverrides, AutoScalingGroupOptions asgOverrides)

    /**
     * @return current log history of the workflow
     */
    @GetState
    List<String> getLogHistory()
}
