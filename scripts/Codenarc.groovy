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
/* This script came from the Grails CodeNarc plugin. We are using just the script so that we can control our dependency
 * on the CodeNarc library.
 */
import grails.util.GrailsUtil
import org.apache.tools.ant.BuildException

includeTargets << grailsScript('_GrailsCompile')

target('codenarc': 'Run CodeNarc') {
    depends(compile)

    runCodenarc()
}

private void runCodenarc() {
    ant.taskdef(name: 'codenarc', classname: 'org.codenarc.ant.CodeNarcTask')

    String configClassName = getBindingValueOrDefault('configClassname', 'BuildConfig')
    ConfigObject config = loadConfig(configClassName)

    List reports = getConfiguredReports(config)

    int maxPriority1Violations = getConfigInt(config, 'maxPriority1Violations', Integer.MAX_VALUE)
    int maxPriority2Violations = getConfigInt(config, 'maxPriority2Violations', Integer.MAX_VALUE)
    int maxPriority3Violations = getConfigInt(config, 'maxPriority3Violations', Integer.MAX_VALUE)
    String ruleSetFiles = config.ruleSetFiles ?:
        'rulesets/basic.xml,rulesets/exceptions.xml,rulesets/imports.xml,rulesets/grails.xml,rulesets/unused.xml'
    List includes = configureIncludes(config)
    boolean systemExitOnBuildException = getConfigBoolean(config, 'systemExitOnBuildException')

    configureCodeNarcPropertiesFile(config)

    println "Running CodeNarc ..."

    try {
        ant.codenarc(ruleSetFiles: ruleSetFiles,
                maxPriority1Violations: maxPriority1Violations,
                maxPriority2Violations: maxPriority2Violations,
                maxPriority3Violations: maxPriority3Violations) {
            reports.each { r ->
                report(type: r.type) {
                    r.each { key, value ->
                        if (key != 'type') {
                            option(name:key, value:value)
                        }
                    }
                }
            }
            fileset(dir: '.', includes: includes.join(','))
        }
    }
    catch (BuildException e) {
        if (systemExitOnBuildException) {
            println "FAILED -- ${e.message}"
            System.exit(1)
        }
        else {
            throw e
        }
    }

    def reportNames = reports.collect { report -> report.outputFile ?: report.type }
    println "CodeNarc finished; report(s) generated: ${reportNames}"
}

private ConfigObject loadConfig(String className) {
    ClassLoader classLoader = Thread.currentThread().contextClassLoader
    classLoader.addURL(new File(classesDirPath).toURL())

    try {
        return new ConfigSlurper(GrailsUtil.environment).parse(classLoader.loadClass(className)).codenarc
    }
    catch (ClassNotFoundException ignored) {
        return new ConfigObject()
    }
}

private getBindingValueOrDefault(String varName, Object defaultValue) {
    Map variables = binding.variables
    variables.containsKey(varName) ? getProperty(varName) : defaultValue
}

class ReportsDslDelegate {
    List reports = []
    def methodMissing(String name, args) {
        println "Adding report ${name}"
        assert args.size() == 2, "Report [${name}] must specify a report type(String or Class) and a Closure"
        assert args[0] instanceof String || args[0] instanceof Class, "Report [${name}] must specify a report type"
        assert args[0], "The report definition for [${name}] must specify the report type that is not empty or null"
        assert args[1] instanceof Closure, "Report name [${name}] must be followed by a Closure"
        def reportClosure = args[1]
        def report = new Expando()
        report.type = args[0] instanceof String ? args[0] : args[0].name
        reportClosure.delegate = report
        reportClosure.resolveStrategy = Closure.DELEGATE_FIRST
        reportClosure.call()
        reports << report.properties
    }
}

private List getConfiguredReports(config) {
    if (config.reports) {
        assert config.reports instanceof Closure, 'The reports property value must be a Closure'
        def closure = config.reports
        def delegate = new ReportsDslDelegate()
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = delegate
        closure.call()
        return delegate.reports
    }
    []
}

private void configureCodeNarcPropertiesFile(ConfigObject config) {
    final PROPERTIES_FILE_PROP = 'codenarc.properties.file'
    if (config.propertiesFile) {
        def propValue = "file:${config.propertiesFile}"
        System.setProperty(PROPERTIES_FILE_PROP, propValue)
    }
}

private int getConfigInt(config, String name, int defaultIfMissing) {
    def value = config[name]
    value instanceof Integer ? value : defaultIfMissing
}

private boolean getConfigBoolean(config, String name, boolean defaultValue = true) {
    def value = config[name]
    value instanceof Boolean ? value : defaultValue
}

private List configureIncludes(config) {
    List includes = []

    if (getConfigBoolean(config, 'processSrcGroovy')) {
        includes << 'src/groovy/**/*.groovy'
    }

    if (getConfigBoolean(config, 'processControllers')) {
        includes << 'grails-app/controllers/**/*.groovy'
    }

    if (getConfigBoolean(config, 'processDomain')) {
        includes << 'grails-app/domain/**/*.groovy'
    }

    if (getConfigBoolean(config, 'processServices')) {
        includes << 'grails-app/services/**/*.groovy'
    }

    if (getConfigBoolean(config, 'processTaglib')) {
        includes << 'grails-app/taglib/**/*.groovy'
    }

    if (getConfigBoolean(config, 'processUtils')) {
        includes << 'grails-app/utils/**/*.groovy'
    }

    if (getConfigBoolean(config, 'processTestUnit')) {
        includes << 'test/unit/**/*.groovy'
    }

    if (getConfigBoolean(config, 'processTestIntegration')) {
        includes << 'test/integration/**/*.groovy'
    }

    if (getConfigBoolean(config, 'processViews', false)) {
        includes << 'grails-app/views/**/*.gsp'
    }

    for (includeDir in config.extraIncludeDirs) {
        includes << "${includeDir}/**/*.groovy"
    }

    includes
}

try {
    // Required for Grails 1.3 and later
    setDefaultTarget('codenarc')
}
catch (MissingMethodException ignored) {
    // Ignore. Older versions of Groovy/Grails do not implement this method
}

