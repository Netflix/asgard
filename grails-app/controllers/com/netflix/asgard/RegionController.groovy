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

import grails.converters.JSON
import grails.converters.XML

class RegionController {

    def configService

    def index() {
        chain(action: 'list', params: [format: request.format])
    }

    def list() {
        List<Map> details = []
        for (Region reg in Region.values()) {

            List<Map<String, String>> envs = grailsApplication.config.grails.awsAccounts.collect { String accountNum ->
                String env = grailsApplication.config.grails.awsAccountNames[accountNum]
                String discovery = configService.getRegionalDiscoveryServer(reg)
                [env: env, account: accountNum, discovery: discovery]
            }

            details += [
                    code: reg.code,
                    location: reg.location,
                    accounts: envs
            ]
        }
        withFormat {
            html { render("$details") }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }
}
