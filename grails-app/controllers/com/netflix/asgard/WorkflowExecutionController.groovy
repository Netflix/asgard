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

import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionInfo
import grails.converters.JSON
import grails.converters.XML

class WorkflowExecutionController {

    def awsSimpleWorkflowService

    def index = { redirect(action: 'list', params: params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        List<WorkflowExecutionInfo> closedExecutions =
            awsSimpleWorkflowService.getClosedWorkflowExecutions(userContext).sort { it.closeTimestamp }
        List<WorkflowExecutionInfo> openExecutions =
            awsSimpleWorkflowService.getOpenWorkflowExecutions(userContext).sort { it.startTimestamp }
        Map result = [
                closedWorkflowExecutions: closedExecutions,
                openWorkflowExecutions: openExecutions
        ]
        withFormat {
            html { result }
            xml { new XML(result).render(response) }
            json { new JSON(result).render(response) }
        }
    }
}
