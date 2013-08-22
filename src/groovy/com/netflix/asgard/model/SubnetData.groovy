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
import com.amazonaws.services.ec2.model.Tag
import grails.converters.JSON
import groovy.transform.Immutable
import org.codehaus.groovy.grails.web.json.JSONElement

/**
 * Immutable Wrapper for an AWS Subnet.
 * Metadata in tags becomes proper attributes here.
 * {@link com.amazonaws.services.ec2.model.Subnet}
 */
@Immutable class SubnetData {

    private static final String METADATA_TAG_KEY = 'immutable_metadata'

    /** {@link com.amazonaws.services.ec2.model.Subnet#subnetId} */
    String subnetId

    /** {@link com.amazonaws.services.ec2.model.Subnet#state} */
    String state

    /** {@link com.amazonaws.services.ec2.model.Subnet#vpcId} */
    String vpcId

    /** {@link com.amazonaws.services.ec2.model.Subnet#cidrBlock} */
    String cidrBlock

    /** {@link com.amazonaws.services.ec2.model.Subnet#availableIpAddressCount} */
    Integer availableIpAddressCount

    /** {@link com.amazonaws.services.ec2.model.Subnet#availabilityZone} */
    String availabilityZone

    /** A label that indicates the purpose of this Subnet's configuration. */
    String purpose

    /** The target the subnet applies to (null means any object type). */
    SubnetTarget target

    /**
     * Construct SubnetData from the original AWS Subnet
     *
     * @param  subnet the mutable AWS Subnet
     * @return a new immutable SubnetData based off values from subnet
     */
    static SubnetData from(Subnet subnet) {
        JSONElement metadata = getJsonMetaData(subnet.tags)
        new SubnetData(purpose: getPurposeFromJson(metadata), target: getTargetFromJson(metadata),
                subnetId: subnet.subnetId, state: subnet.state, vpcId: subnet.vpcId, cidrBlock: subnet.cidrBlock,
                availableIpAddressCount: subnet.availableIpAddressCount, availabilityZone: subnet.availabilityZone)
    }

    private static String getPurposeFromJson(JSONElement json) {
        json?.purpose
    }

    private static SubnetTarget getTargetFromJson(JSONElement json) {
        String targetName = json?.target
        if (!targetName) { return null }
        SubnetTarget.forText(targetName)
    }

    private static JSONElement getJsonMetaData(List<Tag> tags) {
        Tag tag = tags.find { Tag tag ->
            tag.key == METADATA_TAG_KEY
        }
        String json = tag?.value
        if (!json) { return null }
        JSON.parse(json)
    }
}
