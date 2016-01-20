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
grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.work.dir = "target/work"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
grails.project.war.file = "target/${appName}.war"

codenarc {
    reports = {
        AsgardXmlReport('xml') {
            outputFile = 'CodeNarc-Report.xml'
            title = 'Asgard CodeNarc Report'
        }
        AsgardHtmlReport('html') {
            outputFile = 'CodeNarc-Report.html'
            title = 'Asgard CodeNarc Report'
        }
    }
    ruleSetFiles = 'file:grails-app/conf/CodeNarcRuleSet.groovy'
    maxPriority1Violations = 0
    maxPriority2Violations = 0
    maxPriority3Violations = 0
}

grails.project.fork = [
    // configure settings for compilation JVM, note that if you alter the Groovy version forked compilation is required
    //  compile: [maxMemory: 256, minMemory: 64, debug: false, maxPerm: 256, daemon:true],

    // configure settings for the test-app JVM, uses the daemon by default
    //test: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, daemon:true],
    // configure settings for the run-app JVM
    //run: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the run-war JVM
    //war: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the Console UI JVM
    //console: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256]
]

grails.project.dependency.resolver = "maven" // or ivy
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
        mavenLocal()
        grailsCentral()
        mavenCentral()
        mavenRepo "http://dl.bintray.com/spinnaker/spinnaker"
        mavenRepo "http://repo.grails.org/grails/repo/"
    }

    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes e.g.
        // runtime 'mysql:mysql-connector-java:5.1.29'
        // runtime 'org.postgresql:postgresql:9.3-1101-jdbc41'
        test "org.grails:grails-datastore-test-support:1.0.2-grails-2.4"

        // Grails Plugin Testing jar is need when running locally in offline mode to mock out the services.
        // runtime "org.grails:grails-plugin-testing:2.5.2"

        compile(
                // Ease of use library for Amazon Simple Workflow Service (SWF), e.g., WorkflowClientFactory
                'com.netflix.glisten:glisten:0.3',
        ) {
            // If Glisten is using a different AWS Java SDK we don't want to pick up the transitive dep by accident.
            transitive = false
        }

        compile(
                // Amazon Web Services programmatic interface. Transitive dependency of glisten, but also used directly.
                'com.amazonaws:aws-java-sdk:1.7.5',

                // Enables publication of a health check URL for deploying Asgard, and an on/off switch for activities.
                // Transitive dependencies include:
                // rxjava, archaius, ribbon, servo, netflix-commons, netflix-statistics, jersey, guava
                'com.netflix.eureka:eureka-client:1.1.127',

                // Transitive dependencies of eureka and aws-java-sdk, but also used for REST calls, e.g., HttpClient
                'org.apache.httpcomponents:httpcore:4.2',
                'org.apache.httpcomponents:httpclient:4.2.3',

                // Transitive dependency of aws-java-sdk, but also used for JSON marshalling, e.g., JsonProperty
                'com.fasterxml.jackson.core:jackson-core:2.1.1',
                'com.fasterxml.jackson.core:jackson-annotations:2.1.1',

                // Extra collection types and utilities, e.g., Bag
                'commons-collections:commons-collections:3.2.1',

                // Easier Java from of the Apache Foundation, e.g., WordUtils, DateUtils, StringUtils
                'commons-lang:commons-lang:2.4',

                // Easier Java from Joshua Bloch and Google, e.g., Multiset, ImmutableSet, Maps, Table, Splitter
                'com.google.guava:guava:14.0.1',

                // SSH calls to retrieve secret keys from remote servers, e.g., JSch, ChammelExec
                'com.jcraft:jsch:0.1.45',

                // Send emails about system errors and task completions
                'javax.mail:mail:1.4.3',

                // Better date API, e.g., DateTime
                'joda-time:joda-time:1.6.2',

                // Static analysis for Groovy code.
                'org.codenarc:CodeNarc:0.19',

                // This fixes ivy resolution issues we had with our transitive dependency on 1.4.
                'commons-codec:commons-codec:1.5',

                // Rules for AWS named objects, e.g., Names, AppVersion
                'com.netflix.frigga:frigga:0.11',

                // Groovy concurrency framework, e.g., GParsExecutorsPool, Dataflow, Promise
                'org.codehaus.gpars:gpars:1.0.0',

                // Used for JSON parsing of AWS Simple Workflow Service metadata.
                // Previously this was an indirect depencency through Grails itself, but this caused errors in some
                // Grails environments.
                'com.googlecode.json-simple:json-simple:1.1',

                // Spinnaker client is used to retrieve application metadata
                'com.netflix.spinnaker.client:spinnaker-client:0.11'
        ) { // Exclude superfluous and dangerous transitive dependencies
            excludes(
                    // Some libraries bring older versions of JUnit as a transitive dependency and that can interfere
                    // with Grails' built in JUnit
                    'junit',

                    'mockito-core',
            )
            }
    }

    plugins {
        // plugins for the build system only
        build ":tomcat:7.0.55.3" // or ":tomcat:8.0.22"

        // plugins for the compile step
        compile ":scaffolding:2.1.2"
        compile ':cache:1.1.8'
        // asset-pipeline 2.0+ requires Java 7, use version 1.9.x with Java 6
        compile ":asset-pipeline:2.2.3"

        // plugins needed at runtime but not for compilation
        runtime ":hibernate4:4.3.10" // or ":hibernate:3.6.10.18"
        runtime ":database-migration:1.4.0"
        runtime ":jquery:1.11.1"

        compile ":compress:0.4"
        compile ":context-param:1.0"
        compile ':shiro:1.2.1'
        compile ":standalone:1.1.1"

        runtime ":cors:1.0.4"

        test ':code-coverage:2.0.3-3'
    }
}
