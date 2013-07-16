/*
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.asgard.flow

import com.amazonaws.services.simpleworkflow.flow.DynamicWorkflowClientExternal
import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions
import com.amazonaws.services.simpleworkflow.flow.annotations.Execute
import com.amazonaws.services.simpleworkflow.flow.annotations.GetState
import groovy.transform.Canonical
import java.lang.reflect.Method

/**
 * In the eternal words of Yoda, "This is where the magic happens!"
 * Looks like your AWS SWF Flow workflow interface (after a cast) and can actually be used to execute workflows
 * and get the execution state.
 *
 * @see InterfaceBasedWorkflowClient
 */
@Canonical
class WorkflowClientExternalToWorkflowInterfaceAdapter {
    final DynamicWorkflowClientExternal dynamicWorkflowClient
    final Class workflowType
    final WorkflowDescriptionTemplate workflowDescriptionTemplate
    final WorkflowTags workflowTags = new WorkflowTags()

    def methodMissing(String name, args) {
        ReflectionHelper reflectionHelper = new ReflectionHelper(workflowType)
        Method method = reflectionHelper.findMethodForNameAndArgsOrFail(name, args as List)
        if (reflectionHelper.findAnnotationOnMethod(Execute, method)) {
            StartWorkflowOptions workflowOptions = dynamicWorkflowClient.schedulingOptions ?: new StartWorkflowOptions()
            if (workflowDescriptionTemplate) {
                method.invoke(workflowDescriptionTemplate, args)
                workflowTags.withTags(workflowOptions.tagList)
                workflowTags.desc = workflowDescriptionTemplate.description
                workflowOptions.tagList = workflowTags.constructTags()
            }
            dynamicWorkflowClient.startWorkflowExecution(args as Object[], workflowOptions)
        }
        if (reflectionHelper.findAnnotationOnMethod(GetState, method)) {
            return dynamicWorkflowClient.getWorkflowExecutionState(method.returnType)
        }
    }
}
