/*
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.asgard.codenarc

import org.codenarc.rule.AbstractRuleTestCase
import org.codenarc.rule.Rule
import org.junit.Test

class FileEndsWithoutNewlineRuleTest extends AbstractRuleTestCase {

    def skipTestThatUnrelatedCodeHasNoViolations
    def skipTestThatInvalidCodeHasNoViolations

    @Test
    void testRuleProperties() {
        assert rule.priority == 3
        assert rule.name == 'FileEndsWithoutNewline'
    }

    @Test
    void testSuccessScenario() {
        final SOURCE = '''
            class MyClass {
                    def go() { /* ... */ }
            }
        '''.stripIndent()
        assertNoViolations(SOURCE)
    }

    @Test
    void testClassStartsWithDoubleBlankLines() {
        final SOURCE = '''
            class MyClass {
                    void go() { /* ... */ }
            }'''
        assertSingleViolation(SOURCE, 3, '}', "File null does not end with a newline")
    }

    protected Rule createRule() {
        new FileEndsWithoutNewlineRule()
    }
}
