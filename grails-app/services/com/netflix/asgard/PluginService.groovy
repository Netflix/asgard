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

import com.netflix.asgard.plugin.AuthenticationProvider
import com.netflix.asgard.plugin.AuthorizationProvider
import com.netflix.asgard.plugin.TaskFinishedListener
import com.netflix.asgard.plugin.UserDataProvider
import com.netflix.asgard.plugin.AdvancedUserDataProvider
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * This service provides a way for other parts of the code base to gain access to functionality stored in overridable
 * Asgard plugins.
 */
class PluginService implements ApplicationContextAware {

    static final String AUTHENTICATION_PROVIDER = 'authenticationProvider'
    static final String AUTHORIZATION_PROVIDERS = 'authorizationProviders'
    static final String TASK_FINISHED_LISTENERS = 'taskFinishedListeners'
    static final String USER_DATA_PROVIDER = 'userDataProvider'
    static final String ADVANCED_USER_DATA_PROVIDER = 'advancedUserDataProvider'

    ApplicationContext applicationContext
    ConfigService configService
    FlagService flagService

    /**
     * Unless overridden, the default implementation of AdvancedUserDataProvider will be DefaultAdvancedUserDataProvider
     * which delegates user data construction to the legacy userDataProvider plugin, for backward compatibility.
     *
     * See NetflixAdvancedUserDataProvider for an example of how a company can override Asgard's default user data.
     *
     * @return the implementation of the user data provider that can take lots of cloud objects as inputs
     */
    AdvancedUserDataProvider getAdvancedUserDataProvider() {
        Object configuredBeanName = configService.getBeanNamesForPlugin(ADVANCED_USER_DATA_PROVIDER)
        String beanName = configuredBeanName ?: 'defaultAdvancedUserDataProvider'
        applicationContext.getBean(beanName) as AdvancedUserDataProvider
    }

    /**
     * This is maintained only for backward compatibility with existing overrides of userDataProvider. It's not
     * technically deprecated because we don't have plans to remove it yet, but it's not recommended anymore. Better to
     * override AdvancedUserDataProvider instead.
     *
     * @return the chosen implementation of the limited legacy interface for creating user data based solely on
     *          UserContext, ASG name, app name, and launch config name
     */
    UserDataProvider getUserDataProvider() {
        String beanName = configService.getBeanNamesForPlugin(USER_DATA_PROVIDER) ?: 'defaultUserDataProvider'
        applicationContext.getBean(beanName) as UserDataProvider
    }

    /**
     * Gets all the listeners for task completion events, to do things like publishing logs to a topic, or writing
     * comments to a change control system.
     *
     * @return the collection of all task finished listeners that are configured
     */
    Collection<TaskFinishedListener> getTaskFinishedListeners() {
        List<String> beanNames = configService.getBeanNamesForPlugin(TASK_FINISHED_LISTENERS) as List ?: []
        beanNames.collect { applicationContext.getBean(it) as TaskFinishedListener }
    }

    /**
     * @return the configured {@link AuthenticationProvider} Spring bean, null if one isn't configured
     */
    AuthenticationProvider getAuthenticationProvider() {
        if (flagService.isOn(Flag.SUSPEND_AUTHENTICATION_REQUIREMENT)) {
            return null
        }
        String beanName = configService.getBeanNamesForPlugin(AUTHENTICATION_PROVIDER)
        if (beanName) {
            return applicationContext.getBean(beanName) as AuthenticationProvider
        }
    }

    /**
     * @return a list of configured {@link AuthorizationProvider} Spring beans, empty list if none configured
     */
    Collection<AuthorizationProvider> getAuthorizationProviders() {
        if (flagService.isOn(Flag.SUSPEND_AUTHENTICATION_REQUIREMENT)) {
            return []
        }
        List<String> beanNames = configService.getBeanNamesForPlugin(AUTHORIZATION_PROVIDERS) ?: []
        beanNames.collect { applicationContext.getBean(it) as AuthorizationProvider }
    }

}
