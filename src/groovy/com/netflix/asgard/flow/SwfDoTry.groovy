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
import com.amazonaws.services.simpleworkflow.flow.core.TryCatchFinally
import com.google.common.collect.ImmutableSet

/**
 * SWF specific implementation.
 */
class SwfDoTry<T> extends TryCatchFinally implements DoTry<T> {

    private final ImmutableSet<Promise<?>> promises
    private final Closure tryBlock

    private Closure catchBlock
    private finallyBlock
    private Settable result = new Settable()

    private SwfDoTry(Collection<Promise<?>> promises, Closure tryBlock, Closure catchBlock, Closure finallyBlock) {
        super(promises as Promise[])
        this.promises = ImmutableSet.copyOf(promises)
        this.tryBlock = tryBlock
        this.catchBlock = catchBlock
        this.finallyBlock = finallyBlock
    }

    /**
     * Construct a DoTry for the try logic.
     *
     * @param promises that must be ready before the try logic will execute
     * @param tryBlock logic to be preformed
     * @return constructed DoTry
     */
    static DoTry<T> execute(Collection<Promise<?>> promises, Closure<? extends Promise<T>> tryBlock) {
        new SwfDoTry(promises, tryBlock, { Throwable e -> throw e }, {})
    }

    /**
     * Construct a DoTry for the try logic.
     *
     * @param tryBlock logic to be preformed
     * @return constructed DoTry
     */
    static DoTry<T> execute(Closure<? extends Promise<T>> tryBlock) {
        execute([], tryBlock)
    }

    @Override
    DoTry<T> withCatch(Closure<? extends Promise<T>> block) {
        this.catchBlock = block
        this
    }

    @Override
    DoTry<T> withFinally(Closure block) {
        this.finallyBlock = block
        this
    }

    @Override
    Promise<T> getResult() {
        result
    }

    private Promise<?> wrapWithPromise(def result) {
        Promise.isAssignableFrom(result.getClass()) ? result : Promise.asPromise(result)
    }

    @Override
    protected void doTry() throws Throwable {
        result.unchain()
        def blockResult = tryBlock()
        result.chain(wrapWithPromise(blockResult))
    }

    @Override
    protected void doCatch(Throwable e) throws Throwable {
        result.unchain()
        def blockResult = catchBlock(e)
        result.chain(wrapWithPromise(blockResult))
    }

    @Override
    protected void doFinally() throws Throwable {
        finallyBlock()
    }
}
