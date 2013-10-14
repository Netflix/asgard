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
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult
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
import com.amazonaws.services.ec2.model.Subnet
import com.amazonaws.services.ec2.model.Tag
import com.amazonaws.services.ec2.model.TerminateInstancesRequest
import com.amazonaws.services.ec2.model.TerminateInstancesResult
import com.amazonaws.services.ec2.model.UserIdGroupPair
import com.amazonaws.services.ec2.model.Volume
import com.amazonaws.services.ec2.model.VolumeAttachment
import com.amazonaws.services.ec2.model.Vpc
import com.google.common.collect.HashMultiset
import com.google.common.collect.Lists
import com.google.common.collect.Multiset
import com.google.common.collect.TreeMultiset
import com.netflix.asgard.cache.CacheInitializer
import com.netflix.asgard.model.AutoScalingGroupData
import com.netflix.asgard.model.SecurityGroupOption
import com.netflix.asgard.model.Subnets
import com.netflix.asgard.model.ZoneAvailability
import com.netflix.frigga.ami.AppVersion
import groovyx.gpars.GParsExecutorsPool
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.apache.commons.codec.binary.Base64
import org.codehaus.groovy.runtime.StackTraceUtils
import org.springframework.beans.factory.InitializingBean

class AwsEc2Service implements CacheInitializer, InitializingBean {

    static transactional = false

    private static Pattern SECURITY_GROUP_ID_PATTERN = ~/sg-[a-f0-9]+/

    MultiRegionAwsClient<AmazonEC2> awsClient
    def awsClientService
    Caches caches
    def configService
    def restClientService
    def taskService
    ThreadScheduler threadScheduler
    List<String> accounts = [] // main account is accounts[0]

    /** The state names for instances that count against reservation usage. */
    private static final List<String> ACTIVE_INSTANCE_STATES = ['pending', 'running'].asImmutable()

    /** Maximum number of image ids to send in a single create tags request. See ASGARD-895. */
    private static final int TAG_IMAGE_CHUNK_SIZE = 250

    void afterPropertiesSet() {
        awsClient = awsClient ?: new MultiRegionAwsClient<AmazonEC2>({ Region region ->
            AmazonEC2 client = awsClientService.create(AmazonEC2)
            client.setEndpoint("ec2.${region}.amazonaws.com")
            client
        })
        accounts = configService.awsAccounts
    }

    void initializeCaches() {
        caches.allVpcs.ensureSetUp({ Region region -> retrieveVpcs(region) })
        caches.allKeyPairs.ensureSetUp({ Region region -> retrieveKeys(region) })
        caches.allAvailabilityZones.ensureSetUp({ Region region -> retrieveAvailabilityZones(region) },
                { Region region -> caches.allKeyPairs.by(region).fill() })
        caches.allImages.ensureSetUp({ Region region -> retrieveImages(region) })
        caches.allInstances.ensureSetUp({ Region region -> retrieveInstances(region) })
        caches.allReservedInstancesGroups.ensureSetUp({ Region region -> retrieveReservations(region) })
        caches.allSecurityGroups.ensureSetUp({ Region region -> retrieveSecurityGroups(region) })
        caches.allSnapshots.ensureSetUp({ Region region -> retrieveSnapshots(region) })
        caches.allVolumes.ensureSetUp({ Region region -> retrieveVolumes(region) })
        caches.allSubnets.ensureSetUp({ Region region -> retrieveSubnets(region) })
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

    private List<Image> retrieveImages(Region region) {
        List<String> owners = configService.publicResourceAccounts + configService.awsAccounts
        DescribeImagesRequest request = new DescribeImagesRequest().withOwners(owners)
        AmazonEC2 awsClientForRegion = awsClient.by(region)
        List<Image> images = awsClientForRegion.describeImages(request).getImages()
        // Temporary workaround because Amazon can send us the list of images without the tags occasionally.
        // So far it's prevented the image cache from going in a bad state again, but we need a better long term fix.
        if (images && !images.any { it.tags } ) {
            log.trace "Detected image tags missing for region ${region.code}, attempting to request tags explicitly"
            Filter hasTagFilter = new Filter('tag-key', ['*']) // This only requests images that have tags
            DescribeImagesRequest hasTagRequest = request.withFilters(hasTagFilter)
            List<Image> imagesWithTags = awsClientForRegion.describeImages(hasTagRequest).getImages()
            Map<String, Image> imageIdToImageWithTags = imagesWithTags.inject([:]) { map, image ->
                map << [(image.imageId): image]
            } as Map<String, Image>
            // Merge the images with tags issue and add any tagless images to the list
            images = images.collect { imageIdToImageWithTags[it.imageId] ?: it }
        }

        images
    }

    Collection<Image> getAccountImages(UserContext userContext) {
        caches.allImages.by(userContext.region).list()
    }

    private Collection<Subnet> retrieveSubnets(Region region) {
        awsClient.by(region).describeSubnets().subnets
    }

    /**
     * Gets information about all subnets in a region.
     *
     * @param userContext who, where, why
     * @return a wrapper for querying subnets
     */
    Subnets getSubnets(UserContext userContext) {
        Subnets.from(caches.allSubnets.by(userContext.region).list())
    }

    private Collection<Vpc> retrieveVpcs(Region region) {
        awsClient.by(region).describeVpcs().vpcs
    }

    /**
     * Gets information about all VPCs in a region.
     *
     * @param userContext who, where, why
     * @return a list of VPCs
     */
    Collection<Vpc> getVpcs(UserContext userContext) {
        caches.allVpcs.by(userContext.region).list()
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
    Collection<Image> getImagesWithLaunchPermissions(UserContext userContext, Collection<String> executableUsers,
                                                     Collection<String> imageIds) {
        DescribeImagesRequest request = new DescribeImagesRequest(executableUsers: executableUsers, imageIds: imageIds)
        DescribeImagesResult result = awsClient.by(userContext.region).describeImages(request)
        result.images
    }

    /**
     * Gets the images that have the specified package name (usually app name) if any. If the specified package name is
     * null or empty, then this method returns all the images.
     *
     * @param name the package name (usually app name) to look for
     * @return Collection< Image > the images with the specified package name, or all images if name is null or empty
     */
    Collection<Image> getImagesForPackage(UserContext userContext, String name) {
        name ? getAccountImages(userContext).findAll { name == it.packageName } : getAccountImages(userContext)
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
            catch (AmazonServiceException ignored) {
                // If Amazon doesn't know this image id then return null and put null in the allImages CachedMap
            }
            caches.allImages.by(userContext.region).put(imageId, image)
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
        Map<String, Image> imageIdsToImages = [:]
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
                DeregisterImageRequest request = new DeregisterImageRequest().withImageId(image.imageId)
                awsClient.by(userContext.region).deregisterImage(request)
            }
            getImage(userContext, imageId)
        }
        taskService.runTask(userContext, msg, work, Link.to(EntityType.image, imageId), existingTask)
    }

    //mutators

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

    void createImageTags(UserContext userContext, Collection<String> imageIds, String name, String value) {
        Check.notEmpty(imageIds, "imageIds")
        Check.notEmpty(name, "name")
        Check.notEmpty(value, "value")
        List<List<String>> partitionedImageIds = Lists.partition(imageIds as List, TAG_IMAGE_CHUNK_SIZE)
        for (List<String> imageIdsChunk in partitionedImageIds) {
            CreateTagsRequest request = new CreateTagsRequest(resources: imageIdsChunk, tags: [new Tag(name, value)])
            awsClient.by(userContext.region).createTags(request)
        }
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
            hasAccounts += configService.awsAccountNumber
            List<String> addAccounts = configService.awsAccounts.findAll { acct -> !hasAccounts.any { it == acct } }
            if (addAccounts.size() > 0) {
                addImageLaunchers(userContext, imageId, addAccounts, existingTask)
            }
            return addAccounts
        } catch (Exception ignored) {
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

    /**
     * Returns a filtered and sorted list of security groups to show in UI lists. Special groups are suppressed.
     *
     * @param userContext who, where, why
     * @return list of security groups
     */
    List<SecurityGroup> getEffectiveSecurityGroups(UserContext userContext) {
        getSecurityGroups(userContext).findAll {
            isSecurityGroupEditable(it.groupName)
        }.sort { it.groupName.toLowerCase() }
    }

    Boolean isSecurityGroupEditable(String name) {
        name != 'default'
    }

    private List<SecurityGroup> retrieveSecurityGroups(Region region) {
        awsClient.by(region).describeSecurityGroups().securityGroups
    }

    List<SecurityGroup> getSecurityGroupsForApp(UserContext userContext, String appName) {
        def pat = ~"^${appName.toLowerCase()}(-frontend)?\$"
        getSecurityGroups(userContext).findAll { it.groupName ==~ pat }
    }

    SecurityGroup getSecurityGroup(UserContext userContext, String name, From from = From.AWS) {
        Region region = userContext.region
        Check.notNull(name, SecurityGroup, "name")
        String groupName
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest()
        String groupId = ''
        if (name ==~ SECURITY_GROUP_ID_PATTERN) {
            groupId = name
            request.withGroupIds(groupId)
            SecurityGroup cachedSecurityGroup = caches.allSecurityGroups.by(region).list().find { it.groupId == groupId }
            groupName = cachedSecurityGroup?.groupName
        } else {
            request.withGroupNames(name)
            groupName = name
        }
        if (from == From.CACHE) {
            return caches.allSecurityGroups.by(region).get(groupName)
        }
        SecurityGroup group = null
        try {
            DescribeSecurityGroupsResult result = awsClient.by(region).describeSecurityGroups(request)
            group = Check.lone(result?.getSecurityGroups(), SecurityGroup)
            groupName = group?.groupName
        } catch (AmazonServiceException e) {
            // Can't find a security group with that request.
            if (e.errorCode == 'InvalidParameterValue' && !groupId) {
                // It's likely a VPC security group which we can't reference by name. Maybe it has an ID in the cache.
                SecurityGroup cachedGroup = caches.allSecurityGroups.by(region).get(groupName)
                if (cachedGroup) {
                    request = new DescribeSecurityGroupsRequest(groupIds: [cachedGroup.groupId])
                    DescribeSecurityGroupsResult result = awsClient.by(region).describeSecurityGroups(request)
                    group = Check.lone(result?.getSecurityGroups(), SecurityGroup)
                }
            }
        }
        if (groupName) {
            return caches.allSecurityGroups.by(region).put(groupName, group)
        }
        null
    }

    /**
     * Calculates the relationships between most security groups (many potential sources of traffic) and one security
     * group (single target of traffic).
     *
     * @param userContext who, where, why
     * @param targetGroup the security group for the instances receiving traffic
     * @return list of security group options for display
     */
    List<SecurityGroupOption> getSecurityGroupOptionsForTarget(UserContext userContext, SecurityGroup targetGroup) {
        Collection<SecurityGroup> sourceGroups = getEffectiveSecurityGroups(userContext).findAll {
            targetGroup.vpcId == it.vpcId
        }
        String guessedPorts = bestIngressPortsFor(targetGroup)
        sourceGroups.collect { SecurityGroup sourceGroup ->
            buildSecurityGroupOption(sourceGroup, targetGroup, guessedPorts)
        }
    }

    /**
     * Calculates the relationships between one security group (single source of traffic) and most security groups
     * (many potential targets of traffic).
     *
     * @param userContext who, where, why
     * @param sourceGroupName the security group for the instances sending traffic
     * @return list of security group options for display
     */
    List<SecurityGroupOption> getSecurityGroupOptionsForSource(UserContext userContext, SecurityGroup sourceGroup) {
        Collection<SecurityGroup> targetGroups = getEffectiveSecurityGroups(userContext).findAll {
            sourceGroup.vpcId == it.vpcId
        }
        targetGroups.collect { SecurityGroup targetGroup ->
            String guessedPorts = bestIngressPortsFor(targetGroup)
            buildSecurityGroupOption(sourceGroup, targetGroup, guessedPorts)
        }
    }

    private SecurityGroupOption buildSecurityGroupOption(SecurityGroup sourceGroup, SecurityGroup targetGroup,
                                                         String defaultPorts) {
        Collection<IpPermission> ipPermissions = getIngressFrom(targetGroup, sourceGroup)
        String accessiblePorts = permissionsToString(ipPermissions)
        boolean accessible = accessiblePorts ? true : false
        String ports = accessiblePorts ?: defaultPorts
        String groupName = targetGroup.groupName
        new SecurityGroupOption(source: sourceGroup.groupName, target: groupName, allowed: accessible, ports: ports)
    }

    // mutators

    SecurityGroup createSecurityGroup(UserContext userContext, String name, String description, String vpcId = null) {
        Check.notEmpty(name, 'name')
        Check.notEmpty(description, 'description')
        String groupId = null
        taskService.runTask(userContext, "Create Security Group ${name}", { task ->
            def result = awsClient.by(userContext.region).createSecurityGroup(
                    new CreateSecurityGroupRequest(groupName: name, description: description, vpcId: vpcId))
            groupId = result.groupId
        }, Link.to(EntityType.security, name))
        getSecurityGroup(userContext, groupId)
    }

    void removeSecurityGroup(UserContext userContext, String name, String id) {
        taskService.runTask(userContext, "Remove Security Group ${name}", { task ->
            awsClient.by(userContext.region).deleteSecurityGroup(new DeleteSecurityGroupRequest(groupId: id))
        }, Link.to(EntityType.security, name))
        caches.allSecurityGroups.by(userContext.region).remove(name)
    }

    /** High-level permission update for a group pair: given the desired state, make it so. */
    void updateSecurityGroupPermissions(UserContext userContext, SecurityGroup targetGroup, SecurityGroup sourceGroup,
            List<IpPermission> wantPerms) {
        List<IpPermission> havePerms = getIngressFrom(targetGroup, sourceGroup)
        if (!havePerms && !wantPerms) {
            return
        }
        Boolean somethingChanged = false
        havePerms.each { havePerm ->
            if (!wantPerms.any { wp -> wp.fromPort == havePerm.fromPort && wp.toPort == havePerm.toPort } ) {
                revokeSecurityGroupIngress(userContext, targetGroup, sourceGroup, 'tcp',
                        havePerm.fromPort, havePerm.toPort)
                somethingChanged = true
            }
        }
        wantPerms.each { wantPerm ->
            if (!havePerms.any { hp -> hp.fromPort == wantPerm.fromPort && hp.toPort == wantPerm.toPort } ) {
                authorizeSecurityGroupIngress(userContext, targetGroup, sourceGroup, 'tcp',
                        wantPerm.fromPort, wantPerm.toPort)
                somethingChanged = true
            }
        }
        // This method gets called hundreds of times for one user request so don't call Amazon unless necessary.
        if (somethingChanged) {
            getSecurityGroup(userContext, targetGroup.groupId)
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
            portsStr.split(',').each { rangeStr ->
                Matcher m = rangeStr =~ /(-?\d+)(-(-?\d+))?/
                //println "permissionsFromString: ${portStr} => ${m[0]}"
                if (m.matches()) {
                    def rangeParts = m[0]  // 0:all 1:from 2:dashAndTo 3:to
                    String fromPort = rangeParts[1]
                    String toPort = rangeParts[3] ?: fromPort
                    perms += new IpPermission().withFromPort(fromPort.toInteger()).withToPort(toPort.toInteger())
                }
            }
        }
        perms
    }

    /** Returns the ingress permissions from one group to another. Assumes tcp and groups, not cidrs. */
    static Collection<IpPermission> getIngressFrom(SecurityGroup targetGroup, SecurityGroup sourceGroup) {
        targetGroup.ipPermissions.findAll {
            it.userIdGroupPairs.any { it.groupId == sourceGroup.groupId }
        }
    }

    private String bestIngressPortsFor(SecurityGroup targetGroup) {
        Map guess = ['7001' : 1]
        targetGroup.ipPermissions.each {
            if (it.ipProtocol == 'tcp' && it.userIdGroupPairs.size() > 0) {
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

    private void authorizeSecurityGroupIngress(UserContext userContext, SecurityGroup targetgroup, SecurityGroup sourceGroup, String ipProtocol, int fromPort, int toPort) {
        String groupName = targetgroup.groupName
        String sourceGroupName = sourceGroup.groupName
        UserIdGroupPair sourcePair = new UserIdGroupPair().withUserId(accounts[0]).withGroupId(sourceGroup.groupId)
        List<IpPermission> perms = [
                new IpPermission()
                        .withUserIdGroupPairs(sourcePair)
                        .withIpProtocol(ipProtocol).withFromPort(fromPort).withToPort(toPort)
        ]
        taskService.runTask(userContext, "Authorize Security Group Ingress to ${groupName} from ${sourceGroupName} on ${fromPort}-${toPort}", { task ->
            awsClient.by(userContext.region).authorizeSecurityGroupIngress(
                    new AuthorizeSecurityGroupIngressRequest().withGroupId(targetgroup.groupId).withIpPermissions(perms))
        }, Link.to(EntityType.security, groupName))
    }

    private void revokeSecurityGroupIngress(UserContext userContext, SecurityGroup targetgroup, SecurityGroup sourceGroup, String ipProtocol, int fromPort, int toPort) {
        String groupName = targetgroup.groupName
        String sourceGroupName = sourceGroup.groupName
        UserIdGroupPair sourcePair = new UserIdGroupPair().withUserId(accounts[0]).withGroupId(sourceGroup.groupId)
        List<IpPermission> perms = [
                new IpPermission()
                        .withUserIdGroupPairs(sourcePair)
                        .withIpProtocol(ipProtocol).withFromPort(fromPort).withToPort(toPort)
        ]
        taskService.runTask(userContext, "Revoke Security Group Ingress to ${groupName} from ${sourceGroupName} on ${fromPort}-${toPort}", { task ->
            awsClient.by(userContext.region).revokeSecurityGroupIngress(
                    new RevokeSecurityGroupIngressRequest().withGroupId(targetgroup.groupId).withIpPermissions(perms))
        }, Link.to(EntityType.security, groupName))
    }

    // TODO: Delete this method after rewriting AwsResultsRetrieverSpec unit test to use some other use case
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

    /**
     * Gets all instances that are currently active and counted against reservation usage.
     *
     * @param userContext who, where, why
     * @return Collection < Instance > active instances
     */
    Collection<Instance> getActiveInstances(UserContext userContext) {
        getInstances(userContext).findAll { it.state.name in ACTIVE_INSTANCE_STATES }
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

    /**
     * Finds all the instances that were launched with the specified security group.
     *
     * @param userContext who, where, why
     * @param securityGroup the security group for which to find relevant instances
     * @return all the instances associated with the specified security group
     */
    Collection<Instance> getInstancesWithSecurityGroup(UserContext userContext, SecurityGroup securityGroup) {
        getInstances(userContext).findAll {
            String name = securityGroup.groupName
            String id = securityGroup.groupId
            (name && (name in it.securityGroups*.groupName)) || (id && (id in it.securityGroups*.groupId))
        }
    }

    Instance getInstance(UserContext userContext, String instanceId, From from = From.AWS) {
        if (from == From.CACHE) {
            return caches.allInstances.by(userContext.region).get(instanceId)
        }
        List<Instance> instances = getInstanceReservation(userContext, instanceId)?.instances
        instances ? instances[0] : null
    }

    Multiset<AppVersion> getCountedAppVersions(UserContext userContext) {
        Map<String, Image> imageIdsToImages = caches.allImages.by(userContext.region).unmodifiable()
        getCountedAppVersionsForInstancesAndImages(getInstances(userContext), imageIdsToImages)
    }

    private Multiset<AppVersion> getCountedAppVersionsForInstancesAndImages(Collection<Instance> instances,
            Map<String, Image> images) {
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
        if (!instanceId) { return null }
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

    Boolean checkHostHealth(String url) {
        if (configService.isOnline()) {
            Integer responseCode = restClientService.getRepeatedResponseCode(url)
            return restClientService.checkOkayResponseCode(responseCode)
        }
        true
    }

    /**
     * Test health of instances in parallel. One failing health check stops all checks and returns false.
     *
     * @param healthCheckUrls of instances
     * @return indicates if all instances are healthy
     */
    Boolean checkHostsHealth(Collection<String> healthCheckUrls) {
        GParsExecutorsPool.withExistingPool(threadScheduler.scheduler) {
            String unhealthyHostUrl = healthCheckUrls.findAnyParallel { !checkHostHealth(it) }
            !unhealthyHostUrl
        }
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
        String output = result.getOutput()
        output ? new String(Base64.decodeBase64(output.bytes)) : null
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

    /**
     * For a given instance type such as m1.large, this method calculates how many active instance reservations are
     * currently available for use in each availability zone.
     *
     * @param userContext who, where, why
     * @param instanceType the choice of instance type to get reservation counts for, such as m1.large
     * @return List < ZoneAvailability > availability zones with available reservation counts, or empty list if there
     *          are no reservations in any zones for the specified instance type
     */
    List<ZoneAvailability> getZoneAvailabilities(UserContext userContext, String instanceType) {
        Collection<ReservedInstances> reservedInstanceGroups = getReservedInstances(userContext)
        Map<String, Integer> zonesToActiveReservationCounts = [:]
        for (ReservedInstances reservedInstanceGroup in reservedInstanceGroups) {
            if (reservedInstanceGroup.state == 'active' && reservedInstanceGroup.instanceType == instanceType) {
                String zone = reservedInstanceGroup.availabilityZone
                int runningCount = zonesToActiveReservationCounts[zone] ?: 0
                zonesToActiveReservationCounts[zone] = runningCount + reservedInstanceGroup.instanceCount
            }
        }
        Collection<Instance> activeInstances = getActiveInstances(userContext)
        Multiset<String> zoneInstanceCounts = HashMultiset.create()
        for (Instance instance in activeInstances) {
            if (instance.instanceType == instanceType) {
                zoneInstanceCounts.add(instance.placement.availabilityZone)
            }
        }
        Set<String> zonesWithInstances = zoneInstanceCounts.elementSet()
        Set<String> zonesWithReservations = zonesToActiveReservationCounts.keySet()
        List<String> zoneNames = (zonesWithInstances + zonesWithReservations).sort()
        List<ZoneAvailability> zoneAvailabilities = zoneNames.collect { String zone ->
            int instanceCount = zoneInstanceCounts.count(zone)
            Integer reservationCount = zonesToActiveReservationCounts[zone] ?: 0
            new ZoneAvailability(zoneName: zone, totalReservations: reservationCount, usedReservations: instanceCount)
        }
        zoneAvailabilities.any { it.totalReservations } ? zoneAvailabilities : []
    }

    Collection<ReservedInstances> getReservedInstances(UserContext userContext) {
        caches.allReservedInstancesGroups.by(userContext.region).list()
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
        List<String> owners = configService.publicResourceAccounts + configService.awsAccounts
        DescribeSnapshotsRequest request = new DescribeSnapshotsRequest().withOwnerIds(owners)
        awsClient.by(region).describeSnapshots(request).snapshots
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
                    { Exception e -> e instanceof AmazonServiceException && e.errorCode == 'InvalidSnapshot.InUse' },
                    250
            )
            caches.allSnapshots.by(userContext.region).remove(snapshotId)
        }
        taskService.runTask(userContext, msg, work, Link.to(EntityType.snapshot, snapshotId), existingTask)
    }

    /**
     * Determines the zones that should be preselected.
     *
     * @param availabilityZones pool of potential zones
     * @param selectedZoneNames zones names that have previously been selected
     * @param group an optional ASG with zone selections
     * @return preselected zone names
     */
    Collection<String> preselectedZoneNames(Collection<AvailabilityZone> availabilityZones,
            Collection<String> selectedZoneNames, AutoScalingGroupData group = null) {
        Collection<AvailabilityZone> preselectedAvailabilityZones = availabilityZones.findAll {
            it.shouldBePreselected(selectedZoneNames, group)
        }
        preselectedAvailabilityZones*.zoneName
    }
}
