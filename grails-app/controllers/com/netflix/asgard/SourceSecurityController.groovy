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

import com.amazonaws.services.elasticloadbalancing.model.SourceSecurityGroup
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class SourceSecurityController {

    def awsLoadBalancerService

    def list() {
        UserContext userContext = UserContext.of(request)
        Collection<SourceSecurityGroup> groups = awsLoadBalancerService.getSourceSecurityGroups(userContext)
        groups = groups.sort { it.groupName }

        withFormat {
            html { [sourceSecurityGroups: groups] }
            xml { new XML(groups).render(response) }
            json { new JSON(groups).render(response) }
        }
    }
}
