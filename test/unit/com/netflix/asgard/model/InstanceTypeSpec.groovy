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
package com.netflix.asgard.model

import spock.lang.Specification

class InstanceTypeSpec extends Specification {

    def 'should work as list like it did before'() {

        when:
        List<InstanceType> instanceTypes = InstanceType.values() as List
        then:
        instanceTypes.size() >= 28
    }

    def 'should work with new instance types of R3* '(String a, String b) {
        InstanceType type = InstanceType.fromValue(a)

        expect:
        type.toString() == b

        where:
        a | b
        'r3.large'  | 'r3.large'
        'r3.xlarge' | 'r3.xlarge'
        'r3.2xlarge' | 'r3.2xlarge'
        'r3.4xlarge' | 'r3.4xlarge'
        'r3.8xlarge' | 'r3.8xlarge'
    }

    def 'should work w/fromValue like it did before'() {

        when:
        InstanceType type = InstanceType.fromValue('c3.8xlarge')
        then:
        type.toString() == 'c3.8xlarge'
    }

    def 'validate .fromValue() call with blank or null value or bad string'() {

        when:
        InstanceType.fromValue('')
        then:
        thrown(IllegalArgumentException)
        when:
        InstanceType.fromValue(null)
        then:
        thrown(IllegalArgumentException)
        when:
        InstanceType.fromValue('foo')
        then:
        thrown(IllegalArgumentException)
    }

    def 'validate .fromValue() with valid values'() {

        when:
        InstanceType type = InstanceType.fromValue('m3.large')
        then:
        type.toString() == 'm3.large'
        when:
        InstanceType type2 = InstanceType.fromValue('m1.xlarge')
        then:
        type2.toString() == 'm1.xlarge'
    }
}
