/*
 * Copyright 2013 Netflix, Inc.
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

import com.google.common.collect.Lists
import com.netflix.asgard.model.SubnetData
import com.netflix.asgard.model.Subnets
import grails.converters.JSON
import grails.converters.XML

class SubnetController {

    def awsEc2Service

    def index() {
        redirect(action: 'list', params: params)
    }

    def list() {
        UserContext userContext = UserContext.of(request)
        Subnets subnets = awsEc2Service.getSubnets(userContext)
        OrderBy<SubnetData> orderBy = new OrderBy<SubnetData>([{ it.availabilityZone }, { it.purpose }, { it.target }])
        Collection<SubnetData> allSubnets = Lists.newArrayList(subnets.allSubnets).sort(false, orderBy)
        withFormat {
            html { [subnets: allSubnets] }
            xml { new XML(allSubnets).render(response) }
            json { new JSON(allSubnets).render(response) }
        }
    }
}
