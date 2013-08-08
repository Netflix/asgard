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
import com.netflix.asgard.Requests
import com.netflix.asgard.ServerController
import java.util.regex.Pattern
import org.joda.time.DateTime

class TrackingFilters {

    static final Pattern nonBrowserUserAgents = Pattern.compile(
            '.*(libcurl|Python-urllib|Wget|HttpClient|lwp-request|Java).*')

    static filters = {
        all(controller: 'firefox', invert: true) {
            before = {
                Requests.preventCaching(response)

                String userAgent = request.getHeader('user-agent')
                if (userAgent?.contains('MSIE') && !userAgent.contains('chromeframe')) {
                    request['ieWithoutChromeFrame'] = true
                }

                // Firefox has a sporadic, critical bug on some complex pages, when there are multiple select elements
                // with the same name (legal in all other browsers), if the user refreshes the page many times.
                // For now, disable Firefox use entirely until there is time to build a robust workaround.
                // Browser sniffing is not a good long-term solution, but denying Firefox use will protect Asgard users
                // from causing major outages while we build the workaround.
                // For our purposes, detecting the string "Firefox" is adequate.
                // See user agent strings at http://www.zytrax.com/tech/web/browser_ids.htm
                if (userAgent?.contains('Firefox')) {
                    redirect(controller: 'firefox', action: 'warning')
                    return false
                }

                if (!request["originalRequestDump"]) {
                    request["originalRequestDump"] = Requests.stringValue(request)
                }
                if (session.isNew()) {
                    String hostName = Requests.getClientHostName(request)
                    if (userAgent && !userAgent.matches(nonBrowserUserAgents)) {
                        log.info "${new DateTime()} Session started. Client ${hostName}, User-Agent ${userAgent}"
                    }
                }

                // If the last value is falsy and there is no explicit return statement then this filter method will
                // return a falsy value and cause requests to fail silently.
                return true
            }
        }
    }
}
