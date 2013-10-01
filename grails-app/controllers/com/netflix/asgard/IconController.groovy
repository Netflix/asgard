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
package com.netflix.asgard

import grails.converters.JSON
import grails.converters.XML

class IconController {

    def index = { redirect(action: 'list', params:params) }

    private static final String MID_PATH = 'images/tango/'

    @SuppressWarnings('GrailsServletContextReference')
    def list = {
        // Get all the png files within web-app/images/tango/ on a Mac workstation or Linux server
        List iconSets = []
        String tangoPath = "${servletContext.getRealPath('/')}${MID_PATH}"
        if (params.id) {
            tangoPath += params.id
        }

        new File(tangoPath).eachDirRecurse { File dir ->
            if (dir.list().size() > 0) {
                def icons = []
                dir.eachFile{ file ->
                    if (file.name.endsWith(".png")) {
                        icons.push(new Expando(name: file.name, path: makeUri(file, tangoPath)))
                    }
                }
                icons.sort { it.name }
                if (icons.size()) {
                    def iconSet = new Expando(icons: icons, dir: dir.name, path: makeUri(dir, tangoPath))
                    iconSets.push(iconSet)
                }
                iconSets.sort { it.path }
            }
        }

        withFormat {
            html {
                [ 'iconSets' : iconSets ]
            }
            xml { new XML(icons).render(response) }
            json { new JSON(icons).render(response) }
        }
    }

    private String makeUri(File file, String imagesDirPath) {
        "${MID_PATH}${file.path - imagesDirPath}"
    }

}
