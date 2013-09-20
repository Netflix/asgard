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
package com.netflix.asgard.deployment

import spock.lang.Specification

class DeploymentWorkflowDescriptionTemplateSpec extends Specification {

    def 'should construct description'() {
        DeploymentWorkflowDescriptionTemplate  descriptionTemplate = new DeploymentWorkflowDescriptionTemplate()

        when:
        descriptionTemplate.deploy(null, new DeploymentWorkflowOptions(clusterName: 'the_seaward'), null, null)

        then:
        "Deploying new ASG to cluster 'the_seaward'" == descriptionTemplate.description
    }
}
