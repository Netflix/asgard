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
package com.netflix.asgard

import grails.converters.JSON

class FlagController {

    def flagService

    def index() {
        render flagService.getState() as JSON
    }

    def on() {
        flip(params.id, true)
    }

    def off() {
        flip(params.id, false)
    }

    private void flip(String name, Boolean setting) {
        render flagService.setFlag(name, setting) as JSON
    }
}
