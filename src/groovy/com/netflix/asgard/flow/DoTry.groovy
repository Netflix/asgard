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

import com.amazonaws.services.simpleworkflow.flow.core.Promise

/**
 * Used for specifying exception handling logic in an SWF workflow which can be distributed between multiple servers.
 * <code>
 * doTry {
 *     // try logic
 * } withCatch { Throwable e ->
 *     // catch logic
 * } withFinally {
 *     // finally logic
 * }
 * </code>
 */
interface DoTry<T> {

    /**
     * Provide logic to perform in the case of an exception.
     *
     * @param block executed for exception should take a Throwable
     * @return this object with catch logic implemented
     */
    DoTry<T> withCatch(Closure<? extends Promise<T>> block)

    /**
     * Provide logic guaranteed to be performed once the work is done (even in the case of an exception).
     *
     * @param block executed for finally should take not arguments
     * @return this object with finally logic implemented
     */
    DoTry<T> withFinally(Closure block)

    /**
     * Cancel the work being done in the context of this try block.
     *
     * @param cause for canceling the work
     */
    void cancel(Throwable cause)

    /**
     * @return promised result of the work being done in the context of this try block.
     */
    Promise<T> getResult()
}
