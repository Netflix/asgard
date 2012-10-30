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

import com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsRequest
import com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsResult
import com.amazonaws.services.ec2.model.CancelledSpotInstanceRequest
import com.amazonaws.services.ec2.model.CreateTagsRequest
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult
import com.amazonaws.services.ec2.model.LaunchSpecification
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult
import com.amazonaws.services.ec2.model.SpotInstanceRequest
import com.amazonaws.services.ec2.model.SpotPlacement
import com.amazonaws.services.ec2.model.Tag
import com.google.common.collect.ImmutableList
import com.netflix.asgard.cache.CacheInitializer
import com.netflix.asgard.model.SpotInstanceRequestListType
import com.netflix.asgard.model.InstanceTypeData

class SpotInstanceRequestService implements CacheInitializer {

    def awsEc2Service
    Caches caches
    def instanceTypeService
    def awsLoadBalancerService
    def taskService

    static final List<String> SPOT_INSTANCE_REQUEST_LIVE_STATES = ImmutableList.of('open', 'active')

    void initializeCaches() {
        caches.allSpotInstanceRequests.ensureSetUp({ Region region -> retrieveSpotInstanceRequests(region) })
    }

    private List<SpotInstanceRequest> retrieveSpotInstanceRequests(Region region) {
        awsEc2Service.retrieveSpotInstanceRequests(region)
    }

    List<SpotInstanceRequest> createSpotInstanceRequests(UserContext userContext,
              SpotInstanceRequest templateSpotInstanceRequest, Integer instanceCount, String availabilityZone,
              Task existingTask = null) {
        List<Tag> tags = templateSpotInstanceRequest.tags
        String imageId = templateSpotInstanceRequest.launchSpecification.imageId

        String msg = "Create Spot Instance Request for image '${imageId}'"
        LaunchSpecification templateLaunchSpec = templateSpotInstanceRequest.launchSpecification
        List<SpotInstanceRequest> createdSpotInstanceRequests = taskService.runTask(userContext, msg, { Task task ->

            BeanState launchSpecState = BeanState.ofSourceBean(templateLaunchSpec).
                    ignoreProperties(['allSecurityGroups', 'placement'])
            LaunchSpecification targetLaunchSpec = launchSpecState.injectState(new LaunchSpecification())
            if (availabilityZone) {
                targetLaunchSpec.placement = new SpotPlacement(availabilityZone)
            }

            BeanState templateSirBeanState = BeanState.ofSourceBean(templateSpotInstanceRequest).
                    ignoreProperties(['launchSpecification'])
            RequestSpotInstancesRequest request = templateSirBeanState.injectState(new RequestSpotInstancesRequest())
            request.withInstanceCount(instanceCount).withLaunchSpecification(targetLaunchSpec)
            RequestSpotInstancesResult result = awsEc2Service.requestSpotInstances(userContext, request)
            List<SpotInstanceRequest> createdSpotInstanceRequests = result.spotInstanceRequests
            createSpotInstanceRequestTags(userContext, createdSpotInstanceRequests*.spotInstanceRequestId, tags, task)
            createdSpotInstanceRequests
        }, Link.to(EntityType.image, imageId), existingTask) as List
        return createdSpotInstanceRequests
    }

    void createSpotInstanceRequestTags(UserContext userContext, List<String> spotInstanceRequestIds, List<Tag> tags,
                                       Task existingTask = null) {
        Integer count = spotInstanceRequestIds.size()
        String tagMessage = tags.collect { it.key + '=' + it.value }.toString()
        String msg = "Create tags ${tagMessage} on ${count} spot instance request${count == 1 ? '' : 's'}"
        Closure work = { Task task ->
            CreateTagsRequest request = new CreateTagsRequest().withResources(spotInstanceRequestIds).withTags(tags)
            awsEc2Service.createTags(userContext, request)
        }
        Link link = count == 1 ? Link.to(EntityType.spotInstanceRequest, spotInstanceRequestIds[0]) : null
        taskService.runTask(userContext, msg, work, link, existingTask)
    }

    List<CancelledSpotInstanceRequest> cancelSpotInstanceRequests(UserContext userContext,
                                                                 List<String> spotInstanceRequestIds,
                                                                 Task existingTask = null) {
        List<SpotInstanceRequest> sirs = getSpotInstanceRequestsByIds(userContext, spotInstanceRequestIds)
        Integer sirCount = spotInstanceRequestIds.size()

        String taskName = "Cancel ${sirCount} Spot Instance Request${sirCount == 1 ? '' : 's'}"
        List<CancelledSpotInstanceRequest> cancelledSirs = taskService.runTask(userContext, taskName, { Task task ->
            List<String> instanceIds = sirs*.instanceId.findAll { it }
            CancelSpotInstanceRequestsRequest request = new CancelSpotInstanceRequestsRequest().
                    withSpotInstanceRequestIds(spotInstanceRequestIds)
            CancelSpotInstanceRequestsResult result = awsEc2Service.cancelSpotInstanceRequests(userContext, request)

            task.log("Terminating ${instanceIds.size()} instance${instanceIds.size() == 1 ? '' : 's'}")
            if (instanceIds) {
                awsEc2Service.terminateInstances(userContext, instanceIds, task)
            }
            // TODO update ec2Instance cache

            result.cancelledSpotInstanceRequests
        }, null, existingTask) as List

        cancelledSirs
    }

    List<SpotInstanceRequest> getSpotInstanceRequestsByIds(UserContext userContext, List<String> sirIds) {
        DescribeSpotInstanceRequestsRequest request = new DescribeSpotInstanceRequestsRequest().
                withSpotInstanceRequestIds(sirIds)
        DescribeSpotInstanceRequestsResult result = awsEc2Service.describeSpotInstanceRequests(userContext, request)
        List<SpotInstanceRequest> spotInstanceRequests = result.spotInstanceRequests
        Map<String, SpotInstanceRequest> sirIdsToSirs = mapIdsToSpotInstanceRequests(spotInstanceRequests)
        caches.allSpotInstanceRequests.by(userContext.region).putAll(sirIdsToSirs)
        spotInstanceRequests
    }

    SpotInstanceRequest getSpotInstanceRequest(UserContext userContext, String sirId) {
        List<SpotInstanceRequest> spotInstanceRequests = getSpotInstanceRequestsByIds(userContext, [sirId])
        Check.lone(spotInstanceRequests, SpotInstanceRequest)
    }

    List<SpotInstanceRequest> getSpotInstanceRequests(UserContext userContext, SpotInstanceRequestListType type) {
        if (type == SpotInstanceRequestListType.all) {
            return getAllSpotInstanceRequests(userContext)
        }
        getLiveSpotInstanceRequests(userContext)
    }

    private Map<String, SpotInstanceRequest> mapIdsToSpotInstanceRequests(Collection<SpotInstanceRequest> sirs) {
        sirs.inject([:]) { Map map, SpotInstanceRequest sir -> map << [(sir.spotInstanceRequestId): sir] } as Map
    }

    /**
     * Gets all the spot instance requests including ones that lack an instance or have been cancelled.
     *
     * @param userContext who made the call, why, and in what region
     * @return List < SpotInstanceRequest > all spot instance requests including cancelled ones
     */
    private List<SpotInstanceRequest> getAllSpotInstanceRequests(UserContext userContext) {
        caches.allSpotInstanceRequests.by(userContext.region).list() as List
    }

    /**
     * Gets only the spot instance requests that have an instance or are open/active.
     *
     * @param userContext who made the call, why, and in what region
     * @return List < SpotInstanceRequest > spot instance requests that have instances or are in an open or active state
     */
    private List<SpotInstanceRequest> getLiveSpotInstanceRequests(UserContext userContext) {
        getAllSpotInstanceRequests(userContext).findAll {
            it.instanceId || (it.state in SPOT_INSTANCE_REQUEST_LIVE_STATES) }
    }

    /**
     * Recommends an ideal spot price based on the instance type. Currently this will be the on-demand value.
     *
     * @param userContext who made the call, why, and in what region
     * @param instanceType the type of instance
     * @return recommend spot price
     */
    String recommendSpotPrice(UserContext userContext, String instanceType) {
        InstanceTypeData instanceTypeData = instanceTypeService.getInstanceType(userContext, instanceType)
        instanceTypeData.linuxOnDemandPrice.toString()
    }
}
