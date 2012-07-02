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

/*
 * Do you need a CachedMap? Then CachedMapBuilder is for you! It can build any kind of CachedMap (regular CachedMaps,
 * MultiRegionCachedMaps, you name it).
 *
 * Usage:
 * CachedMapBuilder builder = new CachedMapBuilder(threadScheduler, regions)
 * CachedMap cachedMap = builder.of(EntityType.autoScaling).buildCachedMap()
 *
 * CachedMapBuilder is immutable and it is common to reuse the same base CachedMapBuilder.
 *
 * @param < T > Type of object that will be cached
 */
class CachedMapBuilder<T> {
    private final ThreadScheduler threadScheduler
    private final Collection<Region> regions
    final EntityType entityType
    final Integer interval

    // This constructor is private to indicate that it is not part of the public API. See usage above
    private CachedMapBuilder(ThreadScheduler threadScheduler, Collection<Region> regions, EntityType entityType,
                             Integer interval) {
        this.threadScheduler = threadScheduler
        this.regions = regions
        this.entityType = entityType
        this.interval = interval
    }

    CachedMapBuilder(ThreadScheduler threadScheduler, Collection<Region> regions = Region.values()) {
        this(threadScheduler, regions, null, null)
    }

    /**
     * Creates an altered copy of the cached map builder
     *
     * @param < S > Type of object that will be cached
     * @param entityType in the cache
     * @param interval the number of seconds to wait between full cache updates, or null if there should be no timer
     *          because some other process will cause the cache to refresh
     * @return a new CachedMapBuilder object based on the original
     */
    protected <S> CachedMapBuilder<S> of(EntityType<S> entityType, Integer interval) {
        new CachedMapBuilder<S>(threadScheduler, regions, entityType, interval)
    }

    /**
     * Creates an altered copy of the cached map builder
     *
     * @param < S > Type of object that will be cached
     * @param entityType in the cache
     * @return a new CachedMapBuilder object based on the original
     */
    protected <S> CachedMapBuilder<S> of(EntityType<S> entityType) {
        of(entityType, null)
    }

    /**
     * Creates a cached map that still needs further initialization using its ensureSetUp(...).
     *
     * @return CachedMap
     */
    protected CachedMap<T> buildCachedMap(Region region = null) {
        new CachedMap<T>(region, entityType, interval, threadScheduler)
    }

    /**
     * Creates a multi region cached map that still needs further initialization using its ensureSetUp(...).
     *
     * @return MultiRegionCachedMap
     */
    protected MultiRegionCachedMap<T> buildMultiRegionCachedMap(Collection<Region> regions = this.regions) {
        new MultiRegionCachedMap<T>(this, regions)
    }
}
