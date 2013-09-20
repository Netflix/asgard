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
    var url, reloadPage, statusChecker, timeChecker, uptimeUrl, timeUpdater, remainingCachesUrl, remainingCachesLister,
        remainingCacheListUpdater;
    url = window.location.href;
    uptimeUrl = '/server/uptime';
    remainingCachesUrl = '/cache/remaining.json';
    timeUpdater = function(ajaxResponse) {
        jQuery('#timeSinceStartup').html(ajaxResponse);
    };
    remainingCacheListUpdater = function(ajaxResponse) {
        var container = jQuery('<div></div>');
        jQuery.each(ajaxResponse, function(index, item) {
            container.append('<div>' + item + '</div>');
        });
        jQuery('#remainingCaches').html(container);
    };
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

    timeChecker = function() {
        jQuery.get(uptimeUrl, timeUpdater);
    };
    timeChecker();
    window.setInterval(timeChecker, 1000);

    remainingCachesLister = function() {
        jQuery.get(remainingCachesUrl, remainingCacheListUpdater);
    };
    remainingCachesLister();
    window.setInterval(remainingCachesLister, 5000);
});
