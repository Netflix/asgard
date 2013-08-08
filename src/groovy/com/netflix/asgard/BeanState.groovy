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

import com.google.common.base.Predicate
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import org.codehaus.groovy.runtime.metaclass.MixinInstanceMetaProperty

/**
 * This is a tool for dealing with the outwardly visible state of objects (in terms of its bean properties).
 * Currently it can extract the state as a Map where the key is the property name as a String, and the value is the
 * property value.
 * It can also inject this state into another class with similar properties.
 * It is designed to be as forgiving as possible, and not inject state on properties that don't work out (wrong name,
 * incompatible type, final...)
 * See BeanStateSpec for executable examples.
 *
 * Examples of when these things would be useful include:
 * - Pushing state from an AWS object into a corresponding AWS request without tedious code.
 * - Turning an object into a Map and exposing it in a Grails restful endpoint.
 */
class BeanState {

    // These fields are typically not what you care about when looking at an Object's properties
    static final ImmutableSet<String> COMMON_META_PROPERTY_NAMES = ImmutableSet.of('class', 'metaClass')

    // Meta field garbage sometimes starts with these prefixes
    static final ImmutableSet<String> COMMON_META_PROPERTY_NAME_PREFIXES = ImmutableSet.of('__timeStamp', '\$class')

    final ImmutableMap<String, ?> propertyNamesToValues

    BeanState(Map<String, ?> propertyNamesToValues) {
        this.propertyNamesToValues = ImmutableMap.copyOf(propertyNamesToValues)
    }

    static boolean isMetaGarbagePropertyName(String name) {
        COMMON_META_PROPERTY_NAMES.contains(name) || COMMON_META_PROPERTY_NAME_PREFIXES.find { name.startsWith(it) }
    }

    static BeanState ofSourceBean(Object sourceBean) {
        final Map<String, ?> propertyNamesToValues = sourceBean.metaPropertyValues.inject([:]) {
                Map<String, ?> accumulativePropertyValuesByName, PropertyValue propertyValue ->
            propertyValue.with {
                if ((value != null) && !BeanState.isMetaGarbagePropertyName(name)) {
                    accumulativePropertyValuesByName[name] = value
                }
            }
            accumulativePropertyValuesByName
        } as Map
        new BeanState(propertyNamesToValues)
    }

    BeanState ignoreProperties(Collection<String> propertiesToIgnore) {
        return new BeanState(Maps.filterKeys(propertyNamesToValues, { !(it in propertiesToIgnore) } as Predicate))
    }

    public <T> T injectState(T targetBean) {
        // Gather the property metadata from the targetBean for state that we may possess
        final Map<String, MetaProperty> targetMetaPropertiesByName = targetBean.metaClass.getProperties().inject([:]) {
                Map<String, MetaProperty> accumulativeMetaPropertiesByName, MetaProperty metaProperty ->
            String propName = metaProperty.name
            if (propName in propertyNamesToValues.keySet()) {
                accumulativeMetaPropertiesByName[propName] = metaProperty
            }
            accumulativeMetaPropertiesByName
        } as Map<String, MetaProperty>

        // Inject properties that exist and look similar
        propertyNamesToValues.each { key, value ->
            final MetaProperty metaProperty = targetMetaPropertiesByName[key]
            if (!metaProperty || metaProperty instanceof MixinInstanceMetaProperty) {
                return // targetBean doesn't have one of these properties
            }
            // If it is a primitive type, box it to simplify comparing types.
            // Class.isAssignableFrom() cannot handle autoboxing.
            final Class type
            if (metaProperty.type.isPrimitive()) {
                type = targetBean.getProperty(key).class // This works because a primitive won't be null or a subtype
            } else {
                type = metaProperty.type
            }
            final Boolean isPropertyAssignableForThisValue = type.isAssignableFrom(value.getClass())
            final String setterName = MetaProperty.getSetterName(key)
            final MetaMethod setter = targetBean.metaClass.pickMethod(setterName, value.getClass())
            if (setter && isPropertyAssignableForThisValue) {
                targetBean.hasProperty(key)?.setProperty(targetBean, value)
            }
        }
        targetBean
    }

    ImmutableMap asMap() {
        propertyNamesToValues
    }

}
