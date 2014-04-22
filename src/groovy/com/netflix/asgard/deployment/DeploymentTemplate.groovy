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

/**
 * Creates instances of Deployment by name for use as templates.
 */
enum DeploymentTemplate {
    CreateJudgeAndCleanUp({
        newDeploymentWithDefaults
    }), CreateOnly({
        newDeploymentWithDefaults.with{
            scaleUp = ProceedPreference.Yes
            disablePreviousAsg = ProceedPreference.No
            deletePreviousAsg = ProceedPreference.No
            it
        }
    })

    static private DeploymentWorkflowOptions getNewDeploymentWithDefaults() {
        new DeploymentWorkflowOptions(
                delayDurationMinutes: 0,
                doCanary: false,
                canaryCapacity: 1,
                canaryStartUpTimeoutMinutes: 30,
                canaryJudgmentPeriodMinutes: 60,
                scaleUp: ProceedPreference.Ask,
                desiredCapacityStartUpTimeoutMinutes: 40,
                desiredCapacityJudgmentPeriodMinutes: 120,
                disablePreviousAsg: ProceedPreference.Ask,
                fullTrafficJudgmentPeriodMinutes: 240,
                deletePreviousAsg: ProceedPreference.Ask
        )
    }

    Closure<DeploymentWorkflowOptions> customizeDeployment

    DeploymentTemplate(Closure<DeploymentWorkflowOptions> customizeDeployment) {
        this.customizeDeployment = customizeDeployment
    }

    DeploymentWorkflowOptions getDeployment() {
        customizeDeployment()
    }

    static DeploymentTemplate of(String name) {
        try {
            (DeploymentTemplate) DeploymentTemplate.valueOf(name)
        } catch (Exception ignore) {
            null
        }
    }
}
