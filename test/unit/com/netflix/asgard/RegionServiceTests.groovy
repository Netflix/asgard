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

import grails.test.GrailsUnitTestCase
import grails.test.MockUtils
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DefaultGrailsControllerClass
import org.codehaus.groovy.grails.commons.GrailsClass

class RegionServiceTests extends GrailsUnitTestCase {

    RegionService regionService
    static final List<Class> controllers = [HomeController, TaskController, InstanceController, VolumeController]
    static final GrailsClass[] controllerWrappers =
            controllers.collect { new DefaultGrailsControllerClass(it) } as GrailsClass[]

    protected void setUp() {
        super.setUp()
        MockUtils.mockLogging(RegionService)
        regionService = new RegionService()
        regionService.grailsApplication = new DefaultGrailsApplication()
        regionService.grailsApplication.metaClass.controllerClasses = controllerWrappers
        regionService.afterPropertiesSet()
    }

    protected void tearDown() {
        super.tearDown()
        regionService = null
    }

    void testIsControllerRegional() {
        assertFalse regionService.isControllerRegional('home')
        assertFalse regionService.isControllerRegional('task')
        assertFalse regionService.isControllerRegional(new HomeController())
        assertFalse regionService.isControllerRegional(new TaskController())
        assert regionService.isControllerRegional('volume')
        assert regionService.isControllerRegional('instance')
        assert regionService.isControllerRegional(new InstanceController())
        assert regionService.isControllerRegional(new VolumeController())
    }
}
