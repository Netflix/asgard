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
package com.netflix.asgard.mock

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONElement

/**
 * Utility methods for parsing html and json files in the mocks folder.
 */
class MockFileUtils {

    private static Map<String, JSONElement> fileNamesToJsonDocuments = [:]

    private static InputStream getFileAsStream(String fileName) {
        MockFileUtils.classLoader.getResourceAsStream("com/netflix/asgard/mock/${fileName}")
    }

    /**
     * Parses a json file in the mocks package using the built in Grails parser.
     *
     * @param filename to read and parse
     * @return parsed representation of the json file
     */
    static JSONElement parseJsonFile(String fileName) {
        if (fileNamesToJsonDocuments[fileName] == null) {
            InputStream stream = getFileAsStream(fileName)
            if (!stream) {
                throw new IllegalStateException("""Unable to read file ${fileName}.
  If you are running tests in IntelliJ you must add "js" and "json" as file extensions for the compiler.
  Open Preferences, click Compiler, add json and txt to Resource Patterns:
  '?*.properties;?*.xml;?*.gif;?*.png;?*.jpeg;?*.jpg;?*.html;?*.dtd;?*.tld;?*.ftl;?*.txt;?*.json;?*.js'""")
            }

            // Get the content as a string instead of a stream. Strip padding off JSONP if present.
            String content = stream.getText()
            JSONElement data = JSON.parse(content)
            fileNamesToJsonDocuments[fileName] = data
        }
        fileNamesToJsonDocuments[fileName]
    }
}
