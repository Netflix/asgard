import com.netflix.asgard.plugin.AuthorizationProvider
import org.apache.shiro.SecurityUtils

/**
 * Invokes Shiro's access control if any of the configured {@link AuthorizationProvider} objects indicates the current
 * subject does not have access to the requested endpoint.
 */
class AuthorizationFilters {

    def pluginService

    def filters = {
        all(controller: '*', action: '*') {
            before = {
                Collection<AuthorizationProvider> authorizationProviders = pluginService.authorizationProviders
                if (authorizationProviders.any { !it.isAuthorized(request, controllerName, actionName) }) {
                    accessControl()
                }
                true
            }
        }
    }
}
