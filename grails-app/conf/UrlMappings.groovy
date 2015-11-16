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
class UrlMappings {
    static mappings = {

        "/externalImage/$name**"(controller: 'externalImage')

        "/$controller/$action?/$id?(.$format)?" {
            constraints {
                // apply constraints here
            }
        }

        // Optionally allow the region in the URL to the left of the controller.
        "/$region/$controller/$action?/$id?(.$format)?" {
            constraints {
                // apply constraints here
            }
        }

        "/"(controller: 'home', action: 'index')

        // http://en.wikipedia.org/wiki/List_of_HTTP_status_codes
        "400"(view: '/error')
        "401"(view: '/error')
        "403"(view: '/error')
        "404"(view: '/error')
        "405"(view: '/error')
        "406"(view: '/error')
        "409"(view: '/error')
        "414"(view: '/error')
        "415"(view: '/error')
        "500"(view: '/error')
        "501"(view: '/error')
        "502"(view: '/error')
        "503"(view: '/error')
        "504"(view: '/error')
        "505"(view: '/error')
        "509"(view: '/error')
        "510"(view: '/error')

    }
}
