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

class NamesTests extends GroovyTestCase {

    void testLabeledEnvironmentVariables() {
        assert ['export NETFLIX_PARTNERS=sony'] == new Names([partners: 'sony']).labeledEnvironmentVariables('NETFLIX_')
        assert ['export NETFLIX_DEV_PHASE=stage', 'export NETFLIX_PARTNERS=sony'] ==
                new Names([partners: 'sony', devPhase: 'stage']).labeledEnvironmentVariables('NETFLIX_')
    }

    void testParts() {
        assert ['Partners': 'sony'] == new Names([partners: 'sony']).parts()
        assert ['Dev Phase': 'stage', 'Partners': 'sony'] == new Names([partners: 'sony', devPhase: 'stage']).parts()
    }
}
