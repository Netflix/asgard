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
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.JanitorMode
import com.netflix.asgard.model.MassDeleteRequest
import org.joda.time.DateTime

class ImageServiceMassDeleteSpec extends ImageServiceSpec {

    MassDeleteRequest prototypeRequest = new MassDeleteRequest(
        mode: JanitorMode.DRYRUN,
        limit: 100,
        lastReferencedDaysAgo: 45,
        neverReferencedDaysAgo: 14)

    def 'should throw NPE when janitor mode is null'() {
        when:
        prototypeRequest.mode = null
        imageService.massDelete(null, prototypeRequest)

        then:
        NullPointerException e = thrown()
        e.message == 'ERROR: Trying to use Janitor Mode with null mode [DRYRUN, EXECUTE]'
    }

    def 'should throw exception if lastReferencedDaysAgo below threshold'() {
        when:
        prototypeRequest.lastReferencedDaysAgo = 1
        imageService.massDelete(null, prototypeRequest)

        then:
        IllegalArgumentException e = thrown()
        e.message == "ERROR: Illegal 'lastReferencedDaysAgo' value 1 is less than minimum 30"
    }

    def 'should throw exception if unusedDaysAgo below threshold'() {
        when:
        prototypeRequest.neverReferencedDaysAgo = 1
        imageService.massDelete(null, prototypeRequest)

        then:
        IllegalArgumentException e = thrown()
        e.message == "ERROR: Illegal 'neverReferencedDaysAgo' value 1 is less than minimum 3"
    }

    def 'should throw exception if limit above threshold'() {
        when:
        prototypeRequest.limit = 1000
        imageService.massDelete(null, prototypeRequest)

        then:
        IllegalArgumentException e = thrown()
        e.message == "ERROR: Illegal 'limit' value 1000 is greater than maximum 150"
    }

    def 'should throw illegal state exception if not enough recently used'() {
        Image image = createImage(Time.now(), Time.now(), '')
        Image image2 = createImage(Time.now().minusMonths(12), Time.now().minusMonths(6))
        awsEc2Service.getAccountImages(userContext) >> [image, image2]
        awsEc2Service.getInstances(userContext) >> [new Instance(imageId: 1)]
        awsAutoScalingService.getLaunchConfigurations(userContext) >> [new LaunchConfiguration(imageId: 2)]

        when:
        imageService.massDelete(userContext, prototypeRequest)

        then:
        IllegalStateException e = thrown(IllegalStateException)
        e.message == 'Aborting mass delete. In test us-east-1 only 1 AMIs were tagged with last_referenced_time recently, although 2 AMIs are in use. Is tagging broken?'
    }

    def 'should delete if owned by this account and not used recently'() {
        Image image = setupSingleImage(Time.now().minusMonths(3), Time.now().minusMonths(3))

        when:
        List<Image> toDelete = imageService.massDelete(userContext, prototypeRequest)

        then:
        toDelete == [image]
    }

    def 'should not delete if not the owner of this instance'() {
        setupLastReferencedDefaults()
        setupSingleImage(Time.now().minusMonths(3), Time.now().minusMonths(3), 'someone else')

        when:
        List<Image> toDelete = imageService.massDelete(userContext, prototypeRequest)

        then:
        toDelete == []
    }

    def 'should not delete if missing creation time'() {
        setupSingleImage(null, null)

        when:
        List<Image> toDelete = imageService.massDelete(userContext, prototypeRequest)

        then:
        toDelete == []
    }

    def 'should not delete if recently used'() {
        setupSingleImage(Time.now().minusMonths(3), Time.now().minusMonths(1))

        when:
        List<Image> toDelete = imageService.massDelete(userContext, prototypeRequest)

        then:
        toDelete == []
    }

    def 'should not delete if marked as keep forever'() {
        Image image = setupSingleImage(Time.now().minusMonths(3), Time.now().minusMonths(3))
        image.metaClass.isKeepForever { true }

        when:
        List<Image> toDelete = imageService.massDelete(userContext, prototypeRequest)

        then:
        toDelete == []
    }

    def 'should delete if recently created and not referenced'() {
        Image image = setupSingleImage(Time.now().minusMonths(1), null)

        when:
        List<Image> toDelete = imageService.massDelete(userContext, prototypeRequest)

        then:
        toDelete == [image]
    }

    def 'should cap number of deleted by limit parameter'() {
        setupLastReferencedDefaults()
        Image image = createImage(Time.now().minusMonths(3), Time.now().minusMonths(3))
        awsEc2Service.getAccountImages(userContext) >> [image, image]
        awsEc2Service.getInstances(userContext) >> []
        awsAutoScalingService.getLaunchConfigurations(userContext) >> []

        when:
        prototypeRequest.limit = 1
        List<Image> toDelete = imageService.massDelete(userContext, prototypeRequest)

        then:
        toDelete == [image]
    }

    def 'should actually delete when in execute mode'() {
        Image image = setupSingleImage(Time.now().minusMonths(3), Time.now().minusMonths(3))

        when:
        prototypeRequest.mode = JanitorMode.EXECUTE
        List<Image> toDelete = imageService.massDelete(userContext, prototypeRequest)

        then:
        2 * awsEc2Service.getImage(userContext, IMAGE_ID) >> image
        1 * awsEc2Service.deregisterImage(userContext, IMAGE_ID,  _)
        toDelete == [image]

    }

    def 'should send email if deleting throws exception'() {
        Image image = setupSingleImage(Time.now().minusMonths(3), Time.now().minusMonths(3))
        Exception e = new Exception('foo')

        when:
        awsEc2Service.getImage(userContext, IMAGE_ID) >> { throw e }
        prototypeRequest.mode = JanitorMode.EXECUTE
        List<Image> toDelete = imageService.massDelete(userContext, prototypeRequest)

        then:
        1 * emailerService.sendExceptionEmail('Unable to delete AMI id imageId because java.lang.Exception: foo', e)
        toDelete == [image]
    }

    def 'should not delete if in excluded launch permissions'() {
        setupLastReferencedDefaults()
        Image image = createImage(Time.now().minusMonths(3), Time.now().minusMonths(3))
        Image image2 = createImage(Time.now().minusMonths(3), Time.now().minusMonths(3))
        image2.imageId = 'imageId2'
        awsEc2Service.getAccountImages(userContext) >> [image, image2]
        awsEc2Service.getInstances(userContext) >> []
        awsAutoScalingService.getLaunchConfigurations(userContext) >> []
        configService.excludedLaunchPermissionsForMassDelete >> [Mocks.PROD_AWS_ACCOUNT_ID]
        awsEc2Service.getImagesWithLaunchPermissions(userContext, [Mocks.PROD_AWS_ACCOUNT_ID], [IMAGE_ID, 'imageId2']) >> [image2]

        when:
        List<Image> toDelete = imageService.massDelete(userContext, prototypeRequest)

        then:
        toDelete == [image]
    }

    def 'should abort if in use image is marked for delete'() {
        UserContext usEastUserContext = userContext.withRegion(Region.US_EAST_1)
        awsEc2Service.getInstances(usEastUserContext) >> [new Instance(imageId: IMAGE_ID)]
        awsEc2Service.getAccountImages(usEastUserContext) >> [new Image(imageId: IMAGE_ID, name: 'image-name')]
        Image image = setupSingleImage(Time.now().minusMonths(3), Time.now().minusMonths(3))
        image.name = 'image-name'

        when:
        imageService.massDelete(userContext, prototypeRequest)

        then:
        IllegalStateException e = thrown(IllegalStateException)
        e.message == 'Aborting mass delete. In test us-east-1 the following in use images were marked for delete: [imageId]'
    }

    Image setupSingleImage(DateTime creationTime, DateTime lastReferencedTime, String ownerId = Mocks.TEST_AWS_ACCOUNT_ID) {
        setupLastReferencedDefaults()
        Image image = createImage(creationTime, lastReferencedTime, ownerId)
        awsEc2Service.getAccountImages(userContext) >> [image]
        awsEc2Service.getInstances(userContext) >> []
        awsAutoScalingService.getLaunchConfigurations(userContext) >> []
        image
    }

    Image createImage(DateTime creationTime, DateTime lastReferencedTime, String ownerId = Mocks.TEST_AWS_ACCOUNT_ID) {
        Image image = new Image()
        image.metaClass {
            getCreationTime { creationTime ? Time.format(creationTime) : '' }
            getLastReferencedTime { lastReferencedTime ? Time.format(lastReferencedTime) : '' }
            isKeepForever { false }
        }
        image.imageId = IMAGE_ID
        image.ownerId = ownerId
        image.imageLocation = ''
        image
    }

}
