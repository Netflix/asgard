/*
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.asgard.cred

/**
 * Abstraction layer for direct local file access, to enable testability of other classes.
 */
class LocalFileReader {

    /**
     * Reads the first line of a local file.
     *
     * @param directory the directory where the file resides
     * @param fileName the name of the file
     * @return the first line of the file's contents
     */
    String readFirstLine(String directory, String fileName) {
        new File("${directory}/${fileName}").readLines()[0]
    }

    /**
     * Opens an input stream for a specified file path.
     *
     * @param filePath the path to the file
     * @return a {@FileInputStream} open for the specified file
     */
    InputStream openInputStreamForFilePath(String filePath) {
        new FileInputStream(new File(filePath))
    }
}
