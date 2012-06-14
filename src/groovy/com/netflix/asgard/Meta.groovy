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

import com.google.common.base.Joiner
import java.lang.reflect.Modifier
import org.apache.commons.lang.StringUtils

/**
 * Utility methods for getting Groovier than Groovy.
 * testing git-p4
 */
class Meta {

    /**
     * Makes a shallow copy of source bean. Any field that lacks a getter or lacks a setter will be skipped.
     *
     * @param original the bean to copy from
     * @return T a new object with the same type and field values as the original object
     */
    static <T> T copy(T original) {
        BeanState.ofSourceBean(original).injectState(original.getClass().newInstance() as T)
    }

    /**
     * Creates a sorted map of strings to objects from any bean.
     *
     * @param input the bean whose properties will be in the map
     * @return Map < String, ? >
     */
    static Map<String, ?> toMap(Object input) {
        Map sorted = input.properties.sort { a, b -> a.key.toString().compareToIgnoreCase(b.key.toString()) }
        sorted.inject([:], { Map map, prop ->
            String key = prop.key
            if (!BeanState.isMetaGarbagePropertyName(key) && !hasStaticField(input.class, key)) {
                map[key] = prop.value
            }
            map
        }) as Map
    }

    /**
     * Checks whether the specified class has a static field with the specified name.
     *
     * @param clazz the Class to check for the specified static field
     * @param name the field name to check
     * @return Boolean true if the class has a static field with the specified name, false otherwise
     */
    private static Boolean hasStaticField(Class clazz, String name) {
        try {
            return Modifier.isStatic(clazz.getDeclaredField(name).modifiers)
        } catch (NoSuchFieldException ignored) {
            return false
        }
    }

    /**
     * Takes a Class object like AutoScalingGroup.class and returns a human-friendly string like "Auto Scaling Group".
     * If any object other than a Class is passed in, this method returns the string equivalent of the object.
     *
     * @param input a class object whose name will be used, or an object whose string representation will be used
     * @return String a human-friendly rendition of the class name, or a string representation of the object
     */
    static String pretty(Object input) {
        if (input instanceof Class) {
            return splitCamelCase(input.getSimpleName())
        }
        String.valueOf(input)
    }

    /**
     * Takes a camel case string and add spaces or another delimiter between the words.
     *
     * 'AutoScalingGroup' > 'Auto Scaling Group'
     * 'SimpleXMLParser' > 'Simple XML Parser'
     *
     * @param input the camel case string to convert
     * @param delimiter the string to insert between words
     * @return String the phrase with the words separated by the delimiter
     */
    static String splitCamelCase(String input, String delimiter = ' ') {
        Joiner.on(delimiter).join(StringUtils.splitByCharacterTypeCamelCase(input) as List)
    }
}
