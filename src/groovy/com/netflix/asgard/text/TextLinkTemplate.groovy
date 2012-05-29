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
package com.netflix.asgard.text

import com.netflix.asgard.Check
import groovy.text.SimpleTemplateEngine
import groovy.text.Template

/**
 * Used to specify a customized formula for building a link based on a variable server name and a known URL pattern.
 */
class TextLinkTemplate {

    /**
     * The required variable name to replace in the link template
     */
    private static final VARIABLE = 'server'

    /**
     * The formula for creating the URL of the link
     */
    final String urlTemplate

    /**
     * The display text of the link
     */
    final String text

    /**
     * Creates a TextLinkTemplate with a dynamic url template that must contain the expected ${server} variable
     *
     * @param urlTemplate a non-GString String containing a server variable in GString syntax with brackets, to be
     *          used for building a URL
     * @param text the display text of the link
     */
    TextLinkTemplate(String urlTemplate, String text) {
        String msg = "Cannot create link template for ${urlTemplate} because it lacks a \${${VARIABLE}} variable"
        Check.condition(msg, { urlTemplate.contains("\${${VARIABLE}}") })
        this.urlTemplate = urlTemplate
        this.text = text
    }

    /**
     * Builds a TextLink combining the specified server name and pre-defined URL template.
     *
     * @param server the server name to use as the dynamic part of the URL template
     * @return TextLink the resulting URL and display text for the given server name
     */
    TextLink makeLinkForServer(String server) {
        Template template = new SimpleTemplateEngine().createTemplate(urlTemplate)
        String url = template.make([(VARIABLE): server]).toString()
        new TextLink(url, text)
    }
}
