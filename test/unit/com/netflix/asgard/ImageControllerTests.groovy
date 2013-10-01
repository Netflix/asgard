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

import com.amazonaws.services.ec2.model.Image
import com.netflix.asgard.mock.Mocks
import com.netflix.asgard.model.MassDeleteRequest
import grails.test.mixin.TestFor
import org.junit.Before

@TestFor(ImageController)
class ImageControllerTests {

    @Before
    void setUp() {
        Mocks.createDynamicMethods()
        TestUtils.setUpMockRequest()
        controller.awsAutoScalingService = Mocks.awsAutoScalingService()
        controller.awsEc2Service = Mocks.awsEc2Service()
        controller.grailsApplication = Mocks.grailsApplication()
    }

    void testShow() {
        controller.params.imageId = 'ami-8ceb1be5'
        def attrs = controller.show()
        assert 'ami-8ceb1be5' == attrs.image.imageId
        assert 'test' == attrs['accounts']['179000000000']
    }

    void testShowNonExistent() {
        controller.params.imageId = 'ami-doesntexist'
        controller.show()
        assert '/error/missing' == view
        assert "Image 'ami-doesntexist' not found in us-east-1 test" == controller.flash.message
    }

    void testMassDeleteDryRun() {
        controller.params.mode = 'DRYRUN'
        controller.imageService = [massDelete: { UserContext userContext, MassDeleteRequest request -> [new Image()] }]
        controller.massDelete()
        assert controller.response.contentAsString =~ 'Dry run mode. If executed, this job would delete 1 images in us-east-1:'
    }

    void testMassDeleteExecute() {
        controller.params.mode = 'EXECUTE'
        controller.imageService = [massDelete: { UserContext userContext, MassDeleteRequest request -> [new Image()] }]
        controller.massDelete()
        assert controller.response.contentAsString =~ 'Started deleting the following 1 images in us-east-1:'
    }

}
