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

import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.netflix.asgard.cache.Action
import com.netflix.asgard.cache.Fillable
import com.netflix.asgard.cache.JournalRecord
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Semaphore
import org.apache.commons.logging.LogFactory
import org.joda.time.DateTime

/**
 * A cache implementation that maintains a map of name-value pairs populated using a retriever closure. The retriever is
 * expected to return a List which is then copied into the map by extracting the key for each item using a keyer
 * closure.
 * <p>
 * In most cases the cache updates itself en mass on a regular interval, and individual entries can be updated or
 * removed asynchronously. In some cases the cache gets updated as a result of a callback from the update of a different
 * cache.
 *
 * @param < T > the type of object to store as a value in the map
 */
class CachedMap<T> implements Fillable {

    private static final log = LogFactory.getLog(this)

    /**
     * The name of this particular cache, hopefully distinct from all other caches in some way.
     */
    final String name

    /**
     * The entity type of this particular cache, hopefully distinct from all other caches in some way.
     */
    final EntityType entityType

    /**
     * Number of seconds to wait between full refreshes of the cache from the canonical source of data.
     */
    final Integer interval

    /**
     * The function that will be run to collect all the data to fill the cache and to refresh the cache fully.
     *
     * @return Collection< T > the items that should be stored in the cache
     */
    private Closure retriever

    /**
     * (Optional) The function to execute after each cache fill. This is useful for causing secondary caches to fill if
     * they depend on the data from this cache.
     */
    private Closure callback

    /**
     * (Optional) The function to execute to determine whether or not this cache is ready to filled. A cache may
     * require other caches to be filled first. This optional function allows a cache to specify its own rules for
     * when it is ready for filling.
     *
     * @return boolean true if the cache is ready to be filled, false if something else still needs to happen before it
     *          is okay to fill this cache
     */
    private Closure readinessChecker

    /**
     * The lock for a thread to obtain when attempting to fill the cache. It's undesirable for multiple threads to fill
     * the cache at the same time. Even though the ConcurrentMap could handle it, there are cases where many user
     * threads could trigger cache fill events, which often involve large calls to external systems. By keeping the
     * number of concurrent fill executions to a minimum, we avoid unnecessary extra calls to fetch data sets.
     */
    private final Semaphore permitToFillCache = new Semaphore(1)

    /**
     * The underlying in-memory cache.
     */
    final ConcurrentMap<String, T> map = Maps.newConcurrentMap()

    /**
     * This map of recent off-cycle create, update, and delete actions for specific item keys helps handle race
     * conditions intelligently so that a long-running cache fill will not overwrite recent creates, updates, or deletes
     * caused by user actions that occurred after the start of the fill method.
     */
    private final ConcurrentMap<String, JournalRecord> journal = Maps.newConcurrentMap()

    /** When the cache was last filled. Useful for checking the health of the cache filling system. */
    DateTime lastFillTime
    DateTime lastActiveTime
    ThreadScheduler threadScheduler

    /** Whether this cache is actively used. Slow down refresh when not. Read-only from outside class. */
    private Boolean active = false
    Boolean getActive() { active }

    protected CachedMap(Region region, EntityType entityType, Integer interval, ThreadScheduler threadScheduler) {
        this.entityType = entityType
        String regionDisplay = region ? "${region} " : ''
        this.name = "${regionDisplay}${entityType.displayName}"
        this.interval = interval
        this.threadScheduler = threadScheduler
    }

    /** Only log cache size after first fill, not all the time. */
    private Boolean doingFirstFill = true

    private Boolean needsInitialization = true

    Boolean isDoingFirstFill() { doingFirstFill }

    /**
     * Initializes cache if not already done.
     *
     * @param retriever a closure containing the algorithm for creating a new collection of T objects
     * @param callback an optional closure to execute after this cache has been filled, useful for starting another
     *          cache's refresh process
     * @param readinessChecker an optional closure to determine whether or not external state is ready for this
     *          regional cached map to run its own fill algorithm
     */
    void ensureSetUp(Closure retriever, Closure callback = { }, Closure readinessChecker = { true }) {
        if (needsInitialization) {
            this.retriever = retriever
            this.callback = callback
            this.readinessChecker = readinessChecker
            if (this.interval) {
                start(this.interval)
            }
        }
        needsInitialization = false
    }

    private Closure fillCacheAndPerformCallback = {
        try {
            fill()
            callback?.call()
        } catch (Exception e) {
            // For some reason StackTraceUtils does not print anything successfully.
            log.error "Exception filling cache ${name}", e
        }
    }

    private void start(int intervalSeconds) {
        int maxJitterSeconds = Math.min(intervalSeconds, 120) / 6
        threadScheduler.scheduleAtFixedRate(intervalSeconds, maxJitterSeconds, fillCacheAndPerformCallback)
    }

    /**
     * This method does not use any synchronization or locking. Instead, it makes a best effort to make the cached map
     * correct even if users make changes to the map while this method is executing. Any race conditions should result
     * in a cache entry being incorrect for one extra cycle of the cache's refresh interval, which is often about two
     * minutes.
     */
    void fill() {

        // Do not fill this cache yet if the system is not yet ready to execute this cache's fill algorithm.
        // For example, if other caches need to be filled first then do not yet fill this cache.
        if (doingFirstFill && !readinessChecker()) {
            // Not ready yet, so try again very soon.
            threadScheduler.schedule(1, fillCacheAndPerformCallback)
            return
        }
        // Try to obtain the fill lock for this cached map. If another thread is already holding the lock, then let that
        // other thread do the work. No need to fill a second time right afterward. However, this thread needs to wait
        // until the fill process has completed in the other thread before returning. If a user action caused this fill
        // thread to run, then the cache refresh needs to complete before continuing with that user thread. For
        // instance, if a call to Eureka fails, we refresh the Eureka address cache and then retry the failed call.
        boolean obtainedPermitForFilling = permitToFillCache.tryAcquire()
        afterAttemptedFillPermitAcquisition()
        if (!obtainedPermitForFilling) {
            // Some other thread must be doing the filling. This thread should wait until the fill has finished.
            while (permitToFillCache.availablePermits() < 1) {
                Time.sleepCancellably(1)
            }
            return
        }

        try {
            DateTime dataPullStartTime = new DateTime()
            Set<String> cachedKeys = new HashSet<String>(map.keySet())
            Map<String, T> datasource = [:]
            Collection<T> items = retriever()
            items.each { val -> datasource.put(entityType.key(val), val) }

            Set<String> datasourceKeys = datasource.keySet()
            Set<String> deleteCandidates = Sets.difference(cachedKeys, datasourceKeys).immutableCopy()
            Set<String> addCandidates = Sets.difference(datasourceKeys, cachedKeys).immutableCopy()
            Set<String> updateCandidates = cachedKeys.intersect(datasourceKeys)

            // If the cache has an object that is missing from the datasource and the object was created before this
            // fill operation started then delete the object from the cache.
            for (String key in deleteCandidates) {
                JournalRecord record = journal.remove(key)
                Boolean createdRecently = record?.action == Action.CREATE && record.timestamp.isAfter(dataPullStartTime)
                if (!createdRecently) {
                    map.remove(key)
                }
            }

            // If the datasource has an object that is missing from the cache then add the object to the cache, unless
            // the object was deleted by a user since fetching the new data.
            for (String key in addCandidates) {
                JournalRecord record = journal.remove(key)
                Boolean deletedRecently = record?.action == Action.DELETE && record.timestamp.isAfter(dataPullStartTime)
                if (!deletedRecently) {
                    map.putIfAbsent(key, datasource.get(key))
                }
            }

            // For each object that was in both the cache and the datasource, update the cache unless a user changed the
            // object since fetching the new data. If a user changed the object in any way then assume the cached
            // version is already correct and the datasource version is stale.
            for (String key in updateCandidates) {
                JournalRecord record = journal.remove(key)
                Boolean updatedRecently = record?.timestamp?.isAfter(dataPullStartTime)
                if (!updatedRecently) {
                    map.put(key, datasource.get(key))
                }
            }

            active = false
            if (doingFirstFill) {
                if (map.size() >= 20) {
                    log.info String.format("Cached %5d '%s'", map.size(), name)
                }
                doingFirstFill = false
            }

            lastFillTime = new DateTime()
        } finally {
            permitToFillCache.release()
        }
    }

    /**
     * Extension hook for CachedMap subclasses to override in order to add behavior after trying to acquire the fill
     * permit for the current thread. Useful for unit testing multithreaded behavior.
     */
    protected void afterAttemptedFillPermitAcquisition() { }

    boolean isFilled() {
        !doingFirstFill
    }

    T put(String key, T value) {
        registerActivity()
        if (value) {
            // If the journal already has a CREATE record, don't replace the record with an UPDATE.
            if (journal.get(key)?.action != Action.CREATE) {
                Action action = get(key) ? Action.UPDATE : Action.CREATE
                journal.put(key, new JournalRecord(action))
            }
            map.put(key, value)
        } else {
            remove(key)
        }
        value
    }

    void putAll(Map<String, T> map) {
        for (Map.Entry<String, T> entry in map) {
            put(entry.key, entry.value)
        }
    }

    /**
     * Updates specified values and removes any of the specified names that are not in the specified values. This is
     * useful when the user queries for one or more names, and some of the names no longer have any matching objects in
     * the data store or cloud.
     *
     * @param values the values that should be cached found for the specified names
     * @param names the names that were queried for, including names that are missing from the specified values and
     *         should therefore be removed from the cache
     */
    void putAllAndRemoveMissing(Collection<String> names, Collection<T> values) {

        // Things that exist
        Map<String, T> namesToValues = values.collectEntries { [entityType.key(it), it] }
        putAll(namesToValues)

        // Thing that don't exist
        List<String> missingNames = names - namesToValues.keySet()
        missingNames.each { remove(it) }
    }

    void remove(String key) {
        registerActivity()
        journal.put(key, new JournalRecord(Action.DELETE))
        map.remove(key)
    }

    Collection<T> list() {
        registerActivity()
        map.values()
    }

    T get(String key) {
        map.get(key)
    }

    Integer size() {
        map.values().size()
    }

    void registerActivity() {
        active = true
        lastActiveTime = new DateTime()
    }

    Map<String, T> unmodifiable() {
        Collections.unmodifiableMap(map)
    }
}
