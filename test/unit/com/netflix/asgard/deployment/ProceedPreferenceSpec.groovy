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
package com.netflix.asgard.deployment

import spock.lang.Specification
import spock.lang.Unroll

class ProceedPreferenceSpec extends Specification {

    @Unroll('should parse String #value into #proceedPreference')
    def 'should parse String into appropriate ProceedPreference'() {
        expect:
        ProceedPreference.parse(value) == proceedPreference

        where:
        value   |   proceedPreference
        'Yes'   |   ProceedPreference.Yes
        'yes'   |   ProceedPreference.Yes
        'yeS'   |   ProceedPreference.Yes
        'yeah'  |   ProceedPreference.Ask
        'No'    |   ProceedPreference.No
        'no'    |   ProceedPreference.No
        'NO'    |   ProceedPreference.No
        'nah'   |   ProceedPreference.Ask
        'Ask'   |   ProceedPreference.Ask
        'ask'   |   ProceedPreference.Ask
        'wat'   |   ProceedPreference.Ask
        ''      |   ProceedPreference.Ask
        null    |   ProceedPreference.Ask
    }
}
