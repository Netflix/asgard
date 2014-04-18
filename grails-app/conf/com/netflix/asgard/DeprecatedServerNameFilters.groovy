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
package com.netflix.asgard

import com.netflix.asgard.server.DeprecatedServerNames

/**
 * Redirects a deprecated server name request to an equivalent URL on the canonical replacement server name.
 */
class DeprecatedServerNameFilters {

    DeprecatedServerNames deprecatedServerNames

    def filters = {
        all(controller: '*', action: '*') {
            before = {

                String replacementUrl = deprecatedServerNames.replaceDeprecatedServerName(request)
                if (replacementUrl) {
                    redirect(url: replacementUrl)
                    return false
                }

                // If the last value is falsy and there is no explicit return statement then this filter method will
                // return a falsy value and cause requests to fail silently.
                return true
            }
        }
    }
}
