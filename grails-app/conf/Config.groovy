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

import com.netflix.asgard.model.HardwareProfile
import com.netflix.asgard.model.InstanceTypeData
import org.apache.log4j.DailyRollingFileAppender


// http://grails.org/doc/latest/guide/3.%20Configuration.html#3.1.2 Logging
log4j = {

    appenders {
        def catalinaBase = System.properties.getProperty('catalina.base') ?: '.'
        def logDirectory = "${catalinaBase}/logs"

        appender new DailyRollingFileAppender(
                name: 'asgardrolling',
                file: "${logDirectory}/asgard.log",
                layout: pattern(conversionPattern: '[%d{ISO8601}] [%t] %c{4}    %m%n'),
                datePattern: "'.'yyyy-MM-dd")

        rollingFile name: "stacktrace", maxFileSize: 1024,
                file: "${logDirectory}/stacktrace.log"
    }

    root {
        info 'asgardrolling'
    }

    // Set level for all application artifacts
    info 'grails.app'

    // Set for all taglibs
    info "grails.app.tagLib"

    // Set for all controllers
    info "grails.app.controller"

    // Set for a specific controller
    debug "grails.app.controller.com.netflix.asgard.ServerController"

    // Set for a specific service
    debug "grails.app.service.com.netflix.asgard.ServerService"

    // Debug issue with occasional missing ELBs when creating the next ASG in a cluster.
    debug 'com.netflix.asgard.ClusterController'
    debug 'com.netflix.asgard.push.GroupCreateOperation'

    warn 'org.codehaus.groovy.grails'

    // Set this to debug to watch the XML communications to and from Amazon
    error 'org.apache.http.wire'

    // Suppress most noise from libraries
    error 'com.amazonaws', 'grails.spring', 'net.sf.ehcache', 'org.springframework', 'org.hibernate',
            'org.apache.catalina', 'org.apache.commons', 'org.apache.coyote', 'org.apache.http.client.protocol',
            'org.apache.jasper', 'org.apache.tomcat', 'org.codehaus.groovy.grails'

    // Avoid odd extra log4j error message during grails test-app initialization
    error 'com.amazonaws.services.simpleworkflow.flow.worker.DecisionTaskPoller'

    environments {
        def devConfig = {
            console name: 'stdout', layout: pattern(conversionPattern: '[%d{ISO8601}] %c{4}    %m%n')
            root {
                info 'stdout'
            }
        }
        development devConfig
        mcetestLocalDev devConfig
    }
}

asgardHome = System.getenv('ASGARD_HOME') ?: System.getProperty('ASGARD_HOME') ?:
        "${System.getProperty('user.home')}/.asgard"

println "Using ${asgardHome} as ASGARD_HOME"

appConfigured = new File(asgardHome, 'Config.groovy').exists()

// Locations to search for config files that get merged into the main config.
// Config files can either be Java properties files or ConfigSlurper scripts.
grails.config.locations = [
        "file:${asgardHome}/Config.groovy",
        'classpath:sourceVersion.properties'
]

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }
grails.app.context = '/'
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.types = [
        html: ['text/html', 'application/xhtml+xml'],
        xml: ['text/xml', 'application/xml'],
        json: ['application/json', 'text/json'],
        js: 'text/javascript',
        rss: 'application/rss+xml',
        atom: 'application/atom+xml',
        text: 'text-plain',
        css: 'text/css',
        csv: 'text/csv',
        all: '*/*',
        form: 'application/x-www-form-urlencoded',
        multipartForm: 'multipart/form-data'
]

// The default codec used to encode data with ${}
grails.views.default.codec = 'none' // none, html, base64
grails.views.gsp.encoding = 'UTF-8'

// Configuration for JSON and XML converters
grails.converters.encoding = 'UTF-8'
grails.converters.default.pretty.print = true
grails.converters.json.default.deep = false
grails.converters.xml.default.deep = false

// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true

grails.exceptionresolver.params.exclude = ['password', 'j_password']

grails.databinding.convertEmptyStringsToNull = false

thread {
    useJitter = true
}

cloud {
    applicationsDomain = 'CLOUD_APPLICATIONS'

    throttleMillis = 400

    spot.infoUrl = 'http://aws.amazon.com/ec2/spot-instances/'
}

cors.allow.origin.regex = '$.^' // Disable CORS support by default

healthCheck {
    minimumCounts {
        allAutoScalingGroups = 0
        allLaunchConfigurations = 0
        allClusters = 0
        allImages = 0
        allInstances = 0
        allSnapshots = 0
        allVolumes = 0
        allSecurityGroups = 0
        allLoadBalancers = 0
    }
}

plugin {
    taskFinishedListeners = ['snsTaskFinishedListener']
}

promote {
    imageTags = false
}

ticket {
    label = 'Ticket'
}

server {
    online = true
    otherServerNamePortCombos = System.getProperty('otherServers')?.split(',') ?: []
}

environments {
    development {
        server.online = !System.getProperty('offline')
        if (!server.online) {
            println 'Config: working offline'
        }
        plugin {
            refreshDelay = 5000
        }
        workflow.taskList = "asgard_${System.getProperty('user.name')}"
    }
    test {
        if (System.getProperty('regressionSuite') == 'true') {
            server.online = true
        } else {
            server.online = false
        }
    }
    production {
        cloud {
            envStyle = 'prod'
        }
    }
}
