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
package com.netflix.asgard

class StylerTests extends GroovyTestCase {

    void testAvailabilityZoneToStyleClass() {
        assert 'zoneA' == Styler.availabilityZoneToStyleClass('us-east-1a')
        assert 'zoneB' == Styler.availabilityZoneToStyleClass('us-east-1b')
        assert 'zoneC' == Styler.availabilityZoneToStyleClass('us-east-1c')
        assert 'zoneD' == Styler.availabilityZoneToStyleClass('us-east-1d')

        assert 'zoneA' == Styler.availabilityZoneToStyleClass('us-west-1a')
        assert 'zoneB' == Styler.availabilityZoneToStyleClass('us-west-1b')
        assert 'zoneC' == Styler.availabilityZoneToStyleClass('us-west-1c')

        assert 'zoneA' == Styler.availabilityZoneToStyleClass('eu-west-1a')
        assert 'zoneB' == Styler.availabilityZoneToStyleClass('eu-west-1b')
        assert 'zoneC' == Styler.availabilityZoneToStyleClass('eu-west-1c')

        assert 'zoneA' == Styler.availabilityZoneToStyleClass('ap-southeast-1a')
        assert 'zoneB' == Styler.availabilityZoneToStyleClass('ap-southeast-1b')
        assert 'zoneC' == Styler.availabilityZoneToStyleClass('ap-southeast-1c')

        assert 'zoneA' == Styler.availabilityZoneToStyleClass('ap-northeast-1a')
        assert 'zoneB' == Styler.availabilityZoneToStyleClass('ap-northeast-1b')
        assert 'zoneC' == Styler.availabilityZoneToStyleClass('ap-northeast-1c')

        assertNull Styler.availabilityZoneToStyleClass(null)
        assertNull Styler.availabilityZoneToStyleClass('')
        assertNull Styler.availabilityZoneToStyleClass('hello')
        assertNull Styler.availabilityZoneToStyleClass('us-east-1A')
    }
}
