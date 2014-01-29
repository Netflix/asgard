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

package com.netflix.asgard.pages

import com.netflix.asgard.pages.modules.ClusterDetailsModule
import com.netflix.asgard.pages.modules.NewClusterModule

class ManageClusterPage extends BasePage {

    static url = "/us-east-1/cluster/show"

    static at = { header == 'Manage Cluster of Sequential Auto Scaling Groups' }

    static content = {
        cluster(required: false) { index -> moduleList ClusterDetailsModule, $(".clusterAsgForm:not(.create)"), index }
        newCluster(required: false) { module NewClusterModule, $(".clusterAsgForm.create") }
    }

}