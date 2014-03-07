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
import com.amazonaws.services.ec2.model.Volume
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class VolumeController {

    def awsEc2Service

    def index() {
        redirect(action: 'list', params: params)
    }

    def list() {
        UserContext userContext = UserContext.of(request)
        def volumes = (awsEc2Service.getVolumes(userContext) as List).sort { it.volumeId.toLowerCase() }
        def details = ['volumes': volumes, 'zoneList': awsEc2Service.getAvailabilityZones(userContext)]
        withFormat {
            html { details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    def save() {
        UserContext userContext = UserContext.of(request)
        try {
            Volume volume = awsEc2Service.createVolume(userContext, params.volumeSize as Integer,
                    params.availabilityZone)
            flash.message = "EBS Volume '${volume.volumeId}' has been created."
            redirect(action: 'show', params: [id: volume.volumeId])
        } catch (AmazonServiceException ase) {
            flash.message = "Could not create EBS Volume: ${ase}"
            redirect(action: 'list')
        }
    }

    def delete() {
        UserContext userContext = UserContext.of(request)
        def volumeIds = []
        if (params.volumeId) { volumeIds << params.volumeId }
        if (params.selectedVolumes) {
            volumeIds.addAll(Requests.ensureList(params.selectedVolumes))
        }

        def message = ""
        try {
            def deletedCount = 0
            volumeIds.each {
                awsEc2Service.deleteVolume(userContext, it)
                message += (deletedCount > 0) ? ", $it" : "Volume(s) deleted: $it"
                deletedCount++
            }
            flash.message = message
        } catch (Exception e) {
            flash.message = "Could not delete volume: ${e}"
        }
        redirect(action: 'list')
    }

    def show() {
        UserContext userContext = UserContext.of(request)
        String volumeId = EntityType.volume.ensurePrefix(params.volumeId ?: params.id)
        Volume volume = awsEc2Service.getVolume(userContext, volumeId)
        volume?.tags?.sort { it.key }
        if (!volume) {
            Requests.renderNotFound('EBS Volume', volumeId, this)
        } else {
            withFormat {
                html { return ['volume':volume] }
                xml { new XML(volume).render(response) }
                json { new JSON(volume).render(response) }
            }
        }
    }

    def detach() {
        String volumeId = EntityType.volume.ensurePrefix(params.volumeId ?: params.id)
        UserContext userContext = UserContext.of(request)
        try {
            awsEc2Service.detachVolume(userContext, volumeId, params.instanceId, params.device)
            flash.message = "EBS Volume '${volumeId}' has been detached from ${params.instanceId}."
        } catch (Exception e) {
            flash.message = "Could not detach EBS Volume ${volumeId}: ${e}"
        }
        redirect(action: 'show', params: [id: volumeId])
    }
}
