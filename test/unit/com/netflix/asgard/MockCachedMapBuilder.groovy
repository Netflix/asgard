/*
 * Copyright 2013 Netflix, Inc.
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

import com.google.common.collect.ImmutableMap

/**
 * This class makes it easier to mock Caches for testing. You are responsible for initializing the CachedMaps, and the
 * setup enforced by the application is sidestepped. Usage is the same as CachedMapBuilder.
 *
 * @see CachedMapBuilder
 */
class MockCachedMapBuilder<T> extends CachedMapBuilder<T> {
    final ImmutableMap<EntityType, CachedMap> entityTypeToCacheMap
    final EntityType entityType
    final Closure<CachedMap> cachedMapFactory

    /**
     * Used to create a CachedMapBuilder where you have control over the initialization of the CachedMaps.
     *
     * @param entityTypeToCacheMap is a map of fully initialized or mocked CachedMaps keyed by entityType
     * @param cachedMapFactory a way to create CachedMaps that aren't specified in entityTypeToCacheMap
     */
    MockCachedMapBuilder(Map<EntityType, CachedMap> entityTypeToCacheMap, Closure<CachedMap> cachedMapFactory = null) {
        this(cachedMapFactory, entityTypeToCacheMap, null)
    }

    // This constructor is private to indicate that it is not part of the public API.
    private MockCachedMapBuilder(Closure<CachedMap> cachedMapFactory, Map<EntityType, CachedMap> entityTypeToCacheMap,
            EntityType entityType) {
        super(null)
        this.cachedMapFactory = cachedMapFactory
        this.entityTypeToCacheMap = ImmutableMap.copyOf(entityTypeToCacheMap)
        this.entityType = entityType
    }

    protected <S> CachedMapBuilder<S> of(EntityType<S> entityType, Integer interval) {
        of(entityType)
    }

    protected <S> CachedMapBuilder<S> of(EntityType<S> entityType) {
        new MockCachedMapBuilder(cachedMapFactory, entityTypeToCacheMap, entityType)
    }

    protected CachedMap<T> buildCachedMap(Region region = null) {
        CachedMap cachedMap = entityTypeToCacheMap.get(entityType)
        if (!cachedMap) {
            cachedMap = cachedMapFactory ? cachedMapFactory(region) : new CachedMap(region, entityType, null, null)
        }
        cachedMap
    }

    protected MultiRegionCachedMap<T> buildMultiRegionCachedMap(Collection<Region> regions = null) {
        new MockMultiRegionCachedMap(buildCachedMap())
    }

    private static class MockMultiRegionCachedMap extends MultiRegionCachedMap {
        final CachedMap cachedMap

        MockMultiRegionCachedMap(CachedMap cachedMap) {
            super(null, [])
            this.cachedMap = cachedMap
        }

        void ensureSetUp(Closure multiRegionRetriever, Closure multiRegionCallback = { },
                         Closure multiRegionReadinessChecker = { true }) {
            Region region = Region.US_EAST_1
            cachedMap.ensureSetUp({ multiRegionRetriever(region) }, { multiRegionCallback(region) },
                    { multiRegionReadinessChecker(region) })
        }

        void fill() {
            cachedMap.fill()
        }

        boolean isFilled() {
            true
        }

        CachedMap by(Region region) {
            cachedMap
        }
    }
}
