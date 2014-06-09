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
package com.netflix.asgard.deployment.steps

import com.netflix.asgard.model.AsgRoleInCluster
import groovy.transform.Canonical

@Canonical
class ResizeStep implements DeploymentStep {
    /** Indicates the ASG that will be targeted by this operation */
    AsgRoleInCluster targetAsg

    /** Number of instances to resize to */
    int capacity

    /** Time limit for having operational instances at capacity */
    int startUpTimeoutMinutes
}
