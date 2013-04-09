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
package com.netflix.asgard.flow

import com.amazonaws.services.simpleworkflow.model.HistoryEvent
import groovy.transform.Canonical

/**
 * This is a wrapper around an AWS SWF Flow HistoryEvent. It makes it easier to navigate the many potential attributes
 * that a HistoryEvent could have and abstracts common operations.
 */
@Canonical
class EventAttributes {
    final HistoryEvent event

    private def getEventDetails() {
        event.properties.find { String name, value ->
            name.endsWith('EventAttributes') && value
        }.value
    }

    /**
     * @param name the property name
     * @return simple way to access a value from the populated history event attributes
     */
    def propertyMissing(String name) {
        eventDetails.properties[name]
    }

    /**
     * @return indicates an SWF decision history event
     */
    boolean isDecision() {
        eventDetails.getClass().simpleName.startsWith('Decision')
    }

}
