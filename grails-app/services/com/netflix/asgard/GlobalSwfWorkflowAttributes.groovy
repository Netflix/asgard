/*
 * Copyright 2014 Netflix, Inc.
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

/**
 * This class with its static fields is an abomination and I would love to remove it. The problem is that I need to
 * provide some information to all SWF workflows. The Flow framework instantiates and executes the workflow without
 * ever giving me access to it. This is the only way I know how to make this state available.
 */
class GlobalSwfWorkflowAttributes {

    /** SWF taskList that activities will be scheduled with */
    static String taskList
}
