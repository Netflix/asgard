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

import com.netflix.asgard.cache.Fillable
import com.netflix.asgard.model.MultiRegionInstancePrices
import grails.converters.JSON
import grails.converters.XML
import org.joda.time.DateTime

/**
 * Used for debugging in-memory cache state.
 */
class CacheController {

    Caches caches

    def allowedMethods = [fill: 'POST']

    def index = { redirect(action: 'list', params: params) }

    def list = {
        Map<String, ? extends List> result = analyzeCaches()
        withFormat {
            html { result }
            xml { new XML(result).render(response) }
            json { new JSON(result).render(response) }
        }
    }

    def remaining = {
        List<String> unfilled = analyzeCaches().unfilled
        withFormat {
            html { unfilled }
            xml { new XML(unfilled).render(response) }
            json { new JSON(unfilled).render(response) }
        }
    }

    public Map<String, ? extends List> analyzeCaches() {
        Collection<Fillable> fillableCaches = caches.properties*.value.findAll { it instanceof Fillable }
        List<Map> multiRegSummaries = []
        List<Map> globalSummaries = []
        List<Map> prices = []
        fillableCaches.sort { it.name }.each {
            if (it instanceof MultiRegionCachedMap) {
                MultiRegionCachedMap multi = it
                Map<String, Map> regionsToSummaries = [:]
                Collection<Region> regions = multi.regions.sort { it.code }
                regions.each {
                    CachedMap cachedMap = multi.by(it)
                    Map<String, Object> cachedMapSummary = summarize(cachedMap)
                    regionsToSummaries.put(it.code, cachedMapSummary)
                }
                multiRegSummaries << [name: multi.name, filled: multi.filled, regionalCaches: regionsToSummaries]
            } else if (it instanceof CachedMap) {
                globalSummaries << summarize(it)
            } else if (it instanceof MultiRegionInstancePrices) {
                prices << [name: it.name, filled: it.filled]
            }
        }
        List<String> unfilled = listUnfilled(prices) + listUnfilled(globalSummaries) + listUnfilled(multiRegSummaries)
        Map<String, List> result = [unfilled: unfilled, globalCaches: globalSummaries, prices: prices,
                multiRegionCaches: multiRegSummaries]
        result
    }

    private Map<String, Object> summarize(CachedMap c) {
        String timeSinceLastFill = c.lastFillTime ? Time.format(c.lastFillTime, new DateTime()) : null
        Map<String, Object> cachedMapSummary = [name: c.name, filled: c.filled, timeSinceLastFill: timeSinceLastFill,
                lastFillTime: c.lastFillTime?.toString(), size: c.map.size()]
        cachedMapSummary
    }

    private List<String> listUnfilled(List<Map> summaries) {
        summaries.findAll { !it.filled }*.name
    }

    /**
     * Fills a single top-level cache object on demand.
     */
    def fill = {
        String name = params.id
        caches.properties*.value.find { it.name == name }.fill()
        render "Filling cache ${name}"
    }
}
