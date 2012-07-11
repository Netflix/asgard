package com.netflix.asgard

import com.google.common.collect.ImmutableMap
/**
 * This class makes it easier to mock Caches for testing. You are responsible for initializing the CachedMaps, and the
 * setup enforced by the application is sidestepped. Usage is the same as CachedMapBuilder.
 *
 * @see         CachedMapBuilder
 */
class MockCachedMapBuilder<T> extends CachedMapBuilder<T> {
    final ImmutableMap<EntityType, CachedMap> entityTypeToCacheMap
    final EntityType  entityType

    /**
     * Used to create a CachedMapBuilder where you have control over the initialization of the CachedMaps.
     *
     * @param  entityTypeToCacheMap is a map of fully initialized or mocked CachedMaps keyed by entityType
     */
    MockCachedMapBuilder(Map<EntityType, CachedMap> entityTypeToCacheMap) {
        this(entityTypeToCacheMap, null)
    }

    // This constructor is private to indicate that it is not part of the public API.
    private MockCachedMapBuilder(Map<EntityType, CachedMap> entityTypeToCacheMap, EntityType  entityType) {
        super(null)
        this.entityTypeToCacheMap = ImmutableMap.copyOf(entityTypeToCacheMap)
        this.entityType = entityType
    }

    protected <S> CachedMapBuilder<S> of(EntityType<S> entityType, Integer interval) {
        of(entityType)
    }

    protected <S> CachedMapBuilder<S> of(EntityType<S> entityType) {
        new MockCachedMapBuilder(entityTypeToCacheMap, entityType)
    }

    protected CachedMap<T> buildCachedMap(Region region = null) {
        CachedMap cachedMap = entityTypeToCacheMap.get(entityType)
        if (!cachedMap) {
            cachedMap = new CachedMap(region, entityType, null, null)
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

        void ensureSetUp(Closure multiRegionRetriever, Closure multiRegionCallback = {},
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
