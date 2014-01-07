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

import com.amazonaws.services.simpleworkflow.flow.DataConverter
import com.amazonaws.services.simpleworkflow.flow.JsonDataConverter
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import spock.lang.Specification

class ScheduledAnalysisSpec extends Specification {

    def 'should convert to JSON and back'() {
        ScheduledAsgAnalysis expectedScheduledAnalysis = new ScheduledAsgAnalysis('cmccoy: us-east-1.prod: \
helloclay--test_instance_count-canary-analysis Tue Jan 21 17:29:33 GMT 2014', new DateTime(DateTimeZone.UTC).
                withDate(2013, 11, 1).withTime(3, 1, 55, 0))
        DataConverter dataConverter = new JsonDataConverter()

        when:
        String data = dataConverter.toData(expectedScheduledAnalysis)
        ScheduledAsgAnalysis actualScheduledAnalysis = dataConverter.fromData(data, ScheduledAsgAnalysis)

        then:
        expectedScheduledAnalysis == actualScheduledAnalysis
    }
}
