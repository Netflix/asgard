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
import grails.test.mixin.TestFor
import org.junit.Before
import org.junit.Test
import org.springframework.web.context.request.RequestContextHolder

// http://stackoverflow.com/questions/1703952/grails-how-do-you-unit-test-a-command-object-with-a-service-injected-into-it
@TestFor(AutoScalingController)
class GroupCreateCommandTests {

    @Before
    void setUp() {
        MockUtils.prepareForConstraintsTests(GroupCreateCommand)
    }

    private GroupCreateCommand validateParams(Map params) {
        RequestContextHolder.resetRequestAttributes()
        TestUtils.setUpMockRequest(params)

        def cmd = new GroupCreateCommand(params)
        cmd.applicationService = Mocks.applicationService()
        cmd.awsAutoScalingService = Mocks.awsAutoScalingService()
        cmd.validate()
        return cmd
    }

    @Test
    void testEmptyAppNameIsNotValid() {
        GroupCreateCommand cmd = validateParams(appName: "")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "blank" == cmd.errors.appName
    }

    @Test
    void testBlankAppNameIsNotValid() {
        GroupCreateCommand cmd = validateParams(appName: "   ")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "blank" == cmd.errors.appName
    }

    @Test
    void testNullAppNameIsNotValid() {
        GroupCreateCommand cmd = validateParams(appName: null)
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "nullable" == cmd.errors.appName
    }

    @Test
    void testAppNameWithReservedFormatIsInvalid() {
        def cmd = validateParams(appName: "abcachev339")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "name.usesReservedFormat" == cmd.errors.appName
    }

    @Test
    void testAppNameWithBadCharacterIsNotValid() {
        GroupCreateCommand cmd = validateParams(appName: "uses-hyphen")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "application.name.illegalChar" == cmd.errors.appName
    }

    @Test
    void testUnknownAppNameIsValid() {
        def cmd = validateParams(appName: "never_heard_of_this_app")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "application.name.nonexistent" == cmd.errors.appName
    }

    @Test
    void testKnownAppNameIsValid() {
        def cmd = validateParams(appName: "abcache")
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    @Test
    void testEmptyStackIsValid() {
        def cmd = validateParams(appName: "abcache", stack:"")
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    @Test
    void testNullStackIsValid() {
        def cmd = validateParams(appName: "abcache", stack:null)
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    @Test
    void testStackIsValid() {
        def cmd = validateParams(appName: "abcache", stack:"iphone")
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    @Test
    void testStackIsInvalid() {
        def cmd = validateParams(appName: "abcache", stack:"iphone-and-ipad")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "The stack must be empty or consist of alphanumeric characters" == cmd.errors.stack
    }

    @Test
    void testStackWithReservedFormatIsInvalid() {
        def cmd = validateParams(appName: "abcache", stack:"v305")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "name.usesReservedFormat" == cmd.errors.stack
    }

    @Test
    void testEmptyNewStackIsValid() {
        def cmd = validateParams(appName: "abcache", newStack:"")
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    @Test
    void testNullNewStackIsValid() {
        def cmd = validateParams(appName: "abcache", newStack:null)
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    @Test
    void testNewStackIsValid() {
        def cmd = validateParams(appName: "abcache", newStack:"iphone")
        assert !cmd.hasErrors()
        assert 0 == cmd.errors.errorCount
    }

    @Test
    void testNewStackIsInvalid() {
        def cmd = validateParams(appName: "abcache", newStack:"iphone-and-ipad")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "stack.illegalChar" == cmd.errors.newStack
    }

    @Test
    void testNewStackWithReservedFormatIsInvalid() {
        def cmd = validateParams(appName: "abcache", newStack:"v305")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "name.usesReservedFormat" == cmd.errors.newStack
    }

    @Test
    void testStackAndNewStackIsInvalid() {
        def cmd = validateParams(appName: "abcache", stack:"iphone", newStack:"iphone")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "stack.matchesNewStack" == cmd.errors.newStack
    }

    @Test
    void testDetailWithReservedFormatIsInvalid() {
        def cmd = validateParams(appName: "abcache", stack:"iphone", detail:"v305")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "name.usesReservedFormat" == cmd.errors.detail
    }

    @Test
    void testHyphenatedDetailWithReservedFormatIsInvalid() {
        def cmd = validateParams(appName: "abcache", stack:"iphone", detail:"blah-v305")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert "name.usesReservedFormat" == cmd.errors.detail
    }

    @Test
    void testTotalNameIsTooLong() {
        def cmd = validateParams(appName: "videometadata", stack:"navigator",
                detail: "integration-240-usa-iphone-ipad-ios5-even-numbered-days-except-weekends-and-excluding-when-the-moon-is-full")
        assert cmd.hasErrors()
        assert 1 == cmd.errors.errorCount
        assert 'compoundName.invalid.max.size' == cmd.errors.appName
    }

}
