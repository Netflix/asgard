/*
 * Copyright 2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.netflix.asgard.flow.example

import com.amazonaws.services.simpleworkflow.flow.annotations.Execute
import com.amazonaws.services.simpleworkflow.flow.annotations.GetState
import com.amazonaws.services.simpleworkflow.flow.annotations.Workflow
import com.amazonaws.services.simpleworkflow.flow.annotations.WorkflowRegistrationOptions

/**
 * Contract of the hello world workflow
 */
@Workflow
@WorkflowRegistrationOptions(defaultExecutionStartToCloseTimeoutSeconds = 60L)
public interface HelloWorldWorkflow {

    @Execute(version = '1.0')
    void helloWorld(String name)

    @GetState
    List<String> getLogHistory()

}
