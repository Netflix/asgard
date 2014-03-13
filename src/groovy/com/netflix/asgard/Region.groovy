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

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * A way to indicate a choice of region within the Amazon Web Services global service offering.
 */
enum Region {

    US_EAST_1('us-east-1',
            'us-east',
            'us-east-1.png',
            'Virginia'
    ),

    US_WEST_1('us-west-1',
            'us-west',
            'us-west-1.png',
            'California'
    ),

    US_WEST_2('us-west-2',
            'us-west-2',
            'us-west-2.png',
            'Oregon'
    ),

    EU_WEST_1('eu-west-1',
            'eu-ireland',
            'eu-west-1.png',
            'Ireland'
    ),

    AP_NORTHEAST_1('ap-northeast-1',
            'apac-tokyo',
            'ap-northeast-1.png',
            'Tokyo'
    ),

    AP_SOUTHEAST_1('ap-southeast-1',
            'apac-sin',
            'ap-southeast-1.png',
            'Singapore'
    ),

    AP_SOUTHEAST_2('ap-southeast-2',
            'apac-syd',
            'ap-southeast-2.png',
            'Sydney'
    ),

    SA_EAST_1('sa-east-1',
            'sa-east-1',
            'sa-east-1.png',
            'Sao Paulo'
    )

    /**
     * The commonly used code name of the region such as us-east-1.
     */
    String code

    /**
     * The code name of the region used in EC2 pricing json files.
     */
    String pricingJsonCode

    /**
     * The name of the image file to display when the region is selected.
     */
    String mapImageFileName

    /**
     * The geographical location of the region.
     */
    String location

    /**
     * @param code the commonly used code name of the region such as us-east-1
     * @param pricingJsonCode the code name of the region used in EC2 pricing json files
     * @param mapImageFileName the name of the image file to display when the region is selected
     * @param location the geographical location of the region
     */
    Region(String code, String pricingJsonCode, mapImageFileName, location) {
        this.code = code
        this.pricingJsonCode = pricingJsonCode
        this.mapImageFileName = mapImageFileName
        this.location = location
    }

    /**
     * Takes a canonical identifier for an AWS region and returns the matching Region object. If no match exists, this
     * method returns null.
     *
     * @param code a String such as us-east-1 or ap-southeast-1
     * @return Region a matching Region object, or null if no match found
     */
    @JsonCreator
    static Region withCode(String code) {
        Region.values().find { it.code == code || it.name() == code } as Region
    }

    /**
     * Takes a region identifier used in Amazon's pricing JSON data and returns the matching Region object.
     * If no match exists, this method returns null.
     *
     * @param jsonPricingCode a String such as us-east or apac-tokyo
     * @return Region a matching Region object, or null if no match found
     */
    static Region withPricingJsonCode(String pricingJsonCode) {
        return pricingJsonCode ? Region.values().find { it.pricingJsonCode == pricingJsonCode } as Region : null
    }

    /**
     * There are times (such as during development) when it is useful to only use a subset of regions by specifying a
     * system property.
     *
     * @return List < Region > subset of regions if "onlyRegions" system property is specified, otherwise an empty list
     */
    static List<Region> getLimitedRegions() {
        String onlyRegions = System.getProperty('onlyRegions')
        if (onlyRegions) {
            List<String> regionNames = onlyRegions.tokenize(',')
            return regionNames.collect { Region.withCode(it) }
        }
        []
    }
    static Region defaultRegion() { Region.US_EAST_1 }

    /**
     * @return a human readable description of the code name and geographical location of the region
     */
    String getDescription() {
        "$code ($location)"
    }

    /**
     * @return the code name of the region such as us-east-1
     */
    @JsonValue
    @Override
    String toString() { code }
}
