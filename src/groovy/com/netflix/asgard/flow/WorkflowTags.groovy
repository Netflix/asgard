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

import com.amazonaws.services.simpleworkflow.flow.DataConverter
import com.amazonaws.services.simpleworkflow.flow.DataConverterException
import com.amazonaws.services.simpleworkflow.flow.JsonDataConverter
import groovy.transform.Canonical
import org.json.simple.parser.JSONParser

/**
 * Handles reading and writing objects to the tags for a SWF workflow. Marshalling and unmarshalling (JSON) is handled
 * for you. You may handle your own tags simply by extending this class and creating the extra properties on the
 * subclass. Currently there is a limit of five tags.
 */
@Canonical
class WorkflowTags {

    static List<String> propertyNamesToIgnore = ['class', 'metaClass', 'propertyNamesToIgnore']

    /** A textual description of the workflow */
    String desc

    /**
     * Populate this object directly from the SWF workflow tags.
     *
     * @param tags of an SWF workflow
     * @return this object with properties set based on the tags
     */
    WorkflowTags withTags(List<String> tags) {
        if (tags == null) { return this }
        Collection<String> propertyNames = (properties.keySet() - propertyNamesToIgnore)
        tags.each { String json ->
            propertyNames.each { String propertyName ->
                populatePropertyFromJson(json, propertyName)
            }
        }
        this
    }

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
            } catch (DataConverterException ignore) {
                // Could not convert data so the property will not be populated
            }
        }
    }

    /**
     * @return tags based on the properties of this class that can be used in an SWF workflow
     */
    List<String> constructTags() {
        DataConverter dataConverter = new JsonDataConverter()
        Map<String, Object> allPropertiesWithValues = getProperties()
        Map<String, Object> propertiesWithValues = allPropertiesWithValues.findAll { it.value != null }
        List<String> propertyNames = propertiesWithValues.keySet().sort() - propertyNamesToIgnore
        propertyNames.collect { String propertyName ->
            String data = dataConverter.toData(this."${propertyName}")
            "{\"${propertyName}\":${data}}" as String
        }
    }

}
