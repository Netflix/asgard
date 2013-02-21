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

import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class FastPropertyController {

    /**
     * The special marker for looking for fast properties that do not have an application.
     */
    private final String NO_APP_ID = '_noapp'

    def awsAutoScalingService
    def awsEc2Service
    def configService
    def fastPropertyService

    def index = { redirect(action: 'list', params: params) }

    def apps = {
        UserContext userContext = UserContext.of(request)
        List<FastProperty> allProperties = fastPropertyService.getAll(userContext)
        List<String> appNames = allProperties.findResults { it.appId ? it.appId?.toLowerCase() : null }.unique().sort()
        Map result = [appNames: appNames, noAppId: NO_APP_ID]
        withFormat {
            html { result }
            xml { new XML(result).render(response) }
            json { new JSON(result).render(response) }
        }
    }

    def list = {
        UserContext userContext = UserContext.of(request)
        Collection<String> appNames = Requests.ensureList(params.id).collect { it.split(',') }.flatten()
        appNames = appNames.findResults { it.toLowerCase() }.unique()
        List<FastProperty> fastProperties = fastPropertyService.getAll(userContext)
        if (appNames) {
            Boolean noApp = appNames.contains(NO_APP_ID)
            fastProperties = fastProperties.findAll { it.appId?.toLowerCase() in appNames || (noApp && !it.appId) }
        }
        Map result = [fastProperties: fastProperties.sort { it?.key?.toString()?.toLowerCase() }]
        withFormat {
            html { result }
            xml { new XML(result).render(response) }
            json { new JSON(result).render(response) }
        }
    }

    def show = {
        String id = params.id ?: params.name
        UserContext userContext = UserContext.of(request)
        FastProperty fastProperty = fastPropertyService.get(userContext, id)
        if (!fastProperty) {
            Requests.renderNotFound('Fast Property', id, this)
            return
        }
        Map details = [
                fastPropertyInfoUrl: configService.fastPropertyInfoUrl,
                fastProperty: fastProperty
        ]
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
        regionOptions.addAll(configService.specialCaseRegions)
        List<String> asgNames = awsAutoScalingService.getAutoScalingGroups(userContext)*.autoScalingGroupName.sort()
        List<String> clusterNames = awsAutoScalingService.getClusters(userContext)*.name.sort()
        List<String> zoneNames = awsEc2Service.getAvailabilityZones(userContext)*.zoneName.sort()
        Map result = [
                updatedBy: params.updatedBy ?: userContext.username,
                appNames: appNames,
                regionOptions: regionOptions,
                asgNames: asgNames,
                clusterNames: clusterNames,
                zoneNames: zoneNames,
                images: awsEc2Service.getAccountImages(userContext).sort { it.imageLocation.toLowerCase() },
                ttlUnits: FastProperty.TTL_UNITS,
                fastPropertyInfoUrl: configService.fastPropertyInfoUrl
        ]

        withFormat {
            html { result }
            xml { new XML(result).render(response) }
            json { new JSON(result).render(response) }
        }
    }

    def save = { FastPropertySaveCommand cmd ->
        if (cmd.hasErrors()) {
            chain(action: 'create', model: [cmd:cmd], params: params)
            return
        }
        UserContext userContext = UserContext.of(request)
        FastProperty fastProperty = null
        try {
            fastProperty = new FastProperty(
                    key: params.key.trim(),
                    value: params.value?.trim()?.decodeHTML(),
                    env: configService.getAccountName(),
                    appId: params.appId,
                    region: params.fastPropertyRegion,
                    stack: params.stack?.trim()?.decodeHTML(),
                    countries: params.countries?.trim()?.decodeHTML(),
                    updatedBy: params.updatedBy?.trim()?.decodeHTML(),
                    serverId: params.serverId?.trim(),
                    asg: params.asg,
                    cluster: params.cluster,
                    ami: params.ami,
                    zone: params.zone,
                    constraints: params.constraints,
                    sourceOfUpdate: FastProperty.SOURCE_OF_UPDATE,
                    cmcTicket: userContext.ticket
            )
            if (cmd.ttl) {
                fastProperty.setTtlInUnits(cmd.ttl, params.ttlUnit)
            }
            fastProperty.validate()
            fastProperty = fastPropertyService.create(userContext, fastProperty)
            flash.message = "Fast Property '${fastProperty.key}' has been created. The change may take a while to \
propagate."
            redirect(action: 'show', id: fastProperty.id)
        } catch (Exception e) {
            flash.message = e.message ?: e.cause?.message
            params.hasAdvancedAttributes = fastProperty?.hasAdvancedAttributes() ? 'true' : ''
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
        Map details = [
                updatedBy: params.updatedBy ?: userContext.username ?: fastProperty.updatedBy,
                fastPropertyInfoUrl: configService.fastPropertyInfoUrl,
                fastProperty: fastProperty
        ]
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
            redirect(action: 'show', id: id)
        } catch (Exception e) {
            flash.message = "Unable to update Fast Property '${id}': ${e}"
            redirect(action: 'edit', id: id)
        }
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

class FastPropertySaveCommand {
    Integer ttl
}
