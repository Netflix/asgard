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

import javax.servlet.http.Cookie

/**
 * Adapted from http://grails.org/plugin/cookie
 */
class Cookies {

    /** Gets the value of the named cookie. Returns null if no matching cookie found. */
    static String get(String name) {
        Requests.request.cookies.find { it.name == name }?.value
    }

    /** Sets the cookie with name to value, with maxAge in seconds. */
    static void set(response, String name, String value, Integer maxAge) {
        Cookie cookie = new Cookie(name, value)
        cookie.setMaxAge(maxAge)
        cookie.setPath('/')
        response.addCookie(cookie)
    }

    /** Deletes the named cookie. */
    static void delete(response, String name) {
        set(response, name, null, 0)
    }
}
