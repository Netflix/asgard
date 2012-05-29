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

import spock.lang.Specification

class EntityTypeSpec extends Specification {

    def 'should return type from valid id'() {
        expect:
        EntityType.instance == EntityType.fromId('i-1bd7b278')
        EntityType.spotInstanceRequest == EntityType.fromId('sir-cd1a3e14')
        EntityType.image == EntityType.fromId('ami-8ceb1be5')
        EntityType.volume == EntityType.fromId('vol-06892b6c')
        EntityType.snapshot == EntityType.fromId('snap-00b46468')
        null == EntityType.fromId('  i-1bd7b278  ')
        null == EntityType.fromId('nflx-1234')
        null == EntityType.fromId('blah')
        null == EntityType.fromId(null)
        null == EntityType.fromId('')
    }

    def 'should return type values'() {
        expect:
        EntityType.values().each { assert it.getClass() == EntityType }
        EntityType.values().size() > 0
    }

    def 'should return name of type'() {
        expect:
        EntityType.instance.name() == 'instance'
        EntityType.autoScaling.name() == 'autoScaling'
        EntityType.applicationInstance.name() == 'applicationInstance'
        EntityType.application.name() == 'application'
        EntityType.cluster.name() == 'cluster'
    }

    def 'should return type from valid name'() {
        expect:
        EntityType.fromName('instance') == EntityType.instance
        EntityType.fromName('autoScaling') == EntityType.autoScaling
        EntityType.fromName('applicationInstance') == EntityType.applicationInstance
        EntityType.fromName('application') == EntityType.application
        EntityType.fromName('cluster') == EntityType.cluster
    }

    def 'should ensure prefix'() {
        expect:
        EntityType.instance.ensurePrefix('i-1bd7b278') == 'i-1bd7b278'
        EntityType.spotInstanceRequest.ensurePrefix('sir-cd1a3e14') == 'sir-cd1a3e14'
        EntityType.image.ensurePrefix('ami-8ceb1be5') == 'ami-8ceb1be5'
        EntityType.volume.ensurePrefix('vol-06892b6c') == 'vol-06892b6c'
        EntityType.snapshot.ensurePrefix('snap-00b46468') == 'snap-00b46468'

        EntityType.instance.ensurePrefix('1bd7b278') == 'i-1bd7b278'
        EntityType.instance.ensurePrefix('blah') == 'i-blah'
        EntityType.instance.ensurePrefix('') == ''
        EntityType.instance.ensurePrefix(null) == null
    }

}
