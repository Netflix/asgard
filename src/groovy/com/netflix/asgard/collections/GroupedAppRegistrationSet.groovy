/*
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.asgard.collections

import com.netflix.asgard.AppRegistration

/**
 * This data structure is responsible for providing a name-based grouping of {@link AppRegistration} objects.
 * Upon being added to the set, a grouping of AppRegistration objects by appGroup
 */
class GroupedAppRegistrationSet extends TreeSet<AppRegistration> {
    static final String DEFAULT_BLANK_GROUP_NAME = ""

    private final Map<String, List<AppRegistration>> appGroups = [:]
    private final Map<String, List<AppRegistration>> tags = [:]

    /**
     * Constructs a new TreeSet, containing a list of provided @{link AppRegistration} objects. This Set will sort
     * AppRegistration objects on the "name" property.
     */
    GroupedAppRegistrationSet(final List<AppRegistration> apps) {
        super({ AppRegistration a, AppRegistration b ->
            a.name <=> b.name
        } as Comparator<AppRegistration>)

        apps.each { add it }
    }

    @Override
    boolean add(AppRegistration app) {
        def group = app.group ?: DEFAULT_BLANK_GROUP_NAME
        if (!appGroups[group]) {
            appGroups[group] = []
        }
        appGroups[group] << app

        app.tags.each { String tag ->
            if (!tags[tag]) {
                tags[tag] = []
            }
            tags[tag] << app
        }
        super.add app
    }

    @Override
    boolean remove(Object obj) {
        if (!(obj instanceof AppRegistration)) {
            super.remove obj
        }

        def app = (AppRegistration)obj

        removeFromList appGroups[app.group ?: DEFAULT_BLANK_GROUP_NAME], app

        app.tags.each { String tag ->
            removeFromList tags[tag], app
        }

        super.remove app
    }

    /**
     * This method returns a Map, which contains a list of @{link AppRegistration} objects, keyed by application group
     * name.
     *
     * @return Map
     */
    Map<String, List<AppRegistration>> groups() {
        appGroups
    }

    /**
     * This method returns a Map, which contains a list of @{link AppRegistration} objects, keyed by tag
     *
     * @return Map
     */
    Map<String, List<AppRegistration>> tags() {
        tags
    }

    private static void removeFromList(List<AppRegistration> apps, AppRegistration app) {
        if (apps?.contains(app)) {
            apps.remove app
        }
    }
}
