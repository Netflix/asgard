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

import groovy.transform.Immutable

/**
 * Values are taken from http://aws.amazon.com/ec2/instance-types/ and http://aws.amazon.com/ec2/pricing/ because there
 * is not yet an API for this information.
 *
 * The field names are terse in order for the instantiation of many hardware profiles to be more compact and readable.
 */
@Immutable class HardwareProfile {

    /** The API name of the instance type such as "m1.medium" */
    String instanceType

    /** The Instance Family from http://aws.amazon.com/ec2/instance-types/ such as "General purpose" */
    String family

    /** The instance type group heading from http://aws.amazon.com/ec2/pricing/ such as "Second Generation" */
    String group

    /** The description of the size within the group from http://aws.amazon.com/ec2/pricing/ such as "Extra Large" */
    String size

    /** The processor architecture such as "64-bit" or "32-bit or 64-bit" */
    String arch

    /** Number of virtual central processing units such as "2" */
    String vCpu

    /** Number of elastic compute units such as "88", "6.5", or "Variable" */
    String ecu

    /** Number of gigabytes of random access memory such as "34.2", "0.615", or "117" */
    String mem

    /** Count and capacity of storage units in gigabytes such as "4 x 420", "EBS Only" or "2 x 1,024 SSD" */
    String storage

    /** Whether the EBS Optimized feature is available, such as "Yes" or "-" */
    String ebsOptim

    /** The quality of the network performance, such as "Very Low", "Moderate", or "10 Gigabit" */
    String netPerf

    /** An alias for the instance type string is the "name" of the hardware profile */
    String getName() {
        instanceType
    }
}
