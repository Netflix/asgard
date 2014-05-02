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
package com.netflix.asgard.userdata

/**
 * Keys used in user data key-value pairs. Some of these keys are sometimes prepended with a configured namespace such
 * as CLOUD_ or NETFLIX_.
 */
class UserDataPropertyKeys {
    static final String EC2_REGION = 'EC2_REGION'
    static final String ENVIRONMENT = 'ENVIRONMENT'
    static final String MONITOR_BUCKET = 'MONITOR_BUCKET'
    static final String APP = 'APP'
    static final String APP_GROUP = 'APP_GROUP'
    static final String STACK = 'STACK'
    static final String CLUSTER = 'CLUSTER'
    static final String AUTO_SCALE_GROUP = 'AUTO_SCALE_GROUP'
    static final String LAUNCH_CONFIG = 'LAUNCH_CONFIG'
}
