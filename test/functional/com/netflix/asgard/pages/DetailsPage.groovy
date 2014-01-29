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

class DetailsPage extends BasePage {

    String convertToPath(Object[] args) {
        "/us-east-1/${args[0]}/show/${args[1]}"
    }

    static at = { header.contains 'Details' }

    static content = {
        deleteButton(to: [BasePage]) { $("button[name='_action_delete']") }
    }

    void delete() {
        withConfirm(true) { deleteButton.click() }
    }

}