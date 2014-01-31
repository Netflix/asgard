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

import com.amazonaws.services.route53.AmazonRoute53
import com.amazonaws.services.route53.model.Change
import com.amazonaws.services.route53.model.ChangeAction
import com.amazonaws.services.route53.model.ChangeBatch
import com.amazonaws.services.route53.model.ChangeInfo
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest
import com.amazonaws.services.route53.model.CreateHostedZoneRequest
import com.amazonaws.services.route53.model.DeleteHostedZoneRequest
import com.amazonaws.services.route53.model.GetHostedZoneRequest
import com.amazonaws.services.route53.model.HostedZone
import com.amazonaws.services.route53.model.HostedZoneConfig
import com.amazonaws.services.route53.model.ListHostedZonesRequest
import com.amazonaws.services.route53.model.ListHostedZonesResult
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult
import com.amazonaws.services.route53.model.NoSuchHostedZoneException
import com.amazonaws.services.route53.model.ResourceRecordSet
import com.netflix.asgard.cache.CacheInitializer
import com.netflix.asgard.retriever.AwsResultsRetriever
import grails.util.GrailsNameUtils
import org.springframework.beans.factory.InitializingBean

/**
 * Interactions with Amazon's Route53 DNS service to manage hosted zones and their resource record sets.
 */
class AwsRoute53Service implements CacheInitializer, InitializingBean {

    static transactional = false

    AmazonRoute53 awsClient
    def awsClientService
    Caches caches
    def taskService

    void afterPropertiesSet() {

        // Route53 only has one endpoint.
        awsClient = awsClient ?: awsClientService.create(AmazonRoute53)
    }

    void initializeCaches() {
        caches.allHostedZones.ensureSetUp({ Region region -> retrieveHostedZones() })
    }

    /**
     * Gets all the hosted zones from the cache.
     *
     * @return the list of all hosted zones
     */
    List<HostedZone> getHostedZones() {
        caches.allHostedZones.list().sort { it.name }
    }

    /**
     * Gets one hosted zone based on a string that could be a hosted zone ID that has no dots, or a name that
     * resembles a domain name with a dot on the end.
     *
     * @param userContext who, where, why
     * @param idOrName either a hosted zone ID like Z1FLYQKCK7FTD2, or a name ending with a dot like test.example.com.
     *        or a name with the trailing dot missing like test.example.com
     * @return a matching hosted zone, or null if no match found
     */
    HostedZone getHostedZone(UserContext userContext, String idOrName) {
        // Hosted zone names always contain dots, and hosted zone IDs never contain dots.
        // Users may request "test.example.com" for a hosted zone with name "test.example.com."
        if (!idOrName) { return null }
        String id = idOrName.contains('.') ? (zoneByName(idOrName) ?: zoneByName("${idOrName}."))?.id : idOrName
        try {
            HostedZone hostedZone = awsClient.getHostedZone(new GetHostedZoneRequest(id: id)).hostedZone
            return caches.allHostedZones.put(id, hostedZone)
        } catch (NoSuchHostedZoneException ignored) {
            return null
        }
    }

    private HostedZone zoneByName(String name) {
        caches.allHostedZones.list().find { it.name == name }
    }

    /**
     * Creates a new hosted zone.
     *
     * @param userContext who, where, why
     * @param name the name of the hosted zone such as "test.example.com."
     * @param comment an optional human-readable explanation of what makes this hosted zone distinctive
     * @callerReference the unique id of the change request, defaulting to a random universally unique ID value
     * @return the newly created hosted zone
     */
    HostedZone createHostedZone(UserContext userContext, String name, String comment,
                                String callerReference = UUID.randomUUID().toString()) {
        HostedZone hostedZone = taskService.runTask(userContext, "Create Hosted Zone '${name}'", { task ->
            CreateHostedZoneRequest request = new CreateHostedZoneRequest(name, callerReference)
            if (comment) {
                request.hostedZoneConfig = new HostedZoneConfig(comment: comment)
            }
            awsClient.createHostedZone(request).hostedZone
        }, Link.to(EntityType.hostedZone, name)) as HostedZone
        getHostedZone(userContext, hostedZone.id)
    }

    /**
     * Deletes a hosted zone.
     *
     * @param userContext who, where, why
     * @param hostedZoneId the ID of the hosted zone such as Z1FLYQKCK7FTD2
     * @return the object that represents the change request including the request ID
     */
    ChangeInfo deleteHostedZone(UserContext userContext, String hostedZoneId) {
        ChangeInfo changeInfo = taskService.runTask(userContext, "Delete Hosted Zone ${hostedZoneId}", { task ->
            DeleteHostedZoneRequest request = new DeleteHostedZoneRequest(hostedZoneId)
            awsClient.deleteHostedZone(request).changeInfo
        }, Link.to(EntityType.hostedZone, hostedZoneId)) as ChangeInfo
        caches.allHostedZones.remove(hostedZoneId)
        changeInfo
    }

    private AwsResultsRetriever hostedZoneRetriever = new AwsResultsRetriever<HostedZone, ListHostedZonesRequest,
            ListHostedZonesResult>() {

        ListHostedZonesResult makeRequest(Region region, ListHostedZonesRequest request) {
            awsClient.listHostedZones(request)
        }

        List<HostedZone> accessResult(ListHostedZonesResult result) {
            result.hostedZones
        }

        protected void setNextToken(ListHostedZonesRequest request, String nextToken) {
            request.withMarker(nextToken)
        }

        protected String getNextToken(ListHostedZonesResult result) {
            result.nextMarker
        }
    }

    private List<HostedZone> retrieveHostedZones() {
        hostedZoneRetriever.retrieve(null, new ListHostedZonesRequest())
    }

    private AwsResultsRetriever resourceRecordSetRetriever = new AwsResultsRetriever<ResourceRecordSet,
            ListResourceRecordSetsRequest, ListResourceRecordSetsResult>() {

        ListResourceRecordSetsResult makeRequest(Region region, ListResourceRecordSetsRequest request) {
            awsClient.listResourceRecordSets(request)
        }

        List<ResourceRecordSet> accessResult(ListResourceRecordSetsResult result) {
            result.resourceRecordSets
        }

        protected void setNextToken(ListResourceRecordSetsRequest request, String nextToken) {
            request.withStartRecordName(nextToken)
        }

        protected String getNextToken(ListResourceRecordSetsResult result) {
            result.nextRecordName
        }
    }

    /**
     * Gets all the resource record sets for a specified hosted zone ID.
     *
     * @param userContext who, where, why
     * @param hostedZoneId the ID of the hosted zone such as Z1FLYQKCK7FTD2
     * @return all the DNS resource record sets for the hosted zone
     */
    List<ResourceRecordSet> getResourceRecordSets(UserContext userContext, String hostedZoneId) {
        ListResourceRecordSetsRequest request = new ListResourceRecordSetsRequest(hostedZoneId: hostedZoneId)
        resourceRecordSetRetriever.retrieve(null, request)
    }

    /**
     * Creates a new resource record set within a hosted zone.
     *
     * @param userContext who, where, why
     * @param hostedZoneId the ID of the hosted zone such as Z1FLYQKCK7FTD2
     * @param resourceRecordSet the ResourceRecordSet object to create
     * @param comment the optional human-readable reason for the change request
     * @return info about this change request
     */
    ChangeInfo createResourceRecordSet(UserContext userContext, String hostedZoneId,
                                       ResourceRecordSet resourceRecordSet, String comment) {
        taskService.runTask(userContext, "Create Resource Record ${resourceRecordSet.name}", {
            Change change = new Change(action: ChangeAction.CREATE, resourceRecordSet: resourceRecordSet)
            changeResourceRecordSet(userContext, hostedZoneId, change, comment)
        }, Link.to(EntityType.hostedZone, hostedZoneId)) as ChangeInfo
    }

    /**
     * Deletes an existing resource record set from a hosted zone.
     *
     * @param userContext who, where, why
     * @param hostedZoneId the ID of the hosted zone that contains the resource record set
     * @param resourceRecordSet the DNS entry to delete
     * @param comment the optional human-readable reason for the change request
     * @return info about this change request
     */
    ChangeInfo deleteResourceRecordSet(UserContext userContext, String hostedZoneId,
                                       ResourceRecordSet resourceRecordSet, String comment) {
        taskService.runTask(userContext, "Delete Resource Record ${resourceRecordSet.name}", {
            Change change = new Change(action: ChangeAction.DELETE, resourceRecordSet: resourceRecordSet)
            changeResourceRecordSet(userContext, hostedZoneId, change, comment)
        }, Link.to(EntityType.hostedZone, hostedZoneId)) as ChangeInfo
    }

    private ChangeInfo changeResourceRecordSet(UserContext userContext, String hostedZoneId, Change change,
                                               String comment) {
        ChangeBatch changeBatch = new ChangeBatch(comment: comment, changes: [change])
        def request = new ChangeResourceRecordSetsRequest(hostedZoneId: hostedZoneId, changeBatch: changeBatch)
        String action = GrailsNameUtils.getNaturalName(change.action.toLowerCase())
        String msg = "${action} Resource Record ${change.resourceRecordSet.name}"
        taskService.runTask(userContext, msg, {
            awsClient.changeResourceRecordSets(request).changeInfo
        }, Link.to(EntityType.hostedZone, hostedZoneId)) as ChangeInfo
    }
}
