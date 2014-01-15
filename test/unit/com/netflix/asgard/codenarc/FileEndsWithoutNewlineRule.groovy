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

import org.codenarc.rule.AbstractRule
import org.codenarc.source.SourceCode

/**
 * Makes sure the source code file ends with a newline character.
 */
class FileEndsWithoutNewlineRule extends AbstractRule {

    String name = 'FileEndsWithoutNewline'
    int priority = 3

    /**
     * Apply the rule to the given source, writing violations to the given list.
     * @param sourceCode The source to check
     * @param violations A list of Violations that may be added to. It can be an empty list
     */
    @Override
    void applyTo(SourceCode sourceCode, List violations) {
        if (!sourceCode.text.endsWith('\n')) {
            violations.add(createViolation(sourceCode.lines.size() - 1, sourceCode.lines[-1],
                    "File $sourceCode.name does not end with a newline"))
        }
    }
}
