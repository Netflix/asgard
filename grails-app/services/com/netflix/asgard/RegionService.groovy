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

import grails.util.GrailsNameUtils
import org.codehaus.groovy.grails.commons.DefaultGrailsControllerClass
import org.codehaus.groovy.grails.web.metaclass.RedirectDynamicMethod
import org.springframework.beans.factory.InitializingBean

/**
 * Utilities for keeping track of link generation and redirect rules for region-specific controllers and region
 * agnostic controllers.
 */
class RegionService implements InitializingBean {

    static transactional = false

    def grailsApplication

    /** home, task, flag, etc. */
    private Set<String> regionSpecificControllerLogicalPropertyNames = new HashSet<String>()

    void afterPropertiesSet() {
        grailsApplication.controllerClasses.each { DefaultGrailsControllerClass controllerClass ->
            Boolean isRegional = !(controllerClass.clazz.declaredAnnotations.any { it instanceof RegionAgnostic })
            if (isRegional) {
                regionSpecificControllerLogicalPropertyNames << controllerClass.logicalPropertyName
            }
        }

        addRegionParamToRedirectMethods()
    }

    /**
     * Decorate the redirect method of all controllers to ensure that the region parameter gets used in the parameter
     * map implicitly. This causes all controller-based redirects to include the region parameter before building the
     * URL. The happy result is that POST submissions that redirect to GET destinations will look like
     * /us-east-1/autoScaling/list instead of /autoScaling/list so there will be no need for an extra browser redirect
     * to the regional URL style.
     *
     * Hints taken from http://stackoverflow.com/questions/5316727/grails-override-redirect-controller-method
     */
    private void addRegionParamToRedirectMethods() {

        // org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin adds the redirect methods to the controller
        // instances whenever the controllers are created.
        // The controller class gets instantiated again during hot code replacement in development. The redirect
        // functionality to hook into is inside the invoke method of RedirectDynamicMethod
        // TODO: Also modify stuff in at RegexUrlMapping and DefaultUrlMappingsHolder to make URLs consistent
        // when other parameters are used in addition to id and region.
        MetaMethod oldInvokeMethod =
                RedirectDynamicMethod.metaClass.pickMethod('invoke', [Object, String, Object[]] as Class[])
        RedirectDynamicMethod.metaClass.invoke = { Object target, String methodName, Object arguments ->
            if (arguments && isControllerRegional(target) ) {
                // If redirect was called without a params map object then make an empty params map.
                Map params = arguments.params ?: [:]
                if (!params.region) { params.region = Requests.request.region.code }
                arguments.params = params
            }
            // This is weird looking because Groovy supports List syntax better than array syntax, and because there
            // are several levels of meta programming here with different kinds of methods that are called "invoke".
            Object[] argsToInvoke = [arguments] as Object[]
            oldInvokeMethod.invoke(delegate, target, methodName, argsToInvoke)
        }
    }

    Boolean isControllerRegional(def controller) {
        String controllerName
        if (controller instanceof String) {
            controllerName = controller
        } else {
            controllerName = GrailsNameUtils.getLogicalPropertyName(controller.getClass().simpleName, 'Controller')
        }
        regionSpecificControllerLogicalPropertyNames.contains(controllerName)
    }
}
