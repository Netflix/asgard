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

package com.netflix.asgard.navigator

import geb.Browser
import org.openqa.selenium.WebElement

class NonEmptyNavigator extends geb.navigator.NonEmptyNavigator {
    NonEmptyNavigator(Browser browser, Collection<? extends WebElement> contextElements) {
        super(browser, contextElements)
    }

    void select(value) {
        browser.js.exec(firstElement(), value, 'jQuery(arguments[0]).select2("val", arguments[1]);')
    }

    boolean isStatusEnabled() {
        text() == 'Enabled'
    }
}