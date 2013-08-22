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
import com.netflix.asgard.FastProperty
import grails.converters.JSON

class BootStrap {

    /** This "unused" variable needs to be declared here in order to get the referenced service initialized early. */
    def cacheLoadStartService

    def configService
    def initService

    /** This "unused" variable needs to be declared here in order to get the referenced service initialized early. */
    def monkeyPatcherService

    def init = { servletContext ->
        if (configService.appConfigured) { // Only start warming the caches if Asgard has been configured
            initService.initializeApplication()
        }

        JSON.registerObjectMarshaller(FastProperty) {
            it.properties.subMap(FastProperty.ALL_ATTRIBUTES)
        }
    }
}
