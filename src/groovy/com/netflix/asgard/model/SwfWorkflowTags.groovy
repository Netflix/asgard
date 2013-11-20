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

import com.amazonaws.services.simpleworkflow.flow.DataConverter
import com.amazonaws.services.simpleworkflow.flow.JsonDataConverter
import com.netflix.asgard.Link
import com.netflix.asgard.UserContext
import com.netflix.glisten.WorkflowTags
import groovy.transform.Canonical
import org.json.simple.parser.JSONParser

/**
 * Asgard specific tags for an SWF workflow.
 */
@Canonical
class SwfWorkflowTags extends WorkflowTags {

    /** Unique ID assigned by Asgard. It simplifies managing workflow executions if they have a single ID. */
    String id

    /** A link that corresponds to the workflow for use in constructing an Asgard Task */
    Link link

    /** A UserContext that corresponds to the workflow for use in constructing an Asgard Task */
    UserContext user

    /**
     * TODO remove this once the new implementation has been moved to WorkflowTags
     */
    @SuppressWarnings('CatchException')
    protected void populatePropertyFromJson(String json, String key) {
        JSONParser jsonParser = new JSONParser()
        DataConverter dataConverter = new JsonDataConverter()
        String valueString = null
        try {
            valueString = jsonParser.parse(json ?: '""')?."${key}"
        } catch (Exception ignore) {
            // This is not the property we are looking for, no reason to fail
        }
        if (valueString) {
            Class type = hasProperty(key)?.type
            try {
                def value = valueString
                if (type != String) {
                    value = dataConverter.fromData(valueString, type)
                }
                this."${key}" = value
            } catch (Exception ignore) {
                // Could not convert data so the property will not be populated
            }
        }
    }

    /**
     * @return tags based on the properties of this class that can be used in an SWF workflow
     * TODO remove this once constructTag has been moved to WorkflowTags
     */
    @Override
    List<String> constructTags() {
        Map<String, Object> allPropertiesWithValues = properties
        Map<String, Object> propertiesWithValues = allPropertiesWithValues.findAll { it.value != null }
        List<String> propertyNames = propertiesWithValues.keySet().sort() - propertyNamesToIgnore
        propertyNames.collect { constructTag(it) }
    }

    /**
     * @return constructs a single tag value
     * TODO put this in WorkflowTags
     */
    String constructTag(String propertyName) {
        DataConverter dataConverter = new JsonDataConverter()
        String data = dataConverter.toData(this."${propertyName}")
        "{\"${propertyName}\":${data}}"
    }
}
