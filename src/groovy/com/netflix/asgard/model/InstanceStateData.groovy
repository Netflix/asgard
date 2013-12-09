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
package com.netflix.asgard.model

import groovy.transform.Immutable

/**
 * Data holder for fields from {@link com.amazonaws.services.elasticloadbalancing.model.InstanceState} plus additional
 * helpful fields for display about an instance in an Elastic Load Balancer.
 */
@Immutable class InstanceStateData {

    /** {@link com.amazonaws.services.elasticloadbalancing.model.InstanceState#instanceId} */
    String instanceId

    /** {@link com.amazonaws.services.elasticloadbalancing.model.InstanceState#state} */
    String state

    /** {@link com.amazonaws.services.elasticloadbalancing.model.InstanceState#reasonCode} */
    String reasonCode

    /** {@link com.amazonaws.services.elasticloadbalancing.model.InstanceState#description} */
    String description

    /** The name of the auto scaling group the instance is associated with. */
    String autoScalingGroupName

    /** The availability zone in which the instance resides. */
    String availabilityZone
}
