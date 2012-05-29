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


class FlagService {

    static transactional = false

    Map<String, String> getState(String key = null) {
        Map<String, String> state = [:]
        if (key) {
            state.put(key, System.getProperty(key))
        } else {
            for (String name in Flag.names()) {
                state.put(name, System.getProperty(name))
            }
        }
        state
    }

    Map<String, String> setFlag(String name, Boolean on) {
        if (Flag.names().contains(name)) {
            on ? System.setProperty(name, "true") : System.clearProperty(name)
            return getState(name)
        }
        [:]
    }

    Boolean isOn(Flag flag) {
        System.getProperty(flag.name()) != null
    }

    Boolean isOff(Flag flag) {
        !isOn(flag)
    }
}
