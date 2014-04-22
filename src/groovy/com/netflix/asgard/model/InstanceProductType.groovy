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
package com.netflix.asgard.model

/**
 * The set of available operating system and security combinations for instances. This concern is orthogonal to
 * hardware choice and price type.
 */
enum InstanceProductType {

    LINUX_UNIX('Linux/UNIX', 'linux')

    /** The name of the product type in Amazon's spot pricing history API. */
    String spotPricingName

    /** Product types with this field set should have JSON-derived on-demand and reserved prices. */
    String jsonPricingName

    InstanceProductType(String spotPricingName, String jsonPricingName = null) {
        this.spotPricingName = spotPricingName
        this.jsonPricingName = jsonPricingName
    }

    /**
     * Returns true or false depending on the value -- if linux or os then true, otherwise false
     * @param productType String like linux or os
     * @return true if there is a match
     */
    boolean didProductTypeMatch(String productType){
        productType == this.jsonPricingName || productType == 'os'
    }
}
