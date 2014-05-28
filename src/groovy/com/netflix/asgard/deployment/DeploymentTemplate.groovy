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

import com.netflix.asgard.deployment.steps.CreateAsgStep
import com.netflix.asgard.deployment.steps.DeleteAsgStep
import com.netflix.asgard.deployment.steps.DisableAsgStep
import com.netflix.asgard.deployment.steps.ResizeStep
import com.netflix.asgard.model.AsgRoleInCluster

/**
 * Creates instances of Deployment by name for use as templates.
 */
enum DeploymentTemplate {
    CreateAndCleanUpPreviousAsg({ int capacity ->
        new DeploymentWorkflowOptions(
                steps: [
                        new CreateAsgStep(),
                        new ResizeStep(targetAsg: AsgRoleInCluster.Next, capacity: capacity, startUpTimeoutMinutes: 40),
                        new DisableAsgStep(targetAsg: AsgRoleInCluster.Previous),
                        new DeleteAsgStep(targetAsg: AsgRoleInCluster.Previous)
                ],
        )
    }), CreateOnly({ int capacity ->
        new DeploymentWorkflowOptions(
            steps: [
                    new CreateAsgStep(),
                    new ResizeStep(targetAsg: AsgRoleInCluster.Next, capacity: capacity, startUpTimeoutMinutes: 40)
            ],
        )
    })

    Closure<DeploymentWorkflowOptions> constructDeployment

    DeploymentTemplate(Closure<DeploymentWorkflowOptions> constructDeployment) {
        this.constructDeployment = constructDeployment
    }

    DeploymentWorkflowOptions getDeployment(int capacity = 0) {
        constructDeployment(capacity)
    }

    static DeploymentTemplate of(String name) {
        try {
            (DeploymentTemplate) DeploymentTemplate.valueOf(name)
        } catch (Exception ignore) {
            null
        }
    }
}
