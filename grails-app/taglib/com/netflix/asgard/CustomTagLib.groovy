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

import com.netflix.asgard.joke.FailureImage
import org.codehaus.groovy.runtime.StackTraceUtils

class CustomTagLib {

    def emailerService
    def jokeService

    def sendError = {
        try {
            def statusCode = request['javax.servlet.error.status_code']

            // If error is 404 then it's probably a user error or another app is requesting a stale file, so don't email
            if (statusCode != 404) {
                String errorMsg = request['javax.servlet.error.message']
                String referrer = request.getHeader('referer')
                String requestDump = request["originalRequestDump"] as String ?: Requests.stringValue(request)
                Exception exception = request["exception"] as Exception ?:
                        new Exception("Status Code: ${statusCode}, Error: ${errorMsg}, Referrer: ${referrer}")
                emailerService.sendExceptionEmail(requestDump, exception)
            }
        }
        catch (Exception e) {
            // If emailing an exception threw another Exception, just log it and give up
            log.error "Error sending email report", StackTraceUtils.sanitize(e)
        }
    }

    def exclaim = { out << jokeService.randomExclamation() }

    def excuse = { out << jokeService.randomExcuse() }

    def failureImage = {
        FailureImage failureImage = jokeService.randomFailureImage()
        String caption = failureImage.url ? """<figcaption>Creative Commons image by <a \
href="${failureImage.url}">${failureImage.owner}</a></figcaption>""" : ''
        out << """<figure class="failure"><img src="${failureImage.path}"/>${caption}</figure>"""
    }

    private final String firefoxBugMessage = 'There is a form bug in Firefox that makes some operations unsafe.\n' +
            'For now please use Chrome or Safari.'

    /**
     * This buttonSubmit tag is similar to actionSubmit. However, buttonSubmit create a <button> element, while
     * actionSubmit creates an <input> element. The button element allows for more rendering options in some browsers,
     * as well as allowing nested elements inside the button, such as images and paragraphs.
     *
     * Examples:
     *
     * <g:actionSubmit action="Edit" value="Some label for editing" />
     * HTML output:
     * <input type="submit" name="_action_edit" value="Some label for editing"/>
     *
     * Creates a submit button that submits to an action in the controller specified by the form action.
     * The name of the action attribute is translated into the action name, for example "Edit" becomes
     * "_action_edit" or "List People" becomes "_action_listPeople"
     * If the action attribute is not specified, the value attribute will be used as part of the action name.
     * If neither an action nor a value is specified, the action name will be "_action_".
     * This tag requires either a value or a body (inner html).
     *
     * <g:buttonSubmit action="Edit" value="Some label for editing" />
     * <g:buttonSubmit action="Edit">Some label for editing</g:buttonSubmit>
     * HTML output:
     * <button type="submit" name="_action_edit">Some label for editing</button>
     *
     * <g:buttonSubmit value="Edit" />
     * HTML output:
     * <button type="submit" name="_action_edit">Edit</button>
     */
    def buttonSubmit = { attrs, body ->
        attrs.tagName = "buttonSubmit"
        def innerBody = body()
        if (!attrs.value && !innerBody) {
            throwTagError("Tag [$attrs.tagName] requires either a [value] attribute or a body")
        }

        // add action
        def value = attrs.value ? attrs.remove('value') : null
        def action = attrs.action ? attrs.remove('action') : value

        def disabled = attrs.disabled ? attrs.remove('disabled') : null

        // Use the body or the value inside the button
        def inner = innerBody ?: value

        // Firefox has a sporadic, critical bug on some complex pages, when there are multiple select elements
        // with the same name (legal in all other browsers), if the user refreshes the page many times.
        // For now, disable Firefox use entirely until there is time to build a robust workaround.
        // Browser sniffing is not a good long-term solution, but denying Firefox use will protect Asgard users
        // from causing major outages while we build the workaround.
        // For our purposes, detecting the string "Firefox" is adequate.
        // See user agent strings at http://www.zytrax.com/tech/web/browser_ids.htm
        if (request.getHeader('user-agent')?.contains('Firefox')) {
            attrs.remove('title')
            out << "<button disabled=\"disabled\" title=\"${firefoxBugMessage}\" "
            outputAttributes(attrs)
            out << "><div>${inner}</div></button>"
            return
        }

        out << "<button type=\"submit\" name=\"_action_${action}\" "

        // Only add HTML disabled attribute if the value is true or disabled, not empty, missing, or false.
        if (disabled in ['true', 'disabled']) { out << 'disabled="disabled" ' }

        // process remaining attributes
        outputAttributes(attrs)

        // close tag
        out << '><div>'
        out << inner
        out << '</div></button>'
    }

    /**
     * Dump out attributes in HTML compliant fashion.
     * This utility is copied from org.codehaus.groovy.grails.plugins.web.taglib.FormTagLib in Grails 1.3.5
     * Extending that class caused runtime errors because of its declaration of the implicit 'out' variable
     */
    void outputAttributes(attrs) {
        attrs.remove('tagName') // Just in case one is left
        def writer = getOut()
        attrs.each { k, v ->
            writer << "$k=\"${v.encodeAsHTML()}\" "
        }
    }

    /**
     * Shows a styled version of an availability zone name, either in a specified tag or in a span tag by default.
     */
    def availabilityZone = { attrs, body ->
        String innerBody = body()
        String value = attrs.value ? attrs.remove('value') : null
        String tag = attrs.tag ?: 'span'

        // Use the body or the value inside the tag
        String inner = innerBody ?: value
        if (inner) {
            String styleClass = Styler.availabilityZoneToStyleClass(inner)
            out << "<${tag} class=\"${styleClass}\">${inner}</${tag}>"
        }
    }

    /**
     * Shows a question mark help icon with a specified class so JavaScript can find the icon and add tooltip behavior.
     */
    def tip = { attrs, body ->
        def innerBody = body()
        if (!attrs.value && !innerBody) {
            throwTagError("Tip tag requires either a [value] attribute or a body")
        }
        def value = attrs.value ? attrs.remove('value') : null

        String tipStyle = attrs.tipStyle ? attrs.remove('tipStyle') : null
        String dataTipStyleAttribute = tipStyle ? " data-tip-style=\"${tipStyle}\"" : ''

        // Use the value or the body of the tip tag as the tip content
        String imageElement = "<img src=\"${g.resource(dir: 'images/tango/16/apps', file: 'help-browser.png')}\" />"
        String templateElement = "<span class=\"template\">${innerBody ?: value}</span>"

        out << "<span class=\"tip\"${dataTipStyleAttribute}>${imageElement}${templateElement}</span>"
    }
}
