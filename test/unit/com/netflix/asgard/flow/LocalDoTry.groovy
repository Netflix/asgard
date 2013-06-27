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
import com.amazonaws.services.simpleworkflow.flow.core.Settable

class LocalDoTry implements DoTry {

    Exception error
    boolean errorWasCaught = false

    private Settable result = new Settable()

    LocalDoTry(Closure tryBlock) {
        try {
            result.set(tryBlock())
        } catch (Exception e) {
            if (e) {
                error = e
            }
        }
    }

    @Override
    DoTry withCatch(Closure block) {
        errorWasCaught = true
        if (error) {
            block(error)
        }
        this
    }

    @Override
    DoTry withFinally(Closure block) {
        block()
        if (!errorWasCaught) {
            throw error
        }
        this
    }

    @Override
    void cancel(Throwable cause) {
    }

    @Override
    Promise getResult() {
        result
    }
}
