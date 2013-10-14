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

import com.netflix.asgard.auth.ApiToken
import org.apache.shiro.SecurityUtils

class ApiTokenController {

    def configService
    def secretService

    static allowedMethods = [generate: 'POST']

    def beforeInterceptor = {
        if (!configService.apiTokenEnabled) {
            render(status: 401, text: 'This feature is disabled.')
            return false
        }
        if (!SecurityUtils.subject?.authenticated) {
            render(status: 401, text: 'You must be logged in to use this feature.')
            return false
        }
    }

    def index = { redirect(action: 'create', params: params) }

    def create = { }

    def generate = { GenerateApiTokenCommand cmd ->
        if (cmd.hasErrors()) {
            chain(action: 'create', model:[cmd: cmd], params: params)
        } else {
            flash.apiToken = new ApiToken(cmd.purpose, cmd.email, configService.apiTokenExpirationDays,
                    secretService.currentApiEncryptionKey)
            redirect(action: 'show')
        }
    }

    def show = { }

}

class GenerateApiTokenCommand {
    String purpose
    String email

    static constraints = {
        purpose(nullable: false, blank: false, matches: '[-a-zA-Z0-9._]+')
        email(nullable: false, blank: false, email: true)
    }
}