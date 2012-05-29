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

class CheckTests extends GroovyTestCase {

    void testLoneOrNone() {
        Collection<String> strings = ['Olive Yew']
        assert 'Olive Yew' == Check.loneOrNone(strings, String)

        Collection<Integer> integers = []
        assertNull Check.loneOrNone(integers, Integer)

        boolean illegalStateThrown = false
        Collection<String> moreStrings = ['Brock Lee', 'Sue Flay']
        try {
            Check.loneOrNone(moreStrings, String)
        } catch (IllegalStateException ise) {
            assert ise.message.startsWith('ERROR: Found 2 String items instead of 0 or 1')
            illegalStateThrown = true
        }
        assert illegalStateThrown
    }

    void testPositive() {
        assert 1 == Check.positive(1)
        shouldFail(IllegalArgumentException, { Check.positive(0) })
        shouldFail(IllegalArgumentException, { Check.positive(-1) })
        shouldFail(NullPointerException, { Check.positive(null) })
    }

    void testAtLeast() {
        assert 100 == Check.atLeast(1, 100)
        assert 100 == Check.atLeast(99, 100)
        assert -2 == Check.atLeast(-3, -2)
        shouldFail(IllegalArgumentException, { Check.atLeast(90, 10) })
    }

    void testAtMost() {
        assert 1 == Check.atMost(100, 1)
        assert 99 == Check.atMost(100, 99)
        assert -8 == Check.atMost(-1, -8)
        shouldFail(IllegalArgumentException, { Check.atMost(20, 55) })
    }
}
