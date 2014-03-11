/*
 * Copyright 2012 Netflix, Inc.
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

class HealthcheckController {

    def healthcheckService

    def index() {
        if (healthcheckService.readyForTraffic) {
            render 'Healthy'
        } else {
            response.status = 500
            render 'Not ready traffic. Check cache list or server logs.'
        }
    }

    /**
     * This legacy endpoint should be removed after data center traffic switching scripts are no longer calling it.
     *
     * The idea was that if we switch traffic back and forth between two machines so they can be alternately bounced
     * periodically in case of degrading performance over time, then we should avoid sending traffic to a server that
     * has failed to load its caches. This conservative health check was separated from the regular health check for
     * the load balancer which needed to send traffic to the server no matter what if we told it to, even if a cache
     * got out of whack. This is because Asgard is still stateful for now and we can only have one instance serving
     * general traffic at a time for a given AWS account.
     */
    def caches() {
        index()
    }
}
