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
package com.netflix.glisten.example.trip

import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy
import com.netflix.glisten.DoTry
import com.netflix.glisten.SwfWorkflow
import com.netflix.glisten.Workflow

/**
 * SWF workflow implementation for the BayAreaTripWorkflow example.
 */
class BayAreaTripWorkflowImpl implements BayAreaTripWorkflow {

    @Delegate Workflow<BayAreaTripActivities> workflow = SwfWorkflow.of(BayAreaTripActivities)

    @Override
    void start(String name, Collection<BayAreaLocation> previouslyVisited) {

        // decide where to go
        Promise<BayAreaLocation> destinationPromise = doTry {
            if (!previouslyVisited.contains(BayAreaLocation.GoldenGateBridge)) {
                return promiseFor(BayAreaLocation.GoldenGateBridge)
            }
            if (!previouslyVisited.contains(BayAreaLocation.Redwoods)) {
                return promiseFor(BayAreaLocation.Redwoods)
            } else {
                waitFor(activities.askYesNoQuestion('Do you like roller coasters?')) {
                        boolean isThrillSeeker ->
                    if (isThrillSeeker) {
                        return promiseFor(BayAreaLocation.Boardwalk)
                    }
                    promiseFor(BayAreaLocation.Monterey)
                }
            }
        }.result

        waitFor(destinationPromise) { BayAreaLocation destination ->
            waitFor(activities.goTo(name, destination)) {
                status it

                switch (destination) {
                    case BayAreaLocation.GoldenGateBridge:
                        waitFor(activities.hike('across the bridge')) { status it }
                        break

                    case BayAreaLocation.Redwoods:
                        // take time to stretch before hiking
                        status 'And stretched for 10 seconds before hiking.'
                        waitFor(timer(10)) {
                            DoTry<String> hiking = doTry { promiseFor(activities.hike('through redwoods')) }
                            DoTry<Void> countDown = cancellableTimer(30)
                            // hike until done or out of time (which ever comes first)
                            waitFor(anyPromises(countDown.getResult(), hiking.getResult())) {
                                if (hiking.getResult().isReady()) {
                                    countDown.cancel(null)
                                    status "${hiking.getResult().get()}"
                                } else {
                                    hiking.cancel(null)
                                    status 'And ran out of time when hiking.'
                                }
                            }
                        }
                        break

                    case BayAreaLocation.Monterey:
                        // parallel activities (eat while watching)
                        Promise<String> eating = promiseFor(activities.enjoy('eating seafood'))
                        Promise<String> watching = promiseFor(activities.enjoy('watching sea lions'))
                        waitFor(allPromises(eating, watching)) {
                            status "${eating.get()} ${watching.get()}"
                            DoTry<String> tryToEnjoySomething = doTry {
                                waitFor(activities.enjoy('looking for sea glass on the beach')) { status it }
                            } withCatch { Throwable t ->
                                status t.message
                                waitFor(activities.enjoy('the aquarium')) { status it }
                            } withFinally {
                                waitFor(activities.enjoy('the 17-Mile Drive')) { status it }
                            }
                            tryToEnjoySomething.result
                        }
                        break

                    case BayAreaLocation.Boardwalk:
                        int numberOfTokensGiven = 3
                        int numberOfTokens = numberOfTokensGiven
                        RetryPolicy retryPolicy = new ExponentialRetryPolicy(60).
                                withMaximumAttempts(numberOfTokens).withExceptionsToRetry([IllegalStateException])
                        DoTry<String> tryToWin = doTry {
                            retry(retryPolicy) {
                                if (numberOfTokens <= 0) { null }
                                numberOfTokens--
                                return promiseFor(activities.win('a carnival game'))
                            }
                        } withCatch { Throwable e ->
                            status "${e.message} ${numberOfTokensGiven} times."
                        }
                        waitFor(tryToWin.result) {
                            status it
                            if (numberOfTokens > 0) {
                                waitFor(activities.enjoy('a roller coaster')) { status it }
                            }
                            Promise.Void()
                        }
                        break
                }
            }

        }
    }

}
