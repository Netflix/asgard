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

/** Partitions subnets by the type of AWS objects they can be applied to.*/
enum SubnetTarget {
    /** The subnet can be applied only to AWS machine instances. */
    EC2('ec2'),
    /** The subnet can be applied only to AWS Elastic Load Balancers. */
    ELB('elb')

    private final String text

    SubnetTarget(String text) {
        this.text = text
    }

    static SubnetTarget forText(String text) {
        values().find() { it.text == text }
    }
}
