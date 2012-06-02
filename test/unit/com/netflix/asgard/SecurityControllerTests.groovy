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

import com.netflix.asgard.mock.Mocks
import grails.test.MockUtils
import org.junit.Assert
import org.junit.Before

class SecurityControllerTests {

    @Before
    void setUp() {
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
        Assert.assertNotNull attrs['editable']
    }

    void testShowNonExistent() {
        def p = controller.params
        p.name ='doesntexist'
        controller.show()
        assert '/error/missing' == view
        assert "Security Group 'doesntexist' not found in us-east-1 test" == controller.flash.message
    }

    void testSaveWithoutApp() {
        request.method = 'POST'
        def p = controller.params
        p.wrongParam = 'helloworld'
        def cmd = new SecurityCreateCommand()
        cmd.applicationService = Mocks.applicationService()
        cmd.validate()
        assert cmd.hasErrors()
        controller.save(cmd)
        assert '/security/create?wrongParam=helloworld' == response.redirectUrl
    }

    void testSaveSuccessfully() {
        request.method = 'POST'
        def p = controller.params
        p.appName = 'helloworld'
        p.detail ='indiana'
        p.description = 'Only accessible by Indiana Jones'
        SecurityCreateCommand cmd = new SecurityCreateCommand(appName: p.appName, detail: p.detail)
        cmd.applicationService = Mocks.applicationService()
        cmd.validate()
        assert !cmd.hasErrors()
        controller.save(cmd)
        assert '/security/show/helloworld-indiana' == response.redirectUrl
        assert controller.flash.message == "Security Group 'helloworld-indiana' has been created."
    }

}
