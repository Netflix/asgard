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
package com.netflix.asgard.deployment

import com.fasterxml.jackson.annotation.JsonIgnore
import com.netflix.asgard.deployment.steps.ResizeStep
import com.netflix.asgard.model.AutoScalingGroupBeanOptions
import com.netflix.asgard.model.LaunchConfigurationBeanOptions
import groovy.transform.Canonical

/**
 * Wraps objects that describe a deployment.
 */
@Canonical
class StartDeploymentRequest {
    DeploymentWorkflowOptions deploymentOptions
    LaunchConfigurationBeanOptions lcOptions
    AutoScalingGroupBeanOptions asgOptions

    /**
     * @return List of all validation errors
     */
    @JsonIgnore
    List<String> getValidationErrors() {
        List<String> errors = []
        deploymentOptions.steps.findAll { it instanceof ResizeStep }.each { ResizeStep resizeStep ->
            errors.addAll(checkCapacityBounds(resizeStep.capacity, asgOptions))
        }
        errors
    }

    private List<String> checkCapacityBounds(int capacity, AutoScalingGroupBeanOptions asgOptions) {
        List<String> errors = []
        if (asgOptions.minSize > capacity) {
            errors << "Resize ASG capacity '${capacity}' is less than the ASG's minimum instance bound \
'${asgOptions.minSize}'."
        }
        if (asgOptions.maxSize < capacity) {
            errors << "Resize ASG capacity '${capacity}' is greater than the ASG's maximum instance bound \
'${asgOptions.maxSize}'."
        }
        errors
    }
}
