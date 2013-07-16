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

import com.amazonaws.services.simpleworkflow.model.DomainInfo
import grails.converters.JSON
import grails.converters.XML

class WorkflowDomainController {

    def awsSimpleWorkflowService

    def index = { redirect(action: 'list', params: params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        List<DomainInfo> domainInfos = awsSimpleWorkflowService.getDomains(userContext).sort { it.name }
        withFormat {
            html { [domainInfos: domainInfos] }
            xml { new XML(domainInfos).render(response) }
            json { new JSON(domainInfos).render(response) }
        }
    }

    def show = {
        UserContext userContext = UserContext.of(request)
        String name = params.id
        DomainInfo domain = awsSimpleWorkflowService.getDomain(userContext, name)
        if (!domain) {
            Requests.renderNotFound('Workflow Domain', name, this)
        } else {
            withFormat {
                html { return [domain: domain] }
                xml { new XML(domain).render(response) }
                json { new JSON(domain).render(response) }
            }
        }
    }
}
