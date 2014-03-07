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

import com.amazonaws.services.ec2.model.KeyPairInfo
import grails.converters.JSON
import grails.converters.XML

/**
 * Used to interact with SSH key pairs registered in the Amazon Web Services API for the current account.
 */
class KeyPairController {

    def awsEc2Service

    /**
     * Display all the key pairs registered in the current account-region via REST calls.
     */
    def list() {
        Collection<KeyPairInfo> keys = awsEc2Service.getKeys(UserContext.of(request))
        withFormat {
            xml { new XML(keys).render(response) }
            json { new JSON(keys).render(response) }
        }
    }
}
