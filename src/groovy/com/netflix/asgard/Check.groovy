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

class Check {

    /**
     * Wraps a detail in a standard error message for users to read
     * @param detail specifics of this error
     * @return String the full error message to use
     */
    static String message(def detail) {
        "ERROR: ${detail}"
    }

    /**
     * Throws a NullPointerException if the reference is null
     *
     * @param reference the reference variable to check for null
     * @param type the class object or description of the type of thing that we can't retrieve because of the null input
     * @param variableName the name of the variable that we're checking for null
     * @return Object the reference if not null
     */
    static <T> T notNull(T reference, def type, def variableName = "reference") {
        if (reference == null) {
            throw new NullPointerException(message("Trying to use ${Meta.pretty(type)} with null ${variableName}"))
        }
        reference
    }

    /**
     * Throws an exception if the input is not a collection of exactly 1 element
     *
     * @param collection the collection to inspect
     * @param type the class object or description of the type of collection to describe in the error message
     * @return Object the first and only element of the collection if there is exactly one element
     * @throws IllegalStateException if the collection has 0, 2 or more elements
     * @throws NullPointerException if the collection is null
     */
    static <T> T lone(Collection<T> collection, def type) {
        if (notNull(collection, type).size() != 1) {
            String detail = "Found ${collection.size()} ${Meta.pretty(type)} items instead of 1"
            throw new IllegalStateException(message(detail))
        }
        collection.iterator().next()
    }

    /**
     * Throws an exception if the input is not a collection of exactly 0 or 1 element(s)
     *
     * @param collection the collection to inspect
     * @param type the class object or description of the type of collection to describe in the error message
     * @throws IllegalStateException if the collection has 2 or more elements
     * @throws NullPointerException if the collection is null
     */
    static <T> T loneOrNone(Collection<T> collection, def type) {
        notNull(collection, type)
        Iterator<T> iterator = collection.iterator()
        if (iterator.hasNext()) {
            T firstItem = iterator.next()
            if (iterator.hasNext()) {
                throw new IllegalStateException(
                        message("Found ${collection.size()} ${Meta.pretty(type)} items instead of 0 or 1"))
            }
            return firstItem
        } else {
            // Collection is empty
            return null
        }
    }

    static String notEmpty(String value, String variableName = "reference") {
        notNull(value, String, variableName)
        if (value.isEmpty()) {
            throw new IllegalArgumentException(message("Illegal empty string for ${variableName}"))
        }
        value
    }

    static void notEmpty(Collection items, String variableName = "reference") {
        notNull(items, Collection, variableName)
        if (items.isEmpty()) {
            throw new IllegalArgumentException(message("Illegal empty collection for ${variableName}"))
        }
    }

    static Integer positive(Integer value, String variableName = "reference") {
        atLeast(1, value, variableName)
    }

    static Integer atLeast(Integer minimum, Integer value, String variableName = "reference") {
        notNull(value, Integer, variableName)
        if (value >= minimum) {
            return value
        }
        String msg = message("Illegal '${variableName}' value ${value} is less than minimum ${minimum}")
        throw new IllegalArgumentException(msg)
    }

    static Integer atMost(Integer maximum, Integer value, String variableName = "reference") {
        notNull(value, Integer, variableName)
        if (value <= maximum) {
            return value
        }
        String msg = message("Illegal '${variableName}' value ${value} is greater than maximum ${maximum}")
        throw new IllegalArgumentException(msg)
    }

    static <T> void equal(T inputA, T inputB) {
        notNull(inputA, T)
        notNull(inputB, T)
        if (inputA != inputB) {
            throw new IllegalStateException(message("Expected ${inputA} and ${inputB} to be equal."))
        }
    }

    static void condition(String description, Closure booleanFunction) {
        if (!booleanFunction.call()) {
            throw new IllegalStateException(message(description))
        }
    }
}
