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

import org.codehaus.groovy.ast.ImportNode
import org.codenarc.rule.AbstractRule
import org.codenarc.source.SourceCode

/**
 * Makes sure there is a blank line after the imports of a source code file.
 */
class MissingBlankLineAfterImportsRule extends AbstractRule {

    String name = 'MissingBlankLineAfterImports'
    int priority = 3

    @Override
    void applyTo(SourceCode sourceCode, List violations) {

        if (sourceCode.ast?.imports) {
            // Before Groovy 2.1.3 some ImportNode objects lack a way to get the line number, so just parse the text.
            // https://jira.codehaus.org/browse/GROOVY-6094
            List<String> lines = sourceCode.lines
            int lastImportLineNumber = lines.findLastIndexOf { it.trim().startsWith('import ') }
            if (lastImportLineNumber > -1 && !lines[lastImportLineNumber + 1].trim().isEmpty()) {
                violations.add(createViolation(lastImportLineNumber + 1, lines[lastImportLineNumber + 1],
                        "Missing blank line after imports in file $sourceCode.name"))
            }
        }
    }
}
