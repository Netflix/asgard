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
package com.netflix.asgard.model

import spock.lang.Specification
import spock.lang.Unroll

class DeploymentSpec extends Specification {

    @Unroll("should indicate for deployment with status '#status' that isDone is #isDone")
    void 'should indicate if deployment is done'() {
        given: 'a deployment with status'
        Deployment deployment = new Deployment(null, null, null, null, null, null, null, null, status)

        expect:
        isDone == deployment.isDone()

        where:
        status      | isDone
        'running'   | false
        'complete'  | true
        'failed'    | true
        ''          | true
    }

    @Unroll
    void 'should indicate duration deployment ran'() {
        given: 'a deployment with status'
        Deployment deployment = new Deployment(null, null, null, null, null, null, startTime, updateTime, 'complete')

        expect:
        duration == deployment.getDurationString()

        where:
        startTime   | updateTime        | duration
        new Date(0) | new Date(0)       | '0s'
        new Date(0) | new Date(1000)    | '1s'
        new Date(0) | new Date(9999999) | '2h 46m 39s'
    }

    void 'should construct step JSON'() {
        expect:
        Deployment.constructStepJson(7) == '{"step":7}'
    }

    void 'should parse step JSON'() {
        expect:
        Deployment.parseStepIndex('{"step":7}') == 7
    }

    void 'should organize log by steps'() {
        Deployment deployment = new Deployment(null, null, null, null, null, null, null, null, null, [
                '{"step":0}',
                'on the first step',
                'still finishing up step one',
                '{"step":1}',
                'now working on the next one',

        ])

        expect:
        deployment.logForSteps == [
                [
                        'on the first step',
                        'still finishing up step one',
                ],
                [
                        'now working on the next one'
                ]
        ]
    }
}
