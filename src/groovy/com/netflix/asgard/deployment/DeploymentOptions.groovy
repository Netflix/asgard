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

import com.netflix.asgard.model.InstancePriceType
import groovy.transform.Canonical

/**
 * Attributes of the deployment process itself.
 */
@Canonical class DeploymentOptions {

    /** name of the cluster where the deployment is taking place */
    String clusterName

    /** endpoint where deployment notifications will be sent */
    String notificationDestination

    /** delay before deployment will begin */
    int delayDuration

    /** specify if canary testing be done which will scale the ASG up to a minimal number of instances */
    Boolean doCanary

    /** number of instances used for canary testing */
    int canaryCapacity

    /** time limit for having healthy instances at the canary capacity */
    int canaryStartUpTimeout

    /** time allowed for the canary test */
    int canaryAssessmentDuration

    /** how to proceed after the canary test */
    ProceedPreference scaleUp

    /** time limit for having healthy instances at the desired capacity */
    int desiredCapacityStartUpTimeout

    /** time allowed for the desired capacity assessment */
    int desiredCapacityAssessmentDuration

    /** how to proceed after the desired capacity assessment */
    ProceedPreference disablePreviousAsg

    /** time allowed for the full traffic assessment */
    int fullTrafficAssessmentDuration

    /** how to proceed after the full traffic assessment */
    ProceedPreference deletePreviousAsg

    /** subnet purpose for new ASG */
    String subnetPurpose

    /** will traffic be prevented until the first assessment period */
    Boolean initialTrafficPrevented

    /** will availability zone rebalancing be suspended for new ASG */
    Boolean azRebalanceSuspended

    /** type of pricing for instances in new ASG */
    InstancePriceType instancePriceType
}
