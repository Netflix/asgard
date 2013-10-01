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
package com.netflix.asgard.mock

import org.apache.shiro.SecurityUtils
import org.apache.shiro.UnavailableSecurityManagerException
import org.apache.shiro.subject.Subject
import org.apache.shiro.subject.support.SubjectThreadState
import org.apache.shiro.util.LifecycleUtils
import org.apache.shiro.util.ThreadState

/**
 * Static methods to assist with the testing of classes that leverage Shiro.
 *
 * Based on code from http://shiro.apache.org/testing.html.
 */
abstract class ShiroTestUtil {

    private static ThreadState subjectThreadState

    /**
     * Allows subclasses to set the currently executing {@link Subject} instance.
     *
     * @param subject the Subject instance
     */
    static setSubject(Subject subject) {
        clearSubject()
        subjectThreadState = new SubjectThreadState(subject)
        subjectThreadState.bind()
    }

    /**
     * Clears Shiro's thread state, ensuring the thread remains clean for future test execution.
     */
    static clearSubject() {
        if (subjectThreadState != null) {
            subjectThreadState.clear()
            subjectThreadState = null
        }
    }

    /**
     * Call after test class is completed to cleanup Shiro.
     */
    static tearDownShiro() {
        clearSubject()
        try {
            SecurityManager securityManager = SecurityUtils.getSecurityManager()
            LifecycleUtils.destroy(securityManager)
        } catch (UnavailableSecurityManagerException ignored) {
            // We don't care about this when cleaning up the test environment.
        }
        SecurityUtils.setSecurityManager(null)
    }

}
