package com.netflix.asgard

import com.amazonaws.services.ec2.model.Subnet
import com.netflix.asgard.UserContext
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
