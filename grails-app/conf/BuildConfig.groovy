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
grails.project.work.dir = 'work'
grails.project.class.dir = 'target/classes'
grails.project.test.class.dir = 'target/test-classes'
grails.project.test.reports.dir = 'target/test-reports'

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
    ruleSetFiles='file:grails-app/conf/CodeNarcRuleSet.groovy'
    maxPriority1Violations = 0
}

Closure lookForExternalBuildOptions = {
    // TODO: do not duplicate the asgard home logic in Config.groovy
    String asgardHome = System.getenv('ASGARD_HOME') ?: System.getProperty('ASGARD_HOME') ?: "${userHome}/.asgard"
    File optionsFile = new File(asgardHome, 'BuildOptions.groovy')
    if (optionsFile.exists()) {
        logger.println("Build options selected from ${optionsFile.absolutePath}")
        try {
            return new GroovyClassLoader().parseClass(optionsFile)?.newInstance()
        } catch (Exception e) {
            logger.println("Failed to load build options: ${e.message}")
        }
    }
}
def buildOptions = lookForExternalBuildOptions()

grails.project.dependency.resolution = {
    // Inherit Grails' default dependencies
    inherits('global') {}

    log 'warn'

    // Default to public repos for open source build
    Closure defaultRepositories = {
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenCentral()
    }

    repositories buildOptions?.repositories ?: defaultRepositories

    dependencies {

        compile(
                // Amazon Web Services programmatic interface
                buildOptions?.dependencyOverride?.awsSdk ?: 'com.amazonaws:aws-java-sdk:1.3.10',

                // Transitive dependencies of aws-java-sdk, but also used directly
                'org.apache.httpcomponents:httpcore:4.1',
                'org.apache.httpcomponents:httpclient:4.1.1',

                // Extra collection types and utilities
                'commons-collections:commons-collections:3.2.1',

                // Easier Java from of the Apache Foundation
                'commons-lang:commons-lang:2.4',

                // Easier Java from Joshua Bloch and Google
                'com.google.guava:guava:12.0',

                // SSH calls to retrieve secret keys from remote servers
                'com.jcraft:jsch:0.1.45',

                // Send emails about system errors and task completions
                'javax.mail:mail:1.4.1',

                // Better date API
                'joda-time:joda-time:1.6.2',

                // Delete when Amazon provides a proper instance type API. Web scraping API to parse poorly formed HTML.
                'org.jsoup:jsoup:1.6.1',

                // Static analysis for Groovy code.
                'org.codenarc:CodeNarc:0.17',

                // This fixes ivy resolution issues we had with our transitive dependency on 1.4.
                'commons-codec:commons-codec:1.5',
        ) { // Exclude superfluous and dangerous transitive dependencies
            excludes(
                    // Some libraries bring older versions of JUnit as a transitive dependency and that can interfere
                    // with Grails' built in JUnit
                    'junit',

                    'mockito-core',

                    'stax-api',
            )
        }

        buildOptions?.extraDependencies?.each { compile it }

        test 'org.objenesis:objenesis:1.2'
    }

    plugins {
        compile ":hibernate:$grailsVersion"
        compile ":compress:0.4"

        test ':spock:0.6'

        test ':code-coverage:1.2.5'

        build ":tomcat:$grailsVersion"
    }
}
