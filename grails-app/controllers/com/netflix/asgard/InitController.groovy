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

/**
 * Controller that handles all user requests if there is no Config.groovy in ASGARD_HOME
 */
class InitController {

    def initService
    def configService

    static allowedMethods = [save: 'POST']

    def beforeInterceptor = {
        if (configService.appConfigured) {
            redirect(controller: 'home')
            return false
        }
    }

    def index = {
        [asgardHome: configService.asgardHome]
    }

    /**
     * Creates the Config.groovy file from the supplied parameters and redirects to the home page if successful
     */
    def save = { InitializeCommand cmd ->
        if (cmd.hasErrors()) {
            render(view: 'index', model: [cmd: cmd])
            return
        }

        try {
            initService.writeConfig(cmd.toConfigObject())
        } catch (Exception ioe) {
            flash.message = ioe.message
            redirect(action: 'index')
            return
        }
        flash.message = "Created Asgard configuration file at ${configService.asgardHome}/Config.groovy."
        redirect(controller: 'home')
    }
}

class InitializeCommand {
    String accessId
    String secretKey
    String accountNumber
    boolean showPublicAmazonImages
    static constraints = {
        accessId(nullable: false, blank: false, matches: /[A-Z0-9]{20}/)
        secretKey(nullable: false, blank: false, matches: /[A-Za-z0-9\+\/]{40}/)
        accountNumber(nullable: false, blank: false, matches: /\d{4}-?\d{4}-?\d{4}/)
    }

    ConfigObject toConfigObject() {
        ConfigObject rootConfig = new ConfigObject()
        ConfigObject grailsConfig = new ConfigObject()
        rootConfig['grails'] = grailsConfig
        String accountNumber = accountNumber.replace('-', '')
        grailsConfig['awsAccounts'] = [accountNumber]
        grailsConfig['awsAccountNames'] = [(accountNumber): 'prod']

        ConfigObject secretConfig = new ConfigObject()
        rootConfig['secret'] = secretConfig
        secretConfig['accessId'] = accessId.trim()
        secretConfig['secretKey'] = secretKey.trim()

        ConfigObject cloudConfig = new ConfigObject()
        rootConfig['cloud'] = cloudConfig
        cloudConfig['accountName'] = 'prod'
        cloudConfig['publicResourceAccounts'] = showPublicAmazonImages ? ['amazon'] : []

        rootConfig
    }
}
