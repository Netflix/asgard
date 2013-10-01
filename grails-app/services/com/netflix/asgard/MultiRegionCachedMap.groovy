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

import com.google.common.collect.ImmutableSet
import com.netflix.asgard.cache.Fillable

/**
 * An outer container for CachedMap instances stored by Region.
 *
 * @see CachedMap
 * @param < T > the type of object to store as a value in each CachedMap
 */
class MultiRegionCachedMap<T> implements Fillable {

    private Map<Region, CachedMap> regionsToCachedMaps = [:]

    private Boolean needsInitialization = true
    String name
    CachedMapBuilder cachedMapBuilder
    Collection<Region> regions

    MultiRegionCachedMap(CachedMapBuilder cachedMapBuilder, Collection<Region> regions) {
        this.cachedMapBuilder = cachedMapBuilder
        this.regions = ImmutableSet.copyOf(regions)
    }

    /**
     * Initializes cache if not already done.
     *
     * @param keyer a closure containing the algorithm for extracting the map key from a T object
     * @param multiRegionRetriever a closure containing the algorithm for creating a new collection of T objects for a
     *          given region
     * @param multiRegionCallback an optional closure to execute for each region after this cache has been filled,
     *          which can be useful for starting another cache's refresh process
     * @param multiRegionReadinessChecker an optional closure to determine whether or not external state is ready for
     *          each regional cached map to run its own fill algorithm
     */
    void ensureSetUp(Closure multiRegionRetriever, Closure multiRegionCallback = { },
                     Closure multiRegionReadinessChecker = { true }) {
        if (needsInitialization) {
            name = "Multi-region ${cachedMapBuilder.entityType.displayName}"
            regions.each { Region region ->
                Closure retriever = { multiRegionRetriever(region) }
                Closure callback = { multiRegionCallback(region) }
                Closure readinessChecker = { multiRegionReadinessChecker(region) }
                regionsToCachedMaps[region] = cachedMapBuilder.buildCachedMap(region)
                regionsToCachedMaps[region].ensureSetUp(retriever, callback, readinessChecker)
            }
        }
        needsInitialization = false
    }

    void fill() {
        regionsToCachedMaps.values().each { it.fill() }
    }

    boolean isFilled() {
        !regionsToCachedMaps.values().find { !it.filled }
    }

    CachedMap<T> by(Region region) {
        regionsToCachedMaps[region]
    }
}
