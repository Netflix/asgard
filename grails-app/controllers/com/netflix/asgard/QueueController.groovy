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
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class QueueController {

    def awsSqsService

    def allowedMethods = [save: 'POST', update: 'POST', delete: 'POST']

    def index = { redirect(action: 'list', params:params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        List<SimpleQueue> queues = (awsSqsService.getQueues(userContext) as List).sort { it.name?.toLowerCase() }
        Map details = ['queues': queues]
        withFormat {
            html { details }
            xml { new XML(details).render(response) }
            json { new JSON(details).render(response) }
        }
    }

    def create = {
        [:]
    }

    def save = {
        UserContext userContext = UserContext.of(request)
        String queueName = params.id
        Integer visibilityTimeout = params.visibilityTimeout as Integer
        Integer delay = params.delay as Integer
        try {
            awsSqsService.createQueue(userContext, queueName, visibilityTimeout, delay)
            flash.message = "Queue '${queueName}' has been created."
            redirect(action: 'show', params:[id:queueName])
        } catch (Exception e) {
            flash.message = "Could not create Queue: ${e}"
            redirect(action: 'list')
        }
    }

    def delete = {
        UserContext userContext = UserContext.of(request)
        String queueName = params.id
        try {
            awsSqsService.deleteQueue(userContext, queueName)
            flash.message = "Queue '${queueName}' has been deleted."
        } catch (Exception e) {
            flash.message = "Could not delete Queue '${queueName}': ${e}"
        }
        redirect(action: 'list')
    }

    def show = {
        UserContext userContext = UserContext.of(request)
        String queueName = params.id
        SimpleQueue queue = awsSqsService.getQueue(userContext, queueName)
        if (!queue) {
            Requests.renderNotFound('Queue', queueName, this)
        } else {
            Map result = [queue: queue]
            withFormat {
                html { return result }
                xml { new XML(result).render(response) }
                json { new JSON(result).render(response) }
            }
        }
    }

    def edit = {
        UserContext userContext = UserContext.of(request)
        String queueName = params.id
        SimpleQueue queue = awsSqsService.getQueue(userContext, queueName)
        [queue: queue]
    }

    def update = {
        String queueName = params.name
        Integer visibilityTimeout = params.visibilityTimeout as Integer
        Integer delay = params.delay as Integer
        Integer retention = params.retention as Integer
        UserContext userContext = UserContext.of(request)
        try {
            awsSqsService.updateQueue(userContext, queueName, visibilityTimeout, delay, retention)
            flash.message = "Queue '${queueName}' has been updated."
        } catch (Exception e) {
            flash.message = "Failed to update Queue '${queueName}': ${e}"
        }
        redirect(action: 'show', params:[id:queueName])
    }
}
