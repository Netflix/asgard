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

import groovy.transform.Immutable
import org.codehaus.jackson.annotate.JsonCreator
import org.codehaus.jackson.annotate.JsonProperty

/**
 * Names and identification of the previous and next ASGs involved when creating a new ASG for a cluster.
 */
@Immutable class AsgDeploymentNames {

    /** name of the existing auto scaling group used as a template */
    String previousAsgName

    /** name of the existing launch configuration used as a template */
    String previousLaunchConfigName

    /** VPC zone identifier from the previous auto scaling group */
    String previousVpcZoneIdentifier

    /** name of the new auto scaling group being created */
    String nextAsgName

    /** name of the new launch configuration being created */
    String nextLaunchConfigName

    /** VPC zone identifier for the next auto scaling group */
    String nextVpcZoneIdentifier

    @JsonCreator
    static AsgDeploymentNames of(@JsonProperty('previousAsgName') String previousAsgName,
                                 @JsonProperty('previousLaunchConfigName') String previousLaunchConfigName,
                                 @JsonProperty('previousVpcZoneIdentifier') String previousVpcZoneIdentifier,
                                 @JsonProperty('nextAsgName') String nextAsgName,
                                 @JsonProperty('nextLaunchConfigName') String nextLaunchConfigName,
                                 @JsonProperty('nextVpcZoneIdentifier') String nextVpcZoneIdentifier) {
        new AsgDeploymentNames(
                previousAsgName: previousAsgName,
                previousLaunchConfigName: previousLaunchConfigName,
                previousVpcZoneIdentifier: previousVpcZoneIdentifier,
                nextAsgName: nextAsgName,
                nextLaunchConfigName: nextLaunchConfigName,
                nextVpcZoneIdentifier: nextVpcZoneIdentifier
        )
    }

}
