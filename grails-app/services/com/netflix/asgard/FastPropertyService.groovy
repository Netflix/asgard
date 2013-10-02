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
import grails.converters.XML
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder
import java.rmi.ServerException
import java.rmi.server.ServerNotActiveException
import org.apache.commons.collections.Bag
import org.apache.commons.collections.bag.HashBag
import org.apache.http.HttpStatus
import org.springframework.web.util.UriComponentsBuilder

class FastPropertyService implements CacheInitializer {

    static transactional = false

    private final String fastPropertyPath = 'platformservice/REST/v1/props'

    def grailsApplication
    def applicationService
    def awsClientService
    Caches caches
    def configService
    def discoveryService
    def mergedInstanceGroupingService
    def restClientService
    def taskService

    void initializeCaches() {
        caches.allFastProperties.ensureSetUp({ Region region -> retrieveFastProperties(region) }, { }, { true })
    }

    private List<FastProperty> retrieveFastProperties(Region region) {
        UserContext userContext = UserContext.auto(region)
        String hostAndPort = platformServiceHostAndPort(userContext)
        if (hostAndPort) {
            String url = "http://${hostAndPort}/platformservice/REST/v1/props/allprops"
            def fastPropertiesXml = restClientService.getAsXml(url)
            if (fastPropertiesXml && fastPropertiesXml.properties) {
                return fastPropertiesXml.properties.property.collect { GPathResult fastPropertyData ->
                    FastProperty.fromXml(fastPropertyData)
                }
            } else {
                throw new ServerException("Failure to fetch fast property list from ${url}")
            }
        }
        []
    }

    List<FastProperty> getAll(UserContext userContext) {
        caches.allFastProperties.by(userContext.region)?.list() as List ?: []
    }

    /**
     * Finds all fast properties associated with the supplied application name (case insensitive), based on the region
     * in the user context.
     *
     * @param userContext who, where, why
     * @param name the application name
     * @return list of fast properties associated with the application
     */
    List<FastProperty> getFastPropertiesByAppName(UserContext userContext, String name) {
        getAll(userContext).findAll { name.equalsIgnoreCase(it.appId) }
    }

    private UriComponentsBuilder fastPropertyBaseUriBuilder(userContext) {
        String hostAndPort = platformServiceHostAndPort(userContext)
        String url = "http://${hostAndPort}/${fastPropertyPath}" as String
        UriComponentsBuilder.fromHttpUrl(url)
    }

    FastProperty get(UserContext userContext, String fastPropertyId) {
        if (!fastPropertyId) { return null }
        String url = fastPropertyBaseUriBuilder(userContext).pathSegment('property').pathSegment('getPropertyById').
                queryParam('id', fastPropertyId).build().encode().toUriString()
        def fastPropertyXml = restClientService.getAsXml(url)
        FastProperty fastProperty = FastProperty.fromXml(fastPropertyXml)
        caches.allFastProperties.by(userContext.region).put(fastPropertyId, fastProperty)
        fastProperty
    }

    FastProperty create(UserContext userContext, FastProperty fastProperty, Task existingTask = null) {
        taskService.runTask(userContext, "Create Fast Property '${fastProperty.key}'", { Task task ->

            // If a region is specified then try to target a platformservice instance in that region.
            Region region = Region.withCode(fastProperty.region)
            UserContext userContextToFindPlatformService = region ? userContext.withRegion(region) : userContext
            UserContext userContextThatDelivered = userContext

            String hostAndPort = platformServiceHostAndPort(userContextToFindPlatformService)
            if (hostAndPort) {
                userContextThatDelivered = userContextToFindPlatformService
            } else {
                hostAndPort = platformServiceHostAndPort(userContext)
            }

            if (!hostAndPort) {
                List<Region> regionsChecked = [userContext.region, userContextToFindPlatformService.region].unique()
                throw new ServerNotActiveException(
                        "Unable to find working platformservice instance in ${regionsChecked}")
            }
            String uriPath = "http://${hostAndPort}/platformservice/REST/v1/props/property"
            task.log("Sending data to ${uriPath}")
            RestResponse restResponse = restClientService.postAsXml(uriPath, fastProperty.toXml())
            if (restResponse.statusCode != HttpStatus.SC_OK) {
                String errorMsg = XML.parse(restResponse.content).errorMsg
                throw new ValidationException("Failed to create Fast Property. ${errorMsg}")
            }
            FastProperty fastPropertyCreated = FastProperty.fromXml(XML.parse(restResponse.content) as GPathResult)
            // Refresh cache
            get(userContextThatDelivered, fastPropertyCreated.id)
        }, Link.to(EntityType.fastProperty, fastProperty.id), existingTask) as FastProperty
    }

    void updateFastProperty(UserContext userContext, String id, String value, String updatedBy,
                            Task existingTask = null) {
        FastProperty fastProperty = get(userContext, id)
        if (fastProperty) {
            taskService.runTask(userContext, "Update Fast Property '${id}' by ${updatedBy}", { Task task ->

                StringWriter writer = new StringWriter()
                final MarkupBuilder builder = new MarkupBuilder(writer)
                builder.property {
                    builder.propertyId(id)
                    builder.value(value)
                    builder.updatedBy(updatedBy)
                    builder.sourceOfUpdate(FastProperty.SOURCE_OF_UPDATE)
                    builder.cmcTicket(userContext.ticket)
                }
                String xmlString = writer.toString()

                String hostAndPort = platformServiceHostAndPort(userContext)
                if (!hostAndPort) {
                    throw new ServerNotActiveException(
                            "Unable to find working platformservice instance in ${userContext.region}")
                }
                String uriPath = "http://${hostAndPort}/platformservice/REST/v1/props/property"
                task.log("Sending data to ${uriPath}")
                RestResponse restResponse = restClientService.postAsXml(uriPath, xmlString)
                if (restResponse.statusCode != HttpStatus.SC_OK) {
                    String errorMsg = XML.parse(restResponse.content).errorMsg
                    throw new ValidationException("Failed to update Fast Property. ${errorMsg}")
                }
                // Refresh cache
                get(userContext, id)
            }, Link.to(EntityType.fastProperty, id), existingTask)
        } else {
            throw new IllegalArgumentException("Cannot update Fast Property ${id} because it does not exist")
        }
        get(userContext, id)
    }

    void deleteFastProperty(UserContext userContext, String id, String updatedBy, String fastPropertyRegionCode,
                            Task existingTask = null) {

        FastProperty fastProperty = get(userContext, id)
        if (fastProperty) {

            // If a region is specified then try to target a platformservice instance in that region.
            Region region = Region.withCode(fastPropertyRegionCode)
            UserContext userContextToFindPlatformService = region ? userContext.withRegion(region) : userContext

            taskService.runTask(userContextToFindPlatformService, "Delete Fast Property '${id}'", { Task task ->

                UserContext userContextThatDelivered = userContext
                String hostAndPort = platformServiceHostAndPort(userContextToFindPlatformService)
                if (hostAndPort) {
                    userContextThatDelivered = userContextToFindPlatformService
                } else {
                    hostAndPort = platformServiceHostAndPort(userContext)
                }
                if (!hostAndPort) {
                    List<Region> regionsChecked = [userContext.region, userContextToFindPlatformService.region].unique()
                    throw new ServerNotActiveException(
                            "Unable to find working platformservice instance in ${regionsChecked}")
                }

                String uriPath = fastPropertyBaseUriBuilder(userContext).pathSegment('property').
                        pathSegment('removePropertyById').queryParam('id', id).
                        queryParam('source', FastProperty.SOURCE_OF_UPDATE).
                        queryParam('updatedBy', updatedBy).queryParam('cmcTicket', userContext.ticket ?: '').
                        build().encode().toUriString()
                task.log("Deleting data at ${uriPath}")
                restClientService.getAsXml(uriPath)

                // Wait for platformservice instances to propagate the change so get calls fail
                while (get(userContextThatDelivered, id)) {
                    Time.sleepCancellably(500)
                }

                // Update target regional cache. Also update user's current context regional cache if different from target.
                if (userContext.region != userContextThatDelivered.region) {
                    get(userContext, id)
                }
            }, Link.to(EntityType.fastProperty, id), existingTask)
        }
    }

    /**
     * Returns a list of all registered application names. For app names that do not have any Fast Properties,
     * this method will use the default capitalization (all lowercase). For any app name that has one or more Fast
     * Properties, this method will use the capitalization used most often in the Fast Property list for that app name.
     *
     * Example output strings:
     * aaweb
     * ABCLOUD
     * explorer
     * fds
     * GPS
     * helloworld
     * membership_grinder
     * MerchWeb
     * mmdreplicator
     *
     * @return List< String > all registered app names, with capitalization compatible with existing Fast Properties
     */
    List<String> collectFastPropertyAppNames(UserContext userContext) {
        List<String> itemAppNames = getAll(userContext)*.appId
        Bag<String> countedAppNames = new HashBag(itemAppNames)
        List<String> appNames = applicationService.getRegisteredApplications(userContext).collect { AppRegistration app ->
            Collection<String> itemAppNameMatches = countedAppNames.uniqueSet().findAll { String countedAppName ->
                app.name.equalsIgnoreCase(countedAppName)
            }
            String mostPopularCapitalization = app.name
            Integer highestCount = 0
            itemAppNameMatches.each { String matchingItemAppName ->
                Integer count = countedAppNames.getCount(matchingItemAppName)
                if (count > highestCount) {
                    highestCount = count
                    mostPopularCapitalization = matchingItemAppName
                }
            }
            mostPopularCapitalization
        }
        return appNames
    }

    private String platformServiceHostAndPort(UserContext userContext) {
        String host = configService.getRegionalPlatformServiceServer(userContext.region)
        String port = configService.platformServicePort
        (configService.online && host && port) ? "${host}:${port}" : null
    }
}
