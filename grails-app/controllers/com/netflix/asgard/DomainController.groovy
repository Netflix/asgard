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

import com.amazonaws.services.simpledb.model.DomainMetadataResult
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class DomainController {

    def simpleDbDomainService

    def allowedMethods = [save: 'POST', delete: 'POST']

    def index() {
        redirect(action: 'list', params: params)
    }

    def list() {
        UserContext userContext = UserContext.of(request)
        List<String> domains = (simpleDbDomainService.getDomains(userContext) as List).sort { it.toLowerCase() }
        Map details = ['domains': domains]
        withFormat {
            html { details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    def create() {
        [simpleDbUsageMessage: grailsApplication.config.cloud.simpleDbUsageMessage]
    }

    def save() {
        UserContext userContext = UserContext.of(request)
        String domainName = params.name
        try {
            simpleDbDomainService.createDomain(userContext, domainName)
            String msg = "SimpleDB domain '${domainName}' has been created."
            if (grailsApplication.config.cloud.simpleDbDomainCreatedMessage &&
                    grailsApplication.config.cloud.accountName in grailsApplication.config.cloud.highRiskAccountNames) {
                msg += " ${grailsApplication.config.cloud.simpleDbDomainCreatedMessage}"
            }
            flash.message = msg
            redirect(action: 'show', params: [id: domainName])
        } catch (Exception e) {
            flash.message = "Could not create SimpleDB domain: ${e}"
            redirect(action: 'list')
        }
    }

    def delete() {
        UserContext userContext = UserContext.of(request)
        String domainName = params.name ?: params.id
        try {
            simpleDbDomainService.deleteDomain(userContext, domainName)
        } catch (Exception e) {
            flash.message = "Could not delete SimpleDB Domain '${domainName}': ${e}"
        }
        redirect(action: 'list')
    }

    def show() {
        UserContext userContext = UserContext.of(request)
        String domainName = params.id
        DomainMetadataResult result = simpleDbDomainService.getDomainMetadata(userContext, domainName)
        if (!result) {
            Requests.renderNotFound('SimpleDB Domain', domainName, this)
        } else {
            withFormat {
                html { return ['domainName': domainName, 'domainMetadata': result] }
                xml { new XML(result).render(response) }
                json { new JSON(result).render(response) }
            }
        }
    }
}
