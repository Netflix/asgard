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
package com.netflix.asgard

import com.netflix.asgard.model.SimpleDbSequenceLocator

/**
 * This service constructs unique IDs.
 */
class IdService {

    static transactional = false

    def awsSimpleDbService
    def emailerService

    /**
     * Construct a unique ID. Ideally it will be the next ID in a sequence for Simple DB.
     * A UUID will be used as a last resort.
     *
     * @param userContext who, where, why
     * @param sequenceLocator locates the sequence number in SimpleDB
     * @return unique ID
     */
    String nextId(UserContext userContext, SimpleDbSequenceLocator sequenceLocator) {
        try {
            awsSimpleDbService.incrementAndGetSequenceNumber(userContext, sequenceLocator)
        } catch (Exception e) {
            emailerService.sendExceptionEmail(e.toString(), e)
            UUID.randomUUID().toString()
        }
    }
}
