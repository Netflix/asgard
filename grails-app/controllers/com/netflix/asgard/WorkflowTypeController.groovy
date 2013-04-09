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

import com.amazonaws.services.simpleworkflow.model.WorkflowTypeDetail
import com.amazonaws.services.simpleworkflow.model.WorkflowTypeInfo
import grails.converters.JSON
import grails.converters.XML

class WorkflowTypeController {

    def awsSimpleWorkflowService

    def index = { redirect(action: 'list', params: params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        List<WorkflowTypeInfo> workflowTypes =
                awsSimpleWorkflowService.getWorkflowTypes(userContext).sort { it.workflowType.name }
        withFormat {
            html { [workflowTypeInfos: workflowTypes] }
            xml { new XML(workflowTypes).render(response) }
            json { new JSON(workflowTypes).render(response) }
        }
    }

    def show(String name, String version) {
        WorkflowTypeDetail workflowType = awsSimpleWorkflowService.getWorkflowTypeDetail(name, version)
        withFormat {
            html { return [workflowType: workflowType] }
            xml { new XML(workflowType).render(response) }
            json { new JSON(workflowType).render(response) }
        }
    }
}
