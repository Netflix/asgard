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
package com.netflix.asgard.mock

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.route53.AmazonRoute53Client
import com.amazonaws.services.route53.model.ListHostedZonesRequest
import com.amazonaws.services.route53.model.ListHostedZonesResult

/**
 * Used for offline mode.
 */
class MockAmazonRoute53Client extends AmazonRoute53Client {

    MockAmazonRoute53Client(AWSCredentialsProvider credentialsProvider, ClientConfiguration clientConfiguration) {
        super(credentialsProvider, clientConfiguration)
    }

    @Override
    ListHostedZonesResult listHostedZones(ListHostedZonesRequest listHostedZonesRequest) { [] }
}
