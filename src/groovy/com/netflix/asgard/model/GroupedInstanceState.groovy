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

import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

/**
 * A permutation of variables defining the current state of an instance. Used for grouping instances by their current
 * state.
 */
class GroupedInstanceState {

    String discoveryStatus
    String imageId
    String buildJobName
    String buildNumber
    List<String> loadBalancers
    String lifecycleState

    boolean equals(Object obj) {
        if (obj == null) { return false }
        if (obj.is(this)) { return true }
        if (obj.getClass() != getClass()) {
            return false
        }
        GroupedInstanceState that = (GroupedInstanceState) obj
        new EqualsBuilder().
                append(lifecycleState, that.lifecycleState).
                append(loadBalancers, that.loadBalancers).
                append(buildJobName, that.buildJobName).
                append(buildNumber, that.buildNumber).
                append(imageId, that.imageId).
                append(discoveryStatus, that.discoveryStatus).
                isEquals()
    }

    int hashCode() {
        new HashCodeBuilder(17, 37).
                append(lifecycleState).
                append(loadBalancers).
                append(buildJobName).
                append(buildNumber).
                append(imageId).
                append(discoveryStatus).
                toHashCode()
    }
}
