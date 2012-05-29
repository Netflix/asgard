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

import org.apache.commons.codec.binary.Base64

class Ensure {

    static Integer bounded(Integer lower, Integer original, Integer upper) {
        Math.max(lower, Math.min(original, upper))
    }

    static String decoded(String input) {
        Base64.isArrayByteBase64(input.bytes) ? new String(input.decodeBase64()) : input
    }

    static String encoded(String input) {
        Base64.isArrayByteBase64(input.bytes) ? input : input.encodeAsBase64()
    }
}
