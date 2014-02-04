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

/**
 * Created by aglover on 1/31/14.
 */
class InstanceTypeSpec extends Specification{

    def 'this spec serves as test for switching imports of base aws-sdk class - validate .values() call'(){
        when:
        List<InstanceType> instanceTypes = InstanceType.values() as List
        then:
        assert instanceTypes.size() >= 28
    }

    def 'this spec serves as test for switching imports of base aws-sdk class - validate .fromValue() call'(){
        when:
        InstanceType type = InstanceType.fromValue('c3.8xlarge')
        then:
        assert type.toString() == 'c3.8xlarge'
    }

    def 'validate .fromValue() call with blank or null value or bad string'(){
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

    def 'validate .fromValue() with valid values'(){
        when:
        InstanceType type = InstanceType.fromValue('m3.large')
        then:
        assert type.toString() == 'm3.large'
        when:
        InstanceType type2 = InstanceType.fromValue('m1.xlarge')
        then:
        assert type2.toString() == 'm1.xlarge'
    }
}
