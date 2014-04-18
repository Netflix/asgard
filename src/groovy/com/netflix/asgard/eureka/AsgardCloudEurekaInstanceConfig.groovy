/*
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.asgard.eureka

import com.netflix.appinfo.CloudInstanceConfig

/**
 * {@link com.netflix.appinfo.EurekaInstanceConfig} configuration for Asgard when running in the AWS cloud.
 *
 * Unfortunately this class is unusually difficult to unit test because of the test-unfriendly constructor behavior of
 * the superclass from eureka-client.
 */
class AsgardCloudEurekaInstanceConfig extends CloudInstanceConfig {

    /**
     * @see com.netflix.appinfo.EurekaInstanceConfig#getNonSecurePort()
     */
    int nonSecurePort

    /**
     * @see com.netflix.appinfo.EurekaInstanceConfig#getVirtualHostName()
     */
    String virtualHostName

    @Override
    String getAppname() { 'asgard' }

    @Override
    int getNonSecurePort() {
        nonSecurePort ?: super.nonSecurePort
    }

    @Override
    String getVirtualHostName() {
        virtualHostName ?: super.virtualHostName
    }
}
