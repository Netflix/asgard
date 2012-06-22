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

import com.netflix.asgard.cache.CacheInitializer
import grails.test.MockUtils
import org.springframework.context.ApplicationContext
import spock.lang.AutoCleanup
import spock.lang.Specification

@SuppressWarnings("GroovyPointlessArithmetic")
class InitServiceSpec extends Specification {

    static final String ASGARD_HOME = 'asgardtmp'

    @AutoCleanup('delete')
    File configFile = new File(ASGARD_HOME, 'Config.groovy') // only used for cleanup
    @AutoCleanup('delete')
    File asgardHome = new File(ASGARD_HOME)

    def applicationContext = Mock(ApplicationContext)
    def configService = Mock(ConfigService)
    ConfigObject config = new ConfigObject()
    def initService

    void setup() {
        MockUtils.mockLogging(InitService)
        initService = new InitService(applicationContext: applicationContext, configService: configService,
                grailsApplication: [config: config])
    }

    def 'should create config file'() {
        configService.asgardHome >> ASGARD_HOME
        ConfigObject newConfig = new ConfigObject()
        newConfig['test'] = 'testVal'
        CacheInitializer cacheInitializer = Mock(CacheInitializer)
        BackgroundProcessInitializer backgroundProcessInitializer = Mock(BackgroundProcessInitializer)
        applicationContext.getBeansOfType(CacheInitializer) >> [cacheInitializer: cacheInitializer]
        applicationContext.getBeansOfType(BackgroundProcessInitializer) >>
                [backgroundProcessInitializer: backgroundProcessInitializer]

        when:
        initService.writeConfig(newConfig)

        then:
        ConfigObject savedConfig = new ConfigSlurper().parse(new File("${ASGARD_HOME}/Config.groovy").toURI().toURL())
        newConfig == savedConfig
        config.appConfigured == true
        config.test == 'testVal'
        1 * cacheInitializer.initializeCaches()
        1 * backgroundProcessInitializer.initializeBackgroundProcess()
    }
}
