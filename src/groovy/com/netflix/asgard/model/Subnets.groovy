/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.netflix.asgard.model

import com.amazonaws.services.ec2.model.Subnet
import com.google.common.base.Function
import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import com.google.common.collect.Multimaps

/*
 * These are nontrivial queries we like to perform on subnets.
 */
@Immutable class Subnets {
    Collection<SubnetData> allSubnets

    public static Subnets from(Collection<Subnet> subnets) {
        new Subnets(allSubnets: subnets.collect() { SubnetData.from(it) })
    }

    /**
     * Simply find a subnet based on it's ID.
     *
     * @param id of the subnet
     * @return the unique subnet with that ID or null
     */
    SubnetData findSubnetById(String id) {
        Preconditions.checkNotNull(id)
        allSubnets.find { it.subnetId == id }
    }

    /**
     * Find the subnet ID's that map to specific zones
     *
     * @param  zones the zones in AWS that you want Subnet ID's for
     * @param  purpose only subnets with the specified purpose will be returned
     * @param  target is the type of AWS object the subnet applies to (null means any object type)
     * @return the subnet ID's returned in the same order as the zones sent in
     */
    List<String> getSubnetIdsForZones(List<String> zones, String purpose, SubnetData.Target target = null) {
        Preconditions.checkNotNull(zones)
        Preconditions.checkNotNull(purpose)
        Function<SubnetData, String> purposeOfSubnet = { it.purpose } as Function
        Map<String, Collection<SubnetData>> zonesToSubnets = mapZonesToTargetSubnets(target)
        zones.inject([]) { List<String> subnets, String zone ->
            Collection<SubnetData> subnetsForZone = zonesToSubnets[zone]
            if (subnetsForZone == null) { return subnets }
            ImmutableMap<String, SubnetData> purposeToSubnets = Maps.uniqueIndex(subnetsForZone, purposeOfSubnet)
            SubnetData subnet = purposeToSubnets[purpose]
            if (subnet) { subnets << subnet.subnetId }
            subnets
        } as List
    }

    /**
     * Find the purposes that are common to all specified zones
     *
     * @param  zones the zones in AWS that you want purposes for
     * @param  target is the type of AWS object the subnet applies to (null means any object type)
     * @return the set of distinct purposes
     */
    Set<String> getPurposesForZones(Collection<String> zones, SubnetData.Target target = null) {
        Preconditions.checkNotNull(zones)
        Map<String, Collection<SubnetData>> zonesToSubnets = mapZonesToTargetSubnets(target)
        zones.inject(null) { Set<String> purposeIntersection, String zone ->
            Collection<SubnetData> subnetsForZone = zonesToSubnets[zone]
            List<String> purposes = subnetsForZone.collect { it.purpose }
            if (purposeIntersection == null) {
                purposeIntersection = purposes
            } else {
                purposeIntersection.retainAll(purposes)
            }
            purposeIntersection
        } as Set
    }

    private Map<String, Collection<SubnetData>> mapZonesToTargetSubnets(SubnetData.Target target) {
        Collection<SubnetData> targetSubnetsWithPurpose = allSubnets.findAll() { it.target == target && it.purpose }
        Multimaps.index(targetSubnetsWithPurpose, { it.availabilityZone } as Function).asMap()
    }
}
