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

@SuppressWarnings('ConsecutiveBlankLines')
class ConsecutiveBlankLinesRuleTest extends AbstractRuleTestCase {

    @Test
    void testRuleProperties() {
        assert rule.priority == 3
        assert rule.name == 'ConsecutiveBlankLines'
    }

    @Test
    void testSuccessScenario() {
        final SOURCE = '''
            class MyClass {

                    def go() { /* ... */ }
                    def goSomewhere() { /* ... */ }

                    def build() { /* ... */ }
                    def buildSomething() { /* ... */ }

            }
        '''
        assertNoViolations(SOURCE)
    }

    @Test
    void testClassStartsWithDoubleBlankLines() {
        final SOURCE = '''
            class MyClass {


                    void go() { /* ... */ }
            }
        '''
        assertSingleViolation(SOURCE, 3, '', "File null has consecutive blank lines")
    }

    @Test
    void testFileStartsWithDoubleBlankLines() {
        final SOURCE = '''

            class MyClass {
                    void go() { /* ... */ }
            }
        '''
        assertSingleViolation(SOURCE, 1, '', "File null has consecutive blank lines")
    }

    @Test
    void testDoubleBlankLinesBetweenMethods() {
        final SOURCE = '''
            class MyClass {
                    void go() { /* ... */ }


                    void stop() { /* ... */ }


                    void run() { /* ... */ }
            }
        '''
        assertTwoViolations(SOURCE, 4, '', 7, '')
    }

    @Test
    void testTripleBlankLines() {
        final SOURCE = '''
            class MyClass {
                    void go() { /* ... */ }



                    void stop() { /* ... */ }
            }
        '''
        assertTwoViolations(SOURCE, 4, '', 5, '')
    }

    protected Rule createRule() {
        new ConsecutiveBlankLinesRule()
    }
}
