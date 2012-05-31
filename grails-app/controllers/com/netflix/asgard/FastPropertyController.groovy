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

import grails.converters.JSON
import grails.converters.XML

class FastPropertyController {

    def grailsApplication

    def fastPropertyService

    String unavailableMessage = 'The new platformservice editor is not yet operational. It will be online shortly.'

    def index = { redirect(action: 'list', params: params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        Set<String> appNames = Requests.ensureList(params.id).collect { it.split(',') }.flatten() as Set<String>
        List<FastProperty> fastProperties = fastPropertyService.getAll(userContext).sort { it.key }
        if (appNames) {
            fastProperties = fastProperties.findAll { it.appId in appNames }
        }
        Map result = [fastProperties: fastProperties.sort { it?.key?.toString()?.toLowerCase() }]
        withFormat {
            html { result }
            xml { new XML(result).render(response) }
            json { new JSON(result).render(response) }
        }
    }

    def show = {
        String id = params.id
        UserContext userContext = UserContext.of(request)
        FastProperty fastProperty = fastPropertyService.get(userContext, id)
        if (!fastProperty) {
            Requests.renderNotFound('Fast Property', id, this)
            return
        }
        Map details = [fastProperty: fastProperty]
        withFormat {
            html { return details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    def create = {
        final UserContext userContext = UserContext.of(request)
        List<String> appNames = fastPropertyService.collectFastPropertyAppNames(userContext)
        Collection regionOptions = Region.values()
        regionOptions.addAll(grailsApplication.config.cloud.specialCaseRegions)

        Map result = [
                'appNames': appNames,
                'regionOptions': regionOptions
        ]

        withFormat {
            html { result }
            xml { new XML(result).render(response) }
            json { new JSON(result).render(response) }
        }
    }

    def save = {
        UserContext userContext = UserContext.of(request)
        try {
            final String property = params.key.trim()
            final String value = params.value?.trim()?.decodeHTML() ?: ''
            final String appId = params.appId
            final String region = params.fastPropertyRegion
            final String stack = params.stack?.trim()?.decodeHTML()
            final String countries = params.countries?.trim()?.decodeHTML()
            final String updatedBy = params.updatedBy?.trim()?.decodeHTML()

            fastPropertyService.create(userContext, property, value, appId, region, stack, countries, updatedBy)
            flash.message = "Fast Property '${property}' has been created. The change may take a while to propagate."
            redirect(action: 'list')
        } catch (Exception e) {
            flash.message = e.message ?: e.cause?.message
            chain(action: 'create', params: Requests.cap(params))
        }
    }

    def edit = {
        UserContext userContext = UserContext.of(request)
        String id = params.id
        FastProperty fastProperty = fastPropertyService.get(userContext, id)
        if (!fastProperty) {
            Requests.renderNotFound('Fast Property', id, this)
            return
        }
        Map details = [fastProperty: fastProperty]
        withFormat {
            html { return details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    def update = {
        String id = params.id
        String updatedBy = params.updatedBy?.trim()?.decodeHTML()
        String value = params.value?.trim()?.decodeHTML()
        UserContext userContext = UserContext.of(request)
        try {
            fastPropertyService.updateFastProperty(userContext, id, value, updatedBy)
            flash.message = "Fast Property '${id}' has been updated. The change may take a while to propagate."
        } catch (Exception e) {
            flash.message = "Unable to update Fast Property '${id}': ${e}"
        }
        redirect(action: 'show', id: id)
    }

    def delete = {
        UserContext userContext = UserContext.of(request)
        String id = params.id
        String fastPropertyRegion = params.fastPropertyRegion
        String updatedBy = params.updatedBy?.decodeHTML()
        fastPropertyService.deleteFastProperty(userContext, id, updatedBy, fastPropertyRegion)
        flash.message = "Fast Property '${id}' deleted. The change may take a while to propagate."
        redirect(action: 'result')
    }

    def result = { render view: '/common/result' }
}
