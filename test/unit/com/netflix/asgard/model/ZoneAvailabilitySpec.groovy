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

import spock.lang.Specification

class ZoneAvailabilitySpec extends Specification {

    def 'zone availability should be low if and only if available is less than 25% of total'() {
        when:
        ZoneAvailability manyAvailable = new ZoneAvailability(totalReservations: 10, usedReservations: 3)
        ZoneAvailability fewAvailable = new ZoneAvailability(totalReservations: 10, usedReservations: 8)

        then:
        !manyAvailable.low
        manyAvailable.availableReservations == 7
        manyAvailable.percentAvailable == 70
        fewAvailable.low
        fewAvailable.availableReservations == 2
        fewAvailable.percentAvailable == 20
    }

    def 'zone availability should have a minimum of zero'() {
        when:
        ZoneAvailability zoneAvailability = new ZoneAvailability(totalReservations: 1, usedReservations: 3)

        then:
        zoneAvailability.percentAvailable == 0
        zoneAvailability.availableReservations == 0
    }
}
