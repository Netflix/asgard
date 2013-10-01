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

import java.util.regex.Matcher

class Styler {

    // Cache of zone style classes to avoid recalculating them.
    private static Map<String, String> zonesToStyleClasses = [:]

    /**
     * Match patterns like us-east-1a and ap-southeast-1c, plucking out the final letter after the number.
     *
     * @param zone string like us-east-1a
     * @return String style class name like zoneA
     * @throws AssertionError if zone does not end in "hyphen, digit, lowercase letter"
     */
    static String availabilityZoneToStyleClass(String zone) {
        if (zonesToStyleClasses[zone]) {
            return zonesToStyleClasses[zone]
        }
        Matcher zoneMatcher = zone =~ /^.*?-[0-9]([a-z])$/
        if (zoneMatcher.matches()) {
            String zoneLetter = zoneMatcher[0][1]
            return zonesToStyleClasses[zone] = "zone${zoneLetter.toUpperCase()}"
        }
        return null
    }

}
