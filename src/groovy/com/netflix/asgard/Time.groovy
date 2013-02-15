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

import org.apache.commons.lang.time.DateUtils
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Duration
import org.joda.time.Period
import org.joda.time.PeriodType
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder

class Time {

    private static final String READABLE_ISO_FORMAT_STRING = 'yyyy-MM-dd HH:mm:ss z'
    private static final String ZULU_ISO_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    static final PeriodFormatter DAY_HR_MIN_SEC = new PeriodFormatterBuilder().
            appendDays().appendSuffix("d").appendSeparator(" ").
            appendHours().appendSuffix("h").appendSeparator(" ").
            appendMinutes().appendSuffix("m").appendSeparator(" ").
            appendSeconds().appendSuffix("s").toFormatter()

    static final PeriodFormatter DAY_HR_MIN = new PeriodFormatterBuilder().
            appendDays().appendSuffix("d").appendSeparator(" ").
            appendHours().appendSuffix("h").appendSeparator(" ").
            appendMinutes().appendSuffix("m").appendSeparator(" ").toFormatter()

    static String format(Period period, PeriodFormatter periodFormatter = DAY_HR_MIN_SEC) {
        periodFormatter.print(period)
    }

    static String format(Duration duration, PeriodFormatter periodFormatter = DAY_HR_MIN_SEC) {
        format(duration.toPeriod(PeriodType.dayTime().withMillisRemoved()), periodFormatter)
    }

    static String format(DateTime start, DateTime end, PeriodFormatter periodFormatter = DAY_HR_MIN_SEC) {
        format(new Period(start, end, PeriodType.dayTime().withMillisRemoved()), periodFormatter)
    }

    static String format(Date date) {
        date.format(READABLE_ISO_FORMAT_STRING).toString()
    }

    static String format(DateTime dateTime) {
        dateTime.toDate().format(READABLE_ISO_FORMAT_STRING).toString()
    }

   static DateTime parse(String input) {
        try {
            String[] formats = [READABLE_ISO_FORMAT_STRING, ZULU_ISO_FORMAT_STRING]
            Date parsedDate = DateUtils.parseDate(input, formats)
            return new DateTime(parsedDate.time)
        } catch (Exception ignored) {
            // Can't parse, so return null
        }
        null
    }

    static void sleepCancellably(Long millis) {
        sleep millis, { InterruptedException ie -> throw new CancelledException() }
    }

    static DateTime now() { new DateTime() }

    static DateTime nowUtc() { new DateTime(DateTimeZone.UTC) }

    static String nowReadable() { format(now()) }
}
