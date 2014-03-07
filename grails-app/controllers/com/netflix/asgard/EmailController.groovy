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

class EmailController {

    def emailerService

    /**
     * Hit /email?to=me@somewhere.com&subject=hello+world&body=This+is+a+test to send an email to yourself
     */
    def index() {
        // Each "email" is a simple Map
        def email = [
            to: [ params.to ], // "to" expects a List, NOT a single email address
            subject: params.subject,
            text: params.body // "text" is the email body
        ]
        // sendEmails expects a List
        emailerService.sendEmails([email])
        render("done")
    }

    def disable() {
        emailerService.disable()
        render("System emails disabled")
    }

    def enable() {
        emailerService.enable()
        render("System emails enabled")
    }
}
