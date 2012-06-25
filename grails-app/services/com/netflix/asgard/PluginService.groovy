package com.netflix.asgard

import com.netflix.asgard.plugin.TaskFinishedListener
import com.netflix.asgard.plugin.UserDataProvider
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

class PluginService implements ApplicationContextAware {

    static final String USER_DATA_PROVIDER = 'userDataProvider'
    static final String TASK_FINISHED_LISTENERS = 'taskFinishedListeners'

    ApplicationContext applicationContext
    ConfigService configService

    UserDataProvider getUserDataProvider() {
        String beanName = configService.pluginNamesToBeanNames[USER_DATA_PROVIDER] ?: 'defaultUserDataProvider'
        applicationContext.getBean(beanName) as UserDataProvider
    }

    Collection<TaskFinishedListener> getTaskFinishedListeners() {
        List<String> beanNames = configService.pluginNamesToBeanNames[TASK_FINISHED_LISTENERS] ?: [:]
        beanNames.collect { applicationContext.getBean(it) as TaskFinishedListener }
    }

}
