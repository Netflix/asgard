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

import com.amazonaws.SDKGlobalConfiguration
import com.netflix.asgard.server.ServerState
import com.netflix.asgard.server.SwitchAttemptResult
import grails.converters.JSON
import grails.converters.XML

class ServerController {

    def serverService
    def taskService
    def witherService

    def index() {
        render InetAddress.localHost.hostName
    }

    def all() {
        List<ServerState> serverStates = serverService.generateAllServersReport()
        List<Map> report = serverStates.collect { Meta.toMap(it) }
        def output = flash.messages ? [result: flash.messages, servers: report] : report
        withFormat {
            html { [output: output] }
            json { new JSON(output).render(response) }
            xml { new XML(output).render(response) }
        }
    }

    def ip() {
        render InetAddress.localHost.hostAddress
    }

    def version() {
        render serverService.version
    }

    def build() {
        render "${grailsApplication.config.build.number}"
    }

    def change() {
        render "${grailsApplication.config.scm.commit}"
    }

    def waitingToMoveTraffic() {
        render "${serverService.isThisServerWaitingToMoveTraffic()}"
    }

    def env() {
        render "${grailsApplication.config.cloud.accountName}"
    }

    def hoursSinceStartup() {
        render "${serverService.getHoursSinceStartup()}"
    }

    def minutesSinceStartup() {
        render "${serverService.getMinutesSinceStartup()}"
    }

    def uptime() {
        render "${serverService.getUptimeString()}"
    }

    def moveTraffic() {
        String targetServer = pickServer(params)
        String forceNowValue = params.forceNow
        SwitchAttemptResult switchAttemptResult = serverService.moveTrafficTo(targetServer, forceNowValue)
        flash.messages = switchAttemptResult.messages
        redirect(action: 'all', params: [format: 'json'])
    }

    def startTrafficMover() {
        String targetServer = pickServer(params)
        serverService.startTrafficMover(targetServer)
        flash.messages = ['Started thread to move traffic after tasks finish on sister server']
        redirect(action: 'all', params: [format: 'json'])
    }

    def cancelTrafficMover() {
        flash.messages = serverService.cancelTrafficMover()
        redirect(action: 'all', params: [format: 'json'])
    }

    def runningTaskCount() {
        render taskService.getLocalRunningInMemory().size() as String
    }

    /**
     * Starts a thread that will wait until there are zero local in-memory tasks, and then will attempt to terminate
     * this Asgard instance within the instance's Auto Scaling Group.
     *
     * @see WitherService#startWither()
     */
    def startWither() {
        witherService.startWither()
        flash.messages = ['Started withering process to terminate current instance or ASG after tasks are drained']
        redirect(action: 'all', params: [format: 'json'])
    }

    /**
     * Aborts the current withering thread.
     */
    def cancelWither() {
        flash.messages = witherService.cancelWither()
        redirect(action: 'all', params: [format: 'json'])
    }

    /**
     * Displays all environment variables and system properties for debugging.
     */
    def props() {
        Properties sysProps = serverService.systemProperties
        Map<String, String> propsMap = sysProps.stringPropertyNames().sort { it.toLowerCase() }.collectEntries {
            [it, sysProps.getProperty(it)]
        }
        Map<String, String> envVars = serverService.environmentVariables.sort { it.key.toLowerCase() }
        // Hide known sensitive info such as AWS credentials
        List<String> keysToHide = [
            SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR, SDKGlobalConfiguration.ACCESS_KEY_SYSTEM_PROPERTY,
            SDKGlobalConfiguration.ALTERNATE_ACCESS_KEY_ENV_VAR, SDKGlobalConfiguration.SECRET_KEY_ENV_VAR,
            SDKGlobalConfiguration.SECRET_KEY_SYSTEM_PROPERTY, SDKGlobalConfiguration.ALTERNATE_SECRET_KEY_ENV_VAR
        ]
        for (String key in keysToHide) {
            for (Map map in [envVars, propsMap]) {
                if (map[key]) {
                    map[key] = '[hidden]'
                }
            }
        }
        Map<String, Map<String, String>> output = [environmentVariables: envVars, systemProperties: propsMap]
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
