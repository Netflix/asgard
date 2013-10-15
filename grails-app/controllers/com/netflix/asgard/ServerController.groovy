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

import com.netflix.asgard.server.ServerState
import com.netflix.asgard.server.SwitchAttemptResult
import grails.converters.JSON
import grails.converters.XML

class ServerController {

    def serverService
    def taskService

    def index = { render InetAddress.localHost.hostName }

    def all = {
        List<ServerState> serverStates = serverService.generateAllServersReport()
        List<Map> report = serverStates.collect { Meta.toMap(it) }
        def output = flash.messages ? [result: flash.messages, servers: report] : report
        withFormat {
            html { [output: output] }
            json { new JSON(output).render(response) }
            xml { new XML(output).render(response) }
        }
    }

    def ip = { render InetAddress.localHost.hostAddress }

    def version = { render serverService.version }

    def build = { render "${grailsApplication.config.build.number}" }

    def change = { render "${grailsApplication.config.scm.commit}" }

    def waitingToMoveTraffic = { render "${serverService.isThisServerWaitingToMoveTraffic()}" }

    def env = { render "${grailsApplication.config.cloud.accountName}" }

    def hoursSinceStartup = { render "${serverService.getHoursSinceStartup()}" }

    def minutesSinceStartup = { render "${serverService.getMinutesSinceStartup()}" }

    def uptime = { render "${serverService.getUptimeString()}" }

    def moveTraffic = {
        String targetServer = pickServer(params)
        String forceNowValue = params.forceNow
        SwitchAttemptResult switchAttemptResult = serverService.moveTrafficTo(targetServer, forceNowValue)
        flash.messages = switchAttemptResult.messages
        redirect(action: 'all', params: [format: 'json'])
    }

    def startTrafficMover = {
        String targetServer = pickServer(params)
        serverService.startTrafficMover(targetServer)
        flash.messages = ['Started thread to move traffic after tasks finish on sister server']
        redirect(action: 'all', params: [format: 'json'])
    }

    def cancelTrafficMover = {
        flash.messages = serverService.cancelTrafficMover()
        redirect(action: 'all', params: [format: 'json'])
    }

    def runningTaskCount = {
        render taskService.getRunningInMemory().size() as String
    }

    /**
     * Displays all environment variables and system properties for debugging.
     */
    def props = {
        Map<String, String> envVars = System.getenv().sort { it.key.toLowerCase() }
        Map<String, String> systemProperties = [:]
        for (String name in System.properties.stringPropertyNames().sort { it.toLowerCase() }) {
            systemProperties.put(name, System.getProperty(name))
        }
        Map<String, Map<String, String>> output = [environmentVariables: envVars, systemProperties: systemProperties]
        withFormat {
            html { output }
            json { new JSON(output).render(response) }
            xml { new XML(output).render(response) }
        }
    }

    /**
     * By default the target server that will take traffic is the current server, but any server can switch traffic
     * for any of the primary servers. This is useful for testing and for when the primary servers are busy.
     *
     * @param params the servlet parameters for the request
     * @return String the server name to move traffic to
     */
    private String pickServer(Map params) {
        params.server ?: params.id ?: InetAddress.localHost.hostName
    }
}
