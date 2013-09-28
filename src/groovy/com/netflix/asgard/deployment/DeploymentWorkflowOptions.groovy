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
package com.netflix.asgard.deployment

import groovy.transform.Canonical

/**
 * Attributes of the deployment process itself.
 */
@Canonical class DeploymentWorkflowOptions {

    /** Name of the cluster where the deployment is taking place */
    String clusterName

    /** Endpoint where deployment notifications will be sent */
    String notificationDestination

    /** Delay before deployment will begin */
    int delayDurationMinutes

    /** Specify if canary testing be done which will scale the ASG up to a minimal number of instances */
    Boolean doCanary

    /** Number of instances used for canary testing */
    int canaryCapacity

    /** Time limit for having healthy instances at the canary capacity */
    int canaryStartUpTimeoutMinutes

    /** Time allowed for the canary test */
    int canaryAssessmentDurationMinutes

    /** How to proceed after the canary test */
    ProceedPreference scaleUp

    /** Time limit for having healthy instances at the desired capacity */
    int desiredCapacityStartUpTimeoutMinutes

    /** Time allowed for the desired capacity assessment */
    int desiredCapacityAssessmentDurationMinutes

    /** How to proceed after the desired capacity assessment */
    ProceedPreference disablePreviousAsg

    /** Time allowed for the full traffic assessment */
    int fullTrafficAssessmentDurationMinutes

    /** How to proceed after the full traffic assessment */
    ProceedPreference deletePreviousAsg

}
