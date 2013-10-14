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

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.CreateTagsRequest
import com.amazonaws.services.ec2.model.DescribeImagesRequest
import com.amazonaws.services.ec2.model.DescribeImagesResult
import com.amazonaws.services.ec2.model.Filter
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.SecurityGroup
import com.amazonaws.services.ec2.model.SpotInstanceRequest
import com.amazonaws.services.ec2.model.Tag
import com.google.common.collect.Multiset
import com.netflix.asgard.mock.Mocks
import com.netflix.frigga.ami.AppVersion
import grails.test.GrailsUnitTestCase
import grails.test.MockUtils

class AwsEc2ServiceTests extends GrailsUnitTestCase {

    void setUp() {
        Mocks.createDynamicMethods()
    }

    void testGetSpotInstanceRequests() {
        AwsEc2Service awsEc2Service = Mocks.awsEc2Service()
        Region region = Region.defaultRegion()
        List<SpotInstanceRequest> spotInstanceRequests = awsEc2Service.retrieveSpotInstanceRequests(region)
        assert spotInstanceRequests.size() >= 1
    }

    void testIsSecurityGroupEditable() {
        AwsEc2Service awsEc2Service = new AwsEc2Service()
        assert awsEc2Service.isSecurityGroupEditable("nf-infrastructure")
        assert awsEc2Service.isSecurityGroupEditable("nf-datacenter")
        assert !awsEc2Service.isSecurityGroupEditable("default")
        assert awsEc2Service.isSecurityGroupEditable("nccp")
    }

    void testGetEffectiveSecurityGroups() {
        AwsEc2Service awsEc2Service = new AwsEc2Service()
        awsEc2Service.metaClass.getSecurityGroups = { UserContext userContext ->
            return ['nf-infrastructure', 'default', 'nf-datacenter', 'nccp', 'mickeymouse'].
                    collect { new SecurityGroup().withGroupName(it) }
        }

        def allSecurityGroups = awsEc2Service.getSecurityGroups(Mocks.userContext())
        def effectiveSecurityGroups = awsEc2Service.getEffectiveSecurityGroups(Mocks.userContext())

        assertEquals 5, allSecurityGroups.size()
        assert allSecurityGroups.any { it.groupName == "nf-infrastructure" }
        assert allSecurityGroups.any { it.groupName == "default" }
        assert allSecurityGroups.any { it.groupName == "nf-datacenter" }
        assert allSecurityGroups.any { it.groupName == "nccp" }
        assert allSecurityGroups.any { it.groupName == "mickeymouse" }

        assertEquals 4, effectiveSecurityGroups.size()
        assert effectiveSecurityGroups.any { it.groupName == "nccp" }
        assert effectiveSecurityGroups.any { it.groupName == "mickeymouse" }
        assert effectiveSecurityGroups.any { it.groupName == "nf-infrastructure" }
        assert !effectiveSecurityGroups.any { it.groupName == "default" }
    }

    void testGetImage() {
        AwsEc2Service awsEc2Service = Mocks.awsEc2Service()
        assertNull awsEc2Service.getImage(Mocks.userContext(), "doesn't exist")
    }

    @SuppressWarnings("GroovyAccessibility")
    void testgetCountedAppVersionsForInstancesAndImages() {

        AwsEc2Service service = new AwsEc2Service()

        Multiset<AppVersion> noAppVersions = service.getCountedAppVersionsForInstancesAndImages([], [:])
        assert noAppVersions.isEmpty()

        String appVersionString1 = "app-1.2.3-456789.h321/WE-WAPP-app/321"
        String appVersionString2 = "app-1.2.4-567890.h432/WE-WAPP-app/432"

        AppVersion appVersion1 = Relationships.dissectAppVersion(appVersionString1)
        AppVersion appVersion2 = Relationships.dissectAppVersion(appVersionString2)

        Image image1 = (new Image()).withImageId("image-1").withTags(new Tag("appversion", appVersionString1))
        Image image2 = (new Image()).withImageId("image-2").withTags(new Tag("appversion", appVersionString2))
        Image imageNoApp = (new Image()).withImageId("image-noapp")

        Instance img1_inst1 = (new Instance()).withImageId(image1.imageId)
        Instance img1_inst2 = (new Instance()).withImageId(image1.imageId)
        Instance img2_inst1 = (new Instance()).withImageId(image2.imageId)
        Instance img2_inst2 = (new Instance()).withImageId(image2.imageId)
        Instance imgNoApp_inst1 = (new Instance()).withImageId(imageNoApp.imageId)

        Multiset<AppVersion> oneAppOneInstance = service.getCountedAppVersionsForInstancesAndImages([img1_inst1], [(image1.imageId): image1])
        assert 1 == oneAppOneInstance.size()
        assert 1 == oneAppOneInstance.count(appVersion1)
        assert 0 == oneAppOneInstance.count(appVersion2)

        Multiset<AppVersion> oneAppTwoInstance = service.getCountedAppVersionsForInstancesAndImages([img1_inst1, img1_inst2],
            [(image1.imageId): image1])
        assert 2 == oneAppTwoInstance.size()
        assert 2 == oneAppTwoInstance.count(appVersion1)
        assert 0 == oneAppTwoInstance.count(appVersion2)

        Multiset<AppVersion> twoAppThreeInstance = service.getCountedAppVersionsForInstancesAndImages([img1_inst1, img2_inst1, img2_inst2],
            [(image1.imageId): image1, (image2.imageId): image2])
        assert 3 == twoAppThreeInstance.size()
        assert 1 == twoAppThreeInstance.count(appVersion1)
        assert 2 == twoAppThreeInstance.count(appVersion2)

        Multiset<AppVersion> threeAppThreeInstance = service.getCountedAppVersionsForInstancesAndImages([img1_inst1, img2_inst1, imgNoApp_inst1],
            [(image1.imageId): image1, (image2.imageId): image2, (imageNoApp.imageId): imageNoApp])
        assert 2 == threeAppThreeInstance.size()
        assert 1 == threeAppThreeInstance.count(appVersion1)
        assert 1 == threeAppThreeInstance.count(appVersion2)
    }

    void testRetrieveImagesWithTags() {
        Mocks.monkeyPatcherService()
        Image image1 = new Image(imageId: 'imageId1', tags: [new Tag()])
        Image image2 = new Image(imageId: 'imageId2', tags: [new Tag()])
        AwsEc2Service awsEc2Service = new AwsEc2Service()
        awsEc2Service.configService = Mocks.configService()
        def mockAmazonEC2 = mockFor(AmazonEC2)
        awsEc2Service.awsClient = new MultiRegionAwsClient({ mockAmazonEC2.createMock() })
        DescribeImagesResult describeImagesResult = new DescribeImagesResult(images: [image1, image2])
        mockAmazonEC2.demand.describeImages { DescribeImagesRequest request -> describeImagesResult }
        //noinspection GroovyAccessibility
        Collection<Image> images = awsEc2Service.retrieveImages(Region.US_EAST_1)

        assert images == [image1, image2]
    }

    void testRetrieveImagesWithTagsMissing() {
        Mocks.monkeyPatcherService()
        Image image1 = new Image(imageId: 'imageId1')
        Image image2 = new Image(imageId: 'imageId2')
        AwsEc2Service awsEc2Service = new AwsEc2Service()
        MockUtils.mockLogging(AwsEc2Service)
        awsEc2Service.configService = Mocks.configService()
        def mockAmazonEC2 = mockFor(AmazonEC2)
        awsEc2Service.awsClient = new MultiRegionAwsClient({ mockAmazonEC2.createMock() })
        DescribeImagesResult describeImagesResultMissingTags = new DescribeImagesResult(images: [image1, image2])
        Image image1WithTags = new Image(imageId: 'imageId1', tags: [new Tag()])
        DescribeImagesResult describeImagesResultWithTags = new DescribeImagesResult().withImages(image1WithTags)
        // Can only specify behavior for a mockFor method once as per http://jira.grails.org/browse/GRAILS-4611
        mockAmazonEC2.demand.describeImages(2..2) { DescribeImagesRequest request ->
            if (request.filters == [new Filter('tag-key', ['*'])]) {
                describeImagesResultWithTags
            } else {
                describeImagesResultMissingTags
            }
        }

        //noinspection GroovyAccessibility
        Collection<Image> images = awsEc2Service.retrieveImages(Region.US_EAST_1)

        assert images == [image1WithTags, image2]
    }

    void testCreateTags() {
        def imageRange = 1..600
        Collection<String> sixHundredImageIds = imageRange.collect { "image${it}" }
        AwsEc2Service awsEc2Service = new AwsEc2Service()
        def mockAmazonEC2 = mockFor(AmazonEC2)
        awsEc2Service.awsClient = new MultiRegionAwsClient({ mockAmazonEC2.createMock() })
        mockAmazonEC2.demand.createTags(3..3) { CreateTagsRequest request ->
            int imageIdListSize = request.resources.size
            assert imageIdListSize == 250 || imageIdListSize == 100
            assert request.tags == [new Tag('tag', 'value')]
        }

        awsEc2Service.createImageTags(Mocks.userContext(), sixHundredImageIds, 'tag', 'value')
        mockAmazonEC2.verify()
    }
}
