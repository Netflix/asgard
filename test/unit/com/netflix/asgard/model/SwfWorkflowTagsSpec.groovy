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
package com.netflix.asgard.model

import com.netflix.asgard.EntityType
import com.netflix.asgard.Link
import com.netflix.asgard.Region
import com.netflix.asgard.UserContext
import spock.lang.Specification

class SwfWorkflowTagsSpec extends Specification {

    SwfWorkflowTags workflowTags = new SwfWorkflowTags()

    def 'should construct swf tags'() {
        workflowTags.with {
            desc = 'doing a thing'
            user = UserContext.of('jira-123', 'rtargaryen', "King's Landing", '1.2.3.4', Region.EU_WEST_1, true)
            link = Link.to(EntityType.cluster, 'house-targaryen')
        }

        expect:
        workflowTags.constructTags() as Set == [
                '{"desc":"doing a thing"}',
                '{"user":{"ticket":"jira-123","username":"rtargaryen","clientHostName":"King\'s Landing","clientIpAddress":"1.2.3.4","region":"EU_WEST_1","internalAutomation":true}}',
                '{"link":{"type":{"name":"cluster"},"id":"house-targaryen"}}'
        ] as Set
    }

    def 'should be constructed from swf tags'() {
        workflowTags.withTags([
                '{"desc":"doing a thing"}',
                '{"user":{"ticket":"jira-123","username":"rtargaryen","clientHostName":"King\'s Landing","clientIpAddress":"1.2.3.4","region":"EU_WEST_1","internalAutomation":true}}',
                '{"link":{"type":{"name":"cluster"},"id":"house-targaryen"}}'
        ])

        expect:
        workflowTags.desc == 'doing a thing'
        workflowTags.user == UserContext.of('jira-123', 'rtargaryen', "King's Landing", '1.2.3.4',
                Region.EU_WEST_1, true)
        workflowTags.link == Link.to(EntityType.cluster, 'house-targaryen')
    }

    def 'should be able to modify tags without changing original'() {
        List<String> originalTags = [
                '{"desc":"doing a thing"}',
                '{"user":{"ticket":"jira-123","username":"rtargaryen","clientHostName":"King\'s Landing","clientIpAddress":"1.2.3.4","region":"EU_WEST_1","internalAutomation":true}}',
                '{"link":{"type":{"name":"cluster"},"id":"house-targaryen"}}'
        ]
        workflowTags.withTags(originalTags)
        workflowTags.with {
            desc = 'doing a different thing'
            user = UserContext.of('jira-456', 'rtargaryen', "King's Landing", '1.2.3.4', Region.EU_WEST_1, true)
            link = Link.to(EntityType.cluster, 'hizzouse-targaryen')
        }

        expect:
        workflowTags.constructTags() as Set == [
                '{"desc":"doing a different thing"}',
                '{"user":{"ticket":"jira-456","username":"rtargaryen","clientHostName":"King\'s Landing","clientIpAddress":"1.2.3.4","region":"EU_WEST_1","internalAutomation":true}}',
                '{"link":{"type":{"name":"cluster"},"id":"hizzouse-targaryen"}}'
        ] as Set
        originalTags == [
                '{"desc":"doing a thing"}',
                '{"user":{"ticket":"jira-123","username":"rtargaryen","clientHostName":"King\'s Landing","clientIpAddress":"1.2.3.4","region":"EU_WEST_1","internalAutomation":true}}',
                '{"link":{"type":{"name":"cluster"},"id":"house-targaryen"}}'
        ]
    }

    def 'should handle tags in any order'() {
        workflowTags.withTags([
                '{"user":{"ticket":"jira-123","username":"rtargaryen","clientHostName":"King\'s Landing","clientIpAddress":"1.2.3.4","region":"EU_WEST_1","internalAutomation":true}}',
                '{"desc":"doing a thing"}',
                '{"link":{"type":{"name":"cluster"},"id":"house-targaryen"}}'
        ])

        expect:
        workflowTags.desc == 'doing a thing'
        workflowTags.user == UserContext.of('jira-123', 'rtargaryen', "King's Landing", '1.2.3.4',
                Region.EU_WEST_1, true)
        workflowTags.link == Link.to(EntityType.cluster, 'house-targaryen')
    }

    def 'should attempt even with wrong number of tags'() {
        workflowTags.withTags(['{"link":{"type":{"name":"cluster"},"id":"house-targaryen"}}'])

        expect:
        workflowTags.desc == null
        workflowTags.user == null
        workflowTags.link == Link.to(EntityType.cluster, 'house-targaryen')
    }

    def 'should quietly handle unserializable data'() {
        workflowTags.withTags(['', '', null ])

        expect:
        workflowTags.constructTags() == []
        workflowTags.desc == null
        workflowTags.user == null
        workflowTags.link == null
    }

    def 'should do nothing with null tags'() {
        workflowTags.withTags(null)

        expect:
        workflowTags.constructTags() == []
        workflowTags.desc == null
        workflowTags.user == null
        workflowTags.link == null
    }

}
