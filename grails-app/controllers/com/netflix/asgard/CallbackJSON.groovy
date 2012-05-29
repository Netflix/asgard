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
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException

/**
 * Wrapper for the Grails JSON renderer that adds support for JSONP callback wrapped
 * JSON. The callback is detected from the given request param, and enabled automatically.
 */
class CallbackJSON extends JSON {
    def targetX   // X: don't collide with the superclass field
    def callback  // wrap with callback if non-empty
    public CallbackJSON(Object target, callback) {
        super(target)
        this.targetX = target
        this.callback = callback
    }
    public void render(Writer out) throws ConverterException {
        if (callback && callback != '') {
            out.write("${callback}(")
            prepareRender(out)
            try {
                value(this.targetX)
            }
            finally {
                out.write(')')
                finalizeRender(out)
            }
        } else {
            super.render(out)
        }
    }
}
