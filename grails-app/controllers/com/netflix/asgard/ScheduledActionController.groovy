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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup
import com.amazonaws.services.autoscaling.model.ScheduledUpdateGroupAction
import com.netflix.grails.contextParam.ContextParam
import grails.converters.JSON
import grails.converters.XML

@ContextParam('region')
class ScheduledActionController {

    def awsAutoScalingService
    Caches caches

    def allowedMethods = [save: 'POST', update: 'POST', delete: 'POST']

    def index = { redirect(action: 'list', params: params) }

    def list = {
        UserContext userContext = UserContext.of(request)
        List<ScheduledUpdateGroupAction> scheduledActions = awsAutoScalingService.getAllScheduledActions(userContext).sort { it.scheduledActionName }
        withFormat {
            html {
                [
                        scheduledActions: scheduledActions,
                ]
            }
            xml { new XML(scheduledActions).render(response) }
            json { new JSON(scheduledActions).render(response) }
        }
    }

    def create = {
        String groupName = params.id ?: params.group
        AutoScalingGroup group = awsAutoScalingService.getAutoScalingGroup(UserContext.of(request), groupName)
        if (group) {
            [
                    group: groupName,
                    start: params.start,
                    end: params.end,
                    recurrence: params.recurrence,
                    min: params.min,
                    max: params.max,
                    desired: params.desired,
            ]
        } else {
            flash.message = "Group '${groupName}' does not exist."
            redirect(action: 'result')
        }
    }

    def show = {
        UserContext userContext = UserContext.of(request)
        String name = params.id
        ScheduledUpdateGroupAction scheduledAction = awsAutoScalingService.getScheduledAction(userContext, name)
        if (scheduledAction) {
            Map result = [scheduledAction: scheduledAction]
            withFormat {
                html { result }
                xml { new XML(result).render(response) }
                json { new JSON(result).render(response) }
            }
        } else {
            Requests.renderNotFound('Scheduled Action', name, this)
        }
    }

    def edit = {
        UserContext userContext = UserContext.of(request)
        String name = params.id ?: params.actionName
        ScheduledUpdateGroupAction scheduledAction = awsAutoScalingService.getScheduledAction(userContext, name)
        if (scheduledAction) {
            [
                    name: name,
                    group: params.group ?: scheduledAction?.autoScalingGroupName,
                    recurrence: params.recurrence ?: scheduledAction?.recurrence,
                    min: params.min ?: scheduledAction?.minSize,
                    max: params.max ?: scheduledAction?.maxSize,
                    desired: params.desired ?: scheduledAction?.desiredCapacity,
            ]
        } else {
            flash.message = "Scheduled Action '${name}' does not exist."
            redirect(action: 'result')
        }
    }

    def delete = {
        UserContext userContext = UserContext.of(request)
        String name = params.id
        ScheduledUpdateGroupAction scheduledAction = awsAutoScalingService.getScheduledAction(userContext, name)
        if (scheduledAction) {
            awsAutoScalingService.deleteScheduledAction(userContext, scheduledAction)
            flash.message = "Scheduled Action '${name}' has been deleted."
            redirect(controller: 'autoScaling', action: 'show', params: [id: scheduledAction.autoScalingGroupName])
        } else {
            Requests.renderNotFound('Scheduled Action', name, this)
        }
    }

    def save = { ScheduledActionCommand cmd ->
        if (cmd.hasErrors()) {
            chain(action: 'create', model: [cmd: cmd], params: params)
        } else {
            UserContext userContext = UserContext.of(request)
            String id = awsAutoScalingService.nextPolicyId(userContext)
            ScheduledUpdateGroupAction scheduledAction = new ScheduledUpdateGroupAction(
                    scheduledActionName: Relationships.buildScalingPolicyName(cmd.group, id),
                    autoScalingGroupName: cmd.group,
                    recurrence: cmd.recurrence,
                    minSize: cmd.min,
                    maxSize: cmd.max,
                    desiredCapacity: cmd.desired,
            )
            try {
                List<String> names = awsAutoScalingService.createScheduledActions(userContext, [scheduledAction])
                String name = Check.lone(names, String)
                flash.message = "Scheduled Action '${name}' has been created."
                redirect(action: 'show', params: [id: name])
            } catch (Exception e) {
                String msg = 'Could not create Scheduled Action for Auto Scaling Group'
                flash.message = "${msg} '${scheduledAction.autoScalingGroupName}': ${e}"
                chain(action: 'create', model: [cmd: cmd], params: params)
            }
        }
    }

    def update = { ScheduledActionCommand cmd ->
        if (cmd.hasErrors()) {
            chain(action: 'edit', model: [cmd: cmd], params: params)
        } else {
            UserContext userContext = UserContext.of(request)
            ScheduledUpdateGroupAction scheduledAction = awsAutoScalingService.getScheduledAction(userContext,
                    cmd.actionName)
            try {
                scheduledAction.with {
                    autoScalingGroupName = cmd.group
                    recurrence = cmd.recurrence
                    minSize = cmd.min
                    maxSize = cmd.max
                    desiredCapacity = cmd.desired
                }
                awsAutoScalingService.updateScheduledAction(userContext, scheduledAction)
                flash.message = "Scheduled Action '${scheduledAction.scheduledActionName}' has been updated."
                redirect(action: 'show', params: [id: scheduledAction.scheduledActionName])
            } catch (Exception e) {
                flash.message = "Could not update Scheduled Action '${cmd.actionName}': ${e}"
                chain(action: 'edit', model: [cmd: cmd], params: params)
            }
        }
    }

    def result = { render view: '/common/result' }
}

class ScheduledActionCommand {
    String actionName
    String group
    String recurrence
    Integer min
    Integer max
    Integer desired

    static constraints = {
        recurrence(nullable: false, blank: false)
    }
}
