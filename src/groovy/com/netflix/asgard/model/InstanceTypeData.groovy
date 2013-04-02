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

import com.amazonaws.services.ec2.model.InstanceType
import java.text.NumberFormat

/**
 * Hardware specifications and multiple types of pricing data for a type of machine available to use as an EC2 instance.
 */
final class InstanceTypeData {

    /**
     * Technical specifications for a type of physical or virtual machine.
     */
    HardwareProfile hardwareProfile

    /**
     * Fixed hourly cost in dollars for a Linux machine to use promptly and for which there is no reservation.
     */
    BigDecimal linuxOnDemandPrice

    /**
     * Fixed hourly cost in dollars for a Linux machine to use promptly and which has a reservation.
     */
    BigDecimal linuxReservedPrice

    /**
     * Recent market-based hourly cost in dollars for a Linux machine to use eventually, not promptly.
     */
    BigDecimal linuxSpotPrice

    /**
     * Fixed hourly cost in dollars for a Windows machine to use promptly and for which there is no reservation.
     */
    BigDecimal windowsOnDemandPrice

    /**
     * Fixed hourly cost in dollars for a Windows machine to use promptly and which has a reservation.
     */
    BigDecimal windowsReservedPrice

    /**
     * Recent market-based hourly cost in dollars for a Windows machine to use eventually, not promptly.
     */
    BigDecimal windowsSpotPrice

    /**
     * Gets the canonical name of the instance type.
     *
     * @return String instance type name such as m1.large
     */
    String getName() {
        hardwareProfile.name
    }

    /**
     * @deprecated Prove this method is not used. Then delete it. Some new instance types are absent from the enum.
     */
    @Deprecated
    InstanceType getInstanceType() {
        InstanceType.fromValue(hardwareProfile.instanceType)
    }

    /**
     * Calculates the 30-day cost of running a Linux on-demand instance, prepended with a dollar sign.
     *
     * @return String the dollar-sign amount for a
     */
    String getMonthlyLinuxOnDemandPrice() {
        if (linuxOnDemandPrice == null) { return null }
        NumberFormat.getCurrencyInstance().format(linuxOnDemandPrice * 24 * 30)
    }
}
