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

import com.amazonaws.services.ec2.model.SpotInstanceRequest
import grails.converters.JSON
import grails.converters.XML
import com.amazonaws.services.ec2.model.CancelledSpotInstanceRequest
import com.netflix.asgard.model.SpotInstanceRequestListType

class SpotInstanceRequestController {

    def awsEc2Service
    def spotInstanceRequestService

    final static allowedMethods = [cancel: 'POST']

    def index = { redirect(action: list, params: params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        SpotInstanceRequestListType type = SpotInstanceRequestListType.of(params.type)
        List<SpotInstanceRequest> sirs = spotInstanceRequestService.getSpotInstanceRequests(userContext, type)
        Map details = [spotInstanceRequests: sirs]
        withFormat {
            html { details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    def show = {
        UserContext userContext = UserContext.of(request)
        String spotInstanceRequestId = params.id
        SpotInstanceRequest sir = spotInstanceRequestService.getSpotInstanceRequest(userContext, spotInstanceRequestId)
        Map details = [spotInstanceRequest: sir]
        withFormat {
            html { details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    def cancel = {
        UserContext userContext = UserContext.of(request)
        List<String> sirIds = Requests.ensureList(params.selectedSpotInstanceRequests ?: params.spotInstanceRequestId)
        List<CancelledSpotInstanceRequest> cancelledSirs = spotInstanceRequestService.cancelSpotInstanceRequests(
                userContext, sirIds)
        flash.message = "Cancelled Spot Instance Requests: ${cancelledSirs*.spotInstanceRequestId}"
        redirect(action: result)
    }

    def result = { render view: '/common/result' }

}
