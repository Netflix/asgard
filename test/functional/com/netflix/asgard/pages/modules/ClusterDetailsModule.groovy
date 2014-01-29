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

package com.netflix.asgard.pages.modules

import com.netflix.asgard.pages.TaskPage
import geb.Module

class ClusterDetailsModule extends Module {

    static content = {
        details { $('form') }
        resizeButton(to: TaskPage) { $('.resize') }
        deleteButton(to: TaskPage) { $('.delete') }
        enableButton(to: TaskPage) { $('.trafficEnable') }
        disableButton(to: TaskPage) { $('.trafficDisable') }
        suspendProcessDiv(required: false) { $('.suspendProcess') }
        name { $('h2 a').text() }
        instances { index -> moduleList InstanceModule, $("table tr").tail(), index }
    }

    void resizeTo(int min, int max) {
        details.minSize = min
        details.maxSize = max
        resizeButton.click()
    }

    void delete() {
        withConfirm(true) {
            deleteButton.click()
        }
    }

    void disableTraffic() {
        disableButton.click()
    }

    void enableTraffic() {
        enableButton.click()
    }

    void isSuspended() {
        suspendProcessDiv
    }

    int inServiceInstanceCount() {
        assert instances(0).state == 'InService'
        instances(0).count

    }

}

class InstanceModule extends RowModule {
    static content = {
        count { Integer.parseInt(cell(0)) }
        state { cell(1) }
    }
}
