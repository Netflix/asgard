package com.netflix.asgard.auth

import com.netflix.asgard.plugin.AuthorizationProvider
import javax.servlet.http.HttpServletRequest
import org.apache.commons.lang.WordUtils
import org.apache.shiro.SecurityUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired

/**
 * Implementation of {@link AuthorizationProvider} that prevents unauthenticated users from viewing edit pages.
 */
class RestrictEditAuthorizationProvider implements AuthorizationProvider, InitializingBean {

    /**
     * List of default action names that are always treated as edit endpoints.
     */
    static final List<String> DEFAULT_EDIT_ACTIONS = ['create', 'edit']

    /**
     * Cache of the controller name to the list of action names specified in the static editActions field of the
     * controller class.
     */
    Map<String, List<String>> controllerNameToEditActionNames

    @Autowired
    GrailsApplication grailsApplication

    @Override
    void afterPropertiesSet() {
        controllerNameToEditActionNames = grailsApplication.controllerClasses.collectEntries { controllerClass ->
            String controllerName = WordUtils.uncapitalize(controllerClass.name)
            [(controllerName): controllerClass.getStaticPropertyValue('editActions', List)]
        }
    }

    @Override
    boolean isAuthorized(HttpServletRequest request, String controllerName, String action) {
        !isProtectedResource(controllerName, action) || SecurityUtils.subject?.authenticated
    }

    private boolean isProtectedResource(String controllerName, String action) {
        DEFAULT_EDIT_ACTIONS.contains(action) || controllerNameToEditActionNames[controllerName]?.contains(action)
    }

}
