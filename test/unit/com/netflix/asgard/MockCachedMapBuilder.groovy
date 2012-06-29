package com.netflix.asgard

import com.google.common.collect.ImmutableMap

class MockCachedMapBuilder<T>  extends CachedMapBuilder<T> {
    final ImmutableMap entityTypeToCacheMap
    final EntityType  entityType

    MockCachedMapBuilder(Map entityTypeToCacheMap, EntityType  entityType = null) {
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
        entityTypeToCacheMap.get(entityType)
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
            // noop
        }

        void fill() {
            // noop
        }

        boolean isFilled() {
            true
        }

        CachedMap by(Region region) {
            cachedMap
        }
    }
}
