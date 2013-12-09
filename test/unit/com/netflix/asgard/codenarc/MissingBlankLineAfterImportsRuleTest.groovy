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

class MissingBlankLineAfterImportsRuleTest extends AbstractRuleTestCase {

    @Test
    void testRuleProperties() {
        assert rule.priority == 3
        assert rule.name == 'MissingBlankLineAfterImports'
    }

    @Test
    void testSuccessScenario() {
        final SOURCE = '''\
            package org.codenarc

            import org.codenarc.rule.Rule
            import org.codenarc.rule.StubRule

            class MyClass {
                    def go() { /* ... */ }
            }
            '''.stripIndent()
        assertNoViolations(SOURCE)
    }

    @SuppressWarnings('MissingBlankLineAfterImports')
    @Test
    void testNoLinesBetweenPackageAndImports() {
        final SOURCE = '''\
            package org.codenarc

            import org.codenarc.rule.Rule
            import org.codenarc.rule.StubRule
            class MyClass {
                    void go() { /* ... */ }
            }'''.stripIndent()
        assertSingleViolation(SOURCE, 4, 'class MyClass {', 'Missing blank line after imports in file null')
    }

    protected Rule createRule() {
        new MissingBlankLineAfterImportsRule()
    }
}
