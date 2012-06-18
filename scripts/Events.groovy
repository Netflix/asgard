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
/**
 * Generate a property file to be sucked in via Config.groovy, which specifies certain build-time properties.
 * This block is run after every compile, so that the resultant file is available before the tests are run.
 * Ideally this would only run with kind=='compile' and not test too.
 */
eventCompileEnd = { kind ->
    String sourceVersionFile = "${classesDirPath}/sourceVersion.properties"
    ant.propertyfile(file: sourceVersionFile) {
        entry(key: 'scm.commit', value: System.getenv('GIT_COMMIT') ?: '')
        entry(key: 'build.id', value: System.getenv('BUILD_ID') ?: '')
        entry(key: 'build.number', value: System.getenv('BUILD_NUMBER') ?: '')
    }
}
