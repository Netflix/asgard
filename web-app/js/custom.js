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
// Custom jQuery methods
jQuery.fn.extend({

    // Returns true if there are any elements in the jQuery wrapped set. Otherwise returns false.
    exists: function() {
        return jQuery(this).size() >= 1;
    },

    // Gets or sets the src attribute of an element in the jQuery wrapped set.
    src: function(newSrc) {
        return jQuery(this).attr('src', newSrc);
    },

    // Determines whether or not the input is checked.
    checked: function() {
        return jQuery(this).is(":checked");
    },

    // Gets an array of values from form elements
    values: function() {
        var result = [];
        jQuery(this).each(function() {
            result.push(this.value);
        });
        return value;
    },

    // Prevent typing illegal characters in special text inputs.
    restrictCharInput: function(charPattern) {
        jQuery(this).keypress(function (e) {
            var key = String.fromCharCode(e.keyCode || e.which);
            // Is the key allowed?
            if (!(charPattern.test(key))) {
                // Cancel the original event
                e.preventDefault();
                e.stopPropagation();
            }
        });
    },

    // Changes the background to yellow then fades it away to nothing
    yellowFade: function(millis) {
        jQuery(this).effect("highlight", {}, millis || 1500);
    }
});

jQuery.extend({
    // Convert a browser-specific element collection into a regular array. Copied from Prototype's $A function.
    array: function(iterable) {
      if (!iterable) { return []; }
      if ('toArray' in Object(iterable)) { return iterable.toArray(); }
      var length = iterable.length || 0, results = new Array(length);
      while (length--) { results[length] = iterable[length]; }
      return results;
    }
});

jQuery(document).ready(function() {

    /**
     * http://www.openjs.com/scripts/events/keyboard_shortcuts/
     * Version : 2.01.B
     * By Binny V A
     * License : BSD
     */
    var shortcut = {
        'all_shortcuts': {}, // All the shortcuts are stored in this array
        'add': function(shortcut_combination, callback, opt) {
            // Provide a set of default options
            var default_options = {
                'type': 'keydown',
                'propagate': false,
                'disable_in_input': false,
                'target': document,
                'keycode': false
            };
            if (!opt) { opt = default_options; }
            else {
                for (var dfo in default_options) {
                    if (typeof opt[dfo] === 'undefined') { opt[dfo] = default_options[dfo]; }
                }
            }

            var ele = opt.target;
            if (typeof opt.target === 'string') { ele = document.getElementById(opt.target); }
            shortcut_combination = shortcut_combination.toLowerCase();

            // The function to be called at keypress
            var func = function(e) {
                e = e || window.event;

                if (opt['disable_in_input']) { // Don't enable shortcut keys in Input, Textarea fields
                    var element = e.target || e.srcElement;
                    if (element.nodeType === 3) { element = element.parentNode; }
                    if (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA') { return; }
                }

                // Find which key is pressed
                var code = e.keyCode || e.which;
                var character = String.fromCharCode(code).toLowerCase();

                if (code === 188) { character = ","; } // If the user presses , when the type is onkeydown
                if (code === 190) { character = "."; } // If the user presses . when the type is onkeydown

                var keys = shortcut_combination.split("+");
                // Key Pressed - counts the number of valid keypresses - if it is same as the number of keys, the shortcut function is invoked
                var kp = 0;

                // Work around for stupid shift key bug created by using lowercase - as a result the shift+num combination was broken
                var shift_nums = {
                    "`": "~",
                    "1": "!",
                    "2": "@",
                    "3": "#",
                    "4": "$",
                    "5": "%",
                    "6": "^",
                    "7": "&",
                    "8": "*",
                    "9": "(",
                    "0": ")",
                    "-": "_",
                    "=": "+",
                    ";": ":",
                    "'": '"',
                    ",": "<",
                    ".": ">",
                    "/": "?",
                    "\\": "|"
                };
                // Special Keys - and their codes
                var special_keys = {
                    'esc': 27,
                    'escape': 27,
                    'tab': 9,
                    'space': 32,
                    'return': 13,
                    'enter': 13,
                    'backspace': 8,

                    'scrolllock': 145,
                    'scroll_lock': 145,
                    'scroll': 145,
                    'capslock': 20,
                    'caps_lock': 20,
                    'caps': 20,
                    'numlock': 144,
                    'num_lock': 144,
                    'num': 144,

                    'pause': 19,
                    'break': 19,

                    'insert': 45,
                    'home': 36,
                    'delete': 46,
                    'end': 35,

                    'pageup': 33,
                    'page_up': 33,
                    'pu': 33,

                    'pagedown': 34,
                    'page_down': 34,
                    'pd': 34,

                    'left': 37,
                    'up': 38,
                    'right': 39,
                    'down': 40,

                    'f1': 112,
                    'f2': 113,
                    'f3': 114,
                    'f4': 115,
                    'f5': 116,
                    'f6': 117,
                    'f7': 118,
                    'f8': 119,
                    'f9': 120,
                    'f10': 121,
                    'f11': 122,
                    'f12': 123
                };

                var modifiers = {
                    shift: { wanted: false, pressed: false },
                    ctrl : { wanted: false, pressed: false },
                    alt  : { wanted: false, pressed: false },
                    meta : { wanted: false, pressed: false } // Meta is Mac specific
                };

                if (e.ctrlKey)  { modifiers.ctrl.pressed = true; }
                if (e.shiftKey) { modifiers.shift.pressed = true; }
                if (e.altKey)   { modifiers.alt.pressed = true; }
                if (e.metaKey)  { modifiers.meta.pressed = true; }

                var i, k;
                for (i = 0; k = keys[i], i < keys.length; i++) {
                    // Modifiers
                    if (k === 'ctrl' || k === 'control') {
                        kp++;
                        modifiers.ctrl.wanted = true;
                    } else if (k === 'shift') {
                        kp++;
                        modifiers.shift.wanted = true;
                    } else if (k === 'alt') {
                        kp++;
                        modifiers.alt.wanted = true;
                    } else if (k === 'meta') {
                        kp++;
                        modifiers.meta.wanted = true;
                    } else if (k.length > 1) { // If it is a special key
                        if (special_keys[k] === code) { kp++; }
                    } else if (opt['keycode']) {
                        if (opt['keycode'] === code) { kp++; }
                    } else { // The special keys did not match
                        if (character === k) { kp++; }
                        else {
                            if (shift_nums[character] && e.shiftKey) { // Stupid Shift key bug created by using lowercase
                                character = shift_nums[character];
                                if (character === k) { kp++; }
                            }
                        }
                    }
                }

                if (kp === keys.length &&
                            modifiers.ctrl.pressed === modifiers.ctrl.wanted &&
                            modifiers.shift.pressed === modifiers.shift.wanted &&
                            modifiers.alt.pressed === modifiers.alt.wanted &&
                            modifiers.meta.pressed === modifiers.meta.wanted) {
                    callback(e);

                    if (!opt['propagate']) { // Stop the event
                        // e.cancelBubble is supported by IE - this will kill the bubbling process.
                        e.cancelBubble = true;
                        e.returnValue = false;

                        // e.stopPropagation works in Firefox.
                        if (e.stopPropagation) {
                            e.stopPropagation();
                            e.preventDefault();
                        }
                        return false;
                    }
                }
            };
            this.all_shortcuts[shortcut_combination] = {
                'callback': func,
                'target': ele,
                'event': opt['type']
            };
            // Attach the function with the event
            if (ele.addEventListener) { ele.addEventListener(opt['type'], func, false); }
            else if (ele.attachEvent) { ele.attachEvent('on' + opt['type'], func); }
            else { ele['on' + opt['type']] = func; }
        },

        // Remove the shortcut - just specify the shortcut and I will remove the binding
        'remove': function(shortcut_combination) {
            shortcut_combination = shortcut_combination.toLowerCase();
            var binding = this.all_shortcuts[shortcut_combination];
            delete(this.all_shortcuts[shortcut_combination]);
            if (!binding) { return; }
            var type = binding['event'];
            var ele = binding['target'];
            var callback = binding['callback'];

            if (ele.detachEvent) { ele.detachEvent('on' + type, callback); }
            else if (ele.removeEventListener) { ele.removeEventListener(type, callback, false); }
            else { ele['on' + type] = false; }
        }
    };

    /**
     * lscache library
     *
     * https://github.com/pamelafox/lscache
     *
     * Copyright (c) 2011, Pamela Fox
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *       http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */

    /**
     * Creates a namespace for the lscache functions.
     */
    var lscache = function() {
        // Prefixes the key name on the expiration items in localStorage
        var CACHESUFFIX = '-cacheexpiration';

        // Determines if localStorage is supported in the browser;
        // result is cached for better performance instead of being run each time.
        // Feature detection is based on how Modernizr does it;
        // it's not straightforward due to FF4 issues.
        var supportsStorage = function () {
            try {
                return !!localStorage.getItem;
            } catch (e) {
                return false;
            }
        }();

        // Determines if native JSON (de-)serialization is supported in the browser.
        var supportsJSON = (window.JSON != null);

        /**
         * Returns the full string for the localStorage expiration item.
         * @param {String} key
         * @return {string}
         */
        function expirationKey(key) {
            return key + CACHESUFFIX;
        }

        /**
         * Returns the number of minutes since the epoch.
         * @return {number}
         */
        function currentTime() {
            return Math.floor((new Date().getTime())/60000);
        }

        return {

            /**
             * Stores the value in localStorage. Expires after specified number of minutes.
             * @param {string} key
             * @param {Object|string} value
             * @param {number} time
             */
            set: function(key, value, time) {
                if (!supportsStorage) return;

                // If we don't get a string value, try to stringify
                // In future, localStorage may properly support storing non-strings
                // and this can be removed.
                if (typeof value != 'string') {
                    if (!supportsJSON) return;
                    try {
                        value = JSON.stringify(value);
                    } catch (e) {
                        // Sometimes we can't stringify due to circular refs
                        // in complex objects, so we won't bother storing then.
                        return;
                    }
                }

                try {
                    localStorage.setItem(key, value);
                } catch (e) {
                    if (e.name === 'QUOTA_EXCEEDED_ERR' || e.name == 'NS_ERROR_DOM_QUOTA_REACHED') {
                        // If we exceeded the quota, then we will sort
                        // by the expire time, and then remove the N oldest
                        var storedKey, storedKeys = [];
                        for (var i = 0; i < localStorage.length; i++) {
                            storedKey = localStorage.key(i);
                            if (storedKey.indexOf(CACHESUFFIX) > -1) {
                                var mainKey = storedKey.split(CACHESUFFIX)[0];
                                storedKeys.push({key: mainKey, expiration: parseInt(localStorage[storedKey], 10)});
                            }
                        }
                        storedKeys.sort(function(a, b) { return (a.expiration-b.expiration); });

                        for (var i = 0, len = Math.min(30, storedKeys.length); i < len; i++) {
                            localStorage.removeItem(storedKeys[i].key);
                            localStorage.removeItem(expirationKey(storedKeys[i].key));
                        }
                        // TODO: This could still error if the items we removed were small and this is large
                        localStorage.setItem(key, value);
                    } else {
                        // If it was some other error, just give up.
                        return;
                    }
                }

                // If a time is specified, store expiration info in localStorage
                if (time) {
                    localStorage.setItem(expirationKey(key), currentTime() + time);
                } else {
                    // In case they set a time earlier, remove that info from localStorage.
                    localStorage.removeItem(expirationKey(key));
                }
            },

            /**
             * Retrieves specified value from localStorage, if not expired.
             * @param {string} key
             * @return {string|Object}
             */
            get: function(key) {
                if (!supportsStorage) return null;

                /**
                 * Tries to de-serialize stored value if its an object, and returns the
                 * normal value otherwise.
                 * @param {String} key
                 */
                function parsedStorage(key) {
                    if (supportsJSON) {
                        try {
                            // We can't tell if its JSON or a string, so we try to parse
                            var value = JSON.parse(localStorage.getItem(key));
                            return value;
                        } catch(e) {
                            // If we can't parse, it's probably because it isn't an object
                            return localStorage.getItem(key);
                        }
                    } else {
                        return localStorage.getItem(key);
                    }
                }

                // Return the de-serialized item if not expired
                if (localStorage.getItem(expirationKey(key))) {
                    var expirationTime = parseInt(localStorage.getItem(expirationKey(key)), 10);
                    // Check if we should actually kick item out of storage
                    if (currentTime() >= expirationTime) {
                        localStorage.removeItem(key);
                        localStorage.removeItem(expirationKey(key));
                        return null;
                    } else {
                        return parsedStorage(key);
                    }
                } else if (localStorage.getItem(key)) {
                    return parsedStorage(key);
                }
                return null;
            },

            /**
             * Removes a value from localStorage.
             * Equivalent to 'delete' in memcache, but that's a keyword in JS.
             * @param {string} key
             */
            remove: function(key) {
                if (!supportsStorage) return null;
                localStorage.removeItem(key);
                localStorage.removeItem(expirationKey(key));
            }
        };
    }();

    var setUpTicketForm = function() {
        // If the page has any forms worth submitting then bind the visible ticket field to all the regular forms
        var ticketOuter, normalForms, metaInputs, formSubmitInterceptor;
        ticketOuter = jQuery('.ticket');
        normalForms = jQuery('form').has('button[type="submit"]');
        metaInputs = ticketOuter.find('input[type="text"]');
        metaInputs.each(function() {
            var jInput, value, clearError;
            jInput = jQuery(this);
            value = lscache.get(this.name);
            if (value) {
                this.value = value;
            }
            clearError = function() {
                if (jInput.val().trim()) {
                    jInput.removeClass('error');
                }
                return true;
            };
            jInput.change(function() {
                lscache.set(this.name, this.value, 4 * 60); // Minutes
                return clearError();
            });
            jInput.keyup(clearError);
        });
        if (normalForms.exists()) {
            metaInputs.each(function() {
                var metaInput, metaInputName, metaInputValue, hiddenInputs;
                metaInput = jQuery(this);
                metaInputName = this.name;
                metaInputValue = this.value;
                hiddenInputs = [];

                normalForms.each(function() {
                    var hiddenInput, normalForm;
                    normalForm = jQuery(this);
                    hiddenInput = jQuery('<input type="hidden"/>')[0];
                    hiddenInput.name = metaInputName;
                    hiddenInput.value = metaInputValue;
                    normalForm.prepend(hiddenInput);
                    hiddenInputs.push(hiddenInput);
                });

                metaInput.change(function() {
                    var i;

                    // Change the value of the relevant hidden input in every regular form
                    for (i = 0; i < hiddenInputs.length; i++) {
                        hiddenInputs[i].value = this.value;
                    }
                    return true;
                });
            });

            formSubmitInterceptor = function(event) {
                // Validate the ticket meta inputs
                var okayToSubmit = true;
                metaInputs.each(function() {
                    var value, jInput;
                    jInput = jQuery(this);
                    value = this.value ? this.value.trim() : null;
                    if (!value && jInput.hasClass('required')) {
                        jInput.addClass('error');
                        alert(jInput.attr('title') + ' is required for production changes');
                        jQuery(window).scrollTop(0);
                        jInput.addClass('error').focus().parent().yellowFade();
                        okayToSubmit = false;
                    }
                });
                return okayToSubmit;
            };

            normalForms.submit(formSubmitInterceptor);
        }
        ticketOuter.find('.clear').click(function() {
            metaInputs.val('').change();
            return true;
        });
    };
    setUpTicketForm();

    var setUpValidation = function() {
        var requiredDigits = {
            required: true,
            digits: true
        };

        var digitsOnly = {
            digits: true
        };

        // Make forms validate
        jQuery('form.validate').validate({
            rules: {
                relaunchCount: requiredDigits,
                concurrentRelaunches: requiredDigits,
                afterBootWait: requiredDigits,
                blueOutMinutes: digitsOnly,
                '.number': digitsOnly
            }
        });
    };
    setUpValidation();

    var setUpCommonUserInterfaceEnhancements = function() {

        // Don't let any ajax responses use browser cache
        jQuery.ajaxSetup({ cache: false });

        // Decorate the menu buttons that have drop down lists. Do the work that CSS3 isn't ready to do yet.
        jQuery('.menuButton').has('ul').addClass('dropdown');

        // Add confirmation to delete buttons, then display the buttons
        jQuery('button.delete').bind('click keypress', function() {
            var warning = jQuery(this).data('warning') || 'Are you sure you want to delete this object?';
            return confirm(warning);
        }).show();

        // When the user changes the regionSwitcher drop-down, redirect to a similar page for the selected region.
        jQuery('#regionSwitcher').change(function() {
            var params, query, nonQueryParamNames, region, controller, action, id, url, param;
            params = window.browserGlobalsFromServer.params;
            query = '';
            nonQueryParamNames = ['region', 'controller', 'action', 'id'];
            controller = params['controller'];
            action = params['action'];
            id = params['id'];
            region = this.value;
            url = '/' + region + '/' + controller;
            if (action && action !== 'index') {
                url += '/' + action + (id ? '/' + id : '');
            }

            // Are there any other params?
            for (param in params) {
                if (params.hasOwnProperty(param) && jQuery.inArray(param, nonQueryParamNames) < 0) {
                    query += (query ? '&' : '?') + param + '=' + encodeURIComponent(params[param]);
                }
            }
            url += query;

            window.location = url;
        });

        jQuery('.countAndList').each(function() {
            var instanceSetContainer = jQuery(this);
            instanceSetContainer.find('.toggle').click(function(evt) {
                evt.preventDefault();
                instanceSetContainer.toggleClass('hideAdvancedItems');
            });
        });

        // If there's a textarea that should be resizable, make it so.
        jQuery('textarea.resizable').TextAreaResizer();
    };
    setUpCommonUserInterfaceEnhancements();

    // Push prepare page
    var setUpPushPreparePage = function() {
        jQuery('#checkHealth').change(function() {
            var waitTr = jQuery('tr.waitTime');
            if (jQuery(this).checked()) { waitTr.hide(); }
            else { waitTr.show().yellowFade(); }
            return true;
        });
    };
    setUpPushPreparePage();

    // Associate EIP page
    var setUpEipInstanceAssociatePage = function() {
        jQuery('form:has(select#publicIp)').submit(function() {
            var instanceId = jQuery('input#instanceId').val();
            var selectedValue = jQuery('select#publicIp').val();
            return confirm('Be careful not to steal an IP accidentally.\n\nAre you sure you want to reassign elastic IP ' +
                    selectedValue + ' to instance ' + instanceId + ' ?');
        });
    };
    setUpEipInstanceAssociatePage();

    var setCustomOrExistingInput = function() {
        jQuery('.customOrExistingInput').each(function() {
            var togglable, custom, existing, toggler;
            togglable = jQuery(this);
            custom = togglable.find(':has(input[type=text])');
            existing = togglable.find(':has(select)');
            toggler = togglable.find('input[type=checkbox]');
            var toggle = function() {
                var isExistingShown;
                isExistingShown = toggler.checked();
                custom.find(':input').prop('disabled', isExistingShown);
                existing.find(':input').prop('disabled', !isExistingShown);
                custom.toggleClass('concealed', isExistingShown);
                existing.toggleClass('concealed', !isExistingShown);
            };
            toggler.click(toggle);
        });
    };
    setCustomOrExistingInput();

    var setUpEnableVpc = function() {
        var enableVpc, vpcId, isVpc;
        enableVpc = jQuery('#enableVpc');
        vpcId = jQuery('#vpcId');
        enableVpc.click(function() {
            isVpc = enableVpc.is(':checked');
            vpcId.prop('disabled', !isVpc);
            vpcId.select2(isVpc ? 'enable' : 'disable')
        });
    };
    setUpEnableVpc();

    var setJsonValue = function() {
        jQuery('.jsonValue').each(function() {
            var jsonString, jsonValue, formattedJsonValue, originalJson, advancedJson, toggle, jsonContainer;
            originalJson = jQuery(this).find('div.originalJson');
            advancedJson = jQuery(this).find('div.advancedJson');
            toggle = advancedJson.find('span.toggle');
            jsonContainer = advancedJson.find('pre');

            // Make sure the text looks like JSON, some simple strings are not JSON but parse anyway.
            jsonString = originalJson.text();
            if (!(jsonString.substring(0, 1) == '{')) {
                return;
            }
            // Attempt to parse JSON.  If we can't then do nothing.
            try {
                jsonValue = jQuery.parseJSON(jsonString);
            } catch (err) {
                return;
            }

            // There is no turning back now. Hide the original content and try to parse the JSON.
            originalJson.addClass('concealed');
            try {
                formattedJsonValue = JSON.stringify(jsonValue, null, 4);
            } catch (err) {
                // Degrade gracefully if browser does not support JSON formatting.
                formattedJsonValue = jsonString;
            }

            // Add the formatted JSON to the element and style them appropriately.
            jsonContainer.append(formattedJsonValue);
            advancedJson.removeClass('concealed');

            // Add hide/show behavior when clicking.
            toggle.click(function() {
                advancedJson.toggleClass('hideAdvancedItems');
                advancedJson.find('pre:visible').yellowFade();
            });
        });
    };
    setJsonValue();

    var setUpVpcRelatedAttributes = function() {
        jQuery('input[name="subnetPurpose"]').click(function() {
            var vpcId, vpcIdLocator, purpose, purposeLocator, displaySelected;

            displaySelected = function(elements, locator) {
                var selected, unselected;
                selected = elements.filter(locator);
                unselected = elements.not(locator);
                unselected.children(':input').prop('disabled', true);
                unselected.children('select').select2('disable');
                selected.children(':input').prop('disabled', false);
                selected.children('select').select2('enable');
                unselected.addClass('concealed');
                selected.removeClass('concealed');
            };

            vpcId = jQuery(this).data('vpcid');
            vpcIdLocator = '.vpcId' + vpcId;
            displaySelected(jQuery('.securityGroupsSelect'), vpcIdLocator);
            displaySelected(jQuery('.loadBalancersSelect'), vpcIdLocator);

            purpose = jQuery(this).data('purpose');
            purposeLocator = '.subnetPurpose' + purpose;
            displaySelected(jQuery('.zonesSelect'), purposeLocator);
            enableSelect2ForVisible();
        });
    };
    setUpVpcRelatedAttributes();

    // Cluster page
    var setUpClusterPage = function() {
        var config, jCreateContainer, jCreateAdvancedTrs;
        config = {
            defaultBatchSize: window.browserGlobalsFromServer.groupResizeDefaultBatchSize
        };
        jCreateContainer = jQuery('.groupReplacingPush .create');
        jCreateAdvancedTrs = jCreateContainer.find('.advanced');
        jQuery('#showAdvancedOptionsToCreateNextGroup').click(function() {
            jCreateContainer.toggleClass('hideAdvancedItems');
            enableSelect2ForVisible();
            jCreateAdvancedTrs.find(':visible').yellowFade();
        });

        jQuery('.groupReplacingPush > li:not(.create)').each(function() {
            var jGroupContainer, jBatchResizeContainer, jBatchSizeInput, jGroupSizeInputs, inputControllers,
                    doAnyInputsNeedBatching;
            inputControllers = [];
            jGroupContainer = jQuery(this);
            jBatchResizeContainer = jGroupContainer.find('.batchResizeContainer');
            jBatchSizeInput = jBatchResizeContainer.find('input[name=batchSize]');
            jGroupSizeInputs = jGroupContainer.find('input.groupSize');
            doAnyInputsNeedBatching = function() {
                var i;
                for (i = 0; i < inputControllers.length; i++) {
                    if (inputControllers[i].needsBatching()) {
                        return true;
                    }
                }
                return false;
            };
            jGroupSizeInputs.each(function() {
                var jInput, initValue;
                jInput = jQuery(this);
                initValue = parseInt(jInput.val());
                inputControllers.push({
                    needsBatching: function() {
                        var newValue = parseInt(jInput.val());
                        return newValue - initValue > config.defaultBatchSize;
                    }
                });
                jInput.keyup(function() {
                    var needBatching = doAnyInputsNeedBatching();
                    jBatchResizeContainer.toggle(needBatching);
                    jBatchSizeInput.prop('disabled', !needBatching);
                });
            });
        });
    };
    setUpClusterPage();

    // Task page
    var setUpTaskPage = function() {
        var autoScroller;

        // Ajax log polling for task logging page
        if (jQuery('.task textarea#log').exists()) {
            // Auto-scroll support for progressive log output.
            //   See http://radio.javaranch.com/pascarello/2006/08/17/1155837038219.html
            autoScroller = function(scrollContainer) {

                return {
                    bottomThreshold : 25,
                    scrollContainer: scrollContainer,

                    getCurrentHeight : function() {
                        var scrollDiv = this.scrollContainer;
                        if (scrollDiv.scrollHeight > 0) {
                            return scrollDiv.scrollHeight;
                        }
                        else if (scrollDiv.offsetHeight > 0) {
                            return scrollDiv.offsetHeight;
                        }
                        return null;
                    },

                    // return true if we are in the "stick to bottom" mode
                    isSticking : function() {
                        var scrollDiv = this.scrollContainer;
                        var currentHeight = this.getCurrentHeight();
                        var height = ((scrollDiv.style.pixelHeight) ? scrollDiv.style.pixelHeight : scrollDiv.offsetHeight);
                        var diff = currentHeight - scrollDiv.scrollTop - height;
                        return diff < this.bottomThreshold;
                    },

                    scrollToBottom : function() {
                        var scrollDiv = this.scrollContainer;
                        scrollDiv.scrollTop = this.getCurrentHeight();
                    },

                    position : function(newPos) {
                        var scrollDiv = this.scrollContainer;
                        var initScrollTop = scrollDiv.scrollTop;
                        if (newPos) { scrollDiv.scrollTop = newPos; }
                        return initScrollTop
                    }
                };
            };

            var logElem = jQuery('textarea#log');
            var jStatus = jQuery('#taskStatus');
            var jOperation = jQuery('#taskOperation');
            var jDurationString = jQuery('#taskDurationString');
            var jUpdateTime = jQuery('#taskUpdateTime');
            var jCancellationForm = jQuery('#taskCancellationForm');
            var scroller = autoScroller(logElem[0]);
            var poller;
            var ajaxOptions = {
                url: document.location.href + '.json',
                dataType: 'json',
                error: function() {
                    if (poller) { poller.stop = true; }
                    logElem.before('<div class="error">Error polling for log. Please reload page.</div>');
                },
                success: function(taskData) {
                    // Keep scroll position where the user expects it to be
                    var newLogData = taskData.log.join('\n');
                    var status = taskData.status;
                    var operation = taskData.operation;
                    var durationString = taskData.durationString;
                    if (logElem.val() !== newLogData) {
                        var stickToBottom = scroller.isSticking();
                        var pos = scroller.position();
                        logElem.val(newLogData);
                        if (stickToBottom) { scroller.scrollToBottom(); }
                        else { scroller.position(pos); }
                    }
                    if (jStatus.text() !== status) {
                        jStatus.text(status).yellowFade();
                    }
                    if (jOperation.text() !== operation) {
                        jOperation.text(operation).yellowFade();
                    }
                    if (jDurationString.text() !== durationString) {
                        jDurationString.text(durationString);
                        jUpdateTime.text(taskData.updateTime);
                    }
                    if (taskData.status !== 'running') {
                        poller.stop = true;
                        jCancellationForm.slideUp();
                    }
                },
                contentType: 'application/json'
            };
            var pollOptions = {
                min_wait: 1000,
                wait_multiplier: 2
            };
            poller = jQuery.poll(ajaxOptions, pollOptions);
        }
    };
    setUpTaskPage();

    // Auto scaling group create page
    var setUpAutoScalingGroupCreatePage = function() {
        var container, inputs, preview, envVarsContainer, envVarsPreview, alphanumericUnderDot,
                alphanumericUnderDotHyphen, updatePreview, timer;

        container = jQuery('.compoundName');
        if (!container.exists()) { return; }

        inputs = container.find('input:text');
        preview = container.find('.preview .nameText');
        envVarsContainer = container.find('.envVars');
        envVarsPreview = envVarsContainer.find('pre');

        alphanumericUnderDot = /[a-zA-Z0-9_.]/;
        alphanumericUnderDotHyphen = /[-a-zA-Z0-9_.]/;

        // Prevent typing illegal characters in special text inputs.
        inputs.filter('[name=detail]').restrictCharInput(alphanumericUnderDotHyphen);
        inputs.not('[name=detail]').restrictCharInput(alphanumericUnderDot);

        updatePreview = function() {
            var buildDataString = function() {
                var dataString = '';
                container.find('select').add(inputs).each(function() {
                    if (dataString) { dataString += '&'; }
                    dataString += this.getAttribute('name') + '=' + this.value;
                });
                return dataString;
            };

            var ajaxOptions = {
                url: '/autoScaling/generateName.json',
                data: buildDataString(),
                dataType: 'json',
                error: function(jqXhr) {
                    preview.text(jqXhr.responseText || '(Name preview unavailable)');
                },
                success: function(response) {
                    var envVars = response.envVars.join('\n');

                    preview.text(response.groupName);
                    envVarsPreview.text(envVars);
                    envVarsContainer.toggle(envVars ? true : false);
                },
                contentType: 'application/json'
            };

            if (timer) { clearTimeout(timer); }
            timer = setTimeout(function() { jQuery.ajax(ajaxOptions); }, 300);
            return true;
        };

        inputs.keyup(updatePreview);
        container.delegate('select', 'change', updatePreview);
        updatePreview(); // If the page has a validation error then fields are pre-populated on page load.
    };
    setUpAutoScalingGroupCreatePage();

    // Auto Scaling Group detail page
    var setUpGroupDetailScreen = function() {
        var jInstanceCheckboxes, healthCheckLinkEnabled, jHealthCheckRunLinks, runHealthChecks, confirmation;

        jInstanceCheckboxes = jQuery('input[name=instanceId]');
        jQuery('input#allInstances').change(function() {
            jInstanceCheckboxes.attr('checked', this.checked).change();
        });

        confirmation = function() {
            var count, suffix, message, safeToProceed;
            count = jInstanceCheckboxes.filter(':checked').size();
            suffix = count === 1 ? '' : 's';
            message = 'Really terminate ' + count + ' selected instance' + suffix + '? This cannot be undone.';
            safeToProceed = count && confirm(message) ? true : false;
            return safeToProceed;
        };
        jQuery('button.terminateMany').bind('click keypress', confirmation).show();

        healthCheckLinkEnabled = true;
        jHealthCheckRunLinks = jQuery('a.healthCheckRunLink');
        runHealthChecks = function(event) {
            var link, jRows;
            if (!healthCheckLinkEnabled) { return false; }
            healthCheckLinkEnabled = false;
            if (event) { event.preventDefault(); }
            link = this;
            jRows = jQuery('table#instanceTable tr').has('td.health');
            jRows.each(function() {
                var jRow, instanceId, jDiscoveryDataCell, jHealthDataCell, ajaxOptions, randomNumber, region;
                jRow = jQuery(this);
                instanceId = jRow.attr('data-instanceid');
                if (instanceId) {
                    jDiscoveryDataCell = jRow.find('td.discovery').removeClass('inService outOfService');
                    jDiscoveryDataCell.find('span').text('');
                    jHealthDataCell = jRow.find('td.health').removeClass('pass fail');
                    jRow.addClass('waiting');
                    region = window.browserGlobalsFromServer.region;
                    ajaxOptions = {
                        url: '/' + region  + '/instance/diagnose/' + instanceId + ".json",
                        dataType: 'json',
                        error: function() {
                            jQuery(link).replaceWith('<span class="error">An error occurred. Please refresh and try again.</span>');
                        },
                        success: function(instanceData) {
                            var discoveryStatus, discoveryClass;
                            discoveryStatus = instanceData.discInstance ? instanceData.discInstance.status : 'Not Found';
                            discoveryClass = discoveryStatus === 'UP' ? 'inService' : 'outOfService';
                            jDiscoveryDataCell.addClass(discoveryClass).find('span').text(discoveryStatus);
                            jHealthDataCell.addClass(instanceData.healthCheck);
                        },
                        complete: function() {
                            jRow.removeClass('waiting');
                            // If no more rows waiting then re-enable link.
                            if (!jRows.has('td.waiting').exists()) {
                                healthCheckLinkEnabled = true;
                            }
                        },
                        contentType: 'application/json'
                    };
                    // Stagger the ajax calls in case there are a lot of them. Don't start 200 in one second.
                    randomNumber = Math.floor(Math.random() * 100 * jRows.length);
                    setTimeout(function() { jQuery.ajax(ajaxOptions); }, randomNumber);

                }
            });
        };
        jHealthCheckRunLinks.click(runHealthChecks);

        if (jHealthCheckRunLinks.is('.autoLaunch')) {
            jQuery(document).ready(function() { runHealthChecks(); });
        }
    };
    setUpGroupDetailScreen();

    var setOddAndEvenStylingOnRows = function(jRows) {
        // Zebra-striping visible rows of a table where some rows are hidden. Not possible in CSS.
        // CSS even/odd is the opposite of Java iteration even/odd. Avoid page load flicker by reversing CSS meanings.
        jRows.filter(':even').removeClass('even').addClass('odd');
        jRows.filter(':odd').removeClass('odd').addClass('even');
        return jRows;
    };

    // Add filter to all long tables on list pages
    var setUpListFilters = function() {
        var config, prepareTableRows, runTableFilter, identifyRows, resetAndRecordTableWidth, findControlBar,
                addTableFilter, addInitializeButton, storedFilter, getStorageKey, setUpTableFilters;
        config = {
            minRowCount: 3,
            maxDataCellCount: 9000,
            headerRowCount: 1,
            minutesToStoreFilterValue: 4 * 7 * 24 * 60 /* Four weeks */
        };

        prepareTableRows = function(table) {
            var r, c, row, cells, text;
            var rows = table.rows;
            for (r = 0; r < rows.length; r++) {
                row = rows[r];
                text = '';
                cells = row.cells;
                for (c = 0; c < cells.length; c++) {
                    text += jQuery(cells[c]).text() + ' ';
                }
                row.filterContent = text.toLowerCase();
            }
            return true;
        };

        runTableFilter = function(table) {
            var input, filterString, rowLists, jHits;
            input = this;
            filterString = input.value.toLowerCase();

            if (filterString) { jQuery(table).width(jQuery(table).data('initWidth')); }
            else { resetAndRecordTableWidth(table); }

            // If the window is narrower than the table then keeping the old table width is bad
            if (jQuery(window).width() < jQuery(table).width()) {
                resetAndRecordTableWidth(table);
            }

            rowLists = identifyRows(table, filterString);
            jHits = jQuery(rowLists.hits);
            setOddAndEvenStylingOnRows(jHits).show();
            jQuery(rowLists.misses).hide();

            jQuery(table.filterCounter).text(rowLists.hits.length);

            // Save this filter in browser memory only if it shows some results. Otherwise remove stored filter.
            if (rowLists.hits.length > 0) { storedFilter.set(filterString); }
            else { storedFilter.remove(); }
            return true;
        };

        identifyRows = function(table, filterString) {
            var r, row, show;
            var rowLists = {
                hits: [],
                misses: []
            };
            var allRows = jQuery.array(table.rows);
            // All except header rows
            for (r = 0; r < config.headerRowCount; r++) {
                allRows.shift();
            }
            if (filterString) {
                for (r = 0; r < allRows.length; r++) {
                    row = allRows[r];
                    show = true;
                    jQuery(filterString.split(' ')).each(function() {
                        show = show && row.filterContent.indexOf(this) >= 0
                    });
                    if (show) { rowLists.hits.push(row); }
                    else { rowLists.misses.push(row); }
                }
            }
            // No filter string? Then show all rows except header row.
            else {
                rowLists.hits = allRows;
            }
            return rowLists;
        };

        resetAndRecordTableWidth = function(table) {
            jQuery(table).width('');
            jQuery(table).data('initWidth', jQuery(table).width() + 1);
        };

        findControlBar = function(table) {
            return jQuery(table).parent('div.list').children('div.buttons').first();
        };

        addTableFilter = function(table) {
            var storedFilterValue, label, input, counter, filterContainer, timer;

            // Avoid adding two inputs for a table on back button press
            if (table.filterInput) {
                return;
            }
            // Set the table width explicitly so it won't change when wide rows vanish
            resetAndRecordTableWidth(table);
            label = jQuery('<label>Filter: </label>');
            input = jQuery('<input type="text"/>').keyup(
                function() {
                    var input = this;
                    if (timer) { clearTimeout(timer); }
                    timer = setTimeout(function() { runTableFilter.call(input, table); }, 300);
                    return true;
                }
            ).keypress(
                function(event) {
                    var key = event.keyCode || event.which;
                    return key != 13; // Allow normal behavior for all keys except Enter
                }
            );
            counter = jQuery('<label></label>');
            table.filterInput = input[0];
            table.filterCounter = counter[0];
            prepareTableRows(table);
            filterContainer = jQuery('<div class="filter" title="Separate word fragments by spaces for advanced filtering"/>');
            filterContainer.append(label).append(input).append(counter);
            findControlBar(table).append(filterContainer);

            storedFilterValue = storedFilter.get();

            // Use the stored filter if any rows match it. Otherwise remove the stored filter.
            if (identifyRows(table, storedFilterValue).hits.length >= 1) {
                input.val(storedFilterValue);
                runTableFilter.call(input[0], table);
            }
            else {
                storedFilter.remove();
            }
            input.focus();
            runTableFilter.call(input[0], table);

            shortcut.add('esc', function() {
                input.val('').keyup().focus();
            }, {'type': 'keyup'});
        };

        addInitializeButton = function(table) {
            var jInitListFilterButton = jQuery('<button class="initListFilter"><div>Initialize List Filter</div></button>');
            jInitListFilterButton.click(function(e) {
                e.preventDefault();
                setTimeout(function() { addTableFilter(table); }, 50);
                jInitListFilterButton.remove();
            });

            findControlBar(table).append(jInitListFilterButton);
        };

        storedFilter = {
            get: function() { return lscache.get(getStorageKey()); },
            set: function(newValue) { lscache.set(getStorageKey(), newValue, config.minutesToStoreFilterValue); },
            remove: function() { lscache.remove(getStorageKey()); }
        };

        getStorageKey = function() {
            if (!config.storageKey) {
                config.storageKey = window.location.href + 'ListFilter';
            }
            return config.storageKey;
        };

        setUpTableFilters = function() {
            var firstTable, jFirstInput;
            jQuery('.list table').each(function() {
                var i,
                    rows = this.rows,
                    cellCount = 0,
                    longestRowCellCount = 0,
                    doFilterInitOnPageLoad = false;
                // Fast approximation of cell count. Number of rows times longest of the first few rows.
                if (this.rows.length >= config.minRowCount) {
                    for (i = 0; i < config.minRowCount; i++) {
                        longestRowCellCount = Math.max(longestRowCellCount, rows[i].cells.length);
                    }
                    cellCount = longestRowCellCount * rows.length;
                    if (cellCount <= config.maxDataCellCount) {
                        doFilterInitOnPageLoad = true;
                    }
                }

                if (doFilterInitOnPageLoad) {
                    addTableFilter(this);
                    // Keep a reference to the first filtered table and its filter input.
                    if (!firstTable) {
                        firstTable = this;
                        jFirstInput = jQuery(this.filterInput);
                    }
                } else if (cellCount) {
                    addInitializeButton(this);
                }
            });
            if (jFirstInput) {
                jFirstInput.focus();
            }
        };
        jQuery(document).ready(function() { setTimeout(setUpTableFilters, 50); });
    };
    setUpListFilters();

    // Add filter to all long select lists
    var enableSelect2ForVisible = function() {

        var config, convertSelects;

        config = {
            minOptionCountForSearch: 10,
            maxOptionsForRequiredSearch: 1000 // for performance, searching 6000 amis is slow
        };

        convertSelects = function() {
            jQuery('select:visible:not(#regionSwitcher)').each(function() {
                var selectElement = jQuery(this);
                var options = {
                    width: (selectElement.outerWidth() + 10) + 'px',
                    dropdownCss: { width: 'auto'}
                };
                if (config.minOptionCountForSearch) {
                    options['minimumResultsForSearch'] = config.minOptionCountForSearch;
                }
                if (config.maxOptionsForRequiredSearch && this.options.length > config.maxOptionsForRequiredSearch) {
                    options['minimumInputLength'] = 3;
                }
                selectElement.select2(options);
                // hack so select2 selects the first item when it's a blank value
                if (selectElement.is('.allowEmptySelect') && selectElement.select2('val') == '') {
                    selectElement.select2('val', '');
                }
            });
        };
        convertSelects();
    };
    enableSelect2ForVisible();

    /**
      SortTable
      version 2
      7th April 2007
      Stuart Langridge, www.kryogenix.org/code/browser/sorttable/

      Instructions:
      Add class="sortable" to any table you'd like to make sortable
      Click on the headers to sort

      Thanks to many, many people for contributions and suggestions.
      Licenced as X11: www.kryogenix.org/code/browser/licence.html
      This basically means: do what you want with it.
    */
    var setUpTableSortability = function() {

        var sorttable = {
            init: function() {
                // quit if this function has already been called
                if (arguments.callee.done) {
                    return;
                }
                // flag this function so we don't do the same thing twice
                arguments.callee.done = true;

                if (!document.createElement || !document.getElementsByTagName) {
                    return;
                }

                sorttable.DATE_RE = /^(\d\d?)[\/\.\-](\d\d?)[\/\.\-]((\d\d)?\d\d)$/;

                jQuery('table.sortable').each(function() {
                    sorttable.makeSortable(this);
                });
            },

            makeSortable: function(table) {
                var i, sortbottomrows, the, tfo, headrow, mtch, sortrevind, sortfwdind, row_array, col, rows, tb, override;

                var headerClickHandler = function() {

                    // Always try to return focus to the filter input for this table, and move cursor to end of line
                    // in IE by replacing value with same value.
                    if (table.filterInput) {
                        jQuery(table.filterInput).focus().val(table.filterInput.value);
                    }

                    var theadrow;
                    if (this.className.search(/\bsorttable_sorted\b/) != -1) {
                        // if we're already sorted by this column, just
                        // reverse the table, which is quicker
                        sorttable.reverse(this.sorttable_tbody);
                        this.className = this.className.replace('sorttable_sorted',
                                'sorttable_sorted_reverse');
                        this.removeChild(document.getElementById('sorttable_sortfwdind'));
                        sortrevind = document.createElement('span');
                        sortrevind.id = "sorttable_sortrevind";
                        sortrevind.innerHTML = jQuery.browser.ie ? '&nbsp<font face="webdings">5</font>' : '&nbsp;&#x25B4;';
                        this.appendChild(sortrevind);
                        return;
                    }
                    if (this.className.search(/\bsorttable_sorted_reverse\b/) != -1) {
                        // if we're already sorted by this column in reverse, just
                        // re-reverse the table, which is quicker
                        sorttable.reverse(this.sorttable_tbody);
                        this.className = this.className.replace('sorttable_sorted_reverse',
                                'sorttable_sorted');
                        this.removeChild(document.getElementById('sorttable_sortrevind'));
                        sortfwdind = document.createElement('span');
                        sortfwdind.id = "sorttable_sortfwdind";
                        sortfwdind.innerHTML = jQuery.browser.ie ? '&nbsp<font face="webdings">6</font>' : '&nbsp;&#x25BE;';
                        this.appendChild(sortfwdind);
                        return;
                    }

                    // remove sorttable_sorted classes
                    theadrow = this.parentNode;
                    jQuery(theadrow).find('th, td').removeClass('sorttable_sorted sorttable_sorted_reverse');
                    sortfwdind = document.getElementById('sorttable_sortfwdind');
                    if (sortfwdind) { sortfwdind.parentNode.removeChild(sortfwdind); }
                    sortrevind = document.getElementById('sorttable_sortrevind');
                    if (sortrevind) { sortrevind.parentNode.removeChild(sortrevind); }

                    this.className += ' sorttable_sorted';
                    sortfwdind = document.createElement('span');
                    sortfwdind.id = "sorttable_sortfwdind";
                    sortfwdind.innerHTML = jQuery.browser.ie ? '&nbsp<font face="webdings">6</font>' : '&nbsp;&#x25BE;';
                    this.appendChild(sortfwdind);

                    // build an array to sort. This is a Schwartzian transform thing,
                    // i.e., we "decorate" each row with the actual sort key,
                    // sort based on the sort keys, and then put the rows back in order
                    // which is a lot faster because you only do getInnerText once per row
                    row_array = [];
                    col = this.sorttable_columnindex;
                    rows = this.sorttable_tbody.rows;
                    for (var j = 0; j < rows.length; j++) {
                        row_array[row_array.length] = [sorttable.getInnerText(rows[j].cells[col]), rows[j]];
                    }
                    /* If you want a stable sort, uncomment the following line */
                    sorttable.shaker_sort(row_array, this.sorttable_sortfunction);
                    /* and comment out this one */
                    //row_array.sort(this.sorttable_sortfunction);

                    tb = this.sorttable_tbody;
                    for (j = 0; j < row_array.length; j++) {
                        tb.appendChild(row_array[j][1]);
                    }

                    setOddAndEvenStylingOnRows(jQuery(tb).find('tr:visible'));
                };

                if (table.getElementsByTagName('thead').length === 0) {
                    // table doesn't have a tHead. Since it should have, create one and
                    // put the first table row in it.
                    the = document.createElement('thead');
                    the.appendChild(table.rows[0]);
                    table.insertBefore(the, table.firstChild);
                }
                // Safari doesn't support table.tHead, sigh
                if (!table.tHead) {
                    table.tHead = table.getElementsByTagName('thead')[0];
                }

                if (table.tHead.rows.length != 1) {
                    return;
                } // can't cope with two header rows

                // Sorttable v1 put rows with a class of "sortbottom" at the bottom (as
                // "total" rows, for example). This is B&R, since what you're supposed
                // to do is put them in a tfoot. So, if there are sortbottom rows,
                // for backwards compatibility, move them to tfoot (creating it if needed).
                sortbottomrows = [];
                for (i = 0; i < table.rows.length; i++) {
                    if (table.rows[i].className.search(/\bsortbottom\b/) != -1) {
                        sortbottomrows[sortbottomrows.length] = table.rows[i];
                    }
                }
                if (sortbottomrows.length >= 1) {
                    if (!table.tFoot) {
                        // table doesn't have a tfoot. Create one.
                        tfo = document.createElement('tfoot');
                        table.appendChild(tfo);
                    }
                    for (i = 0; i < sortbottomrows.length; i++) {
                        tfo.appendChild(sortbottomrows[i]);
                    }
                }

                // work through each column and calculate its type
                headrow = table.tHead.rows[0].cells;
                for (i = 0; i < headrow.length; i++) {
                    // manually override the type with a sorttable_type attribute
                    if (!headrow[i].className.match(/\bsorttable_nosort\b/)) { // skip this col
                        mtch = headrow[i].className.match(/\bsorttable_([a-z0-9]+)\b/);
                        if (mtch) {
                            override = mtch[1];
                        }
                        if (mtch && typeof sorttable["sort_" + override] == 'function') {
                            headrow[i].sorttable_sortfunction = sorttable["sort_" + override];
                        } else {
                            headrow[i].sorttable_sortfunction = sorttable.guessType(table, i);
                        }
                        // make it clickable to sort
                        headrow[i].sorttable_columnindex = i;
                        headrow[i].sorttable_tbody = table.tBodies[0];
                        jQuery(headrow[i]).click(headerClickHandler);
                    }
                }
            },

            guessType: function(table, column) {
                var text, possdate, first, second;
                // guess the type of a column based on its first non-blank row
                var sortfn = sorttable.sort_alpha;
                for (var i = 0; i < table.tBodies[0].rows.length; i++) {
                    text = sorttable.getInnerText(table.tBodies[0].rows[i].cells[column]);
                    if (text != '') {
                        if (text.match(/^-?[$]?[\d,.]+%?$/)) {
                            return sorttable.sort_numeric;
                        }
                        // check for a date: dd/mm/yyyy or dd/mm/yy
                        // can have / or . or - as separator
                        // can be mm/dd as well
                        possdate = text.match(sorttable.DATE_RE);
                        if (possdate) {
                            // looks like a date
                            first = parseInt(possdate[1], 10);
                            second = parseInt(possdate[2], 10);
                            if (first > 12) {
                                // definitely dd/mm
                                return sorttable.sort_ddmm;
                            } else if (second > 12) {
                                return sorttable.sort_mmdd;
                            } else {
                                // looks like a date, but we can't tell which, so assume
                                // that it's dd/mm (English imperialism!) and keep looking
                                sortfn = sorttable.sort_ddmm;
                            }
                        }
                    }
                }
                return sortfn;
            },

            getInnerText: function(node) {
                // gets the text we want to use for sorting for a cell.
                // strips leading and trailing whitespace.
                // this is *not* a generic getInnerText function; it's special to sorttable.
                // for example, you can override the cell text with a customkey attribute.
                // it also gets .value for <input> fields.

                var hasInputs = (typeof node.getElementsByTagName == 'function') &&
                        node.getElementsByTagName('input').length;
                var sortableText = '';
                var i, innerText = '';

                if (node.getAttribute("sorttable_customkey") !== null) {
                    sortableText = node.getAttribute("sorttable_customkey");
                }
                else if (typeof node.textContent != 'undefined' && !hasInputs) {
                    sortableText = node.textContent;
                }
                else if (typeof node.innerText != 'undefined' && !hasInputs) {
                    sortableText = node.innerText;
                }
                else if (typeof node.text != 'undefined' && !hasInputs) {
                    sortableText = node.text;
                }
                else {
                    switch (node.nodeType) {
                        case 3:
                            sortableText = (node.nodeName.toLowerCase() === 'input') ? node.value : node.nodeValue;
                            break;
                        case 4:
                            sortableText = node.nodeValue;
                            break;
                        case 1:
                        case 11:
                            for (i = 0; i < node.childNodes.length; i++) {
                                innerText += sorttable.getInnerText(node.childNodes[i]);
                            }
                            sortableText = innerText;
                            break;
                        default:
                            sortableText = '';
                    }
                }
                return sortableText.replace(/^\s+|\s+$/g, '');
            },

            reverse: function(tbody) {
                var i;
                // reverse the rows in a tbody
                var newrows = [];
                for (i = 0; i < tbody.rows.length; i++) {
                    newrows[newrows.length] = tbody.rows[i];
                }
                for (i = newrows.length - 1; i >= 0; i--) {
                    tbody.appendChild(newrows[i]);
                }
            },

            /* sort functions
             each sort function takes two parameters, a and b
             you are comparing a[0] and b[0] */
              sort_numeric: function(a,b) {
                  var aa = parseFloat(a[0].replace(/[^0-9.\-]/g,''));
                  if (isNaN(aa)) { aa = 0; }
                  var bb = parseFloat(b[0].replace(/[^0-9.\-]/g,''));
                  if (isNaN(bb)) { bb = 0; }
                  return aa-bb;
              },
              sort_alpha: function(a,b) {
                  if (a[0] == b[0]) { return 0; }
                  if (a[0] < b[0]) { return -1; }
                  return 1;
              },
              sort_ddmm: function(a,b) {
                  var mtch = a[0].match(sorttable.DATE_RE);
                  var y = mtch[3]; var m = mtch[2]; var d = mtch[1];
                  if (m.length == 1) { m = '0'+m; }
                  if (d.length == 1) { d = '0'+d; }
                  var dt1 = y+m+d;
                  mtch = b[0].match(sorttable.DATE_RE);
                  y = mtch[3]; m = mtch[2]; d = mtch[1];
                  if (m.length == 1) { m = '0'+m; }
                  if (d.length == 1) { d = '0'+d; }
                  var dt2 = y+m+d;
                  if (dt1 == dt2) { return 0; }
                  if (dt1 < dt2) { return -1; }
                  return 1;
              },
              sort_mmdd: function(a,b) {
                  var mtch = a[0].match(sorttable.DATE_RE);
                  var y = mtch[3]; var d = mtch[2]; var m = mtch[1];
                  if (m.length == 1) { m = '0'+m; }
                  if (d.length == 1) { d = '0'+d; }
                  var dt1 = y+m+d;
                  mtch = b[0].match(sorttable.DATE_RE);
                  y = mtch[3]; d = mtch[2]; m = mtch[1];
                  if (m.length == 1) { m = '0'+m; }
                  if (d.length == 1) { d = '0'+d; }
                  var dt2 = y+m+d;
                  if (dt1 == dt2) { return 0; }
                  if (dt1 < dt2) { return -1; }
                  return 1;
              },

            shaker_sort: function(list, comp_func) {
                // A stable sort function to allow multi-level sorting of data
                // see: en.wikipedia.org/wiki/Cocktail_sort
                // thanks to Joseph Nahmias
                var i, q;
                var b = 0;
                var t = list.length - 1;
                var swap = true;

                while (swap) {
                    swap = false;
                    for (i = b; i < t; ++i) {
                        if (comp_func(list[i], list[i + 1]) > 0) {
                            q = list[i]; list[i] = list[i + 1]; list[i + 1] = q;
                            swap = true;
                        }
                    } // for
                    t--;

                    if (!swap) { break; }

                    for (i = t; i > b; --i) {
                        if (comp_func(list[i], list[i - 1]) < 0) {
                            q = list[i]; list[i] = list[i - 1]; list[i - 1] = q;
                            swap = true;
                        }
                    } // for
                    b++;

                } // while(swap)
            }
        };

        jQuery(document).ready(function() {
            setTimeout(sorttable.init, 10);
        });
    };
    setUpTableSortability();

});
