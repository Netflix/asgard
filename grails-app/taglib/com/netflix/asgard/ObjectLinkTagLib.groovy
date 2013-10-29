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

import com.netflix.asgard.model.SimpleQueue
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib

class ObjectLinkTagLib extends ApplicationTagLib {

    def configService

    def linkObject = { attrs, body ->
        String objectId = attrs.remove('name')
        if (!objectId) { return }
        String objectType = attrs.remove('type')
        EntityType type = objectType ? EntityType.fromName(objectType) : EntityType.fromId(objectId)
        type?.entitySpecificLinkGeneration(attrs, objectId)
        attrs['class'] = type.name()
        attrs.controller = type.name()
        attrs.action = attrs.action ?: 'show'
        attrs.params = attrs.params ?: [id: objectId]
        def compact = attrs.compact ? attrs.remove('compact') : null
        attrs.title = compact ? "${objectId} ${type.displayName}" : type.linkPurpose
        String displayName = body() ?: objectId

        String linkText = compact ? '' : displayName
        def writer = getOut()
        writer << link(attrs, linkText)
    }

    /**
     * Creates a grails application link from a set of attributes. This
     * link can then be included in links, ajax calls etc. Generally used as a method call
     * rather than a tag eg.<br/>
     *
     * &lt;a href="${createLink(action:'list')}"&gt;List&lt;/a&gt;
     *
     * @attr controller The name of the controller to use in the link, if not specified the current controller will be linked
     * @attr action The name of the action to use in the link, if not specified the default action will be linked
     * @attr uri relative URI
     * @attr url A map containing the action,controller,id etc.
     * @attr base Sets the prefix to be added to the link target address, typically an absolute server URL. This overrides the behaviour of the absolute property, if both are specified.
     * @attr absolute If set to "true" will prefix the link target address with the value of the grails.serverURL property from Config, or http://localhost:&lt;port&gt; if no value in Config and not running in production.
     * @attr id The id to use in the link
     * @attr fragment The link fragment (often called anchor tag) to use
     * @attr params A map containing URL query parameters
     * @attr mapping The named URL mapping to use to rewrite the link
     * @attr event Webflow _eventId parameter
     */
    Closure createLink = { attrs ->

        String controller = attrs.controller ?: controllerName
        if (grailsApplication.controllerNamesToContextParams[(controller)].contains('region')) {
            // Get value for region parameter from tag attribute or request attribute. Ensure link has region parameter.
            String region = attrs.region ? attrs.remove('region') : request['region'].toString()
            if (attrs.params) {
                attrs.params.put('region', region)
            } else {
                attrs.params = ['region': region]
            }
        }
        super.createLink.call(attrs)
    }

    /**
     * Shows a styled version of an SNS subscription endpoint which will be a link if it is an SQS queue.
     */
    def snsSubscriptionEndpoint = { attrs, body ->
        String endpoint = body()
        SimpleQueue queue = SimpleQueue.fromArn(endpoint)
        String accountNumber = configService.awsAccountNumber
        if (queue?.accountNumber == accountNumber) {
            out << linkObject([
                    type: 'queue',
                    name: queue.name,
                    region: queue.region
            ], { endpoint })
        } else {
            out << endpoint
        }
    }
}
