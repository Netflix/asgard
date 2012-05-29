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
package com.netflix.asgard.cache

import org.joda.time.DateTime

/** A short-term record of a user action creating, updating, or deleting an object, with a timestamp. */
class JournalRecord {
    DateTime timestamp
    Action action

    JournalRecord(Action action) {
        this.timestamp = new DateTime()
        this.action = action
    }
}

enum Action {
    CREATE,
    UPDATE,
    DELETE
}
