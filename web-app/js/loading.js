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
jQuery(function() {
    var url, reloadPage, statusChecker;
    url = window.location.href;
    reloadPage = function() {
        window.location.replace(url);
    };
    statusChecker = function() {
        jQuery.ajax({
            url: url,
            success: reloadPage,
            error: function(jqXHR, textStatus, errorThrown){
                if (jqXHR.status == 503) {
                    window.setTimeout(statusChecker, 5000);
                } else {
                    // If the page is throwing another type of error let the user see it
                    reloadPage();
                }
            }
        });
    };
    window.setTimeout(statusChecker, 5000);
});
