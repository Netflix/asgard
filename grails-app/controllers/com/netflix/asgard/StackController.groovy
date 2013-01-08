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

import com.netflix.asgard.model.StackAsg
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class StackController {

    def applicationService
    def awsAutoScalingService
    Caches caches
    def configService

    def index = { redirect(action: 'list', params: params) }

    def list() {
        UserContext userContext = UserContext.of(request)
        Set<String> asgNames = awsAutoScalingService.getAutoScalingGroups(userContext)*.autoScalingGroupName
        Map<String, List<String>> stackToAsgNames = asgNames.groupBy { Relationships.stackNameFromGroupName(it) }
        List<String> allStackNames = stackToAsgNames.keySet().sort() - ['']
        List<String> significantStackNames = allStackNames.findAll { it in configService.significantStacks }
        def details = [
                allStackNames: allStackNames,
                significantStackNames: significantStackNames,
                stackToAsgNames: stackToAsgNames
        ]
        withFormat {
            html { return details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    def show(String id) {
        UserContext userContext = UserContext.of(request)
        List<StackAsg> stackAsgs = awsAutoScalingService.getStack(userContext, id)
        List<String> registeredAppNames = applicationService.getRegisteredApplications(userContext)*.name
        def details = [
                buildServer: configService.buildServerUrl,
                stackAsgs: stackAsgs.sort { it.group.autoScalingGroupName },
                registeredAppNames: registeredAppNames,
                isSignificantStack: id in configService.significantStacks
        ]
        withFormat {
            html { return details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

}
