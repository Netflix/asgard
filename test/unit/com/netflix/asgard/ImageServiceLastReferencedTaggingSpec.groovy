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

import com.amazonaws.services.autoscaling.model.LaunchConfiguration
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.Instance
import grails.converters.JSON

@SuppressWarnings("GroovyAssignabilityCheck")
class ImageServiceLastReferencedTaggingSpec extends ImageServiceSpec {

    def 'should tag if image is referenced in test instance'() {
        UserContext usEastUserContext = userContext.withRegion(Region.US_EAST_1)
        awsEc2Service.getInstances(usEastUserContext) >> [new Instance(imageId: IMAGE_ID)]
        awsEc2Service.getAccountImages(usEastUserContext) >> [new Image(imageId: IMAGE_ID, name: 'image-name')]
        setupLastReferencedDefaults()

        when:
        imageService.tagAmiLastReferencedTime(userContext)

        then:
        1 * awsEc2Service.createImageTags(usEastUserContext, [IMAGE_ID], 'last_referenced_time', _)
    }

    def 'should tag if image is referenced in local launch config'() {
        UserContext usEastUserContext = userContext.withRegion(Region.US_EAST_1)
        awsAutoScalingService.getLaunchConfigurations(usEastUserContext) >> [new LaunchConfiguration(imageId: IMAGE_ID)]
        awsEc2Service.getAccountImages(usEastUserContext) >> [new Image(imageId: IMAGE_ID, name: 'image-name')]
        setupLastReferencedDefaults()

        when:
        imageService.tagAmiLastReferencedTime(userContext)

        then:
        1 * awsEc2Service.createImageTags(usEastUserContext, [IMAGE_ID], 'last_referenced_time', _)
    }

    def 'should tag if image is used in production'() {
        UserContext usEastUserContext = userContext.withRegion(Region.US_EAST_1)
        //noinspection GroovyAssignabilityCheck
        restClientService.getAsJson({ it =~ /\/us-east-1\/image\/used.json/ }) >> JSON.parse("[${IMAGE_ID}]")
        awsEc2Service.getAccountImages(usEastUserContext) >> [new Image(imageId: IMAGE_ID, name: 'image-name')]
        setupLastReferencedDefaults()

        when:
        imageService.tagAmiLastReferencedTime(userContext)

        then:
        1 * awsEc2Service.createImageTags(usEastUserContext, [IMAGE_ID], 'last_referenced_time', _)
    }

    def 'should tag if base image is referenced in test instance'() {
        UserContext usEastUserContext = userContext.withRegion(Region.US_EAST_1)
        awsEc2Service.getInstances(usEastUserContext) >> [new Instance(imageId: IMAGE_ID)]
        String baseAmiId = 'ami-50886239'
        def image = new Image(imageId: IMAGE_ID, description: "base_ami_id=${baseAmiId}", name: 'image-name')
        def baseAmiImage = new Image(imageId: baseAmiId, name: 'base-image-name')
        awsEc2Service.getAccountImages(usEastUserContext) >> [image, baseAmiImage]
        setupLastReferencedDefaults()

        when:
        imageService.tagAmiLastReferencedTime(userContext)

        then:
        1 * awsEc2Service.createImageTags(usEastUserContext, [IMAGE_ID, baseAmiId], 'last_referenced_time', _)
    }

    def 'should tag accross regions'() {
        UserContext usEastUserContext = userContext.withRegion(Region.US_EAST_1)
        UserContext euWestUserContext = userContext.withRegion(Region.EU_WEST_1)
        awsEc2Service.getInstances(usEastUserContext) >> [new Instance(imageId: IMAGE_ID)]
        awsEc2Service.getAccountImages(usEastUserContext) >> [new Image(imageId: IMAGE_ID, name: 'image-name')]
        awsEc2Service.getAccountImages(euWestUserContext) >> [new Image(imageId: 'imageId2', name: 'image-name')]
        setupLastReferencedDefaults()

        when:
        imageService.tagAmiLastReferencedTime(userContext)

        then:
        1 * awsEc2Service.createImageTags(usEastUserContext, [IMAGE_ID], 'last_referenced_time', _)
        1 * awsEc2Service.createImageTags(euWestUserContext, ['imageId2'], 'last_referenced_time', _)
    }

    def 'should only tag once if names are duplicated'() {
        UserContext usEastUserContext = userContext.withRegion(Region.US_EAST_1)
        UserContext euWestUserContext = userContext.withRegion(Region.EU_WEST_1)
        awsEc2Service.getInstances(usEastUserContext) >> [new Instance(imageId: IMAGE_ID)]
        awsEc2Service.getInstances(euWestUserContext) >> [new Instance(imageId: IMAGE_ID)]
        awsEc2Service.getAccountImages(usEastUserContext) >> [new Image(imageId: IMAGE_ID, name: 'image-name')]
        awsEc2Service.getAccountImages(euWestUserContext) >> [new Image(imageId: 'imageId2', name: 'image-name')]
        setupLastReferencedDefaults()

        when:
        imageService.tagAmiLastReferencedTime(userContext)

        then:
        1 * awsEc2Service.createImageTags(usEastUserContext, [IMAGE_ID], 'last_referenced_time', _)
        1 * awsEc2Service.createImageTags(euWestUserContext, ['imageId2'], 'last_referenced_time', _)
    }

    def 'should not call create image tags if nothing to tag'() {
        setupLastReferencedDefaults()

        when:
        imageService.tagAmiLastReferencedTime(userContext)

        then:
        0 * awsEc2Service.createImageTags(_, [], 'last_referenced_time', _)
    }

}
