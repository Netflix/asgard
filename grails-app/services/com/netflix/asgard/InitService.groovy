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

import com.netflix.asgard.cache.CacheInitializer
import com.netflix.asgard.cache.Fillable
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

class InitService implements ApplicationContextAware {

    static transactional = false

    ApplicationContext applicationContext
    Caches caches

    def configService
    def grailsApplication // modifying the config object directly here

    /**
     * Creates the Asgard Config.groovy file and updates the in memory configuration to reflect the configured state
     *
     * @param configObject The configuration to persist
     */
    void writeConfig(ConfigObject configObject) throws IOException {
        File asgardHomeDir = new File(configService.asgardHome)
        asgardHomeDir.mkdirs()
        if (!asgardHomeDir.exists()) {
            throw new IOException("Unable to create directory ${configService.asgardHome}")
        }

        File configFile = new File(configService.asgardHome, 'Config.groovy')
        boolean fileCreated = configFile.createNewFile()
        if (!fileCreated) {
            throw new IOException("Unable to create Config.groovy file in directory ${configService.asgardHome}")
        }
        configFile.withWriter { writer ->
            configObject.writeTo(writer)
        }
        grailsApplication.config.appConfigured = true
        grailsApplication.config.merge(configObject)

        initializeApplication()
    }

    /**
     * Kicks off populating of caches and background threads
     */
    void initializeApplication() {
        log.info 'Starting caches'
        Collection<CacheInitializer> cacheInitializers = applicationContext.getBeansOfType(CacheInitializer).values()
        for (CacheInitializer cacheInitializer in cacheInitializers) {
            cacheInitializer.initializeCaches()
        }
        log.info 'Starting background threads'
        Collection<BackgroundProcessInitializer> backgroundProcessInitializers =
                applicationContext.getBeansOfType(BackgroundProcessInitializer).values()
        for (BackgroundProcessInitializer backgroundProcessInitializer in backgroundProcessInitializers) {
            backgroundProcessInitializer.initializeBackgroundProcess()
        }
    }

    /**
     * @return true if all caches have completed their initial load, false otherwise
     */
    boolean cachesFilled() {
        Map<String, Object> allBlockingCaches = caches.properties
        // Do not wait on optional caches to fill.
        // Waiting on an unimportant cache due to AWS issues should not keep all of Asgard from starting.
        List<String> optionalCacheNames = ['allVolumes']
        optionalCacheNames.each {
            allBlockingCaches.remove(it)
        }
        Collection<Fillable> fillableCaches = allBlockingCaches*.value.findAll { it instanceof Fillable }
        !fillableCaches.find { !it.filled }
    }
}
