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
package com.netflix.asgard

/**
 * Controller for redirect destination page to explain why Firefox is not currently safe to use with Asgard.
 */
class FirefoxController {

    def warning() {
        render '''Please switch to Google Chrome or Safari. You appear to be using Firefox. A critical bug in Firefox
currently makes it unsafe to use with Asgard. The Firefox bug is sporadic but reproducible. It will take time for the
Asgard developers to provide a robust solution. In the meantime, please use Google Chrome or Safari for Asgard usage.'''
    }
}
