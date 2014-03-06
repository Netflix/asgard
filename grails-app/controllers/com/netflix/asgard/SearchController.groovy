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
import grails.converters.XML
import org.apache.commons.lang.StringEscapeUtils

class SearchController {

    def searchService

    def allowedMethods = [save: 'POST', update: 'POST', delete: 'POST']

    def index() {
        UserContext userContext = UserContext.of(request)
        String query = params.q ?: ''
        Map<Region, Map<EntityType, List>> results = searchService.findResults(userContext, query)
        Map details = ['results': results]
        withFormat {
            html { details << ['query': StringEscapeUtils.escapeHtml(query)] }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

}
