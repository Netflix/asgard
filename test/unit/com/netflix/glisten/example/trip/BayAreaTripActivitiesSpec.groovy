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
package com.netflix.glisten.example.trip

import com.netflix.glisten.Activity
import com.netflix.glisten.example.trip.BayAreaLocation
import com.netflix.glisten.example.trip.BayAreaTripActivitiesImpl
import spock.lang.Specification

class BayAreaTripActivitiesSpec extends Specification {

    Activity mockActivity = Mock(Activity)
    BayAreaTripActivitiesImpl bayAreaTripActivities = new BayAreaTripActivitiesImpl(
            activity: mockActivity, hikeNameToLengthInSteps: ['there': 3]
    )

    def 'should go to Monterey'() {
        expect:
        bayAreaTripActivities.goTo('Clay', BayAreaLocation.Monterey) == 'Clay went to Monterey Bay.'
    }

    def 'should enjoy something'() {
        expect:
        bayAreaTripActivities.enjoy('ice cream') == 'And enjoyed ice cream.'
    }

    def 'should hike'() {
        when:
        String expectedResult = bayAreaTripActivities.hike('there')

        then:
        expectedResult == 'And hiked there.'
        with(mockActivity) {
            1 * recordHeartbeat('Took 1 steps.')
            1 * recordHeartbeat('Took 2 steps.')
            1 * recordHeartbeat('Took 3 steps.')
        }
        0 * _
    }

    def 'should win'() {
        bayAreaTripActivities.isWinner = { true }

        expect:
        bayAreaTripActivities.win('a chess match') == 'And won a chess match.'
    }

    def 'should lose'() {
        bayAreaTripActivities.isWinner = { false }

        when:
        bayAreaTripActivities.win('a chess match')

        then:
        IllegalStateException e = thrown()
        e.message == 'And lost a chess match.'
    }

    def 'should ask question'() {
        when:
        bayAreaTripActivities.askYesNoQuestion('Are you going to answer this question with a lie?')

        then:
        with(mockActivity) {
            1 * getTaskToken()
            1 * getWorkflowExecution()
        }
    }

}
