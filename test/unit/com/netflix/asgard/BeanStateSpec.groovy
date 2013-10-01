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
package com.netflix.asgard

import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableMap
import com.amazonaws.services.rds.model.DBInstance
import com.amazonaws.services.rds.model.CreateDBInstanceRequest
import groovy.transform.Canonical
import spock.lang.Specification

class BeanStateSpec extends Specification {

    final Bean originalBean = new Bean()
    BeanState originalState

    def setup() {
        originalBean.with {
            string1 = "abc"
            string2 = "123"
            int1 = 42
            collection1 = ImmutableSet.of("Groucho", "Chico", "Harpo")
        }
        originalState = BeanState.ofSourceBean(originalBean)
    }

    def "should copy state to a Map" () {
        expect:
            originalState.asMap() == ImmutableMap.copyOf([
                string1: "abc",
                string2: "123",
                int1: 42,
                collection1: ["Groucho", "Chico", "Harpo"] as Set
            ])
    }

    def "should copy state to same class" () {
        when:
            final Bean targetBean = originalState.injectState(new Bean())
        then:
            targetBean == originalBean
            !targetBean.is(originalBean)
    }

    def "should not copy ignored properties" () {
        when:
            final Bean targetBean = originalState.ignoreProperties(['string1', 'collection1']).injectState(new Bean())
        then:
            null == targetBean.string1
            42 == targetBean.int1
            null == targetBean.collection1
            "123" == targetBean.string2
    }

    def "should copy some state to class with overlapping state" () {
        when:
            final BeanWithOverlappingState targetBean = originalState.injectState(new BeanWithOverlappingState())
        then:
            "abc" == targetBean.string1
            42 == targetBean.int1
            0 == targetBean.int2
            ["Groucho", "Chico", "Harpo"] as Set == targetBean.collection1
            null == targetBean.collection2
    }

    def "should not copy state to immutable fields" () {
        when:
            final ImmutableBean targetBean = originalState.injectState(new ImmutableBean("def", 7, ["John", "Paul", "Ringo", "George"]))
        then:
            "def" == targetBean.string1
            7 == targetBean.int1
            ["John", "Paul", "Ringo", "George"] == targetBean.collection1
            'Ricky Gervais' == targetBean.string2
    }

    def "should copy state to wider type" () {
        when:
            final BeanWithWiderType targetBean = originalState.injectState(new BeanWithWiderType())
        then:
            "abc" == targetBean.string1
    }

    def "should not copy state to narrower type" () {
        when:
            final BeanWithNarrowerType targetBean = originalState.injectState(new BeanWithNarrowerType())
        then:
            null == targetBean.string1
    }

    def "should copy state when autoboxing" () {
        when:
            final BeanWithObjectWrapper targetBean = originalState.injectState(new BeanWithObjectWrapper())
        then:
            42 == targetBean.int1
    }

    def "should copy state when unboxing" () {
        given:
            final beanWithObjectWrapper = new BeanWithObjectWrapper()
            beanWithObjectWrapper.int1 = 42
            final BeanState stateOfBeanWithObjectWrapper = BeanState.ofSourceBean(beanWithObjectWrapper)

        when:
            final BeanWithPrimitive targetBean = stateOfBeanWithObjectWrapper.injectState(new BeanWithPrimitive())
        then:
            42 == targetBean.int1
    }

    def "should not copy state when null" () {
        given:
            final BeanWithObjectWrapper beanWithObjectWrapper = new BeanWithObjectWrapper()
            beanWithObjectWrapper.int1 = null
            final BeanState stateOfBeanWithObjectWrapper = BeanState.ofSourceBean(beanWithObjectWrapper)
            final beanWithPrimitive = new BeanWithPrimitive()
            beanWithPrimitive.int1 = 42

        when:
            final targetBean = stateOfBeanWithObjectWrapper.injectState(beanWithPrimitive)
        then:
            42 == targetBean.int1
    }

    def "unfortunately copies state to Generics are not the same" () {
        when:
            final BeanWithDifferentGenerics targetBean = originalState.injectState(new BeanWithDifferentGenerics())
            //noinspection GroovyUnusedAssignment
            final Integer thisIsNotAnInteger = targetBean.collection1.iterator().next()
        then:
            thrown(ClassCastException)
            ["Groucho", "Chico", "Harpo"] as Set == targetBean.collection1
    }

    // The AWS class would not inject with the naive approach, and I'm not sure why.
    // TODO - make a more specific unit test when it is understood
    def "should inject aws class" () {
        final DBInstance templateDbInstance = new DBInstance().withMasterUsername("me")
        final BeanState templateDbInstanceState = BeanState.ofSourceBean(templateDbInstance)
        when:
            final CreateDBInstanceRequest request = templateDbInstanceState.injectState(new CreateDBInstanceRequest())
        then:
            request
    }

    @Canonical
    static class Bean {
        String string1
        String string2
        int int1
        Collection<String> collection1
    }

    @Canonical
    static class BeanWithOverlappingState {
        String string1
        int int1
        int int2
        Collection collection1
        Collection collection2
    }

    @Canonical
    static class BeanWithWiderType {
        Object string1
    }

    @Canonical
    static class BeanWithNarrowerType {
        StringBuffer string1
    }

    @Canonical
    static class BeanWithPrimitive {
        int int1
    }

    @Canonical
    static class BeanWithObjectWrapper {
        Integer int1
    }

    @Canonical
    static class BeanWithDifferentGenerics {
        Collection<Integer> collection1
    }

    @Canonical
    static class ImmutableBean {
        final String string1
        final int int1
        final Collection collection1

        // not final, but still no constructor
        String getString2() { 'Ricky Gervais' }
    }

}
