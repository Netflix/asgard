/*
 * Copyright 2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.netflix.asgard.flow.example

import com.netflix.asgard.AwsAutoScalingService
import com.netflix.asgard.Region
import com.netflix.asgard.UserContext
import com.netflix.asgard.flow.Activity
import com.netflix.asgard.flow.SwfActivity

/**
 * Implementation of the hello world activities
 */
class HelloWorldActivitiesImpl implements HelloWorldActivities {

    AwsAutoScalingService awsAutoScalingService

    @Delegate
    Activity activity = new SwfActivity()

    void printHello(String name) {
//      println name
    }

    String getHello() {
        'Hello'
    }

    Collection<String> getClusterNames() {
        awsAutoScalingService.getClusters(new UserContext(region: Region.US_WEST_1))*.name
    }

    void throwException() {
        throw new IllegalStateException('Uh oh, something bad happened!')
    }

    Boolean takeNap(long seconds) {
        activity.recordHeartbeat "Taking nap for ${seconds} seconds..."
        (0..seconds).each {
            sleep(1000)
//            println "@@ Waited ${it} of ${seconds} seconds..."
            activity.recordHeartbeat "Waited ${it} of ${seconds} seconds..."
        }
        activity.recordHeartbeat "Done. Waited for ${seconds} seconds."
        true
    }
}
