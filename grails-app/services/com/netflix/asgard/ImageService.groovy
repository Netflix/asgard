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

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.LaunchSpecification
import com.amazonaws.services.ec2.model.Placement
import com.amazonaws.services.ec2.model.Reservation
import com.amazonaws.services.ec2.model.RunInstancesRequest
import com.amazonaws.services.ec2.model.RunInstancesResult
import com.amazonaws.services.ec2.model.SpotInstanceRequest
import com.amazonaws.services.ec2.model.SpotPlacement
import com.amazonaws.services.ec2.model.Tag
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.netflix.asgard.model.InstanceProductType
import com.netflix.asgard.model.JanitorMode
import com.netflix.asgard.model.MassDeleteRequest
import groovy.util.slurpersupport.GPathResult
import java.rmi.server.ServerNotActiveException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import org.codehaus.groovy.grails.web.json.JSONArray
import org.joda.time.DateTime

class ImageService implements BackgroundProcessInitializer {

    static transactional = false

    def awsAutoScalingService
    def awsEc2Service
    def awsS3Service
    def configService
    def emailerService
    def grailsApplication
    def instanceTypeService
    def launchTemplateService
    def restClientService
    def spotInstanceRequestService
    def mergedInstanceGroupingService
    def taskService

    private ScheduledExecutorService replicationExecutor

    void initializeBackgroundProcess() {
        int priority = Thread.MIN_PRIORITY
        String format = 'image-tag-replicator-%s'
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(format).setPriority(priority).build()
        replicationExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory)
        replicationExecutor.scheduleWithFixedDelay({ runReplicateImageTags() } as Runnable, 30, 180, TimeUnit.SECONDS)
    }

    List<SpotInstanceRequest> requestSpotInstances(UserContext userContext, String imageId, Integer count,
            Collection<String> securityGroups, String instanceType, String zone, String ownerName) {
        Check.notEmpty(ownerName, 'Owner')
        String keyName = awsEc2Service.getDefaultKeyName()
        Image image = awsEc2Service.getImage(userContext, imageId)
        String userData = Ensure.encoded(launchTemplateService.buildUserDataForImage(userContext, image))
        Map<String, String> tagPairs = buildTagPairs(image, ownerName)

        List<Tag> tags = tagPairs.collect { String key, String value -> new Tag(key, value) }

        BigDecimal spotPrice = instanceTypeService.calculateLeisureLinuxSpotBid(userContext, instanceType)

        LaunchSpecification launchSpec = new LaunchSpecification().withImageId(imageId).
                withInstanceType(instanceType).withSecurityGroups(securityGroups).withKeyName(keyName).
                withPlacement(zone ? new SpotPlacement(zone) : null).withUserData(userData)
        SpotInstanceRequest spotInstanceRequest = new SpotInstanceRequest().withType('one-time').
                withLaunchSpecification(launchSpec).withTags(tags).withSpotPrice(spotPrice.toString()).
                withProductDescription(InstanceProductType.LINUX_UNIX.name())
        List<SpotInstanceRequest> sirs = spotInstanceRequestService.createSpotInstanceRequests(userContext,
                spotInstanceRequest, 1, zone)
        sirs
    }

    List<Instance> runOnDemandInstances(UserContext userContext, String imageId, Integer count,
            Collection<String> securityGroups, String instanceType, String zone, String ownerName) {
        Check.notEmpty(ownerName, 'Owner')
        String keyName = awsEc2Service.getDefaultKeyName()
        Image image = awsEc2Service.getImage(userContext, imageId)
        String userData = Ensure.encoded(launchTemplateService.buildUserDataForImage(userContext, image))
        Map<String, String> tagPairs = buildTagPairs(image, ownerName)

        String taskName = "Launch image ${imageId}, keyName ${keyName}, instanceType ${instanceType}, zone ${zone}"
        List<Instance> instances = taskService.runTask(userContext, taskName, { task ->
            RunInstancesRequest request = new RunInstancesRequest().withImageId(imageId).
                    withMinCount(count).withMaxCount(count).withKeyName(keyName).withSecurityGroups(securityGroups).
                    withUserData(userData).withInstanceType(instanceType).
                    withPlacement(new Placement().withAvailabilityZone(zone))
            RunInstancesResult result = awsEc2Service.runInstances(userContext, request)
            Reservation reservation = result.reservation
            List<Instance> instances = reservation.instances
            List<String> instanceIds = instances*.instanceId
            awsEc2Service.createInstanceTags(userContext, instanceIds, tagPairs, task)
            return instances
        }, Link.to(EntityType.image, imageId)) as List
        instances
    }

    private Map<String, String> buildTagPairs(Image image, String ownerName) {
        [appversion: image.appVersion ?: '', owner: ownerName]
    }

    /**
     * Finds any AMIs that are in use and changes their last_referenced_time tag to the current date time. AMIs are
     * considered in use if getLocalImageIdsInUse returns the image id or a call to getLocalImageIdsInUse on the
     * promotion server contains the image id. It will also look at all of the images from this list of image ids, and
     * append any image ids referenced as the base AMI in the description field. It will then find the images
     * corresponding to the image ids in the list, and build a list of image names. Using this list of names, the
     * process will add the last_referenced_time tag to all AMIs matching that image name across regions. Image name is
     * used since it should be the same for the same AMI between regions while the AMI's image id will differ.
     *
     * @param userContext who, where, why
     */
    void tagAmiLastReferencedTime(UserContext userContext) {
        String now = Time.nowReadable()
        String taskMessage = "Tagging all in-use AMIs with last_referenced_time ${now}"
        taskService.startTask(userContext, taskMessage, { Task task ->
            Collection<String> inUseImageNames = findInUseImageNamesForAllRegions(task)
            Region.values().each { Region region ->
                UserContext userContextForRegion = task.userContext.withRegion(region)
                Collection<Image> images = awsEc2Service.getAccountImages(userContextForRegion)
                Collection<String> imageIdsToTag = images.findAll { inUseImageNames.contains(it.name) }*.imageId
                task.log "Tagging ${imageIdsToTag} in ${region} with last_referenced_time ${now}"
                if (imageIdsToTag) {
                    awsEc2Service.createImageTags(userContextForRegion, imageIdsToTag, 'last_referenced_time', now)
                }
                task.log "Total ${imageIdsToTag.size()} AMIs tagged in ${region}"
            }
        })
    }

    private Set<String> findInUseImageNamesForAllRegions(Task task) {
        Region.values().collect { Region region ->
            findInUseImageNamesForRegion(task, region)
        }.flatten() as Set
    }

    private Collection<String> findInUseImageNamesForRegion(Task task, Region region) {
        UserContext userContextForRegion = task.userContext.withRegion(region)

        Collection<String> inUseImageIdsForRegion = [] as Set
        Set<String> remoteUsedImageIds = getRemoteImageIdsInUse(region, task)
        log.debug "Remote in use AMI count: ${remoteUsedImageIds.size()}"
        inUseImageIdsForRegion += remoteUsedImageIds
        Collection<String> localUsedImageIds = getLocalImageIdsInUse(userContextForRegion)
        log.debug "Local in use AMI count: ${localUsedImageIds.size()}"
        inUseImageIdsForRegion += localUsedImageIds

        Collection<Image> amis = awsEc2Service.getAccountImages(userContextForRegion)
        Collection<String> inUseBaseImageIds = getInUseBaseImageIds(amis, inUseImageIdsForRegion)
        log.debug "In use base AMI count: ${inUseBaseImageIds.size()}"
        inUseImageIdsForRegion += inUseBaseImageIds
        amis.findAll { inUseImageIdsForRegion.contains(it.imageId) }*.name
    }

    /**
     * Identifies any AMIs for a region that are either referenced by an instance or a launch configuration. The
     * resulting collection will contain unique values.
     *
     * @param userContext who, where, why
     */
    Collection<String> getLocalImageIdsInUse(UserContext userContext) {
        Collection<String> imageIds = awsEc2Service.getInstances(userContext)*.imageId.unique()
        imageIds += awsAutoScalingService.getLaunchConfigurations(userContext)*.imageId.unique()
        imageIds.unique()
    }

    private Set<String> getRemoteImageIdsInUse(Region region, Task task) {
        Set<String> remoteImageIdsInUse = Sets.newHashSet()
        String remoteServer = grailsApplication.config.promote.targetServer
        String url = "${remoteServer}/${region.code}/image/used.json"
        task.tryUntilSuccessful {
            JSONArray jsonListOfImageIds = restClientService.getAsJson(url) as JSONArray
            remoteImageIdsInUse.addAll(jsonListOfImageIds)
        }
        remoteImageIdsInUse
    }

    private Collection getInUseBaseImageIds(Collection<Image> amis, Collection<String> inUseImageIdsForRegion) {
        Collection<String> inUseBaseImageIds = [] as Set
        amis.each { Image ami ->
            if (inUseImageIdsForRegion.contains(ami.imageId)) {
                String baseAmiId = Relationships.baseAmiIdFromDescription(ami.description)
                if (baseAmiId) {
                    inUseBaseImageIds << baseAmiId
                }
            }
        }
        inUseBaseImageIds
    }

    /**
     * Check if local host name is the same as the current canonical server for the account that owns the images.
     */
    private Boolean imageTagReplicationShouldRun() {
        // Config values that might not be defined mustn't be assumed to be of type String. If you change def to
        // String here then the falsy value becomes "{}" which is a truthy value and breaks the if expression.
        Boolean imageTagPromotionEnabled = grailsApplication.config.promote.imageTags ?: false
        String promotionTargetServer = grailsApplication.config.promote.targetServer ?: ''
        String canonicalServerForBakeEnvironment = grailsApplication.config.promote.canonicalServerForBakeEnvironment ?: ''
        if (!imageTagPromotionEnabled || !promotionTargetServer || !canonicalServerForBakeEnvironment) {
            log.debug 'Environment not configured for tag replication.'
            return false
        }
        String bakeEnvHostName = restClientService.getAsText("${canonicalServerForBakeEnvironment}/server", 1000)
        String localHostName = InetAddress.getLocalHost().getHostName()
        String env = grailsApplication.config.cloud.accountName
        Boolean environmentOwnsImages = (env == grailsApplication.config.cloud.imageTagMasterAccount)
        Boolean localServerIsCanonical = (bakeEnvHostName == localHostName)
        return environmentOwnsImages && localServerIsCanonical
    }

    /**
     * Queries the promotion server for the current state of its image tags and promotes any differences via REST calls.
     * This will only run if the host name matches the canonicalServerForBakeEnvironment setting and this server's
     * account name matches the imageTagMasterAccount setting. Wrapped with a single thread pool executor to prevent
     * this from being called more than once at a time.
     */
    void replicateImageTags() {
        replicationExecutor.execute({ runReplicateImageTags() } as Runnable)
    }

    protected void runReplicateImageTags() {
        // If anything goes wrong during the timer task execution, prevent the exception from halting the timer.
        try {
            if (!imageTagReplicationShouldRun()) {
                return
            }

            log.info 'ImageTagReplicator starting'

            // Try to connect to promotion target server. Abort if server is unavailable.
            String promotionTargetServer = grailsApplication.config.promote.targetServer
            checkServerHealth(promotionTargetServer)

            Region.values().each { replicateTagsForRegion(promotionTargetServer, it) }
            log.info 'ImageTagReplicator done'
        } catch (Exception e) {
            log.error "ImageTagReplicator failed: ${e}"
        }
    }

    private replicateTagsForRegion(String promotionTargetServer, Region region) {
        // Get the test and production AMI data
        Collection<Image> testImages = awsEc2Service.getAccountImages(UserContext.auto(region))

        String url = "${promotionTargetServer}/${region.code}/image/list.xml"
        def prodImagesXml = restClientService.getAsXml(url)
        if (prodImagesXml == null) {
            log.info 'Promotion target server unresponsive, continuing to attempt replication.'
            return
        }

        Collection<Image> prodImages = parseImagesXml(prodImagesXml)

        Multimap<String, String> deletableTagNamesToImageIds = ArrayListMultimap.create()
        Multimap<Tag, String> addableTagsToImageIds = ArrayListMultimap.create()

        // Look through all the prod images. For each one, find its counterpart in the test images.
        // Correct any mismatches.
        prodImages.each { Image prodImage ->
            Image testImage = testImages.find { it.imageId == prodImage.imageId }
            if (!testImage) {
                return
            }
            Map<String, String> testTagMap = mapTags(testImage.tags)
            Map<String, String> prodTagMap = mapTags(prodImage.tags)
            Set<String> testTagNames = testTagMap.keySet()
            Collection<String> tagNamesInProdOnly = prodTagMap.keySet() - testTagNames
            tagNamesInProdOnly.each { deletableTagNamesToImageIds.put(it, prodImage.imageId) }
            testTagNames.each { name ->
                String testValue = testTagMap.get(name)
                String prodValue = prodTagMap.get(name)
                if (testValue && (testValue != prodValue)) {
                    addableTagsToImageIds.put(new Tag(name, testValue), prodImage.imageId)
                }
            }
        }
        deletableTagNamesToImageIds.keySet().each { tagKey ->
            deleteRemoteTags(region, deletableTagNamesToImageIds.get(tagKey), tagKey)
            Time.sleepCancellably(grailsApplication.config.cloud.throttleMillis ?: 250)
        }
        addableTagsToImageIds.keySet().each { tag ->
            addRemoteTags(region, addableTagsToImageIds.get(tag), tag)
            Time.sleepCancellably(grailsApplication.config.cloud.throttleMillis ?: 250)
        }
        log.info "ImageTagReplicator finished in region ${region}"
    }

    private Collection<Image> parseImagesXml(GPathResult prodImagesXml) {
        Collection<Image> prodImages = prodImagesXml.image.collect { imageXml ->
            Collection<Tag> tags = imageXml.tags.tag.collect { GPathResult tagXml ->
                new Tag().withKey(tagXml['key'].text().trim()).withValue(tagXml['value'].text().trim())
            }
            new Image().withImageId(imageXml.imageId.text().trim()).withTags(tags)
        }
        return prodImages
    }

    private checkServerHealth(String promotionTargetServer) {
        String healthCheckUrl = "${promotionTargetServer}/healthcheck"
        Integer responseCode = restClientService.getResponseCode(healthCheckUrl)
        if (responseCode != HttpURLConnection.HTTP_OK) {
            log.info "ImageTagReplication aborted because server ${healthCheckUrl} returned ${responseCode}"
            throw new ServerNotActiveException("Response code from ${promotionTargetServer} was ${responseCode}")
        }
    }

    private Map<String, String> mapTags(List<Tag> tagList) {
        Map<String, String> tagMap = new TreeMap<String, String>()
        tagList.inject(tagMap, {
            Map map, Tag tag -> map.put(tag.key, tag.value)
            map
        })
        tagMap
    }

    private deleteRemoteTags(Region region, Collection<String> remoteImageIds, String name) {
        log.info "Deleting prod image tags ${name} for ${remoteImageIds} in ${region}"
        String promotionTargetServer = grailsApplication.config.promote.targetServer
        String url = "${promotionTargetServer}/${region.code}/image/removeTags"
        Map<String, String> query = ['imageIds': remoteImageIds.join(','), 'name': name]
        postForReplication(url, query)
    }

    private addRemoteTags(Region region, Collection<String> remoteImageIds, Tag tag) {
        log.info "Adding tag ${tag.key}=${tag.value} to remote images ${remoteImageIds} in ${region}"
        String promotionTargetServer = grailsApplication.config.promote.targetServer
        String url = "${promotionTargetServer}/${region.code}/image/addTags"
        Map<String, String> query = ['imageIds': remoteImageIds.join(','), 'name': tag.key, 'value': tag.value]
        postForReplication(url, query)
    }

    private postForReplication(String url, Map<String, String> query) {
        try {
            log.debug "Calling ${url} with params ${query} for tag replication."
            int responseCode = restClientService.post(url, query)
            if (responseCode >= 300) {
                String msg = "Call to ${url} with params ${query} returned status code ${responseCode}"
                throw new ServerNotActiveException(msg)
            }
        } catch (Exception e) {
            // Let the thread continue even if promotion target server returns an error
            String msg = "Error copying tag: ${url} ${query} ${e}"
            log.error msg
            emailerService.sendExceptionEmail(msg, e)
        }
    }

    void deleteImage(UserContext userContext, String imageId, Task existingTask = null) {
        String msg = "Deleting image ${imageId}"
        Closure work = { Task task ->
            Image image = awsEc2Service.getImage(userContext, imageId)
            if (!image) {
                task.log("Unable to find image '${imageId}'")
                return
            }
            String snapshotId = image.blockDeviceMappings.findResult { it.ebs?.snapshotId }
            String location = image.imageLocation
            if (location.contains('/') && location.endsWith(AwsS3Service.MANIFEST_SUFFIX)) {
                task.log("Deleting S3 bundle ${location}")
                awsS3Service.deleteBundle(userContext, location)
            }
            awsEc2Service.deregisterImage(userContext, imageId, task)
            if (snapshotId) {
                try {
                    awsEc2Service.deleteSnapshot(userContext, snapshotId, task)
                } catch (AmazonServiceException ase) {
                    task.log("Unable to delete snapshot '${snapshotId}': ${ase}")
                    if (ase.errorCode != 'InvalidSnapshot.NotFound') {
                        throw ase
                    }
                }
            }
            awsEc2Service.getImage(userContext, imageId)
        }
        taskService.runTask(userContext, msg, work, Link.to(EntityType.image, imageId), existingTask)
    }

    /**
     * Deletes AMIs that have not been created or used recently.
     *
     * @param userContext who, why, where
     * @param request parameters for this delete job
     * @return List < Image > data about the images that were deleted
     */
    List<Image> massDelete(UserContext userContext, MassDeleteRequest request) {
        request.checkIfValid()
        DateTime lastReferencedTime = Time.now().minusDays(request.lastReferencedDaysAgo)
        DateTime neverReferencedTime = Time.now().minusDays(request.neverReferencedDaysAgo)

        Collection<Image> images = awsEc2Service.getAccountImages(userContext).sort { it.creationTime }
        abortIfNotEnoughRecentlyUsed(userContext, images)

        String accountId = grailsApplication.config.grails.awsAccounts[0]

        List<Image> imagesEligibleForDelete = images.findAll { image ->
            image.ownerId == accountId && withinMassDeleteThreshold(image, lastReferencedTime, neverReferencedTime)
        }
        List<Image> deletableImages = removeExcludedLaunchPermissions(userContext, imagesEligibleForDelete)
        abortIfInUseImageMarkedForDelete(userContext, deletableImages)
        List<Image> imagesToDelete = deletableImages.subList(0, Math.min(deletableImages.size(), request.limit))

        String msg = "Mass delete of ${imagesToDelete.size()} images " +
            "either last referenced more than ${request.lastReferencedDaysAgo} days ago " +
            "or never referenced and created more than ${request.neverReferencedDaysAgo} days ago"
        taskService.startTask(userContext, msg, { task ->
            imagesToDelete.each { image -> deleteImageForMassDelete(userContext, request.mode, image, task) }
        })
        imagesToDelete
    }

    private deleteImageForMassDelete(UserContext userContext, JanitorMode mode, Image image, Task task) {
        if (mode == JanitorMode.DRYRUN) {
            log.info("Dry run mode. If executed, this job would delete ${image}")
        } else {
            try {
                deleteImage(userContext, image.imageId, task)
            } catch (Exception e) {
                String msg = "Unable to delete AMI id ${image.imageId} because ${e}"
                log.error(msg)
                emailerService.sendExceptionEmail(msg, e)
            }
        }
    }

    private void abortIfInUseImageMarkedForDelete(UserContext userContext, Collection<Image> images) {
        Collection<String> inUseImageNames = findInUseImageNamesForAllRegions(new Task(userContext: userContext))
        Collection<Image> inUseImagesMarkedForDelete = images.findAll { inUseImageNames.contains(it.name) }
        if (inUseImagesMarkedForDelete) {
            throw new IllegalStateException("Aborting mass delete. " +
                    "In ${grailsApplication.config.cloud.accountName} ${userContext.region} the following in use " +
                    "images were marked for delete: ${inUseImagesMarkedForDelete*.imageId}")
        }
    }

    /**
     * Checks that there are a lot of lastReferencedTime values from the past 24 hours. Otherwise the reference time
     * tagging system is broken, which would mean it's not safe to delete any images.
     *
     * @param userContext who, where, why
     * @param images all the images in the account
     */
    private void abortIfNotEnoughRecentlyUsed(UserContext userContext, Collection<Image> images) {
        DateTime yesterday = Time.now().minusDays(1)
        Integer recentlyUsedCount = 0
        Integer localInUseAmiCountForRegion = getLocalImageIdsInUse(userContext).size()
        for (Image image in images) {
            DateTime lastReferencedTime = Time.parse(image.getLastReferencedTime())
            if (lastReferencedTime?.isAfter(yesterday)) {
                recentlyUsedCount++
            }
            if (recentlyUsedCount >= localInUseAmiCountForRegion) {
                // That's enough. We can stop parsing and counting now.
                break
            }
        }
        if (recentlyUsedCount < localInUseAmiCountForRegion) {
            throw new IllegalStateException("Aborting mass delete. " +
                    "In ${grailsApplication.config.cloud.accountName} ${userContext.region} only " +
                    "${recentlyUsedCount} AMIs were tagged with last_referenced_time recently, although " +
                    "${localInUseAmiCountForRegion} AMIs are in use. Is tagging broken?")
        }
    }

    private boolean withinMassDeleteThreshold(Image image, DateTime lastReferencedCutoff,
                                              DateTime neverReferencedCutoff) {
        DateTime creationTime = Time.parse(image.creationTime)
        DateTime lastReferencedTime = Time.parse(image.lastReferencedTime)

        // If there is no creation time then it's probably not safe to delete the AMI.
        if (!creationTime || image.keepForever) {
            return false
        }

        if (lastReferencedTime) {
            return lastReferencedTime.isBefore(lastReferencedCutoff)
        }
        creationTime.isBefore(neverReferencedCutoff)
    }

    private List<Image> removeExcludedLaunchPermissions(UserContext userContext, List<Image> imagesEligibleForDelete) {
        if (!imagesEligibleForDelete) { // Don't call Amazon again if there's nothing to delete
            return imagesEligibleForDelete
        }
        Collection<Image> imagesToExclude = awsEc2Service.getImagesWithLaunchPermissions(userContext,
                configService.getExcludedLaunchPermissionsForMassDelete(), imagesEligibleForDelete*.imageId)
        if (!imagesToExclude) {
            return imagesEligibleForDelete
        }
        log.debug("Excluding image ids ${imagesToExclude*.imageId} from mass delete because of launch permissions")
        Set<String> imageIdsToExclude = imagesToExclude*.imageId as Set // Use Set for constant time on contains()
        imagesEligibleForDelete.findAll { image -> !imageIdsToExclude.contains(image.imageId) }
    }

}
