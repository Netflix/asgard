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

import com.amazonaws.services.simpleworkflow.model.ActivityTypeDetail
import com.amazonaws.services.simpleworkflow.model.ActivityTypeInfo
import grails.converters.JSON
import grails.converters.XML

class ActivityTypeController {

    def awsSimpleWorkflowService

    def index = { redirect(action: 'list', params: params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        Collection<ActivityTypeInfo> activityTypeInfos = awsSimpleWorkflowService.getActivityTypes(userContext)
        withFormat {
            html { [activityTypeInfos: activityTypeInfos] }
            xml { new XML(activityTypeInfos).render(response) }
            json { new JSON(activityTypeInfos).render(response) }
        }
    }

    def show(String name, String version) {
        ActivityTypeDetail activityType = awsSimpleWorkflowService.getActivityTypeDetail(name, version)
        withFormat {
            html { return [activityType: activityType] }
            xml { new XML(activityType).render(response) }
            json { new JSON(activityType).render(response) }
        }
    }

}
