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
import grails.test.GrailsUnitTestCase
import grails.test.MockUtils

class SecurityCreateCommandTests extends GrailsUnitTestCase {

    def appService

    void setUp() {
        super.setUp()
        TestUtils.setUpMockRequest()
        MockUtils.prepareForConstraintsTests(SecurityCreateCommand)
        appService = Mocks.applicationService()
    }

    private SecurityCreateCommand validateParams(Map params) {
        def cmd = new SecurityCreateCommand(params)
        cmd.applicationService = appService
        cmd.validate()
        return cmd
    }

    void testEmptyAppNameIsNotValid() {
        SecurityCreateCommand cmd = validateParams(appName: "")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "blank" == cmd.errors.appName
    }

    void testBlankAppNameIsNotValid() {
        SecurityCreateCommand cmd = validateParams(appName: "   ")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "blank" == cmd.errors.appName
    }

    void testNullAppNameIsNotValid() {
        SecurityCreateCommand cmd = validateParams(appName: null)
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "nullable" == cmd.errors.appName
    }

    void testAppNameWithBadCharacterIsNotValid() {
        SecurityCreateCommand cmd = validateParams(appName: "uses-hyphen")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "application.name.illegalChar" == cmd.errors.appName
    }

    void testUnknownAppNameIsValid() {
        def cmd = validateParams(appName: "never_heard_of_this_app")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "application.name.nonexistent" == cmd.errors.appName
    }

    void testKnownAppNameIsValid() {
        def cmd = validateParams(appName: "abcache")
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    void testEmptyDetailIsValid() {
        def cmd = validateParams(appName: "abcache", detail:"")
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    void testNullDetailIsValid() {
        def cmd = validateParams(appName: "abcache", detail:null)
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    void testDetailIsValid() {
        def cmd = validateParams(appName: "abcache", detail:"iphone")
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    void testDetailIsInvalid() {
        def cmd = validateParams(appName: "abcache", detail:"iphone/and/ipad")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "The detail must be empty or consist of alphanumeric characters and hyphens" == cmd.errors.detail
    }

    void testTotalNameIsTooLong() {
        def cmd = validateParams(appName: "videometadata",
                detail:"integration-240-usa-iphone-ipad-ios5-even-numbered-days-except-weekends-and-excluding-when-the-moon-is-full")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "The complete name cannot exceed 96 characters" == cmd.errors.appName
    }
}
