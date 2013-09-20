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

import com.netflix.asgard.server.Environment
import com.netflix.asgard.server.Server
import com.netflix.asgard.server.ServerState
import com.netflix.asgard.server.SwitchAttemptResult
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.Minutes
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormatterBuilder
import org.springframework.beans.factory.InitializingBean

class ServerService implements InitializingBean {

    static transactional = false

    def grailsApplication
    def emailerService
    def restClientService
    def secretService
    def sshService

    private DateTime serverStartupTime = new DateTime()
    private List<Environment> environments
    private String serverSuffix
    private List<Server> allServers
    private List<String> allServerNames
    private Thread trafficMover

    void afterPropertiesSet() {
        environments = grailsApplication.config.server.environments ?: []
        serverSuffix = grailsApplication.config.server.suffix ?: ''
        allServers = environments.collect { it.servers }.flatten().sort { Server s -> s.name } as List<Server>
        allServerNames = allServers*.name
    }

    List<String> cancelTrafficMover() {
        if (trafficMover) {
            trafficMover.interrupt()
            trafficMover = null
            return ['Cancelled']
        }
        return ['Traffic mover thread was not running']
    }

    void startTrafficMover(String targetServerName) {

        // If process is already running then give up
        if (trafficMover?.isAlive()) {
            throw new IllegalStateException("Traffic switching thread is already running.")
        }

        trafficMover = Thread.start("Switch traffic to ${targetServerName}") {
            SwitchAttemptResult switchAttemptResult = null
            Server sisterServer = identifySisterServer(targetServerName)
            while (!switchAttemptResult?.succeeded) {

                // If sister server does not respond with a task count, then retry until task count is 0.
                Integer taskCount = countTasks(sisterServer)
                if (taskCount == 0) {
                    switchAttemptResult = moveTrafficTo(targetServerName)
                }
                if (!switchAttemptResult?.succeeded) {
                    List<String> msgs = switchAttemptResult?.messages ?: ["${sisterServer.name} tasks = ${taskCount}"]
                    log.info("Failed to move traffic to ${targetServerName}: ${msgs}")
                    // Don't use Time.sleepCancellably here. This should not run in a task or throw CancelledException.
                    Thread.sleep(10 * 1000)
                }
            }
            String msg = "Traffic switched to ${targetServerName}"
            emailerService.sendSystemEmail(msg, "${Time.nowReadable()} ${msg}")
        }
    }

    SwitchAttemptResult moveTrafficTo(String targetServerName, String forceNowValue = null) {

        String forceCodeForThisHour = nowYearMonthDayHour()
        if (forceNowValue && forceNowValue != forceCodeForThisHour) {
            List<String> messages = ['Error: wrong forceNow URL parameter value.',
                    "Remove forceNow for safety or use forceNow=${forceCodeForThisHour}"]
            return new SwitchAttemptResult(false, messages)
        }

        // Is the target server one of the known servers whose traffic we can control?
        if (!allServerNames.contains(targetServerName)) {
            String msg = "You requested ${targetServerName} but only these servers can get traffic: ${allServerNames}"
            return new SwitchAttemptResult(false, [msg])
        }

        // Which environment is the server a part of?
        Environment env = identifyEnvironment(targetServerName)
        Server targetServer = env.servers.find { it.name == targetServerName }

        // If administrator is using force to ignore rules then it's OKAY to move traffic to target server.
        Boolean moveTrafficDespiteSafetyRules = forceNowValue == forceCodeForThisHour

        // Has the traffic switch already happened?
        String currentInUseServerName = determineActiveServerName(env)
        Boolean trafficIsAlreadyMoved = currentInUseServerName == targetServerName
        if (trafficIsAlreadyMoved && !moveTrafficDespiteSafetyRules) {
            List<String> messages = [
                    "Aborted because ${env.canonicalDnsName} is already pointing to ${targetServerName}",
                    "Add URL parameter forceNow=${forceCodeForThisHour} to move traffic anyway"
            ]
            return new SwitchAttemptResult(true, messages)
        }

        // Is the target server healthy enough for traffic?
        Boolean targetServerIsHealthy = checkHealth(targetServer)

        // Is the sister server sick? Is it running any tasks right now? Be careful: countTasks returns null on error.
        Server sisterServer = identifySisterServer(targetServerName)
        Integer sisterServerTaskCount = countTasks(sisterServer)
        Boolean sisterServerIsHealthy = checkHealth(sisterServer)
        Boolean sisterServerIsSick = !sisterServerIsHealthy
        Boolean sisterServerIsIdle = sisterServerTaskCount <= 0

        // If sister server is broken or idle and target server is healthy then it's OKAY to move traffic to target
        Boolean switchingFromSickOrIdleServerToHealthyServer = targetServerIsHealthy && (sisterServerIsSick ||
                sisterServerIsIdle)

        Boolean okayToMoveTraffic = moveTrafficDespiteSafetyRules || switchingFromSickOrIdleServerToHealthyServer

        if (okayToMoveTraffic) {
            String enableCommand = targetServer.enableCommand
            String disableCommand = sisterServer.disableCommand

            sendCommandToLoadBalancer enableCommand
            sendCommandToLoadBalancer disableCommand
            List<String> messages = [enableCommand, disableCommand]
            return new SwitchAttemptResult(true, messages)
        } else {
            List<String> messages = [
                    "Cancelled moving traffic from ${currentInUseServerName} to ${targetServerName}",
                    "${sisterServer.name} has health=${sisterServerIsHealthy} tasks=${sisterServerTaskCount}",
                    "${targetServerName} has health=${targetServerIsHealthy}",
                    "Add URL parameter forceNow=${forceCodeForThisHour} to move traffic anyway"
            ]
            return new SwitchAttemptResult(false, messages)
        }
    }

    private static final DateTimeFormatter YEAR_MONTH_DAY_HR = new DateTimeFormatterBuilder().appendYear(4, 4).
            appendLiteral('-').appendMonthOfYear(2).appendLiteral('-').appendDayOfMonth(2).appendLiteral('-').
            appendHourOfDay(2).toFormatter()

    private Integer checkUptimeMinutes(Server server) {
        assert server.name in allServerNames
        String url = "http://${server.name}${serverSuffix}/server/minutesSinceStartup"
        String minutesText = restClientService.getAsText(url, 1000)
        if (minutesText?.isInteger()) {
            return minutesText as Integer
        }
        null
    }

    private Integer findBuild(Server server) {
        assert server.name in allServerNames
        String buildText = restClientService.getAsText("http://${server.name}${serverSuffix}/server/build", 1000)
        if (buildText?.isInteger()) {
            return buildText as Integer
        }
        null
    }

    private Boolean checkHealth(Server server) {
        assert server.name in allServerNames
        restClientService.getResponseCode("http://${server.name}${serverSuffix}/healthcheck/caches") == 200
    }

    private String nowYearMonthDayHour() {
        YEAR_MONTH_DAY_HR.print(new DateTime())
    }

    private Environment identifyEnvironment(String targetServerName) {
        Environment env = environments.find { targetServerName in it.serverNames }
        assert env
        env
    }

    private Server identifySisterServer(String targetServerName) {
        Environment env = identifyEnvironment(targetServerName)
        Server sisterServer = env.servers.find { it.name != targetServerName }
        if (!sisterServer) {
            throw new IllegalArgumentException("Target server ${targetServerName} has no sister server")
        }
        return sisterServer
    }

    private sendCommandToLoadBalancer(String command) {
        String userName = secretService.loadBalancerUserName
        String password = secretService.loadBalancerPassword
        String loadBalancerServerName = grailsApplication.config.server.loadBalancerServerName
        sshService.call(userName, loadBalancerServerName, command, password)
    }

    private Integer countTasks(Server server) {
        assert server.name in allServerNames
        String runningCountUrl = "http://${server.name}${serverSuffix}/server/runningTaskCount"
        String countText = restClientService.getAsText(runningCountUrl, 3000)
        if (countText?.isInteger()) {
            return countText as Integer
        }
        null
    }

    private Boolean checkIfWaitingToMoveTraffic(Server server) {
        assert server.name in allServerNames
        String url = "http://${server.name}${serverSuffix}/server/waitingToMoveTraffic"
        String waitingText = restClientService.getAsText(url, 1000)
        waitingText?.toBoolean() ?: false
    }

    List<ServerState> generateAllServersReport() {

        Map<Environment, String> environmentsToActiveServerNames = [:]
        environments.each { Environment env ->
            environmentsToActiveServerNames.put(env, determineActiveServerName(env))
        }

        // For each server show traffic source, tasks, health, uptime minutes
        List<ServerState> serverDataMaps = allServers.collect { Server server ->
            Environment env = environments.find { server in it.servers }
            Boolean serverIsActive = server.name == environmentsToActiveServerNames[env]

            return new ServerState(
                    server: server.name,
                    build: findBuild(server),
                    traffic: serverIsActive ? env.canonicalDnsName : null,
                    tasks: countTasks(server),
                    healthy: checkHealth(server),
                    uptimeMinutes: checkUptimeMinutes(server),
                    waitingToMoveTraffic: checkIfWaitingToMoveTraffic(server)
            )
        }

        return serverDataMaps
    }

    Boolean isThisServerWaitingToMoveTraffic() {
        trafficMover?.isAlive() ?: false
    }

    /**
     * Gets the time the server has been up, expressed as a short string like '13s' or '6h 20m 3s'.
     *
     * @return the time the server has been up, as an abbreviated string
     */
    String getUptimeString() {
        Time.format(serverStartupTime, new DateTime())
    }

    Integer getMinutesSinceStartup() {
        Minutes.minutesBetween(serverStartupTime, new DateTime()).minutes
    }

    Integer getHoursSinceStartup() {
        Hours.hoursBetween(serverStartupTime, new DateTime()).hours
    }

    /**
     * Gets the Asgard version number.
     *
     * @return the version of Asgard that is currently running
     */
    String getVersion() {
        grailsApplication.metadata['app.version']
    }

    private String determineActiveServerName(Environment env) {
        restClientService.getAsText("http://${env.canonicalDnsName}${serverSuffix}/server", 1000)
    }
}
