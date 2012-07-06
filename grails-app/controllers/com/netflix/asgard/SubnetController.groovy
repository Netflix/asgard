/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.netflix.asgard

import com.amazonaws.services.ec2.model.Subnet
import grails.converters.JSON
import grails.converters.XML

class SubnetController {

    def awsEc2Service

    def index = { redirect(action:list, params:params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        Collection<Subnet> subnets = awsEc2Service.getSubnets(userContext)
        Map details = ['subnets': subnets]
        withFormat {
            html { details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }
}
