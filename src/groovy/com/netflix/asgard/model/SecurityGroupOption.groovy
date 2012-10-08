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
 * Data holder for the accessibility state of one SecurityGroup or load-balancer-oriented SourceSecurityGroup in
 * relation to another SecurityGroup. Useful for displaying a list of selected and unselected security groups and the
 * port ranges on which they are allowed ingress.
 */
@Immutable class SecurityGroupOption {

    /**
     * Name of the security group of the source of traffic
     */
    String source

    /**
     * Name of the security group of the target of traffic
     */
    String target

    /**
     * Whether ingress is allowed between the two security groups in the relationship
     */
    boolean allowed

    /**
     * The port ranges on which ingress is allowed, or the default port ranges
     */
    String ports
}
