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

import org.apache.ivy.plugins.resolver.FileSystemResolver
import org.apache.ivy.plugins.resolver.URLResolver


grails.project.work.dir = 'work'
grails.project.class.dir = 'target/classes'
grails.project.test.class.dir = 'target/test-classes'
grails.project.test.reports.dir = 'target/test-reports'
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

grails.project.dependency.resolution = {
    // Inherit Grails' default dependencies
    inherits('global') {}

    log 'warn'

    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenCentral()

        // Optional custom repository for dependencies.
        Closure internalRepo = {
            String repoUrl = 'http://artifacts/ext-releases-local'
            String artifactPattern = '[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]'
            String ivyPattern = '[organisation]/[module]/[revision]/[module]-[revision]-ivy.[ext]'
            URLResolver urlLibResolver = new URLResolver()
            urlLibResolver.with {
                name = repoUrl
                addArtifactPattern("${repoUrl}/${artifactPattern}")
                addIvyPattern("${repoUrl}/${ivyPattern}")
                m2compatible = true
            }
            resolver urlLibResolver

            String localDir = System.getenv('IVY_LOCAL_REPO') ?: "${System.getProperty('user.home')}/ivy2-local"
            FileSystemResolver localLibResolver = new FileSystemResolver()
            localLibResolver.with {
                name = localDir
                addArtifactPattern("${localDir}/${artifactPattern}")
                addIvyPattern("${localDir}/${ivyPattern}")
            }
            resolver localLibResolver
        }
        // Comment or uncomment the next line to toggle the use of an internal artifacts repository.
        //internalRepo()
    }

    dependencies {

        compile(
                // Ease of use library for Amazon Simple Workflow Service (SWF), e.g., WorkflowClientFactory
                'com.netflix.glisten:glisten:0.3',
        ) {
            // If Glisten is using a different AWS Java SDK we don't want to pick up the transitive dep by accident.
            transitive = false
        }

        compile(
                // Amazon Web Services programmatic interface. Transitive dependency of glisten, but also used directly.
                'com.amazonaws:aws-java-sdk:1.8.3',

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

                // Call Perforce in process. Delete when user data no longer come from Perforce at deployment time.
                'com.perforce:p4java:2010.1.269249',

                // Rules for AWS named objects, e.g., Names, AppVersion
                'com.netflix.frigga:frigga:0.11',

                // Groovy concurrency framework, e.g., GParsExecutorsPool, Dataflow, Promise
                'org.codehaus.gpars:gpars:1.0.0',

                // Used for JSON parsing of AWS Simple Workflow Service metadata.
                // Previously this was an indirect depencency through Grails itself, but this caused errors in some
                // Grails environments.
                'com.googlecode.json-simple:json-simple:1.1'
        ) { // Exclude superfluous and dangerous transitive dependencies
            excludes(
                    // Some libraries bring older versions of JUnit as a transitive dependency and that can interfere
                    // with Grails' built in JUnit
                    'junit',

                    'mockito-core',
            )
        }

        // Spock in Grails 2.2.x http://grails.org/plugin/spock
        test "org.spockframework:spock-grails-support:0.7-groovy-2.0"

        // Optional dependency for Spock to support mocking objects without a parameterless constructor.
        test 'org.objenesis:objenesis:1.2'
    }

    plugins {
        compile ":hibernate:$grailsVersion"
        compile ":compress:0.4"
        compile ":context-param:1.0"
        compile ':shiro:1.1.4'
        compile ":standalone:1.1.1"

        runtime ":cors:1.0.4"

        // Spock in Grails 2.2.x http://grails.org/plugin/spock
        test(":spock:0.7") {
            exclude "spock-grails-support"
        }

        test ':code-coverage:1.2.5'

        build ":tomcat:$grailsVersion"
    }
}
