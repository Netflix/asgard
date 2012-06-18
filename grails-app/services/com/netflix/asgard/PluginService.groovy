package com.netflix.asgard

import com.netflix.asgard.plugin.UserDataProvider;

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

class PluginService implements ApplicationContextAware {

    static final String USER_DATA_PROVIDER = 'userDataProvider'

    ApplicationContext applicationContext
    ConfigService configService

    UserDataProvider getUserDataProvider() {
        String beanName = configService.pluginNamesToBeanNames[USER_DATA_PROVIDER] ?: 'defaultUserDataProvider'
        applicationContext.getBean(beanName) as UserDataProvider
    }

}
