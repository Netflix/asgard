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

import grails.test.ControllerUnitTestCase
import grails.test.MockUtils
import com.netflix.asgard.mock.Mocks

class SecurityControllerTests extends ControllerUnitTestCase {

    void setUp() {
        super.setUp()
        TestUtils.setUpMockRequest()
        MockUtils.prepareForConstraintsTests(SecurityCreateCommand)
        controller.awsEc2Service = Mocks.awsEc2Service()
        controller.applicationService = Mocks.applicationService()
        controller.metaClass.grailsApplication = Mocks.grailsApplication()
    }

    void testShow() {
        controller.params.name = 'helloworld'
        def attrs = controller.show()
        assert 'helloworld' == attrs['app'].name
        assert 'helloworld' == attrs['group'].groupName
        assert 'test' == attrs['accountNames']['179000000000']
        assertNotNull attrs['editable']
    }

    void testShowNonExistent() {
        def p = controller.params
        p.name ='doesntexist'
        controller.show()
        assert '/error/missing' == controller.renderArgs.view
        assert "Security Group 'doesntexist' not found in us-east-1 test" == controller.flash.message
    }

    void testSaveWithoutApp() {
        mockRequest.method = 'POST'
        def p = controller.params
        p.wrongParam = 'helloworld'
        def cmd = new SecurityCreateCommand()
        cmd.applicationService = Mocks.applicationService()
        cmd.validate()
        assert cmd.hasErrors()
        controller.save(cmd)
        assert controller.create == controller.chainArgs.action
        assert p == controller.chainArgs.params
        assert cmd == controller.chainArgs.model['cmd']
    }

    void testSaveSuccessfully() {
        mockRequest.method = 'POST'
        def p = mockParams
        p.appName = 'helloworld'
        p.detail ='indiana'
        p.description = 'Only accessible by Indiana Jones'
        SecurityCreateCommand cmd = new SecurityCreateCommand(appName: p.appName, detail: p.detail)
        cmd.applicationService = Mocks.applicationService()
        cmd.validate()
        assert !cmd.hasErrors()
        controller.save(cmd)
        assert controller.show == controller.redirectArgs.action
        assert 'helloworld-indiana' == controller.redirectArgs.params.id
        assert controller.flash.message == "Security Group 'helloworld-indiana' has been created."
    }

}
