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

import com.netflix.asgard.Ensure

/**
 * Data holder for display info about how many instances are reserved, in use, and available in an availability zone,
 * for a given instance type. Useful for making a decision about whether to do a large deployment that would require
 * additional instances.
 */
@Immutable class ZoneAvailability {

    /**
     * The name of the available zone where availability is being measured.
     */
    String zoneName

    /**
     * The number of reservations (used and available) that are have been purchased for the given instance type in
     * the specified availability zone.
     */
    Integer totalReservations

    /**
     * The number of instances of the given instance type currently running in the specified zone.
     */
    Integer usedReservations

    /**
     * Calculates the number of reservations currently available for use for future instances of the given instance type
     * in the specified availability zone. Cannot be less than zero.
     *
     * @return Integer the number of available reservations
     */
    Integer getAvailableReservations() {
        Ensure.bounded(0, totalReservations - usedReservations, totalReservations)
    }

    /**
     * Calculates the approximate percentage (floored to an integer between 0 and 100) of available reservations vs
     * total reservations.
     *
     * @return Integer the percentage of reservations that are currently available for use
     */
    Integer getPercentAvailable() {
        totalReservations ? availableReservations / totalReservations * 100 : 0
    }

    /**
     * Determines whether the number of available reservations for the given instance type and zone is below 25%
     *
     * @return boolean true if the percentage of available reservations is below 25%, false otherwise
     */
    boolean isLow() {
        percentAvailable < 25
    }
}
