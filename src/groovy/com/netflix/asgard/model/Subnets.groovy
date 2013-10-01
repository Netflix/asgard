/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.netflix.asgard.model

import com.amazonaws.services.ec2.model.Subnet
import com.google.common.base.Function
import com.google.common.base.Supplier
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.netflix.asgard.Check
import com.netflix.asgard.Relationships
import groovy.transform.Canonical

/**
 * These are nontrivial queries we like to perform on subnets.
 */
@Canonical class Subnets {
    /** All of the subnets contained in this object. */
    final private Collection<SubnetData> allSubnets

    private Subnets(Collection<SubnetData> allSubnets) {
        this.allSubnets = ImmutableSet.copyOf(allSubnets)
    }

    /**
     * Construct Subnets from AWS Subnets
     *
     * @param  subnets the actual AWS Subnets
     * @return a new immutable Subnets based off the subnets
     */
    public static Subnets from(Collection<Subnet> subnets) {
        new Subnets(subnets.collect() { SubnetData.from(it) })
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
     * Find the subnet associated with the first Subnet ID. This is useful in cases where the attribute you care about
     * is guaranteed to be the same for all subnets.
     *
     * @param  subnetIds Subnet IDs
     * @return the Subnet or null
     */
    SubnetData coerceLoneOrNoneFromIds(Collection<String> subnetIds) {
        subnetIds ? findSubnetById(subnetIds.iterator().next()?.trim()) : null
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
    List<String> getSubnetIdsForZones(Collection<String> zones, String purpose, SubnetTarget target = null) {
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
        }.findAll { it != null }
    }

    /**
     * Group zones by subnet purposes they contain.
     *
     * @param  allAvailabilityZones complete list of zones to group
     * @param  target is the type of AWS object the subnet applies to (null means any object type)
     * @return zone name to subnet purposes, a null key indicates zones allowed for use outside of VPC
     */
    Map<String, Collection<String>> groupZonesByPurpose(Collection<String> allAvailabilityZones,
            SubnetTarget target = null) {
        Multimap<String, String> zonesGroupedByPurpose = Multimaps.newSetMultimap([:], { [] as SortedSet } as Supplier)
        zonesGroupedByPurpose.putAll(null, allAvailabilityZones)
        allSubnets.each {
            if (it.availabilityZone in allAvailabilityZones && (!it.target || it.target == target)) {
                zonesGroupedByPurpose.put(it.purpose, it.availabilityZone)
            }
        }
        zonesGroupedByPurpose.keySet().inject([:]) { Map zoneListsByPurpose, String purpose ->
            zoneListsByPurpose[purpose] = zonesGroupedByPurpose.get(purpose) as List
            zoneListsByPurpose
        } as Map
    }

    /**
     * Find all purposes across all specified zones for the specified target.
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
        zones.inject([]) { Collection<String> allPurposes, String zone ->
            allPurposes + zonesToSubnets[zone].collect { it.purpose }
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
     * Provides a one to one mapping from a Subnet purpose to its VPC ID. Purposes that span VPCs in the same region
     * are invalid and will be left out of the map.
     *
     * @return map of subnet purposes to their VPC ID
     */
    Map<String, String> mapPurposeToVpcId() {
        Map<String, List<SubnetData>> subnetsGroupedByPurpose = allSubnets.groupBy { it.purpose }
        subnetsGroupedByPurpose.inject([:]) { Map purposeToVpcId, Map.Entry entry ->
            String purpose = entry.key
            if (!purpose) {
                return purposeToVpcId
            }
            List<SubnetData> subnets = entry.value as List
            Collection<String> distinctVpcIds = subnets*.vpcId.unique()
            // There should only be one VPC ID per purpose or the mapping from purpose back to VPC is ambiguous.
            // We just ignore purposes that are misconfigured so that the rest of the subnet purposes can be used.
            if (distinctVpcIds.size() == 1) {
                purposeToVpcId[purpose] = distinctVpcIds.iterator().next()
            }
            purposeToVpcId
        } as Map
    }

    /**
     * Construct a new VPC Zone Identifier based on an existing VPC Zone Identifier and a list of zones.
     * A VPC Zone Identifier is really just a comma delimited list of subnet IDs.
     * I'm not happy that this method has to exist. It's just a wrapper around other methods that operate on a cleaner
     * abstraction without knowledge of the unfortunate structure of VPC Zone Identifier.
     *
     * @param  vpcZoneIdentifier is used to derive a subnet purpose from
     * @param  zones which the new VPC Zone Identifier will contain
     * @return a new VPC Zone Identifier or null if no purpose was derived
     */
    String constructNewVpcZoneIdentifierForZones(String vpcZoneIdentifier, List<String> zones) {
        if (!zones) {
            // No zones were selected because there was no chance to change them. Keep the VPC Zone Identifier.
            return vpcZoneIdentifier
        }
        String purpose = getPurposeFromVpcZoneIdentifier(vpcZoneIdentifier)
        constructNewVpcZoneIdentifierForPurposeAndZones(purpose, zones)
    }

    /**
     * Figure out the subnet purpose given a VPC zone identifier.
     *
     * @param  vpcZoneIdentifier is used to derive a subnet purpose from
     * @return the subnet purpose indicated by the vpcZoneIdentifier
     */
    String getPurposeFromVpcZoneIdentifier(String vpcZoneIdentifier) {
        List<String> oldSubnetIds = Relationships.subnetIdsFromVpcZoneIdentifier(vpcZoneIdentifier)
        // All subnets used in the vpcZoneIdentifier will have the same subnet purpose if set up in Asgard.
        coerceLoneOrNoneFromIds(oldSubnetIds)?.purpose
    }

    /**
     * Construct a new VPC Zone Identifier based on a subnet purpose and a list of zones.
     *
     * @param  purpose is used to derive a subnet purpose from
     * @param  zones which the new VPC Zone Identifier will contain
     * @return a new VPC Zone Identifier or null if no purpose was specified
     */
    String constructNewVpcZoneIdentifierForPurposeAndZones(String purpose, Collection<String> zones) {
        if (purpose) {
            List<String> newSubnetIds = getSubnetIdsForZones(zones, purpose, SubnetTarget.EC2) // This is only for ASGs.
            if (newSubnetIds) {
                return Relationships.vpcZoneIdentifierFromSubnetIds(newSubnetIds)
            }
        }
        null
    }
}
