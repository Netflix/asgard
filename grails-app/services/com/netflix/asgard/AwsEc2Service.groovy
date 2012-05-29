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
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.Address
import com.amazonaws.services.ec2.model.AssociateAddressRequest
import com.amazonaws.services.ec2.model.AttachVolumeRequest
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest
import com.amazonaws.services.ec2.model.AvailabilityZone
import com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsRequest
import com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsResult
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest
import com.amazonaws.services.ec2.model.CreateSnapshotRequest
import com.amazonaws.services.ec2.model.CreateTagsRequest
import com.amazonaws.services.ec2.model.CreateVolumeRequest
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest
import com.amazonaws.services.ec2.model.DeleteSnapshotRequest
import com.amazonaws.services.ec2.model.DeleteTagsRequest
import com.amazonaws.services.ec2.model.DeleteVolumeRequest
import com.amazonaws.services.ec2.model.DeregisterImageRequest
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult
import com.amazonaws.services.ec2.model.DescribeImageAttributeRequest
import com.amazonaws.services.ec2.model.DescribeImageAttributeResult
import com.amazonaws.services.ec2.model.DescribeImagesRequest
import com.amazonaws.services.ec2.model.DescribeImagesResult
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeRequest
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeResult
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.model.DescribeInstancesResult
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest
import com.amazonaws.services.ec2.model.DescribeReservedInstancesOfferingsRequest
import com.amazonaws.services.ec2.model.DescribeReservedInstancesOfferingsResult
import com.amazonaws.services.ec2.model.DescribeReservedInstancesRequest
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryRequest
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryResult
import com.amazonaws.services.ec2.model.DescribeVolumesRequest
import com.amazonaws.services.ec2.model.DetachVolumeRequest
import com.amazonaws.services.ec2.model.Filter
import com.amazonaws.services.ec2.model.GetConsoleOutputRequest
import com.amazonaws.services.ec2.model.GetConsoleOutputResult
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.InstanceStateChange
import com.amazonaws.services.ec2.model.IpPermission
import com.amazonaws.services.ec2.model.KeyPairInfo
import com.amazonaws.services.ec2.model.ModifyImageAttributeRequest
import com.amazonaws.services.ec2.model.RebootInstancesRequest
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult
import com.amazonaws.services.ec2.model.Reservation
import com.amazonaws.services.ec2.model.ReservedInstances
import com.amazonaws.services.ec2.model.ResetImageAttributeRequest
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest
import com.amazonaws.services.ec2.model.RunInstancesRequest
import com.amazonaws.services.ec2.model.RunInstancesResult
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.ec2.model.Snapshot
import com.amazonaws.services.ec2.model.SpotInstanceRequest
import com.amazonaws.services.ec2.model.Tag
import com.amazonaws.services.ec2.model.TerminateInstancesRequest
import com.amazonaws.services.ec2.model.TerminateInstancesResult
import com.amazonaws.services.ec2.model.UserIdGroupPair
import com.amazonaws.services.ec2.model.Volume
import com.amazonaws.services.ec2.model.VolumeAttachment
import com.google.common.collect.Multiset
import com.google.common.collect.TreeMultiset
import com.netflix.asgard.cache.CacheInitializer
import org.apache.commons.codec.binary.Base64
import org.codehaus.groovy.runtime.StackTraceUtils
import org.springframework.beans.factory.InitializingBean

class AwsEc2Service implements CacheInitializer, InitializingBean {

    static transactional = false

    def configService
    MultiRegionAwsClient<AmazonEC2> awsClient
    def awsClientService
    Caches caches
    def restClientService
    def taskService
    List<String> accounts  // main account is accounts[0]

    void afterPropertiesSet() {
        awsClient = new MultiRegionAwsClient<AmazonEC2>({ Region region ->
            AmazonEC2 client = awsClientService.create(AmazonEC2)
            client.setEndpoint("ec2.${region}.amazonaws.com")
            client
        })
        accounts = configService.awsAccounts
    }

    void initializeCaches() {
        caches.allKeyPairs.ensureSetUp({ Region region -> retrieveKeys(region) })
        caches.allAvailabilityZones.ensureSetUp({ Region region -> retrieveAvailabilityZones(region) },
                { Region region -> caches.allKeyPairs.by(region).fill() })
        caches.allImages.ensureSetUp({ Region region -> retrieveImages(region) })
        caches.allInstances.ensureSetUp({ Region region -> retrieveInstances(region) })
        caches.allReservedInstancesGroups.ensureSetUp({ Region region -> retrieveReservations(region) })
        caches.allSecurityGroups.ensureSetUp({ Region region -> retrieveSecurityGroups(region) })
        caches.allSnapshots.ensureSetUp({ Region region -> retrieveSnapshots(region) })
        caches.allVolumes.ensureSetUp({ Region region -> retrieveVolumes(region) })
    }

    // Availability Zones

    private List<AvailabilityZone> retrieveAvailabilityZones(Region region) {
        DescribeAvailabilityZonesResult result =
                awsClient.by(region).describeAvailabilityZones(new DescribeAvailabilityZonesRequest())
        result.getAvailabilityZones().sort { it.zoneName }
    }

    Collection<AvailabilityZone> getAvailabilityZones(UserContext userContext) {
        caches.allAvailabilityZones.by(userContext.region).list().sort { it.zoneName }
    }

    Collection<AvailabilityZone> getRecommendedAvailabilityZones(UserContext userContext) {
        List<String> discouragedAvailabilityZones = configService.discouragedAvailabilityZones
        getAvailabilityZones(userContext).findAll { !(it.zoneName in discouragedAvailabilityZones) }
    }

    // Images

    /** A cache of image collections for a given package name within a certain AWS region. */
    Map<Region, Map<String, Collection<Image>>> regionalPackagesToImageLists = [:]

    private List<Image> retrieveImages(Region region) {
        DescribeImagesRequest request = new DescribeImagesRequest().withOwners(accounts)
        AmazonEC2 awsClientForRegion = awsClient.by(region)
        List<Image> images = awsClientForRegion.describeImages(request).getImages()
        // Temporary workaround because Amazon can send us the list of images without the tags occasionally.
        // So far it's prevented the image cache from going in a bad state again, but we need a better long term fix.
        if (images && !images.any { it.tags } ) {
            log.warn "Detected image tags missing for region ${region.code}, attempting to request tags explicitly"
            Filter hasTagFilter = new Filter('tag-key', ['*']) // This only requests images that have tags
            DescribeImagesRequest hasTagRequest = request.withFilters(hasTagFilter)
            List<Image> imagesWithTags = awsClientForRegion.describeImages(hasTagRequest).getImages()
            Map<String, Image> imageIdToImageWithTags = imagesWithTags.inject([:]) { map, image ->
                map << [(image.imageId): image]
            } as Map<String, Image>
            // Merge the images with tags issue and add any tagless images to the list
            images = images.collect { imageIdToImageWithTags[it.imageId] ?: it }
        }

        regionalPackagesToImageLists[region] = getPackagesToImageLists(images)
        images
    }

    private void refreshImageCacheForPackageName(UserContext userContext, String packageName) {
        if (packageName) {
            Map<String, Collection<Image>> packagesToImageLists = regionalPackagesToImageLists[userContext.region]
            if (packagesToImageLists) {
                Collection<Image> imagesForPackage = getAccountImages(userContext).
                        findAll { packageName == it.packageName }
                packagesToImageLists[packageName] = imagesForPackage
            }
        }
    }

    private Map<String, Collection<Image>> getPackagesToImageLists(Collection<Image> images) {
        Map<String, Collection<Image>> namesToLists = new HashMap<String, Collection<Image>>()
        images.each { Image image ->
            String packageName = image.packageName
            if (packageName) {
                Collection<Image> imagesForPackage = namesToLists[packageName] ?: []
                imagesForPackage << image
                namesToLists[packageName] = imagesForPackage
            }
        }
        namesToLists
    }

    Collection<Image> getAccountImages(UserContext userContext) {
        caches.allImages.by(userContext.region).list()
    }

    /**
     * Based on a list of users and image ids, gives back a list of image objects for those ids that would be executable
     * by any of those users.
     *
     * @param executableUsers Amazon account ids to check launch permissions on the image for. Will return image if it
     *         matches any of the ids
     * @param imageIds List of image ids to filter on
     * @return Collection< Image > The images which match the imageIds where any of the users in executableUsers have
     *         launch permissions
     */
    Collection<Image> getImagesWithLaunchPermissions(UserContext userContext, Collection<String> executableUsers, Collection<String> imageIds) {
        DescribeImagesRequest request = new DescribeImagesRequest(executableUsers: executableUsers, imageIds: imageIds)
        DescribeImagesResult result = awsClient.by(userContext.region).describeImages(request)
        result.images
    }

    /**
     * Gets the images that have the specified package name (usually app name) if any. If there are no images for the
     * specified package name, or if the specified package name is null or empty, then this method returns all the
     * images.
     *
     * @param name the package name (usually app name) to look for
     * @return Collection< Image > the images with the specified package name, or all images if none matched
     */
    Collection<Image> getImagesForPackage(UserContext userContext, String name) {
        Map<String, Collection<Image>> packagesToImageLists = regionalPackagesToImageLists[userContext.region]
        if (name && packagesToImageLists) {
            Collection<Image> images = packagesToImageLists[name]
            return images ?: []
        }
        getAccountImages(userContext)
    }

    Image getImage(UserContext userContext, String imageId, From preferredDataSource = From.AWS) {
        Image image = null
        if (imageId) {
            if (preferredDataSource == From.CACHE) {
                image = caches.allImages.by(userContext.region).get(imageId)
                if (image) { return image }
            }
            def request = new DescribeImagesRequest().withImageIds(imageId)
            try {
                List<Image> images = awsClient.by(userContext.region).describeImages(request).getImages()
                image = Check.loneOrNone(images, Image)
            }
            catch (AmazonServiceException ase) {
                // If Amazon doesn't know this image id then return null and put null in the allImages CachedMap
            }
            caches.allImages.by(userContext.region).put(imageId, image)
            refreshImageCacheForPackageName(userContext, image?.packageName)
        }
        image
    }

    List<String> getImageLaunchers(UserContext userContext, String imageId) {
        DescribeImageAttributeRequest request = new DescribeImageAttributeRequest()
                .withImageId(imageId)
                .withAttribute('launchPermission')
        DescribeImageAttributeResult result = awsClient.by(userContext.region).describeImageAttribute(request)
        result.getImageAttribute().getLaunchPermissions().collect { it.userId }
    }

    Map<String, Image> mapImageIdsToImagesForMergedInstances(UserContext userContext,
                                                             Collection<MergedInstance> mergedInstances) {
        Map<String, Image> imageIdsToImages = new HashMap<String, Image>()
        for (MergedInstance mergedInstance : mergedInstances) {
            String imageId = mergedInstance?.amiId
            if (!(imageId in imageIdsToImages.keySet())) {
                // Avoid wasteful calls to Amazon when possible by using cached image data.
                imageIdsToImages.put(imageId, getImage(userContext, imageId, From.CACHE))
            }
        }
        imageIdsToImages
    }

    void deregisterImage(UserContext userContext, String imageId, Task existingTask = null) {
        String msg = "Deregister image ${imageId}"

        Closure work = { Task task ->
            // Verify that the image exists
            Image image = getImage(userContext, imageId)
            if (image) {
                String packageName = image.packageName
                DeregisterImageRequest request = new DeregisterImageRequest().withImageId(image.imageId)
                awsClient.by(userContext.region).deregisterImage(request)
                refreshImageCacheForPackageName(userContext, packageName)
            }
            getImage(userContext, imageId)
        }
        taskService.runTask(userContext, msg, work, Link.to(EntityType.image, imageId), existingTask)
    }

    //mutators

    void setImageName(UserContext userContext, String imageId, String name) {
        taskService.runTask(userContext, "Set image ${imageId} name to ${name}", { task ->
            ModifyImageAttributeRequest request = new ModifyImageAttributeRequest()
                    .withImageId(imageId)
                    .withAttribute("name")
                    .withOperationType("add")
                    .withValue(name)
            awsClient.by(userContext.region).modifyImageAttribute(request)
        }, Link.to(EntityType.image, imageId))
        getImage(userContext, imageId)
    }

    void setImageDescription(UserContext userContext, String imageId, String description) {
        taskService.runTask(userContext, "Set image ${imageId} description to ${description}", { task ->
            ModifyImageAttributeRequest request = new ModifyImageAttributeRequest()
                    .withImageId(imageId)
                    .withAttribute("description")
                    .withOperationType("add")
                    .withValue(description)
            awsClient.by(userContext.region).modifyImageAttribute(request)
        }, Link.to(EntityType.image, imageId))
        getImage(userContext, imageId)
    }

    void addImageLaunchers(UserContext userContext, String imageId, List<String> userIds, Task existingTask = null) {
        taskService.runTask(userContext, "Add to image ${imageId}, launchers ${userIds}", { task ->
            ModifyImageAttributeRequest request = new ModifyImageAttributeRequest()
                    .withImageId(imageId)
                    .withAttribute('launchPermission')
                    .withOperationType('add')
                    .withUserIds(userIds)
            awsClient.by(userContext.region).modifyImageAttribute(request)
        }, Link.to(EntityType.image, imageId), existingTask)
        getImage(userContext, imageId)
    }

    void removeImageLaunchers(UserContext userContext, String imageId, List<String> userIds) {
        taskService.runTask(userContext, "Remove from image ${imageId}, launchers ${userIds}", { task ->
            ModifyImageAttributeRequest request = new ModifyImageAttributeRequest()
                    .withImageId(imageId)
                    .withAttribute('launchPermission')
                    .withOperationType('remove')
                    .withUserIds(userIds)
            awsClient.by(userContext.region).modifyImageAttribute(request)
        }, Link.to(EntityType.image, imageId))
        getImage(userContext, imageId)
    }

    void setImageLaunchers(UserContext userContext, String imageId, List<String> userIds) {
        taskService.runTask(userContext, "Set image ${imageId} launchers to ${userIds}", { task ->
            awsClient.by(userContext.region).resetImageAttribute(new ResetImageAttributeRequest()
                    .withImageId(imageId)
                    .withAttribute('launchPermission'))
            ModifyImageAttributeRequest request = new ModifyImageAttributeRequest()
                    .withImageId(imageId)
                    .withAttribute('launchPermission')
                    .withOperationType('add')
                    .withUserIds(userIds)
            awsClient.by(userContext.region).modifyImageAttribute(request)
        }, Link.to(EntityType.image, imageId))
        getImage(userContext, imageId)
    }

    void createImageTag(UserContext userContext, String imageId, String name, String value) {
        Check.notEmpty(imageId, "imageId")
        createImageTags(userContext, [imageId], name, value)
    }

    void deleteImageTag(UserContext userContext, String imageId, String name) {
        Check.notEmpty(imageId, "imageId")
        deleteImageTags(userContext, [imageId], name)
    }

    void createImageTags(UserContext userContext, Collection<String> imageIds, String name, String value) {
        Check.notEmpty(imageIds, "imageIds")
        Check.notEmpty(name, "name")
        Check.notEmpty(value, "value")
        awsClient.by(userContext.region).createTags(
                new CreateTagsRequest().withResources(imageIds).withTags(new Tag(name, value)))
    }

    void deleteImageTags(UserContext userContext, Collection<String> imageIds, String name) {
        Check.notEmpty(imageIds, "imageIds")
        Check.notEmpty(name, "name")
        awsClient.by(userContext.region).deleteTags(
                new DeleteTagsRequest().withResources(imageIds).withTags(new Tag(name))
        )
    }

    /**
     * Adds all secondary accounts to a given image and returns the list of all those added.
     * Fails silently if there is a permission problem adding.
     */
    List<String> authorizeSecondaryImageLaunchers(UserContext userContext, String imageId, Task existingTask = null) {
        try {
            List<String> hasAccounts = getImageLaunchers(userContext, imageId)
            hasAccounts += accounts[0]
            List<String> addAccounts = accounts.findAll {account -> !hasAccounts.any {it == account}}
            if (addAccounts.size() > 0) {
                addImageLaunchers(userContext, imageId, addAccounts, existingTask)
            }
            return addAccounts
        } catch (Exception e) {
            return []  // permission problem
        }
    }

    RunInstancesResult runInstances(UserContext userContext, RunInstancesRequest request) {
        awsClient.by(userContext.region).runInstances(request)
    }

    // Security

    Collection<KeyPairInfo> getKeys(UserContext userContext) {
        caches.allKeyPairs.by(userContext.region).list()
    }

    private List<KeyPairInfo> retrieveKeys(Region region) {
        awsClient.by(region).describeKeyPairs(new DescribeKeyPairsRequest()).keyPairs
    }

    String getDefaultKeyName() {
        configService.defaultKeyName
    }

    Collection<SecurityGroup> getSecurityGroups(UserContext userContext) {
        caches.allSecurityGroups.by(userContext.region).list()
    }

    /** Returns a filtered and sorted list of security groups to show in UI lists. Special groups are suppressed. */
    Collection<SecurityGroup> getEffectiveSecurityGroups(UserContext userContext) {
        getSecurityGroups(userContext).findAll{ isSecurityGroupEditable(it.groupName) }.sort{ it.groupName.toLowerCase() }
    }

    Boolean isSecurityGroupEditable(String name) {
        name != 'default'
    }

    private List<SecurityGroup> retrieveSecurityGroups(Region region) {
        awsClient.by(region).describeSecurityGroups(new DescribeSecurityGroupsRequest()).securityGroups
    }

    List<SecurityGroup> getSecurityGroupsForApp(UserContext userContext, String appName) {
        def pat = ~"^${appName.toLowerCase()}(-frontend)?\$"
        getSecurityGroups(userContext).findAll { it.groupName ==~ pat }
    }

    SecurityGroup getSecurityGroup(UserContext userContext, String name, From from = From.AWS) {
        Check.notNull(name, SecurityGroup, "name")
        SecurityGroup group
        if (from == From.CACHE) {
            return caches.allSecurityGroups.by(userContext.region).get(name)
        }
        try {
            def result = awsClient.by(userContext.region).describeSecurityGroups(
                    new DescribeSecurityGroupsRequest().withGroupNames([name]))
            group = Check.lone(result.getSecurityGroups(), SecurityGroup)
        } catch (com.amazonaws.AmazonServiceException e) {
            group = null
        }
        caches.allSecurityGroups.by(userContext.region).put(name, group)
    }

    // mutators

    SecurityGroup createSecurityGroup(UserContext userContext, String name, String description) {
        Check.notEmpty(name, 'name')
        Check.notEmpty(description, 'description')
        taskService.runTask(userContext, "Create Security Group ${name}", { task ->
            awsClient.by(userContext.region).createSecurityGroup(
                    new CreateSecurityGroupRequest().withGroupName(name).withDescription(description))
        }, Link.to(EntityType.security, name))
        getSecurityGroup(userContext, name)
    }

    void removeSecurityGroup(UserContext userContext, String name) {
        taskService.runTask(userContext, "Remove Security Group ${name}", { task ->
            def request = new DeleteSecurityGroupRequest().withGroupName(name)
            awsClient.by(userContext.region).deleteSecurityGroup(request)  // no result
        }, Link.to(EntityType.security, name))
        caches.allSecurityGroups.by(userContext.region).remove(name)
    }

    /** High-level permission update for a group pair: given the desired state, make it so. */
    void updateSecurityGroupPermissions(UserContext userContext, SecurityGroup targetGroup, String sourceGroupName, String wantPorts) {
        List<IpPermission> havePerms = targetGroup.ipPermissions.findAll { it.userIdGroupPairs.any { it.groupName == sourceGroupName }}
        List<IpPermission> wantPerms = permissionsFromString(wantPorts)
        //println " access from ${sourceGroup} to ${targetGroup.groupName}? have:${permissionsToString(havePerms)} want:${permissionsToString(wantPerms)}"
        Boolean somethingChanged = false
        havePerms.each { havePerm ->
            if (!wantPerms.any { wp -> wp.fromPort == havePerm.fromPort && wp.toPort == havePerm.toPort } ) {
                revokeSecurityGroupIngress(userContext, targetGroup.groupName, sourceGroupName, 'tcp',
                        havePerm.fromPort, havePerm.toPort)
                somethingChanged = true
            }
        }
        wantPerms.each { wantPerm ->
            if (!havePerms.any { hp -> hp.fromPort == wantPerm.fromPort && hp.toPort == wantPerm.toPort} ) {
                authorizeSecurityGroupIngress(userContext, targetGroup.groupName, sourceGroupName, 'tcp',
                        wantPerm.fromPort, wantPerm.toPort)
                somethingChanged = true
            }
        }
        // This method gets called hundreds of times for one user request so don't call Amazon unless necessary.
        if (somethingChanged) {
            getSecurityGroup(userContext, targetGroup.groupName)
        }
    }

    /** Converts a list of IpPermissions into a string representation, or null if none. */
    private static String permissionsToString(Collection<IpPermission> permissions) {
        if (permissions.size() > 0) {
            return permissions.inject('') { String result, IpPermission it ->
                def p = portString(it.fromPort, it.toPort)
                result.length() > 0 ? result + ',' + p : p
            }
        } else {
            return null
        }
    }

    /** Returns the canonical string representation of a from-to port pair. */
    public static String portString(int fromPort, int toPort) {
        toPort == fromPort ? "${fromPort}" : "${fromPort}-${toPort}"
    }

    /** Converts a string ports representation into a list of partially populated IpPermission instances. */
    static List<IpPermission> permissionsFromString(String portsStr) {
        List<IpPermission> perms = []
        if (portsStr) {
            portsStr.split(',').each {rangeStr->
                def m = rangeStr =~ /(-?\d+)(-(-?\d+))?/
                //println "permissionsFromString: ${portStr} => ${m[0]}"
                if (m.matches()) {
                    def rangeParts = m[0]  // 0:all 1:from 2:dashAndTo 3:to
                    def fromPort = rangeParts[1]
                    def toPort = rangeParts[3] ?: fromPort
                    perms += new IpPermission().withFromPort(fromPort.toInteger()).withToPort(toPort.toInteger())
                }
            }
        }
        perms
    }

    /** Returns the ingress permissions from one group to another as a strings. Assumes tcp and groups, not cidrs. */
    static String getIngressFrom(SecurityGroup targetGroup, String srcGroupName) {
        Collection<IpPermission> ipPermissions = targetGroup.ipPermissions.findAll {
            it.userIdGroupPairs.any { it.groupName == srcGroupName }
        }
        permissionsToString(ipPermissions)
    }

    String bestIngressPortsFor(SecurityGroup targetGroup) {
        Map guess = ['7001' : 1]
        targetGroup.ipPermissions.each {
            if (it.ipProtocol == 'tcp' &&  it.userIdGroupPairs.size() > 0) {
                Integer count = it.userIdGroupPairs.size()
                String portRange = portString(it.fromPort, it.toPort)
                guess[portRange] = guess[portRange] ? guess[portRange] + count : count
            }
        }
        String g = guess.sort { -it.value }.collect { it.key }[0]
        //println "guess: ${target.groupName} ${guess} => ${g}"
        g
    }

    // TODO refactor the following two methods to take IpPermissions List from callers now that AWS API takes those.

    private void authorizeSecurityGroupIngress(UserContext userContext, String groupName, String sourceGroupName, String ipProtocol, int fromPort, int toPort) {
        UserIdGroupPair sourcePair = new UserIdGroupPair().withUserId(accounts[0]).withGroupName(sourceGroupName)
        List<IpPermission> perms = [
                new IpPermission()
                        .withUserIdGroupPairs(sourcePair)
                        .withIpProtocol(ipProtocol).withFromPort(fromPort).withToPort(toPort)
        ]
        taskService.runTask(userContext, "Authorize Security Group Ingress to ${groupName} from ${sourceGroupName} on ${fromPort}-${toPort}", { task ->
            awsClient.by(userContext.region).authorizeSecurityGroupIngress(
                    new AuthorizeSecurityGroupIngressRequest().withGroupName(groupName).withIpPermissions(perms))
        }, Link.to(EntityType.security, groupName))
    }

    private void revokeSecurityGroupIngress(UserContext userContext, String groupName, String sourceGroupName, String ipProtocol, int fromPort, int toPort) {
        UserIdGroupPair sourcePair = new UserIdGroupPair().withUserId(accounts[0]).withGroupName(sourceGroupName)
        List<IpPermission> perms = [
                new IpPermission()
                        .withUserIdGroupPairs(sourcePair)
                        .withIpProtocol(ipProtocol).withFromPort(fromPort).withToPort(toPort)
        ]
        taskService.runTask(userContext, "Revoke Security Group Ingress to ${groupName} from ${sourceGroupName} on ${fromPort}-${toPort}", { task ->
            awsClient.by(userContext.region).revokeSecurityGroupIngress(
                    new RevokeSecurityGroupIngressRequest().withGroupName(groupName).withIpPermissions(perms))
        }, Link.to(EntityType.security, groupName))
    }

    DescribeReservedInstancesOfferingsResult describeReservedInstancesOfferings(Region region) {
        awsClient.by(region).describeReservedInstancesOfferings(new DescribeReservedInstancesOfferingsRequest())
    }

    DescribeSpotPriceHistoryResult describeSpotPriceHistory(Region region,
            DescribeSpotPriceHistoryRequest describeSpotPriceHistoryRequest) {
        awsClient.by(region).describeSpotPriceHistory(describeSpotPriceHistoryRequest)
    }

    // Spot Instance Requests

    List<SpotInstanceRequest> retrieveSpotInstanceRequests(Region region) {
        awsClient.by(region).describeSpotInstanceRequests().spotInstanceRequests
    }

    DescribeSpotInstanceRequestsResult describeSpotInstanceRequests(UserContext userContext,
                                                                    DescribeSpotInstanceRequestsRequest request) {
        awsClient.by(userContext.region).describeSpotInstanceRequests(request)
    }

    void createTags(UserContext userContext, CreateTagsRequest request) {
        awsClient.by(userContext.region).createTags(request)
    }

    CancelSpotInstanceRequestsResult cancelSpotInstanceRequests(UserContext userContext,
                                                                CancelSpotInstanceRequestsRequest request) {
        awsClient.by(userContext.region).cancelSpotInstanceRequests(request)
    }

    RequestSpotInstancesResult requestSpotInstances(UserContext userContext, RequestSpotInstancesRequest request) {
         awsClient.by(userContext.region).requestSpotInstances(request)
    }

    // Instances

    private List<Instance> retrieveInstances(Region region) {
        List<Instance> instances = []
        def result = awsClient.by(region).describeInstances(new DescribeInstancesRequest())
        def reservations = result.getReservations()
        for (res in reservations) {
            for (ri in res.getInstances()) {
                instances.add(ri)
            }
        }
        instances
    }

    Collection<Instance> getInstances(UserContext userContext) {
        caches.allInstances.by(userContext.region)?.list() ?: []
    }

    List<Instance> getInstancesByIds(UserContext userContext, List<String> instanceIds, From from = From.CACHE) {
        List<Instance> instances = []
        if (from == From.AWS) {
            DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceIds)
            DescribeInstancesResult result = awsClient.by(userContext.region).describeInstances(request)
            List<Reservation> reservations = result.reservations
            instances.addAll(reservations*.instances.flatten())
            Map<String, Instance> instanceIdsToInstances = instances.inject([:]) { Map map, Instance instance ->
                map << [(instance.instanceId): instance]
            } as Map
            caches.allInstances.by(userContext.region).putAll(instanceIdsToInstances)
        } else if (from == From.CACHE) {
            for (String instanceId in instanceIds) {
                Instance instance = caches.allInstances.by(userContext.region).get(instanceId)
                if (instance) { instances << instance }
            }
        }
        instances
    }

    Collection<Instance> getInstancesUsingImageId(UserContext userContext, String imageId) {
        Check.notEmpty(imageId)
        getInstances(userContext).findAll { Instance instance -> instance.imageId == imageId }
    }

    Instance getInstance(UserContext userContext, String instanceId, From from = From.AWS) {
        if (from == From.CACHE) {
            return caches.allInstances.by(userContext.region).get(instanceId)
        }
        List<Instance> instances = getInstanceReservation(userContext, instanceId)?.instances
        instances ? instances[0] : null
    }

    Multiset<AppVersion> getCountedAppVersions(UserContext userContext) {
        getCountedAppVersions(getInstances(userContext), caches.allImages.by(userContext.region).unmodifiable())
    }

    private Multiset<AppVersion> getCountedAppVersions(Collection<Instance> instances, Map<String, Image> images) {
        Multiset<AppVersion> appVersions = TreeMultiset.create()
        instances.each { Instance instance ->
            Image image = images.get(instance.imageId)
            AppVersion appVersion = image?.parsedAppVersion
            if (appVersion) {
                appVersions.add(appVersion)
            }
        }
        appVersions
    }

    Reservation getInstanceReservation(UserContext userContext, String instanceId) {
        Check.notNull(instanceId, Reservation, "instanceId")
        def result
        try {
            result = awsClient.by(userContext.region).describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId))
        }
        catch (AmazonServiceException ase) {
            log.info "Request for instance ${instanceId} failed because ${ase}"
            return null
        }
        List<Reservation> reservations = result.reservations
        if (reservations.size() < 1 || reservations[0].instances.size() < 1) {
            log.info "Request for instance ${instanceId} failed because the instance no longer exists"
            return null
        }
        Reservation reservation = Check.lone(reservations, Reservation)
        caches.allInstances.by(userContext.region).put(instanceId, Check.lone(reservation.instances, Instance))
        reservation
    }

    void createInstanceTags(UserContext userContext, List<String> instanceIds, Map<String, String> tagNameValuePairs,
                           Task existingTask = null) {
        Integer instanceCount = instanceIds.size()
        List<Tag> tags = tagNameValuePairs.collect { String key, String value -> new Tag(key, value) }
        String tagSuffix = tags.size() == 1 ? '' : 's'
        String instanceSuffix = instanceCount == 1 ? '' : 's'
        String msg = "Create tag${tagSuffix} '${tagNameValuePairs}' on ${instanceCount} instance${instanceSuffix}"
        Closure work = { Task task ->
            CreateTagsRequest request = new CreateTagsRequest().withResources(instanceIds).withTags(tags)
            awsClient.by(userContext.region).createTags(request)
        }
        Link link = instanceIds.size() == 1 ? Link.to(EntityType.instance, instanceIds[0]) : null
        taskService.runTask(userContext, msg, work, link, existingTask)
    }

    void createInstanceTag(UserContext userContext, List<String> instanceIds, String name, String value,
                           Task existingTask = null) {
        createInstanceTags(userContext, instanceIds, [(name): value], existingTask)
    }

    void deleteInstanceTag(UserContext userContext, String instanceId, String name) {
        taskService.runTask(userContext, "Delete tag '${name}' from instance ${instanceId}", { task ->
            DeleteTagsRequest request = new DeleteTagsRequest().withResources(instanceId).withTags(new Tag(name))
            awsClient.by(userContext.region).deleteTags(request)
        }, Link.to(EntityType.instance, instanceId))
    }

    String getUserDataForInstance(UserContext userContext, String instanceId) {
        if (!instanceId) { return null }
        DescribeInstanceAttributeResult attrResult = awsClient.by(userContext.region).describeInstanceAttribute(
                new DescribeInstanceAttributeRequest().withInstanceId(instanceId).withAttribute("userData"))
        String userData = attrResult.getInstanceAttribute().getUserData()
        userData ? new String(Base64.decodeBase64(userData.bytes)) : null
    }

    Integer getRepeatedResponseCode(String url) {
        Integer responseCode = restClientService.getResponseCode(url)
        if (checkOkayResponseCode(responseCode)) {
            return responseCode
        }
        // First try failed but that might have been a network fluke.
        // If the next two staggered attempts pass, then assume the host is healthy.
        Time.sleepCancellably 2000
        responseCode = restClientService.getResponseCode(url)
        if (checkOkayResponseCode(responseCode)) {
            // First try failed, second try passed. Use the tie-breaker as the final answer.
            Time.sleepCancellably 2000
            return restClientService.getResponseCode(url)
        }
        // First two tries both failed. Give up and return the latest failure code.
        return responseCode
    }

    Boolean checkHostHealth(String url) {
        checkOkayResponseCode(getRepeatedResponseCode(url))
    }

    private Boolean checkOkayResponseCode(Integer responseCode) {
        responseCode == 200
    }

    List<InstanceStateChange> terminateInstances(UserContext userContext, Collection<String> instanceIds,
                                                 Task existingTask = null) {
        Check.notEmpty(instanceIds, 'instanceIds')
        List<InstanceStateChange> terminatingInstances = []
        Closure work = { Task task ->
            TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(instanceIds)
            TerminateInstancesResult result = awsClient.by(userContext.region).terminateInstances(request)

            // Refresh instance cache
            getInstancesByIds(userContext, instanceIds as List, From.AWS)

            result.terminatingInstances
        }
        String msg = "Terminate ${instanceIds.size()} instance${instanceIds.size() == 1 ? '' : 's'} ${instanceIds}"
        taskService.runTask(userContext, msg, work, null, existingTask)
        terminatingInstances
    }

    void rebootInstance(UserContext userContext, String instanceId) {
        taskService.runTask(userContext, "Reboot ${instanceId}", { task ->
            awsClient.by(userContext.region).rebootInstances(new RebootInstancesRequest().withInstanceIds(instanceId))
        }, Link.to(EntityType.instance, instanceId))
    }

    String getConsoleOutput(UserContext userContext, String instanceId) {
        GetConsoleOutputResult result = awsClient.by(userContext.region).getConsoleOutput(
                new GetConsoleOutputRequest().withInstanceId(instanceId))
        new String(Base64.decodeBase64(result.getOutput().bytes))
    }

    // Elastic IPs

    Map<String, String> describeAddresses(UserContext userContext) {
        List<Address> addresses = awsClient.by(userContext.region).describeAddresses().addresses
        addresses.inject([:]) { Map memo, address -> memo[address.publicIp] = address.instanceId; memo } as Map
    }
    //describeAddresses(List<String>) => List<Address> => [instanceId,publicIp]

    void associateAddress(UserContext userContext, String publicIp, String instanceId) {
        taskService.runTask(userContext, "Associate ${publicIp} with ${instanceId}", { task ->
            awsClient.by(userContext.region).associateAddress(new AssociateAddressRequest().withPublicIp(publicIp).withInstanceId(instanceId))
        }, Link.to(EntityType.instance, instanceId))
    }

    // allocateAddress() => publicIp
    // releaseAddress(publicIp)

    // Reservations

    private List<ReservedInstances> retrieveReservations(Region region) {
        awsClient.by(region).describeReservedInstances().reservedInstances
    }

    Collection<ReservedInstances> getReservedInstances(UserContext userContext) {
        caches.allReservedInstancesGroups.by(userContext.region).list()
    }

    ReservedInstances getReservedInstances(UserContext userContext, String reservationId) {
        Check.notNull(reservationId, ReservedInstances, "reservationId")
        def result = awsClient.by(userContext.region).describeReservedInstances(
                new DescribeReservedInstancesRequest().withReservedInstancesIds(reservationId))
        ReservedInstances lone = Check.lone(result.getReservedInstances(), ReservedInstances)
        caches.allReservedInstancesGroups.by(userContext.region).put(reservationId, lone)
    }

    //Volumes

    Collection<Volume> getVolumes(UserContext userContext) {
        caches.allVolumes.by(userContext.region).list()
    }

    private List<Volume> retrieveVolumes(Region region) {
        awsClient.by(region).describeVolumes(new DescribeVolumesRequest()).volumes
    }

    Volume getVolume(UserContext userContext, String volumeId, From from = From.AWS) {
        if (volumeId) {
            if (from == From.CACHE) {
                return caches.allVolumes.by(userContext.region).get(volumeId)
            }
            try {
                def result = awsClient.by(userContext.region).describeVolumes(
                        new DescribeVolumesRequest().withVolumeIds([volumeId]))
                def volumes = result.getVolumes()
                if (volumes.size() > 0) {
                    def volume = Check.lone(volumes, Volume)
                    caches.allVolumes.by(userContext.region).put(volumeId, volume)
                    return volume
                }
            } catch (AmazonServiceException ase) {
                log.error("Error retrieving volume ${volumeId}", StackTraceUtils.sanitize(ase))
            }
        }
        null
    }

    VolumeAttachment detachVolume(UserContext userContext, String volumeId, String instanceId, String device) {
        def result = awsClient.by(userContext.region).detachVolume(new DetachVolumeRequest()
            .withVolumeId(volumeId)
            .withInstanceId(instanceId)
            .withDevice(device))
        return result.attachment
    }

    VolumeAttachment attachVolume(UserContext userContext, String volumeId, String instanceId, String device) {
        def result = awsClient.by(userContext.region).attachVolume(new AttachVolumeRequest()
            .withVolumeId(volumeId)
            .withInstanceId(instanceId)
            .withDevice(device))
        return result.attachment
    }

    void deleteVolume(UserContext userContext, String volumeId) {
        awsClient.by(userContext.region).deleteVolume(new DeleteVolumeRequest().withVolumeId(volumeId))
        // Do not remove it from the allVolumes map, as this prevents
        // the list page from showing volumes that are in state "deleting".
        // Volume deletes can take 20 minutes to process.
    }

    Volume createVolume(UserContext userContext, Integer size, String zone) {
        createVolume(userContext, size, zone, null)
    }

    Volume createVolumeFromSnapshot(UserContext userContext, Integer size, String zone, String snapshotId) {
        createVolume(userContext, size, zone, snapshotId)
    }

    Volume createVolume(UserContext userContext, Integer size, String zone, String snapshotId) {
        def request = new CreateVolumeRequest()
            .withSize(size)
            .withAvailabilityZone(zone)
        if (snapshotId) { request.setSnapshotId(snapshotId) }
        Volume volume = awsClient.by(userContext.region).createVolume(request).volume
        caches.allVolumes.by(userContext.region).put(volume.getVolumeId(), volume)
        return volume
    }

    // Snapshots

    Collection<Snapshot> getSnapshots(UserContext userContext) {
        caches.allSnapshots.by(userContext.region).list()
    }

    private List<Snapshot> retrieveSnapshots(Region region) {
        awsClient.by(region).describeSnapshots().snapshots
    }

    Snapshot getSnapshot(UserContext userContext, String snapshotId, From from = From.AWS) {
        if (snapshotId) {
            if (from == From.CACHE) {
                return caches.allSnapshots.by(userContext.region).get(snapshotId)
            }
            try {
                def result = awsClient.by(userContext.region).describeSnapshots(
                        new DescribeSnapshotsRequest().withSnapshotIds([snapshotId]))
                def snapshots = result.getSnapshots()
                if (snapshots.size() > 0) {
                    Snapshot snapshot = Check.lone(snapshots, Snapshot)
                    caches.allSnapshots.by(userContext.region).put(snapshotId, snapshot)
                    return snapshot
                }
            } catch (AmazonServiceException ase) {
                log.error("Error retrieving snapshot ${snapshotId}", StackTraceUtils.sanitize(ase))
            }
        }
        null
    }

    Snapshot createSnapshot(UserContext userContext, String volumeId, String description) {
        Snapshot snapshot = null
        String msg = "Create snapshot for volume '${volumeId}' with description '${description}'"
        taskService.runTask(userContext, msg, { task ->
            def result = awsClient.by(userContext.region).createSnapshot(new CreateSnapshotRequest()
                .withVolumeId(volumeId)
                .withDescription(description))
            snapshot = result.snapshot
            task.log("Snapshot ${snapshot.getSnapshotId()} created")
            caches.allSnapshots.by(userContext.region).put(snapshot.getSnapshotId(), snapshot)
        }, Link.to(EntityType.volume, volumeId))
        snapshot
    }

    void deleteSnapshot(UserContext userContext, String snapshotId, Task existingTask = null) {
        String msg = "Delete snapshot ${snapshotId}"
        Closure work = { Task task ->
            task.tryUntilSuccessful(
                    {
                        DeleteSnapshotRequest request = new DeleteSnapshotRequest().withSnapshotId(snapshotId)
                        awsClient.by(userContext.region).deleteSnapshot(request)
                    },
                    { Exception e -> e instanceof AmazonServiceException && e.errorCode == 'InvalidSnapshot.InUse' }
            )
            caches.allSnapshots.by(userContext.region).remove(snapshotId)
        }
        taskService.runTask(userContext, msg, work, Link.to(EntityType.snapshot, snapshotId), existingTask)
    }
}
