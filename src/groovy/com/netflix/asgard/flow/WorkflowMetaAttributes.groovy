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

import com.amazonaws.services.simpleworkflow.flow.annotations.Execute
import com.amazonaws.services.simpleworkflow.model.WorkflowType
import groovy.transform.Canonical
import java.lang.reflect.Method

/**
 * This wrapper around an AWS SWF Flow workflow makes it easier get meta information about the workflow based on the
 * structure of the implementing class.
 */
@Canonical
class WorkflowMetaAttributes {
    final Class<?> type

    /**
     * @return an AWS SWF Flow WorkflowType which can be used to identify a workflow
     */
    WorkflowType getWorkflowType() {
        Execute execute = null
        Method method = type.getMethods().find { method ->
            method.declaredAnnotations.find {
                if (it.annotationType() == Execute) {
                    execute = (Execute) it
                }
                execute
            }
        }
        WorkflowType workflowType = new WorkflowType()
        workflowType.name = "${method.declaringClass.simpleName}.${method.name}"
        if (execute?.name()) {
            workflowType.name = execute?.name()
        }
        workflowType.version = execute?.version()
        workflowType
    }

}
