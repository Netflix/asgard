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
import com.amazonaws.services.ec2.model.Tag
import grails.converters.XML
import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder

@SuppressWarnings(["GroovyAssignabilityCheck"])
class ImageServiceReplicateTagsSpec extends ImageServiceSpec {

    def 'should add tags if missing from production'() {
        Image testImage1 = new Image(imageId: 'imageId', tags: [new Tag('key', 'value')])
        Image testImage2 = new Image(imageId: 'imageId2', tags: [new Tag('key', 'value')])
        Image prodImage1 = new Image(imageId: 'imageId')
        Image prodImage2 = new Image(imageId: 'imageId2')
        setupReplicateTestAndProdImages([testImage1, testImage2], [prodImage1, prodImage2])

        Map<String, String> expectedPostData = [imageIds: 'imageId,imageId2', name: 'key', value: 'value']

        when:
        imageService.runReplicateImageTags()

        then:
        2 * restClientService.post({ it =~ /\/image\/addTags/ }, expectedPostData) >> 200
    }

    def 'should call separate updates for same key and different value'() {
        Image testImage1 = new Image(imageId: 'imageId', tags: [new Tag('key', 'value3')])
        Image testImage2 = new Image(imageId: 'imageId2', tags: [new Tag('key', 'value4')])
        Image prodImage1 = new Image(imageId: 'imageId', tags: [new Tag('key', 'value')])
        Image prodImage2 = new Image(imageId: 'imageId2', tags: [new Tag('key', 'value2')])
        setupReplicateTestAndProdImages([testImage1, testImage2], [prodImage1, prodImage2])

        Map<String, String> expectedPostData = [imageIds: 'imageId', name: 'key', value: 'value3']
        Map<String, String> expectedPostData2 = [imageIds: 'imageId2', name: 'key', value: 'value4']

        when:
        imageService.runReplicateImageTags()

        then:
        2 * restClientService.post({ it =~ /\/image\/addTags/ }, expectedPostData) >> 200
        2 * restClientService.post({ it =~ /\/image\/addTags/ }, expectedPostData2) >> 200
    }

    def 'should delete tags if missing from production'() {
        Image testImage1 = new Image(imageId: 'imageId')
        Image testImage2 = new Image(imageId: 'imageId2')
        Image prodImage1 = new Image(imageId: 'imageId', tags: [new Tag('key', 'value')])
        Image prodImage2 = new Image(imageId: 'imageId2', tags: [new Tag('key', 'value')])
        setupReplicateTestAndProdImages([testImage1, testImage2], [prodImage1, prodImage2])

        Map<String, String> expectedPostData = [imageIds: 'imageId,imageId2', name: 'key']

        when:
        imageService.runReplicateImageTags()

        then:
        2 * restClientService.post({ it =~ /\/image\/removeTags/ }, expectedPostData) >> 200
    }

    private setupReplicateTestAndProdImages(List<Image> testImages, List<Image> prodImages) {
        StringWriter sw = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(sw)
        builder.list {
            prodImages.each { theImage ->
                image {
                    imageId theImage.imageId
                    tags {
                        theImage.tags.each { theTag ->
                            tag {
                                key(theTag.key)
                                value(theTag.value)
                            }
                        }
                    }
                }
            }
        }
        GPathResult prodImagesXml = XML.parse(sw.toString()) as GPathResult
        awsEc2Service.getAccountImages(UserContext.auto()) >> testImages
        restClientService.getAsXml({ it =~ /\/us-east-1\/image\/list\.xml/ }) >> prodImagesXml
        configService.getPromotionTargetServerRootUrls() >> ['http://staging', 'http://prod']
        restClientService.getAsText(_, _) >> InetAddress.getLocalHost().getHostName()
        restClientService.getResponseCode(_) >> 200
        awsEc2Service.getAccountImages(_) >> []
        restClientService.getAsXml(_) >> (XML.parse('<list/>') as GPathResult)
    }

}
