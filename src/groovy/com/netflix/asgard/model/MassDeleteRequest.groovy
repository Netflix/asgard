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
package com.netflix.asgard.model

import com.netflix.asgard.Check

class MassDeleteRequest {

    private static final MIN_DAYS_TO_RETAIN_IMAGES_SINCE_LAST_REFERENCE = 30
    private static final MIN_DAYS_TO_RETAIN_NEVER_REFERENCED_IMAGES = 3
    private static final MAXIMUM_IMAGE_DELETION_BATCH_SIZE = 150

    /**
     * Number of days since the lastReferenced tag to check for images to delete.
     */
    Integer lastReferencedDaysAgo

    /**
     * Number of days since the creationDate tag to check for images to delete if the image has no lastReferenced tag.
     */
    Integer neverReferencedDaysAgo

    /**
     * Execution mode to run in.  Either DRYRUN or EXECUTE to indicate if to actually delete or not.
     */
    JanitorMode mode

    /**
     * Maximum number of images to delete in a single job.
     */
    Integer limit

    /**
     * Checks for invalid inputs, making sure that a bad request won't delete too much.
     */
    void checkIfValid() {
        Check.notNull(mode, JanitorMode, "mode ${JanitorMode.values()}")
        Check.atLeast(MIN_DAYS_TO_RETAIN_IMAGES_SINCE_LAST_REFERENCE, lastReferencedDaysAgo, 'lastReferencedDaysAgo')
        Check.atLeast(MIN_DAYS_TO_RETAIN_NEVER_REFERENCED_IMAGES, neverReferencedDaysAgo, 'neverReferencedDaysAgo')
        Check.atMost(MAXIMUM_IMAGE_DELETION_BATCH_SIZE, limit, 'limit')
    }
}
