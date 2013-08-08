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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.SecurityGroup
import com.netflix.asgard.model.InstancePriceType
import com.netflix.asgard.model.Subnets
import com.netflix.asgard.model.ZoneAvailability
import com.netflix.asgard.push.GroupActivateOperation
import com.netflix.asgard.push.GroupCreateOperation
import com.netflix.asgard.push.GroupCreateOptions
import com.netflix.asgard.push.GroupDeactivateOperation
import com.netflix.asgard.push.GroupDeleteOperation
import com.netflix.asgard.push.GroupResizeOperation
import com.netflix.asgard.push.InitialTraffic
import com.netflix.asgard.push.RollingPushOperation
import com.netflix.asgard.push.RollingPushOptions
import java.rmi.NoSuchObjectException
import java.util.concurrent.ConcurrentLinkedQueue

class PushService {

    static transactional = false

    def awsAutoScalingService
    def awsEc2Service
    def configService
    def imageService
    def instanceTypeService
    def restClientService
    def grailsApplication

    private Queue<RollingPushOperation> rollingPushes = new ConcurrentLinkedQueue<RollingPushOperation>()
    private Queue<GroupCreateOperation> groupCreates = new ConcurrentLinkedQueue<GroupCreateOperation>()
    private Queue<GroupDeleteOperation> groupDeletes = new ConcurrentLinkedQueue<GroupDeleteOperation>()
    private Queue<GroupResizeOperation> groupResizes = new ConcurrentLinkedQueue<GroupResizeOperation>()
    private Queue<GroupActivateOperation> groupActivates = new ConcurrentLinkedQueue<GroupActivateOperation>()
    private Queue<GroupDeactivateOperation> groupDeactivates = new ConcurrentLinkedQueue<GroupDeactivateOperation>()

    RollingPushOperation startRollingPush(RollingPushOptions pushOptions) {
        RollingPushOperation operation = new RollingPushOperation(pushOptions)
        operation.start()
        rollingPushes << operation
        operation
    }

    GroupCreateOperation startGroupCreate(GroupCreateOptions options) {
        GroupCreateOperation operation = new GroupCreateOperation(options)
        operation.start()
        groupCreates << operation
        operation
    }

    GroupDeleteOperation startGroupDelete(UserContext userContext, AutoScalingGroup group) {
        GroupDeleteOperation operation = new GroupDeleteOperation(userContext: userContext, autoScalingGroup: group)
        operation.start()
        groupDeletes << operation
        operation
    }

    GroupResizeOperation startGroupResize(UserContext userContext, String groupName, Integer min, Integer max,
            Integer batchSize) {
        GroupResizeOperation operation = new GroupResizeOperation(userContext: userContext,
                autoScalingGroupName: groupName, eventualMin: min, newMin: min, newMax: max, batchSize: batchSize,
                initialTraffic: InitialTraffic.ALLOWED
        )
        operation.start()
        groupResizes << operation
        operation
    }

    GroupActivateOperation startGroupActivate(UserContext userContext, String groupName) {
        GroupActivateOperation operation = new GroupActivateOperation(userContext: userContext,
                autoScalingGroupName: groupName)
        operation.start()
        groupActivates << operation
        operation
    }

    GroupDeactivateOperation startGroupDeactivate(UserContext userContext, String groupName) {
        GroupDeactivateOperation operation = new GroupDeactivateOperation(userContext: userContext,
                autoScalingGroupName: groupName)
        operation.start()
        groupDeactivates << operation
        operation
    }

    /** This needs to work multi-region. See http://jira/browse/ENGTOOLS-963 */
    void addAccountsForImage(UserContext userContext, String imageId, Task task) {
        List<String> addedAccounts = awsEc2Service.authorizeSecondaryImageLaunchers(userContext, imageId, task)
        if (addedAccounts.size()) {
            List<String> addedNames = addedAccounts.collect { grailsApplication.config.grails.awsAccountNames[it] }
            task.log "Image $imageId can now be used in $addedNames account${addedNames.size() == 1 ? '' : 's'}"
            String promotionTargetServer = grailsApplication.config.promote?.targetServer ?: null
            if (promotionTargetServer) {
                // Update the cache of the promotion target server (such as production)
                Integer responseCode = restClientService.getResponseCode("${promotionTargetServer}/")
                if (responseCode == 200) {
                    String url = "${promotionTargetServer}/${userContext.region.code}/image/show/${imageId}"
                    restClientService.getAsText(url, 3000)

                    // Try to promote new image tags asynchronously, ignoring any errors.
                    imageService.replicateImageTags()
                }
            }
        }
    }

    /** Run processing that is common to multiple edit screens for preparing a push. */
    Map<String, Object> prepareEdit(UserContext userContext, String groupName, boolean showAllImages, String actionName,
                    Collection<String> selectedSecurityGroups) {
        String name = groupName

        // name is specific AS group, appName will be derived, aborting if not possible.
        String appName = Relationships.appNameFromGroupName(name)
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(userContext, name)
        if (!group) {
            throw new NoSuchObjectException("Auto scaling group '${name}' not found")
        }
        Integer relaunchCount = group.instances.size()
        LaunchConfiguration lc = awsAutoScalingService.getLaunchConfiguration(userContext,
                group.launchConfigurationName)
        String instanceType = lc.instanceType
        List<ZoneAvailability> zoneAvailabilities = awsEc2Service.getZoneAvailabilities(userContext, instanceType)
        Collection<Image> images = awsEc2Service.getAccountImages(userContext)
        Integer fullCount = images.size()
        Image currentImage = awsEc2Service.getImage(userContext, lc.imageId)
        if (currentImage && !showAllImages) {
            images = awsEc2Service.getImagesForPackage(userContext, currentImage.packageName)
        }
        Boolean imageListIsShort = images.size() < fullCount
        Subnets subnets = awsEc2Service.getSubnets(userContext)
        List<SecurityGroup> effectiveSecurityGroups = awsEc2Service.getEffectiveSecurityGroups(userContext)
        List<String> subnetIds = Relationships.subnetIdsFromVpcZoneIdentifier(group.VPCZoneIdentifier)
        String vpcId = subnets.coerceLoneOrNoneFromIds(subnetIds)?.vpcId
        Map<String, String> purposeToVpcId = subnets.mapPurposeToVpcId()
        String pricing = lc.spotPrice ? InstancePriceType.SPOT.name() : InstancePriceType.ON_DEMAND.name()
        Map<String, Object> result = [
                appName: appName,
                name: name,
                cluster: Relationships.clusterFromGroupName(name),
                variables: Relationships.parts(name),
                actionName: actionName,
                allTerminationPolicies: awsAutoScalingService.terminationPolicyTypes,
                terminationPolicy: group.terminationPolicies[0],

                // launch config values: list & selection pairs
                images: images.sort { it.imageLocation.toLowerCase() },
                image: lc.imageId,
                imageListIsShort: imageListIsShort,
                instanceTypes: instanceTypeService.getInstanceTypes(userContext),
                instanceType: instanceType,
                zoneAvailabilities: zoneAvailabilities,
                vpcId: vpcId,
                purposeToVpcId: purposeToVpcId,
                securityGroupsGroupedByVpcId: effectiveSecurityGroups.groupBy { it.vpcId },
                selectedSecurityGroups: selectedSecurityGroups ?: lc.securityGroups,
                defKey: lc.keyName,
                keys: awsEc2Service.getKeys(userContext).sort { it.keyName.toLowerCase() },
                iamInstanceProfile: lc.iamInstanceProfile ?: configService.defaultIamRole,
                // Rolling push process options
                relaunchCount: relaunchCount,
                concurrentRelaunches: 1,
                checkHealth: configService.doesRegionalDiscoveryExist(userContext.region),
                afterBootWait: 30,
                rudeShutdown: false,
                pricing: pricing,
                spotUrl: configService.spotUrl
        ]
        result
    }
}
