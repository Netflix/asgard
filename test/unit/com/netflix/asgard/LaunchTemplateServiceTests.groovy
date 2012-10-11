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

import com.netflix.asgard.mock.Mocks

class LaunchTemplateServiceTests extends GroovyTestCase {

    void setUp() {
        Mocks.createDynamicMethods()
    }

    void testIncludeDefaultSecurityGroups() {
        LaunchTemplateService launchTemplateService = Mocks.launchTemplateService()
        List<String> original = ["account_batch", "abcache"]
        Collection<String> result = launchTemplateService.includeDefaultSecurityGroups(original)

        assert !original.contains("nf-infrastructure")
        assert !original.contains("nf-datacenter")
        assert result.contains("nf-infrastructure")
        assert result.contains("nf-datacenter")
        assert result.contains("account_batch")
        assert result.contains("abcache")

        assert 4 == result.size()
    }

    void testIncludeDefaultSecurityGroupsWithoutDuplication() {
        LaunchTemplateService launchTemplateService = Mocks.launchTemplateService()
        List<String> original = ["account_batch", "nf-infrastructure"]
        Collection<String> result = launchTemplateService.includeDefaultSecurityGroups(original)

        assert original.contains("nf-infrastructure")
        assert !original.contains("nf-datacenter")
        assert result.contains("nf-infrastructure")
        assert result.contains("nf-datacenter")
        assert result.contains("account_batch")

        assert 3 == result.size()
    }
}
