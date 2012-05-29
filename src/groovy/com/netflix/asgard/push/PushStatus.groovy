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
package com.netflix.asgard.push

class PushStatus extends ArrayList<Expando> {

    RollingPushOptions options

    PushStatus(List<Slot> relaunchSlots, RollingPushOptions options) {
        this.options = options
        for (it in relaunchSlots) {
            this << new Expando(
                    "oldId": it.old.id,
                    "oldState": it.old.state?.name(),
                    "oldLastChange": it.old.lastChange,
                    "newId": it.fresh.id,
                    "newState": it.fresh.state?.name(),
                    "newLastChange": it.fresh.lastChange
            )
        }
    }

    Boolean getAllDone() {
        return options.relaunchCount <= 0 || countReadyNewInstances() >= options.relaunchCount
    }

    Integer countReadyNewInstances() {
        findAll { InstanceState.ready.name() == it.newState }.size()
    }
}
