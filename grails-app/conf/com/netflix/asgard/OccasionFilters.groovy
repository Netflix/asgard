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
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import org.joda.time.chrono.GregorianChronology

class OccasionFilters {

    private static final Integer WEEK_IN_SECONDS = 60 * 60 * 24 * 7
    private static final String APRIL_FOOLS_COOKIE_NAME = 'aprilfoolsjokeviewed'

    static filters = {
        all(controller: '*', action: '*') {
            before = {
                Occasion occasion = Occasion.byName(params.occasionOverride) ?: Occasion.forNow()
                request['occasion'] = occasion

                // The large April Fool's joke is intrusive so only show it once per browser per server.
                if (occasion == Occasion.APRILFOOLS && !request.getCookie(APRIL_FOOLS_COOKIE_NAME)) {
                    request['autoLaunchFullAprilFoolsJoke'] = true

                    // Suppress the large April Fool's joke for future requests
                    response.setCookie(APRIL_FOOLS_COOKIE_NAME, 'true', WEEK_IN_SECONDS)
                }

                // If the last value is falsy and there is no explicit return statement then this filter method will
                // return a falsy value and cause requests to fail silently.
                return true
            }
        }
    }
}

enum Occasion {
    DEFAULT('logo.png', 'Welcome to Asgard'),
    CHRISTMAS('christmas.png', 'Merry Christmas'),
    NEWYEARSEVE('new-year.png', 'Happy New Year'),
    CHINESENEWYEAR('chinese-new-year.png', 'Happy Chinese New Year'),
    VALENTINESDAY('valentines-day.png', "Happy Valentine's Day"),
    MARDIGRAS('mardi-gras.png', 'Happy Mardi Gras'),
    EASTER('easter-basket.png', 'Happy Easter'),
    STPATRICKSDAY('st-patricks-day.png', "Happy St. Patrick's Day"),
    APRILFOOLS('terminator.png', "I am Asgard. Human error will be deleted."),
    CINCODEMAYO('cinco-de-mayo.png', 'Happy Cinco De Mayo'),
    INDEPENDENCEDAY('independence-day.png', 'Happy Independence Day'),
    LABORDAY('labor-day.png', 'Happy Labor Day'),
    HALLOWEEN('halloween.png', 'Happy Halloween!', 'halloween'),
    DAYOFTHEDEAD('day-of-the-dead.png', 'Happy Day of the Dead', 'halloween'),
    THANKSGIVING('thanksgiving.png', 'Happy Thanksgiving')

    // Calculate complicated dates once per server start for the current year.
    private static LocalDate EASTER_DATE = Occasion.getGregorianEasterSunday(new DateTime().year)
    private static LocalDate MARDI_GRAS_DATE = Occasion.getMardiGrasDate(EASTER_DATE)
    private static LocalDate CHINESE_NEW_YEAR_DATE = Occasion.getChineseNewYearDate(new DateTime().year)

    String iconFileName
    String message
    String styleClass

    Occasion(String iconFileName, String message, String styleClass = '') {
        this.iconFileName = iconFileName
        this.message = message
        this.styleClass = styleClass
    }

    static Occasion forNow() {
        DateTime now = new DateTime()
        Integer month = now.monthOfYear
        Integer dayOfMonth = now.dayOfMonth
        if (month == DateTimeConstants.JANUARY) {
            if (dayOfMonth == 1) {
                return NEWYEARSEVE
            } else if (now.toLocalDate() == CHINESE_NEW_YEAR_DATE) {
                return CHINESENEWYEAR
            }
        } else if (month == DateTimeConstants.FEBRUARY) {
            if (dayOfMonth == 14) {
                return VALENTINESDAY
            }
            LocalDate localDate = now.toLocalDate()
            if (localDate == CHINESE_NEW_YEAR_DATE) {
                return CHINESENEWYEAR
            } else if (localDate == MARDI_GRAS_DATE) {
                return MARDIGRAS
            }
        } else if (month == DateTimeConstants.MARCH) {
            if (dayOfMonth == 17) {
                return STPATRICKSDAY
            }
            LocalDate localDate = now.toLocalDate()
            if (localDate == EASTER_DATE) {
                return EASTER
            } else if (localDate == MARDI_GRAS_DATE) {
                return MARDIGRAS
            }
        } else if (month == DateTimeConstants.APRIL) {
            // April Fool's will start at 4am so that the joke text can mention 4:01 am as a significant start time,
            // for an extra clue that it's an April Fool's gag.
            if (dayOfMonth == 1 && now.hourOfDay >= 4) {
                return APRILFOOLS
            } else if (now.toLocalDate() == EASTER_DATE) {
                return EASTER
            }
        } else if (month == DateTimeConstants.MAY) {
            if (dayOfMonth == 5) {
                return CINCODEMAYO
            }
        } else if (month == DateTimeConstants.JULY) {
            if (dayOfMonth == 4) {
                return INDEPENDENCEDAY
            }
        } else if (month == DateTimeConstants.SEPTEMBER) {
            // First Monday in September is Labor Day
            if (dayOfMonth <= 7 && now.dayOfWeek == DateTimeConstants.MONDAY) {
                return LABORDAY
            }
        } else if (month == DateTimeConstants.OCTOBER) {
            if (dayOfMonth == 31) {
                return HALLOWEEN
            }
        } else if (month == DateTimeConstants.NOVEMBER) {
            if (dayOfMonth == 1) {
                return DAYOFTHEDEAD
            }
            // Fourth Thursday in November is Thanksgiving
            if (dayOfMonth >= 22 && dayOfMonth <= 28 && now.dayOfWeek == DateTimeConstants.THURSDAY) {
                return THANKSGIVING
            }
        } else if (month == DateTimeConstants.DECEMBER) {
            // Christmas season is a long and happy time
            if (dayOfMonth >= 23 & dayOfMonth <= 30) {
                return CHRISTMAS
            } else if (dayOfMonth == 31) {
                return NEWYEARSEVE
            }
        }

        return DEFAULT
    }

    static Occasion byName(String name) {
        try {
            return (Occasion) Enum.valueOf(Occasion, name.toUpperCase())
        }
        catch (IllegalArgumentException ignored) {
            return null
        }
        catch (NullPointerException ignored) {
            return null
        }
    }

    /**
     * Copyright 2010 Sven Diedrichsen
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     * http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an
     * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
     * express or implied. See the License for the specific language
     * governing permissions and limitations under the License.
     *
     * @author Sven Diedrichsen
     *
     * https://jollyday.svn.sourceforge.net/svnroot/jollyday/tags/v_0_4_6/src/main/java/de/jollyday/util/CalendarUtil.java
     *
     * Returns the easter sunday within the gregorian chronology.
     *
     * @param year
     * @return gregorian easter sunday.
     */
    static LocalDate getGregorianEasterSunday(int year) {
        int a, b, c, d, e, f, g, h, i, j, k, l;
        int x, month, day;
        a = year % 19;
        b = year / 100;
        c = year % 100;
        d = b / 4;
        e = b % 4;
        f = (b + 8) / 25;
        g = (b - f + 1) / 3;
        h = (19 * a + b - d - g + 15) % 30;
        i = c / 4;
        j = c % 4;
        k = (32 + 2 * e + 2 * i - h - j) % 7;
        l = (a + 11 * h + 22 * k) / 451;
        x = h + k - 7 * l + 114;
        month = x / 31;
        day = (x % 31) + 1;
        new LocalDate(year, (month == 3 ? DateTimeConstants.MARCH : DateTimeConstants.APRIL), day,
                GregorianChronology.getInstance());
    }

    static LocalDate getMardiGrasDate(LocalDate easter) {
        easter.minusDays(47)
    }

    private static LocalDate addDate(Map<Integer, LocalDate> yearToLocalDate, int year, int month, int day) {
        yearToLocalDate.put(year, new LocalDate(year, month, day, GregorianChronology.getInstance()))
    }

    private static LocalDate getChineseNewYearDate(int year) {

        // Chinese New Year is complex. Use known values from
        // http://en.wikipedia.org/wiki/Chinese_new_year and
        // http://www.timeanddate.com/holidays/china/spring-festival
        Map<Integer, LocalDate> map = [:]
        addDate(map, 2012, DateTimeConstants.JANUARY, 23)
        addDate(map, 2013, DateTimeConstants.FEBRUARY, 10)
        addDate(map, 2014, DateTimeConstants.JANUARY, 31)
        addDate(map, 2015, DateTimeConstants.FEBRUARY, 19)
        addDate(map, 2016, DateTimeConstants.FEBRUARY, 8)
        addDate(map, 2017, DateTimeConstants.JANUARY, 28)
        addDate(map, 2018, DateTimeConstants.FEBRUARY, 16)
        addDate(map, 2019, DateTimeConstants.FEBRUARY, 5)
        addDate(map, 2020, DateTimeConstants.JANUARY, 25)
        addDate(map, 2021, DateTimeConstants.FEBRUARY, 12)
        addDate(map, 2022, DateTimeConstants.FEBRUARY, 1)
        addDate(map, 2023, DateTimeConstants.JANUARY, 22)
        addDate(map, 2024, DateTimeConstants.FEBRUARY, 10)
        addDate(map, 2025, DateTimeConstants.JANUARY, 29)
        addDate(map, 2026, DateTimeConstants.FEBRUARY, 17)
        addDate(map, 2027, DateTimeConstants.FEBRUARY, 7)
        addDate(map, 2028, DateTimeConstants.JANUARY, 27)
        addDate(map, 2029, DateTimeConstants.FEBRUARY, 13)
        addDate(map, 2030, DateTimeConstants.FEBRUARY, 3)
        addDate(map, 2031, DateTimeConstants.JANUARY, 23)
        addDate(map, 2032, DateTimeConstants.FEBRUARY, 11)
        addDate(map, 2033, DateTimeConstants.JANUARY, 31)
        addDate(map, 2034, DateTimeConstants.FEBRUARY, 19)
        addDate(map, 2036, DateTimeConstants.JANUARY, 28)
        addDate(map, 2037, DateTimeConstants.FEBRUARY, 15)
        addDate(map, 2038, DateTimeConstants.FEBRUARY, 4)
        addDate(map, 2039, DateTimeConstants.JANUARY, 24)
        addDate(map, 2040, DateTimeConstants.FEBRUARY, 12)
        addDate(map, 2041, DateTimeConstants.FEBRUARY, 1)
        addDate(map, 2042, DateTimeConstants.JANUARY, 22)
        addDate(map, 2043, DateTimeConstants.FEBRUARY, 10)
        addDate(map, 2044, DateTimeConstants.JANUARY, 30)
        addDate(map, 2045, DateTimeConstants.FEBRUARY, 17)
        addDate(map, 2046, DateTimeConstants.FEBRUARY, 6)
        addDate(map, 2047, DateTimeConstants.JANUARY, 26)
        addDate(map, 2048, DateTimeConstants.FEBRUARY, 14)
        addDate(map, 2049, DateTimeConstants.FEBRUARY, 2)
        addDate(map, 2050, DateTimeConstants.JANUARY, 23)
        addDate(map, 2051, DateTimeConstants.FEBRUARY, 11)
        addDate(map, 2052, DateTimeConstants.FEBRUARY, 1)
        addDate(map, 2053, DateTimeConstants.FEBRUARY, 19)
        addDate(map, 2054, DateTimeConstants.FEBRUARY, 8)
        addDate(map, 2055, DateTimeConstants.JANUARY, 28)
        addDate(map, 2056, DateTimeConstants.FEBRUARY, 15)
        addDate(map, 2057, DateTimeConstants.FEBRUARY, 4)
        addDate(map, 2058, DateTimeConstants.JANUARY, 24)
        addDate(map, 2059, DateTimeConstants.FEBRUARY, 12)
        addDate(map, 2060, DateTimeConstants.FEBRUARY, 2)
        addDate(map, 2061, DateTimeConstants.JANUARY, 21)
        addDate(map, 2062, DateTimeConstants.FEBRUARY, 9)
        addDate(map, 2063, DateTimeConstants.JANUARY, 29)
        addDate(map, 2064, DateTimeConstants.FEBRUARY, 17)
        addDate(map, 2065, DateTimeConstants.FEBRUARY, 5)
        addDate(map, 2066, DateTimeConstants.JANUARY, 26)
        addDate(map, 2067, DateTimeConstants.FEBRUARY, 14)
        addDate(map, 2068, DateTimeConstants.FEBRUARY, 3)
        addDate(map, 2069, DateTimeConstants.JANUARY, 23)
        addDate(map, 2070, DateTimeConstants.FEBRUARY, 11)

        map[year]
    }
}
