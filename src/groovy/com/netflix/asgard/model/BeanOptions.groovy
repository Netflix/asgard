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
package com.netflix.asgard.model

import com.google.common.collect.Sets

/**
 * Bean options are classes that hold related mutable state that often changes together. It results in more
 * maintainable code to pass these related attributes around as a group, and have relevant operations in one
 * place in the codebase. Operations that are common across specific implementations can go here.
 * An example where this is useful is for AWS objects where similar data is often translated into various
 * representations including requests to the AWS API.
 */
abstract class BeanOptions {

    /**
     * Copies a non null collection to a new Set. It does not deep copy.
     *
     * @param source collection to copy
     * @return a new Set containing the same objects or null if source was null
     */
    protected static <T> Set<T> copyNonNullToSet(Collection<T> source) {
        source == null ? null : Sets.newHashSet(source)
    }
}
