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

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy
import com.netflix.asgard.flow.DoTry
import com.netflix.asgard.flow.SwfWorkflow
import com.netflix.asgard.flow.Workflow

/**
 * Implementation of the hello world workflow
 */
class HelloWorldWorkflowImpl implements HelloWorldWorkflow {

    @Delegate
    Workflow<HelloWorldActivities> workflow = SwfWorkflow.of(HelloWorldActivities)

    void helloWorld(String name) {
        status 'starting task'

        // Calling activities and waiting for promises.
        Promise<String> hello = promiseFor(activities.getHello())
        status 'here1'
        Promise<String> doneMsg = waitFor(hello) {
            status 'hello received1'
            activities.printHello("${hello.get()} There ${name}" as String)
            status 'hello received2'
            promiseFor('Done printing HW')
        }

        useInjectedService()
        demonstrateRetryLogic()
        cancelTryBlockOnceTimerFires(doneMsg)
    }

    void useInjectedService() {
        Promise<Collection<String>> clusterNames = promiseFor(activities.getClusterNames())
        status 'pre clusterNames received'
        waitFor(clusterNames) {
            status 'clusterNames received'
            activities.printHello(clusterNames.get().join(', '))
            promiseFor(true)
        }
    }

    void demonstrateRetryLogic() {
        doTry {
            retry(new ExponentialRetryPolicy(1L).withMaximumAttempts(3)) {
                activities.throwException()
                Promise.Void()
            }
            Promise.asPromise(true)
        } withCatch { Throwable e ->
            activities.printHello("doCatch ${e}" as String)
            Promise.asPromise(false)
        } withFinally {
            status 'retry done'
            activities.printHello('doFinally')
        }
    }

    void cancelTryBlockOnceTimerFires(Promise<String> doneMsg) {
        DoTry<Boolean> nap = doTry(doneMsg) {
            Promise<Boolean> result = promiseFor(activities.takeNap(3L))
            waitFor(result) {
                activities.printHello('woke up on my own')
                Promise.Void()
            }
            result
        } withCatch { Throwable e ->
            activities.printHello("nap cancelled ${e}" as String)
            Promise.asPromise(false)
        }
        waitFor(anyPromises(nap.result, timer(2L))) {
            status 'nap finished'
            if (!nap.result.isReady()) {
                nap.cancel(null)
            }
            activities.printHello('Awake now!')
            promiseFor(true)
        }
    }

}
