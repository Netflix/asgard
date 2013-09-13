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

import com.amazonaws.services.simpledb.model.Item
import com.google.common.base.Objects
import com.netflix.asgard.model.MonitorBucketType

/**
 * Application registration record.
 */
class AppRegistration {

    static final List<String> ASGARD_ATTRIBUTES = ['monitorBucketType', 'group', 'type', 'description', 'owner',
            'email', 'createTs', 'updateTs']

    String name
    String group
    String type
    String description
    String owner
    String email
    MonitorBucketType monitorBucketType
    Date createTime
    Date updateTime
    Map<String, String> additionalAttributes

    static AppRegistration from(Item item) {
        if (item) {
            String bucketTypeString = item.getAttribute('monitorBucketType')?.value
            MonitorBucketType bucketType = MonitorBucketType.byName(bucketTypeString)
            bucketType = bucketType ?: MonitorBucketType.getDefaultForOldApps()
            Map<String, String> additionalAttributes = item.attributes
                    .findAll { !ASGARD_ATTRIBUTES.contains(it.name) }
                    .collectEntries { [it.name, it.value] }

            return new AppRegistration(
                name: item.name.toLowerCase(),
                group: item.getAttribute('group')?.value,
                type: item.getAttribute('type')?.value,
                description: item.getAttribute('description')?.value,
                owner: item.getAttribute('owner')?.value,
                email: item.getAttribute('email')?.value,
                monitorBucketType: bucketType,
                createTime: asDate(item.getAttribute('createTs')?.value),
                updateTime: asDate(item.getAttribute('updateTs')?.value),
                additionalAttributes: additionalAttributes
            )
        }
        null
    }

    static asDate(timeStamp) {
        timeStamp ? new Date(Long.parseLong(timeStamp)) : null
    }

    int hashCode() {
        Objects.hashCode(name, group, type, description, owner, email, monitorBucketType, createTime, updateTime)
    }

    boolean equals(Object obj) {
        if (!(obj instanceof AppRegistration)) {
            return false
        }
        AppRegistration other = (AppRegistration) obj
        Objects.equal(name, other.name) && Objects.equal(group, other.group) && Objects.equal(type, other.type) &&
            Objects.equal(description, other.description) && Objects.equal(owner, other.owner) &&
            Objects.equal(email, other.email) && Objects.equal(monitorBucketType, other.monitorBucketType) &&
            Objects.equal(createTime, other.createTime) && Objects.equal(updateTime, other.updateTime)
    }

    String toString() {
        "${name} ${group} ${description} ${owner} ${email} ${monitorBucketType} ${createTime} ${updateTime}"
    }
}
