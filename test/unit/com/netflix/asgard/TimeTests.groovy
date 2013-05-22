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

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat

class TimeTests extends GroovyTestCase {

    void testParseZulu() {
        DateTime parsedDateTime = Time.parse('2010-11-08T21:50:33Z')
        assert 2010 == parsedDateTime.year
        assert 11 == parsedDateTime.monthOfYear
        assert 8 == parsedDateTime.dayOfMonth
    }

    void testParseReadable() {
        DateTime parsedDateTime = Time.parse('2011-04-20 16:18:20 HAST').withZone(DateTimeZone.forID('US/Hawaii'))
        assert 2011 == parsedDateTime.year
        assert 4 == parsedDateTime.monthOfYear
        assert 20 == parsedDateTime.dayOfMonth
    }

    void testParseInvalid() {
        assertNull Time.parse('2011-04-20 16:18:20')
        assertNull Time.parse('')
        assertNull Time.parse(null)
    }

    void testNowUtc() {

        // 2011-08-21T16:38:05.087-07:00
        DateTimeZone pacificTime = DateTimeZone.forID('America/Los_Angeles')
        String nowString = new DateTime(pacificTime).toString()
        assert nowString.startsWith('201') // Good for 8 years
        assert nowString.endsWith(':00')
        assert nowString[10] == 'T'

        // 2011-08-21T23:38:05.203Z
        String nowUtcString = Time.nowUtc().toString()
        assert nowUtcString.startsWith('201')
        assert nowUtcString.endsWith('Z')
        assert nowUtcString[10] == 'T'

        // Hour should be different between time zones
        assert nowString[11..12] != nowUtcString[11..12]

        DateTimeFormatter parser = ISODateTimeFormat.dateTime()
        DateTime nowUtcBackToLocal = parser.parseDateTime(nowUtcString).toDateTime(pacificTime)
        String nowUtcBackToLocalString = nowUtcBackToLocal.toString()

        // Year, month, day, hour should be equal between the local date string and the utc date converted back to local
        assert nowUtcBackToLocalString[0..13] == nowString[0..13]
    }
}
