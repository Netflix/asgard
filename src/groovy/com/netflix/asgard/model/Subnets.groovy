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
import com.google.common.collect.Maps
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.netflix.asgard.Check

/**
 * These are nontrivial queries we like to perform on subnets.
 */
@Immutable class Subnets {
    /** All of the subnets contained in this object. */
    Collection<SubnetData> allSubnets

    /**
     * Construct Subnets from AWS Subnets
     *
     * @param  subnets the actual AWS Subnets
     * @return a new immutable Subnets based off the subnets
     */
    public static Subnets from(Collection<Subnet> subnets) {
        new Subnets(allSubnets: subnets.collect() { SubnetData.from(it) })
    }

    /**
     * Simply find a subnet based on its ID.
     *
     * @param id of the subnet
     * @return the unique subnet with that ID or null
     */
    SubnetData findSubnetById(String id) {
        Check.notNull(id, String)
        allSubnets.find { it.subnetId == id }
    }

    /**
     * Find the subnet IDs that map to specific zones
     *
     * @param  zones the zones in AWS that you want Subnet IDs for
     * @param  purpose only subnets with the specified purpose will be returned
     * @param  target is the type of AWS object the subnet applies to (null means any object type)
     * @return the subnet IDs returned in the same order as the zones sent in or an empty List
     * @throws IllegalArgumentException if there are multiple subnets with the same purpose and zone
     */
    List<String> getSubnetIdsForZones(List<String> zones, String purpose, SubnetTarget target = null) {
        if (!zones) {
            return Collections.emptyList()
        }
        Check.notNull(purpose, String)
        Function<SubnetData, String> purposeOfSubnet = { it.purpose } as Function
        Map<String, Collection<SubnetData>> zonesToSubnets = mapZonesToTargetSubnets(target).asMap()
        zonesToSubnets.subMap(zones).values().collect { Collection<SubnetData> subnetsForZone ->
            if (subnetsForZone == null) {
                return null
            }
            SubnetData subnetForPurpose = Maps.uniqueIndex(subnetsForZone, purposeOfSubnet)[purpose]
            subnetForPurpose?.subnetId
        }.findAll { it != null}
    }

    /**
     * Find the purposes that are common to all specified zones
     *
     * @param  zones the zones in AWS that you want purposes for
     * @param  target is the type of AWS object the subnet applies to (null means any object type)
     * @return the set of distinct purposes or an empty Set
     */
    Set<String> getPurposesForZones(Collection<String> zones, SubnetTarget target = null) {
        if (!zones) {
            return Collections.emptySet()
        }
        Map<String, Collection<SubnetData>> zonesToSubnets = mapZonesToTargetSubnets(target).asMap()
        zones.inject(null) { Collection<String> purposeIntersection, String zone ->
            Collection<SubnetData> subnetsForZone = zonesToSubnets[zone]
            List<String> purposes = subnetsForZone.collect { it.purpose }
            purposeIntersection == null ? purposes : purposeIntersection.intersect(purposes)
        } as Set
    }

    private Multimap<String, SubnetData> mapZonesToTargetSubnets(SubnetTarget target) {
        Collection<SubnetData> targetSubnetsWithPurpose = allSubnets.findAll() {
            // Find ones with a purpose, and if they have a target then it should match
            (!it.target || it.target == target) && it.purpose
        }
        Multimaps.index(targetSubnetsWithPurpose, { it.availabilityZone } as Function)
    }

    /**
     * Find the zones with VPCs that have the specified purpose
     *
     * @param  purpose the VPC purpose want AWS zone names for
     * @param  target is the type of AWS object the subnet applies to (null means any object type)
     * @return the set of distinct zones that contain a VPC with the specified purpose or an empty Set
     */
    Set<String> getZonesForPurpose(String purpose, SubnetTarget target) {
        if (!purpose) {
            return Collections.emptySet()
        }
        Collection<SubnetData> subnetsForPurpose = allSubnets.findAll {
            // Find ones with a matching purpose, and if they have a target then it should match
            it.purpose == purpose && (!it.target || it.target == target)
        }
        subnetsForPurpose*.availabilityZone as Set
    }
}
