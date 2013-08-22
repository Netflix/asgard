/*
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.asgard.flow

import com.google.common.primitives.Primitives
import groovy.transform.Canonical
import java.lang.reflect.Method

/**
 * Common reflection operations on a class.
 */
@Canonical
class ReflectionHelper {

    final Class type

    /**
     * Find a method on the class.
     *
     * @param name the methodName
     * @param args the argument types
     * @return a Method if the method is found, null if not
     */
    Method findMethodForNameAndArgs(String name, List<Object> args) {
        type.getMethods().find { method ->
            if (method.name != name || method.parameterTypes.size() != args.size()) { return false }
            int index = 0
            Class argType
            Object parameterOfMethodThatDoesNotMatchMethodSignature = args.find { arg ->
                argType = method.parameterTypes[index]
                index++
                !Primitives.wrap(argType).isAssignableFrom(arg.getClass())
            }
            parameterOfMethodThatDoesNotMatchMethodSignature == null
        }
    }

    /**
     * Find a method on the class.
     *
     * @param name the methodName
     * @param args the argument types
     * @return a Method if the method is found, fail if not
     * @throws IllegalStateException if no matching method is found
     */
    Method findMethodForNameAndArgsOrFail(String name, List<Object> args) {
        Method method = findMethodForNameAndArgs(name, args)
        if (method) { return method }
        String msg = "No method found on ${type.simpleName} named '${name}' for args ${args}."
        throw new IllegalStateException(msg)
    }

    /**
     * Find an annotation on the class.
     *
     * @param annotationType the type of annotation to look for
     * @return the annotation if found, null if not
     */
    public <T> T findAnnotationOnClass(Class<T> annotationType) {
        (T) type.declaredAnnotations.find { it.annotationType() == annotationType }
    }

    /**
     * Find an annotation on the class's methods.
     *
     * @param annotationType the type of annotation to look for
     * @param method the method to find the annotation on
     * @return the annotation if found, null if not
     */
    public <T> T findAnnotationOnMethod(Class<T> annotationType, Method method) {
        if (!method) {
            throw new IllegalArgumentException("No method supplied to inspect for annotation ${annotationType}")
        }
        (T) method.declaredAnnotations.find { it.annotationType() == annotationType }
    }

}
