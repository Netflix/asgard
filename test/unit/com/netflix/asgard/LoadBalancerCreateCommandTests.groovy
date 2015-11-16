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

import grails.test.MockUtils
import grails.test.mixin.TestFor
import org.junit.Before
import org.junit.Test

// http://stackoverflow.com/questions/1703952/grails-how-do-you-unit-test-a-command-object-with-a-service-injected
@TestFor(LoadBalancerController)
class LoadBalancerCreateCommandTests {

    def appService

    static doWithConfig(c) {
        c.grails.databinding.convertEmptyStringsToNull = false
    }

    @Before
    void setUp() {
        TestUtils.setUpMockRequest()
        MockUtils.prepareForConstraintsTests(LoadBalancerCreateCommand)
        def appServiceMock = mockFor(ApplicationService)
        appServiceMock.demand.getRegisteredApplicationForLoadBalancer { UserContext uc, String name ->
            name == 'abcache' ? new AppRegistration(name: name) : null
        }
        appService = appServiceMock.createMock()
    }

    private LoadBalancerCreateCommand validateParams(Map params) {
        LoadBalancerCreateCommand cmd = new LoadBalancerCreateCommand(params)
        cmd.applicationService = appService
        cmd.protocol1 = "HTTP"
        cmd.lbPort1 = cmd.lbPort1 ?: 80
        cmd.instancePort1 = cmd.instancePort1 ?: 80
        cmd.target = "HTTP:7001/healthcheck"
        cmd.interval = 10
        cmd.timeout = 5
        cmd.unhealthy = 2
        cmd.healthy = 10
        cmd.validate()
        cmd
    }

    @Test
    void testEmptyAppNameIsNotValid() {
        LoadBalancerCreateCommand cmd = validateParams(appName: "")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "blank" == cmd.errors.appName
    }

    @Test
    void testBlankAppNameIsNotValid() {
        LoadBalancerCreateCommand cmd = validateParams(appName: "   ")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "blank" == cmd.errors.appName
    }

    @Test
    void testNullAppNameIsNotValid() {
        LoadBalancerCreateCommand cmd = validateParams(appName: null)
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "nullable" == cmd.errors.appName
    }

    @Test
    void testAppNameWithBadCharacterIsNotValid() {
        LoadBalancerCreateCommand cmd = validateParams(appName: "uses-hyphen")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "application.name.illegalChar" == cmd.errors.appName
    }

    @Test
    void testUnknownAppNameIsValid() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "neverheardofthisapp")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "application.name.nonexistent" == cmd.errors.appName
    }

    @Test
    void testKnownAppNameIsValid() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache")
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    @Test
    void testEmptyStackIsValid() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache", stack:"")
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    @Test
    void testNullStackIsValid() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache", stack: null)
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    @Test
    void testStackIsValid() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache",
                stack: "iphone")
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    @Test
    void testStackIsInvalid() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache",
                stack: "iphone-and-ipad")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "The stack must be empty or consist of alphanumeric characters" == cmd.errors.stack
    }

    @Test
    void testEmptyNewStackIsValid() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache", newStack:"")
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    @Test
    void testNullNewStackIsValid() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache",
                newStack: null)
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    @Test
    void testNewStackIsValid() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache",
                newStack: "iphone")
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    @Test
    void testStackWithReservedFormatIsNotValid() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache", stack:"v293")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "name.usesReservedFormat" == cmd.errors.stack
    }

    @Test
    void testNewStackWithReservedFormatIsNotValid() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache",
                newStack:"v293")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "name.usesReservedFormat" == cmd.errors.newStack
    }

    @Test
    void testNewStackIsInvalid() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache",
                newStack: "iphone-and-ipad")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "stack.illegalChar" == cmd.errors.newStack
    }

    @Test
    void testStackAndNewStackIsInvalid() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache",
                stack: "iphone", newStack: "iphone")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "stack.matchesNewStack" == cmd.errors.newStack
    }

    @Test
    void testDetailWithReservedFormatIsInvalid() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache",
                detail: "v021")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "name.usesReservedFormat" == cmd.errors.detail
    }

    @Test
    void testPortNumbers() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache",
                newStack: "iphone", lbPort1: 99999999, instancePort1: 99999999)
        assert cmd.hasErrors()
        assert 2 == cmd.errors.errorCount
    }

    @Test
    void testIncompleteListener2() {
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache",
                newStack: "iphone", protocol2: "HTTP")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "Please enter port numbers for the second protocol" == cmd.errors.protocol2
    }

    @Test
    void testTotalNameIsTooLong() {
        def deets = "integration-24-usa-iphone-ios5-even-numbered-days-except-weekends-and-except-when-the-moon-is-full"
        LoadBalancerCreateCommand cmd = validateParams(applicationService: appService, appName: "abcache",
                stack: "navigator", detail: deets)
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "The complete load balancer name cannot exceed 96 characters" == cmd.errors.appName
    }

}
