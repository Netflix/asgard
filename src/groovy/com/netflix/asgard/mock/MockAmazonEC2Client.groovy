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
package com.netflix.asgard.mock

import com.amazonaws.AmazonServiceException
import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.ClientConfiguration
import com.amazonaws.ResponseMetadata
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.ActivateLicenseRequest
import com.amazonaws.services.ec2.model.AllocateAddressRequest
import com.amazonaws.services.ec2.model.AllocateAddressResult
import com.amazonaws.services.ec2.model.AssociateDhcpOptionsRequest
import com.amazonaws.services.ec2.model.AttachVolumeRequest
import com.amazonaws.services.ec2.model.AttachVolumeResult
import com.amazonaws.services.ec2.model.AttachVpnGatewayRequest
import com.amazonaws.services.ec2.model.AttachVpnGatewayResult
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest
import com.amazonaws.services.ec2.model.AvailabilityZone
import com.amazonaws.services.ec2.model.BundleInstanceRequest
import com.amazonaws.services.ec2.model.BundleInstanceResult
import com.amazonaws.services.ec2.model.CancelBundleTaskRequest
import com.amazonaws.services.ec2.model.CancelBundleTaskResult
import com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsRequest
import com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsResult
import com.amazonaws.services.ec2.model.ConfirmProductInstanceRequest
import com.amazonaws.services.ec2.model.ConfirmProductInstanceResult
import com.amazonaws.services.ec2.model.CreateCustomerGatewayRequest
import com.amazonaws.services.ec2.model.CreateCustomerGatewayResult
import com.amazonaws.services.ec2.model.CreateDhcpOptionsRequest
import com.amazonaws.services.ec2.model.CreateDhcpOptionsResult
import com.amazonaws.services.ec2.model.CreateImageRequest
import com.amazonaws.services.ec2.model.CreateImageResult
import com.amazonaws.services.ec2.model.CreateKeyPairRequest
import com.amazonaws.services.ec2.model.CreateKeyPairResult
import com.amazonaws.services.ec2.model.CreatePlacementGroupRequest
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult
import com.amazonaws.services.ec2.model.CreateSnapshotRequest
import com.amazonaws.services.ec2.model.CreateSnapshotResult
import com.amazonaws.services.ec2.model.CreateSpotDatafeedSubscriptionRequest
import com.amazonaws.services.ec2.model.CreateSpotDatafeedSubscriptionResult
import com.amazonaws.services.ec2.model.CreateSubnetRequest
import com.amazonaws.services.ec2.model.CreateSubnetResult
import com.amazonaws.services.ec2.model.CreateTagsRequest
import com.amazonaws.services.ec2.model.CreateVolumeRequest
import com.amazonaws.services.ec2.model.CreateVolumeResult
import com.amazonaws.services.ec2.model.CreateVpcRequest
import com.amazonaws.services.ec2.model.CreateVpcResult
import com.amazonaws.services.ec2.model.CreateVpnConnectionRequest
import com.amazonaws.services.ec2.model.CreateVpnConnectionResult
import com.amazonaws.services.ec2.model.CreateVpnGatewayRequest
import com.amazonaws.services.ec2.model.CreateVpnGatewayResult
import com.amazonaws.services.ec2.model.DeactivateLicenseRequest
import com.amazonaws.services.ec2.model.DeleteCustomerGatewayRequest
import com.amazonaws.services.ec2.model.DeleteDhcpOptionsRequest
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest
import com.amazonaws.services.ec2.model.DeletePlacementGroupRequest
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest
import com.amazonaws.services.ec2.model.DeleteSnapshotRequest
import com.amazonaws.services.ec2.model.DeleteSpotDatafeedSubscriptionRequest
import com.amazonaws.services.ec2.model.DeleteSubnetRequest
import com.amazonaws.services.ec2.model.DeleteTagsRequest
import com.amazonaws.services.ec2.model.DeleteVolumeRequest
import com.amazonaws.services.ec2.model.DeleteVpcRequest
import com.amazonaws.services.ec2.model.DeleteVpnConnectionRequest
import com.amazonaws.services.ec2.model.DeleteVpnGatewayRequest
import com.amazonaws.services.ec2.model.DeregisterImageRequest
import com.amazonaws.services.ec2.model.DescribeAddressesRequest
import com.amazonaws.services.ec2.model.DescribeAddressesResult
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesRequest
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult
import com.amazonaws.services.ec2.model.DescribeBundleTasksRequest
import com.amazonaws.services.ec2.model.DescribeBundleTasksResult
import com.amazonaws.services.ec2.model.DescribeCustomerGatewaysRequest
import com.amazonaws.services.ec2.model.DescribeCustomerGatewaysResult
import com.amazonaws.services.ec2.model.DescribeDhcpOptionsRequest
import com.amazonaws.services.ec2.model.DescribeDhcpOptionsResult
import com.amazonaws.services.ec2.model.DescribeImageAttributeRequest
import com.amazonaws.services.ec2.model.DescribeImageAttributeResult
import com.amazonaws.services.ec2.model.DescribeImagesRequest
import com.amazonaws.services.ec2.model.DescribeImagesResult
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeRequest
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeResult
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.model.DescribeInstancesResult
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult
import com.amazonaws.services.ec2.model.DescribeLicensesRequest
import com.amazonaws.services.ec2.model.DescribeLicensesResult
import com.amazonaws.services.ec2.model.DescribePlacementGroupsRequest
import com.amazonaws.services.ec2.model.DescribePlacementGroupsResult
import com.amazonaws.services.ec2.model.DescribeRegionsRequest
import com.amazonaws.services.ec2.model.DescribeRegionsResult
import com.amazonaws.services.ec2.model.DescribeReservedInstancesOfferingsRequest
import com.amazonaws.services.ec2.model.DescribeReservedInstancesOfferingsResult
import com.amazonaws.services.ec2.model.DescribeReservedInstancesRequest
import com.amazonaws.services.ec2.model.DescribeReservedInstancesResult
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult
import com.amazonaws.services.ec2.model.DescribeSnapshotAttributeRequest
import com.amazonaws.services.ec2.model.DescribeSnapshotAttributeResult
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult
import com.amazonaws.services.ec2.model.DescribeSpotDatafeedSubscriptionRequest
import com.amazonaws.services.ec2.model.DescribeSpotDatafeedSubscriptionResult
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryRequest
import com.amazonaws.services.ec2.model.DescribeSpotPriceHistoryResult
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest
import com.amazonaws.services.ec2.model.DescribeSubnetsResult
import com.amazonaws.services.ec2.model.DescribeTagsRequest
import com.amazonaws.services.ec2.model.DescribeTagsResult
import com.amazonaws.services.ec2.model.DescribeVolumesRequest
import com.amazonaws.services.ec2.model.DescribeVolumesResult
import com.amazonaws.services.ec2.model.DescribeVpcsRequest
import com.amazonaws.services.ec2.model.DescribeVpcsResult
import com.amazonaws.services.ec2.model.DescribeVpnConnectionsRequest
import com.amazonaws.services.ec2.model.DescribeVpnConnectionsResult
import com.amazonaws.services.ec2.model.DescribeVpnGatewaysRequest
import com.amazonaws.services.ec2.model.DescribeVpnGatewaysResult
import com.amazonaws.services.ec2.model.DetachVolumeRequest
import com.amazonaws.services.ec2.model.DetachVolumeResult
import com.amazonaws.services.ec2.model.DetachVpnGatewayRequest
import com.amazonaws.services.ec2.model.GetConsoleOutputRequest
import com.amazonaws.services.ec2.model.GetConsoleOutputResult
import com.amazonaws.services.ec2.model.GetPasswordDataRequest
import com.amazonaws.services.ec2.model.GetPasswordDataResult
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.ImageAttribute
import com.amazonaws.services.ec2.model.ImportKeyPairRequest
import com.amazonaws.services.ec2.model.ImportKeyPairResult
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.InstanceAttribute
import com.amazonaws.services.ec2.model.InstanceState
import com.amazonaws.services.ec2.model.IpPermission
import com.amazonaws.services.ec2.model.KeyPairInfo
import com.amazonaws.services.ec2.model.LaunchPermission
import com.amazonaws.services.ec2.model.LaunchSpecification
import com.amazonaws.services.ec2.model.ModifyImageAttributeRequest
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeRequest
import com.amazonaws.services.ec2.model.ModifySnapshotAttributeRequest
import com.amazonaws.services.ec2.model.MonitorInstancesRequest
import com.amazonaws.services.ec2.model.MonitorInstancesResult
import com.amazonaws.services.ec2.model.Monitoring
import com.amazonaws.services.ec2.model.Placement
import com.amazonaws.services.ec2.model.PurchaseReservedInstancesOfferingRequest
import com.amazonaws.services.ec2.model.PurchaseReservedInstancesOfferingResult
import com.amazonaws.services.ec2.model.RebootInstancesRequest
import com.amazonaws.services.ec2.model.RegisterImageRequest
import com.amazonaws.services.ec2.model.RegisterImageResult
import com.amazonaws.services.ec2.model.ReleaseAddressRequest
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult
import com.amazonaws.services.ec2.model.Reservation
import com.amazonaws.services.ec2.model.ReservedInstancesOffering
import com.amazonaws.services.ec2.model.ResetImageAttributeRequest
import com.amazonaws.services.ec2.model.ResetInstanceAttributeRequest
import com.amazonaws.services.ec2.model.ResetSnapshotAttributeRequest
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest
import com.amazonaws.services.ec2.model.RunInstancesRequest
import com.amazonaws.services.ec2.model.RunInstancesResult
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.ec2.model.Snapshot
import com.amazonaws.services.ec2.model.SpotInstanceRequest
import com.amazonaws.services.ec2.model.SpotPlacement
import com.amazonaws.services.ec2.model.StartInstancesRequest
import com.amazonaws.services.ec2.model.StartInstancesResult
import com.amazonaws.services.ec2.model.StopInstancesRequest
import com.amazonaws.services.ec2.model.StopInstancesResult
import com.amazonaws.services.ec2.model.Tag
import com.amazonaws.services.ec2.model.TerminateInstancesRequest
import com.amazonaws.services.ec2.model.TerminateInstancesResult
import com.amazonaws.services.ec2.model.UnmonitorInstancesRequest
import com.amazonaws.services.ec2.model.UnmonitorInstancesResult
import com.amazonaws.services.ec2.model.UserIdGroupPair
import com.amazonaws.services.ec2.model.Volume
import com.amazonaws.services.ec2.model.VolumeAttachment
import org.codehaus.groovy.grails.web.json.JSONArray
import org.joda.time.format.ISODateTimeFormat

class MockAmazonEC2Client extends AmazonEC2Client {

    private Collection<Image> mockImages
    private Collection<Instance> mockInstances
    private Collection<SpotInstanceRequest> mockSpotInstanceRequests
    private Collection<SecurityGroup> mockSecurityGroups
    private Collection<Volume> mockVolumes
    private Collection<Snapshot> mockSnapshots

    private List<Image> loadMockImages() {
        JSONArray jsonArray = Mocks.parseJsonString(MockImages.DATA)
        jsonArray.collect {

            new Image().withArchitecture(it.architecture).
                    withDescription(Mocks.jsonNullable(it.description)).withImageId(it.imageId).
                    withImageLocation(it.imageLocation).withImageType(it.imageType).
                    withKernelId(Mocks.jsonNullable(it.kernelId)).
                    withName(Mocks.jsonNullable(it.name)).withOwnerId(it.ownerId).
                    withRamdiskId(Mocks.jsonNullable(it.ramdiskId)).
                    withRootDeviceName(Mocks.jsonNullable(it.rootDeviceName)).withRootDeviceType(it.rootDeviceType).
                    withState(it.state as String).
                    withTags(it.tags.collect { new Tag(it.key, it.value) }).
                    withVirtualizationType(it.virtualizationType as String)
        }
    }

    private List<Instance> loadMockInstances() {
        JSONArray jsonArray = Mocks.parseJsonString(MockInstances.DATA)
        jsonArray.findAll { Mocks.jsonNullable(it.ec2Instance) }.
                collect { it.ec2Instance }.collect {
            new Instance().withArchitecture(Mocks.jsonNullable(it.architecture)).
                    withClientToken(it.clientToken).
                    withInstanceId(Mocks.jsonNullable(it.instanceId)).
                    withImageId(Mocks.jsonNullable(it.imageId)).
                    withInstanceLifecycle(Mocks.jsonNullable(it.instanceLifecycle)).
                    withInstanceType(Mocks.jsonNullable(it.instanceType)).
                    withKernelId(Mocks.jsonNullable(it.kernelId)).
                    withKeyName(it.keyName).
                    withLaunchTime(Mocks.jsonNullable(it.launchTime) ?
                            ISODateTimeFormat.dateTimeParser().parseDateTime(it.launchTime).toDate() : null).
                    withMonitoring(new Monitoring().withState(it.monitoring.state)).
                    withPlacement(new Placement().withAvailabilityZone(it.placement.availabilityZone)).
                    withPrivateDnsName(it.privateDnsName).
                    withPrivateIpAddress(Mocks.jsonNullable(it.privateIpAddress)).
                    withPublicDnsName(it.publicDnsName).
                    withPublicIpAddress(it.publicIpAddress).
                    withRamdiskId(Mocks.jsonNullable(it.ramdiskId)).
                    withRootDeviceName(Mocks.jsonNullable(it.rootDeviceName)).
                    withRootDeviceType(it.rootDeviceType).
                    withSpotInstanceRequestId(Mocks.jsonNullable(it.spotInstanceRequestId)).
                    withState(new InstanceState().withCode(it.state.code).withName(it.state.name as String)).
                    withStateTransitionReason(it.stateTransitionReason).
                    withSubnetId(Mocks.jsonNullable(it.subnetId)).
                    withVirtualizationType(it.virtualizationType as String).
                    withVpcId(Mocks.jsonNullable(it.vpcId))
        }
    }

    private List<SpotInstanceRequest> loadMockSpotInstanceRequests() {
        [
                new SpotInstanceRequest().
                        withSpotInstanceRequestId('sir-deadbeef').
                        withInstanceId('i-feedbead').
                        withLaunchSpecification(new LaunchSpecification().
                                withImageId('ami-3178b958').
                                withKeyName('nf-test-keypair-a').
                                withInstanceType('m1.large').
                                withSecurityGroups('helloworld')).
                        withState('open'),
                new SpotInstanceRequest().withSpotInstanceRequestId('sir-12345678').withInstanceId('i-87654321').
                        withLaunchSpecification(new LaunchSpecification().
                                withImageId('ami-3178b958').
                                withKeyName('nf-test-keypair-a').
                                withInstanceType('m1.large').
                                withSecurityGroups('helloworld').
                                withPlacement(new SpotPlacement('us-east-1a'))).
                        withState('active')
        ]
    }

    private List<SecurityGroup> loadMockSecurityGroups() {
        JSONArray jsonArray = Mocks.parseJsonString(MockSecurityGroups.DATA)
        jsonArray.collect {
            new SecurityGroup().withDescription(it.description).
                    withGroupId(it.groupId).
                    withGroupName(it.groupName).
                    withIpPermissions(it.ipPermissions.collect {
                        new IpPermission().withFromPort(it.fromPort).withIpProtocol(it.ipProtocol).
                                withIpRanges(it.ipRanges as List).withToPort(it.toPort).
                                withUserIdGroupPairs(it.userIdGroupPairs.collect {
                                    new UserIdGroupPair().withGroupName(it.groupName).withGroupId(it.groupId).
                                            withUserId(it.userId)
                                })
                    }).
                    withOwnerId(it.ownerId).
                    withTags(it.tags.collect {
                        new Tag().withKey(it.key).withValue(it.value)
                    }).
                    withVpcId(Mocks.jsonNullable(it.vpcId))
        }
    }

    private List<Volume> loadMockVolumes() {
        JSONArray jsonArray = Mocks.parseJsonString(MockVolumes.DATA)
        jsonArray.collect {
            List<VolumeAttachment> volumeAttachments = it.attachments.collect {
                new VolumeAttachment().withAttachTime(Mocks.parseJsonDate(it.attachTime)).withInstanceId(it.instanceId).
                        withState(it.state as String).withVolumeId(it.volumeId)
            }
            new Volume().withAttachments(volumeAttachments).withAvailabilityZone(it.availabilityZone).
                    withCreateTime(Mocks.parseJsonDate(it.createTime)).withSize(it.size as Integer).
                    withSnapshotId(it.snapshotId).withState(it.state as String).withVolumeId(it.volumeId).
                    withTags(it.tags.collect { new Tag().withKey(it.key).withValue(it.value) })
        }
    }

    private List<Snapshot> loadMockSnapshots() {
        JSONArray jsonArray = Mocks.parseJsonString(MockSnapshots.DATA)
        jsonArray.collect {
            new Snapshot().withDescription(it.description).
                    withOwnerAlias(Mocks.jsonNullable(it.ownerAlias)).withOwnerId(it.ownerId).
                    withProgress(it.progress).withSnapshotId(it.snapshotId).
                    withStartTime(ISODateTimeFormat.dateTimeParser().parseDateTime(it.startTime).toDate()).
                    withState(it.state as String).withTags(it.tags.collect {
                        new Tag().withKey(it.key).withValue(it.value)
                    }).withVolumeId(it.volumeId).withVolumeSize(it.volumeSize)
        }
    }

    MockAmazonEC2Client(BasicAWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        super(awsCredentials as BasicAWSCredentials, clientConfiguration)
        mockImages = loadMockImages()
        mockInstances = loadMockInstances()
        mockSpotInstanceRequests = loadMockSpotInstanceRequests()
        mockSecurityGroups = loadMockSecurityGroups()
        mockVolumes = loadMockVolumes()
        mockSnapshots = loadMockSnapshots()
    }

    void setEndpoint(String s) { }

    void rebootInstances(RebootInstancesRequest rebootInstancesRequest) { }

    DescribePlacementGroupsResult describePlacementGroups(
            DescribePlacementGroupsRequest describePlacementGroupsRequest) { null }

    RunInstancesResult runInstances(RunInstancesRequest runInstancesRequest) { null }

    DescribeReservedInstancesResult describeReservedInstances(
            DescribeReservedInstancesRequest describeReservedInstancesRequest) { null }

    DescribeSubnetsResult describeSubnets(DescribeSubnetsRequest describeSubnetsRequest) { new DescribeSubnetsResult() }

    DescribeAvailabilityZonesResult describeAvailabilityZones(
            DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest) {
        new DescribeAvailabilityZonesResult().withAvailabilityZones(
                ['us-east-1a', 'us-east-1b', 'us-east-1c', 'us-east-1d'].collect {
                    new AvailabilityZone().withZoneName(it)
                }
        )
    }

    DetachVolumeResult detachVolume(DetachVolumeRequest detachVolumeRequest) { null }

    void deleteKeyPair(DeleteKeyPairRequest deleteKeyPairRequest) { }

    DescribeInstancesResult describeInstances(DescribeInstancesRequest describeInstancesRequest) {

        if (!describeInstancesRequest.instanceIds) {
            return new DescribeInstancesResult().withReservations(
                    mockInstances.collect { new Reservation().withInstances(it) })
        }

        if (describeInstancesRequest.instanceIds.size() == 1) {
            String instanceId = describeInstancesRequest.instanceIds[0]
            List<String> knownInstanceIds = mockInstances*.instanceId
            if (knownInstanceIds.contains(instanceId)) {
                return new DescribeInstancesResult().withReservations(
                        new Reservation().withInstances(mockInstances.find { it.instanceId == instanceId }))
            } else {
                throw new AmazonServiceException("Status Code: 400, AWS Request ID: 123unittest, " +
                        "AWS Error Code: InvalidInstanceID.NotFound, AWS Error Message: " +
                        "The Instance ID '${instanceId}' does not exist")
            }
        }

        return new DescribeInstancesResult()
    }

    DescribeImagesResult describeImages(DescribeImagesRequest describeImagesRequest) {

        if (!describeImagesRequest.owners.isEmpty()) {
            return new DescribeImagesResult().withImages(mockImages)
        }

        if (describeImagesRequest.imageIds.size() == 1) {
            String imageId = describeImagesRequest.imageIds[0]
            List<String> knownImageIds = mockImages*.imageId
            if (knownImageIds.contains(imageId)) {
                return new DescribeImagesResult().withImages(mockImages.find { it.imageId == imageId })
            } else {
                throw new AmazonServiceException("Status Code: 400, AWS Request ID: 123unittest, " +
                    "AWS Error Code: InvalidAMIID.NotFound, AWS Error Message: The AMI ID '${imageId}' does not exist")
            }
        }
        new DescribeImagesResult()
    }

    StartInstancesResult startInstances(StartInstancesRequest startInstancesRequest) { null }

    UnmonitorInstancesResult unmonitorInstances(UnmonitorInstancesRequest unmonitorInstancesRequest) { null }

    AttachVpnGatewayResult attachVpnGateway(AttachVpnGatewayRequest attachVpnGatewayRequest) { null }

    void modifyInstanceAttribute(ModifyInstanceAttributeRequest modifyInstanceAttributeRequest) { }

    void deleteDhcpOptions(DeleteDhcpOptionsRequest deleteDhcpOptionsRequest) { }

    void deleteSecurityGroup(DeleteSecurityGroupRequest deleteSecurityGroupRequest) { }

    CreateImageResult createImage(CreateImageRequest createImageRequest) { null }

    void authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest) { }

    CreateSecurityGroupResult createSecurityGroup(CreateSecurityGroupRequest createSecurityGroupRequest) {
        String name = createSecurityGroupRequest.getGroupName()
        String description = createSecurityGroupRequest.getDescription()
        String vpcId = createSecurityGroupRequest.getVpcId()
        SecurityGroup securityGroup = new SecurityGroup(groupName: name, description: description, vpcId: vpcId)
        mockSecurityGroups.add(securityGroup)
        new CreateSecurityGroupResult().withGroupId(name)
    }

    DescribeSpotInstanceRequestsResult describeSpotInstanceRequests(DescribeSpotInstanceRequestsRequest request) {
        List<String> spotInstanceRequestIds = request.spotInstanceRequestIds
        if (spotInstanceRequestIds) {
            return new DescribeSpotInstanceRequestsResult().withSpotInstanceRequests(
                    mockSpotInstanceRequests.findAll { it.spotInstanceRequestId in spotInstanceRequestIds } )
        }
        new DescribeSpotInstanceRequestsResult().withSpotInstanceRequests(mockSpotInstanceRequests)
    }

    void associateDhcpOptions(AssociateDhcpOptionsRequest associateDhcpOptionsRequest) { }

    GetPasswordDataResult getPasswordData(GetPasswordDataRequest getPasswordDataRequest) { null }

    CreateVpcResult createVpc(CreateVpcRequest createVpcRequest) { null }

    StopInstancesResult stopInstances(StopInstancesRequest stopInstancesRequest) { null }

    DescribeCustomerGatewaysResult describeCustomerGateways(
            DescribeCustomerGatewaysRequest describeCustomerGatewaysRequest) { null }

    ImportKeyPairResult importKeyPair(ImportKeyPairRequest importKeyPairRequest) { null }

    DescribeSpotPriceHistoryResult describeSpotPriceHistory(
            DescribeSpotPriceHistoryRequest describeSpotPriceHistoryRequest) { null }

    DescribeRegionsResult describeRegions(DescribeRegionsRequest describeRegionsRequest) { null }

    CreateDhcpOptionsResult createDhcpOptions(CreateDhcpOptionsRequest createDhcpOptionsRequest) { null }

    void resetSnapshotAttribute(ResetSnapshotAttributeRequest resetSnapshotAttributeRequest) { }

    DescribeSecurityGroupsResult describeSecurityGroups(DescribeSecurityGroupsRequest describeSecurityGroupsRequest) {

        List<String> requestedNames = describeSecurityGroupsRequest.groupNames

        Collection<SecurityGroup> securityGroups = mockSecurityGroups
        if (requestedNames.size() >= 1) {
            Boolean everyRequestedNameFound = requestedNames.every { String requestedName ->
                mockSecurityGroups.find { it.groupName == requestedName }
            }
            if (!everyRequestedNameFound) {
                throw Mocks.makeAmazonServiceException(null, 400, 'SecurityGroupNotFound', '123unittest')
            }
            securityGroups = requestedNames.collect { String requestedName ->
                mockSecurityGroups.find { it.groupName == requestedName }
            }
        }
        return new DescribeSecurityGroupsResult().withSecurityGroups(securityGroups)
    }

    RequestSpotInstancesResult requestSpotInstances(RequestSpotInstancesRequest requestSpotInstancesRequest) { null }

    void createTags(CreateTagsRequest createTagsRequest) { }

    void detachVpnGateway(DetachVpnGatewayRequest detachVpnGatewayRequest) { }

    void deregisterImage(DeregisterImageRequest deregisterImageRequest) { }

    DescribeSpotDatafeedSubscriptionResult describeSpotDatafeedSubscription(
            DescribeSpotDatafeedSubscriptionRequest describeSpotDatafeedSubscriptionRequest) { null }

    void deleteTags(DeleteTagsRequest deleteTagsRequest) { }

    DescribeTagsResult describeTags(DescribeTagsRequest describeTagsRequest) { null }

    void deleteSubnet(DeleteSubnetRequest deleteSubnetRequest) { }

    CreateVpnGatewayResult createVpnGateway(CreateVpnGatewayRequest createVpnGatewayRequest) { null }

    CancelBundleTaskResult cancelBundleTask(CancelBundleTaskRequest cancelBundleTaskRequest) { null }

    void deleteVpnGateway(DeleteVpnGatewayRequest deleteVpnGatewayRequest) { }

    CancelSpotInstanceRequestsResult cancelSpotInstanceRequests(
            CancelSpotInstanceRequestsRequest cancelSpotInstanceRequestsRequest) { null }

    AttachVolumeResult attachVolume(AttachVolumeRequest attachVolumeRequest) { null }

    DescribeLicensesResult describeLicenses(DescribeLicensesRequest describeLicensesRequest) { null }

    PurchaseReservedInstancesOfferingResult purchaseReservedInstancesOffering(
            PurchaseReservedInstancesOfferingRequest purchaseReservedInstancesOfferingRequest) { null }

    void activateLicense(ActivateLicenseRequest activateLicenseRequest) { }

    void resetImageAttribute(ResetImageAttributeRequest resetImageAttributeRequest) { }

    DescribeVpnConnectionsResult describeVpnConnections(
            DescribeVpnConnectionsRequest describeVpnConnectionsRequest) { null }

    CreateSnapshotResult createSnapshot(CreateSnapshotRequest createSnapshotRequest) { null }

    void deleteVolume(DeleteVolumeRequest deleteVolumeRequest) { }

    void modifySnapshotAttribute(ModifySnapshotAttributeRequest modifySnapshotAttributeRequest) { }

    TerminateInstancesResult terminateInstances(TerminateInstancesRequest terminateInstancesRequest) { null }

    void deleteSpotDatafeedSubscription(DeleteSpotDatafeedSubscriptionRequest deleteSpotDatafeedSubscriptionRequest) { }

    DescribeVpcsResult describeVpcs(DescribeVpcsRequest describeVpcsRequest) { new DescribeVpcsResult() }

    void deactivateLicense(DeactivateLicenseRequest deactivateLicenseRequest) { }

    DescribeSnapshotAttributeResult describeSnapshotAttribute(
            DescribeSnapshotAttributeRequest describeSnapshotAttributeRequest) { null }

    void deleteCustomerGateway(DeleteCustomerGatewayRequest deleteCustomerGatewayRequest) { }

    DescribeAddressesResult describeAddresses(DescribeAddressesRequest describeAddressesRequest) { null }

    DescribeKeyPairsResult describeKeyPairs(DescribeKeyPairsRequest describeKeyPairsRequest) {
        new DescribeKeyPairsResult().withKeyPairs(['amzn-linux', 'hadoop', 'nf-test-keypair-a', 'nf-support'].collect {
                new KeyPairInfo().withKeyName(it) })
    }

    DescribeImageAttributeResult describeImageAttribute(DescribeImageAttributeRequest describeImageAttributeRequest) {
        ImageAttribute imageAttribute = new ImageAttribute().withLaunchPermissions(new LaunchPermission())
        new DescribeImageAttributeResult().withImageAttribute(imageAttribute)
    }

    ConfirmProductInstanceResult confirmProductInstance(
            ConfirmProductInstanceRequest confirmProductInstanceRequest) { null }

    CreateVolumeResult createVolume(CreateVolumeRequest createVolumeRequest) { null }

    DescribeVpnGatewaysResult describeVpnGateways(DescribeVpnGatewaysRequest describeVpnGatewaysRequest) { null }

    CreateSubnetResult createSubnet(CreateSubnetRequest createSubnetRequest) { null }

    DescribeReservedInstancesOfferingsResult describeReservedInstancesOfferings(
            DescribeReservedInstancesOfferingsRequest describeReservedInstancesOfferingsRequest) {
        new DescribeReservedInstancesOfferingsResult().withReservedInstancesOfferings(['c1.medium', 'c1.xlarge',
                'cc1.4xlarge', 'cg1.4xlarge', 'm1.large', 'm1.small', 'm1.xlarge', 'm2.2xlarge', 'm2.4xlarge',
                'm2.xlarge', 't1.micro'].collect { new ReservedInstancesOffering().withInstanceType(it) } )
    }

    DescribeVolumesResult describeVolumes(DescribeVolumesRequest describeVolumesRequest) {
        new DescribeVolumesResult().withVolumes(mockVolumes)
    }

    void deleteSnapshot(DeleteSnapshotRequest deleteSnapshotRequest) { }

    DescribeDhcpOptionsResult describeDhcpOptions(DescribeDhcpOptionsRequest describeDhcpOptionsRequest) { null }

    MonitorInstancesResult monitorInstances(MonitorInstancesRequest monitorInstancesRequest) { null }

    void createPlacementGroup(CreatePlacementGroupRequest createPlacementGroupRequest) { }

    DescribeBundleTasksResult describeBundleTasks(DescribeBundleTasksRequest describeBundleTasksRequest) { null }

    BundleInstanceResult bundleInstance(BundleInstanceRequest bundleInstanceRequest) { null }

    void deletePlacementGroup(DeletePlacementGroupRequest deletePlacementGroupRequest) { }

    void revokeSecurityGroupIngress(RevokeSecurityGroupIngressRequest revokeSecurityGroupIngressRequest) { }

    void deleteVpc(DeleteVpcRequest deleteVpcRequest) { }

    GetConsoleOutputResult getConsoleOutput(GetConsoleOutputRequest getConsoleOutputRequest) { null }

    AllocateAddressResult allocateAddress(AllocateAddressRequest allocateAddressRequest) { null }

    void modifyImageAttribute(ModifyImageAttributeRequest modifyImageAttributeRequest) { }

    void releaseAddress(ReleaseAddressRequest releaseAddressRequest) { }

    CreateCustomerGatewayResult createCustomerGateway(
            CreateCustomerGatewayRequest createCustomerGatewayRequest) { null }

    void resetInstanceAttribute(ResetInstanceAttributeRequest resetInstanceAttributeRequest) { }

    CreateSpotDatafeedSubscriptionResult createSpotDatafeedSubscription(
            CreateSpotDatafeedSubscriptionRequest createSpotDatafeedSubscriptionRequest) { null }

    CreateKeyPairResult createKeyPair(CreateKeyPairRequest createKeyPairRequest) { null }

    DescribeSnapshotsResult describeSnapshots(DescribeSnapshotsRequest describeSnapshotsRequest) {
        new DescribeSnapshotsResult().withSnapshots(mockSnapshots)
    }

    RegisterImageResult registerImage(RegisterImageRequest registerImageRequest) { null }

    void deleteVpnConnection(DeleteVpnConnectionRequest deleteVpnConnectionRequest) { }

    CreateVpnConnectionResult createVpnConnection(CreateVpnConnectionRequest createVpnConnectionRequest) { null }

    DescribeInstanceAttributeResult describeInstanceAttribute(
            DescribeInstanceAttributeRequest describeInstanceAttributeRequest) {
        new DescribeInstanceAttributeResult().withInstanceAttribute(new InstanceAttribute().withUserData(null))
    }

    DescribePlacementGroupsResult describePlacementGroups() { null }

    DescribeReservedInstancesResult describeReservedInstances() {
        new DescribeReservedInstancesResult()
    }

    DescribeSubnetsResult describeSubnets() { new DescribeSubnetsResult() }

    DescribeAvailabilityZonesResult describeAvailabilityZones() { null }

    DescribeInstancesResult describeInstances() { null }

    DescribeImagesResult describeImages() { null }

    DescribeSpotInstanceRequestsResult describeSpotInstanceRequests() {
        new DescribeSpotInstanceRequestsResult().withSpotInstanceRequests(mockSpotInstanceRequests)
    }

    DescribeCustomerGatewaysResult describeCustomerGateways() { null }

    DescribeSpotPriceHistoryResult describeSpotPriceHistory() { null }

    DescribeRegionsResult describeRegions() { null }

    DescribeSecurityGroupsResult describeSecurityGroups() {
        new DescribeSecurityGroupsResult().withSecurityGroups(mockSecurityGroups)
    }

    DescribeSpotDatafeedSubscriptionResult describeSpotDatafeedSubscription() { null }

    DescribeTagsResult describeTags() { null }

    DescribeLicensesResult describeLicenses() { null }

    DescribeVpnConnectionsResult describeVpnConnections() { null }

    void deleteSpotDatafeedSubscription() { }

    DescribeVpcsResult describeVpcs() { new DescribeVpcsResult() }

    DescribeAddressesResult describeAddresses() { null }

    DescribeKeyPairsResult describeKeyPairs() {
        describeKeyPairs(new DescribeKeyPairsRequest())
    }

    DescribeVpnGatewaysResult describeVpnGateways() { null }

    DescribeReservedInstancesOfferingsResult describeReservedInstancesOfferings() { null }

    DescribeVolumesResult describeVolumes() { new DescribeVolumesResult() }

    DescribeDhcpOptionsResult describeDhcpOptions() { null }

    DescribeBundleTasksResult describeBundleTasks() { null }

    AllocateAddressResult allocateAddress() { null }

    DescribeSnapshotsResult describeSnapshots() { new DescribeSnapshotsResult().withSnapshots(mockSnapshots) }

    void shutdown() { }

    ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest amazonWebServiceRequest) { null }
}
