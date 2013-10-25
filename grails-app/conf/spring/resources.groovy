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
import com.google.common.base.CaseFormat
import com.netflix.asgard.CachedMapBuilder
import com.netflix.asgard.Caches
import com.netflix.asgard.DefaultAdvancedUserDataProvider
import com.netflix.asgard.DefaultUserDataProvider
import com.netflix.asgard.NetflixAdvancedUserDataProvider
import com.netflix.asgard.Region
import com.netflix.asgard.ServiceInitLoggingBeanPostProcessor
import com.netflix.asgard.SnsTaskFinishedListener
import com.netflix.asgard.ThreadScheduler
import com.netflix.asgard.auth.OneLoginAuthenticationProvider
import com.netflix.asgard.auth.RestrictEditAuthorizationProvider
import com.netflix.asgard.deployment.DeploymentActivitiesImpl
import groovy.io.FileType

beans = {
    serviceInitLoggingBeanPostProcessor(ServiceInitLoggingBeanPostProcessor)

    threadScheduler(ThreadScheduler, ref('configService'))

    List<Region> limitedRegions = Region.limitedRegions ?: Region.values()
    cachedMapBuilder(CachedMapBuilder, ref('threadScheduler'), limitedRegions)

    caches(Caches, ref('cachedMapBuilder'), ref('configService'))

    defaultUserDataProvider(DefaultUserDataProvider) { bean ->
        bean.lazyInit = true
    }

    defaultAdvancedUserDataProvider(DefaultAdvancedUserDataProvider) { bean ->
        bean.lazyInit = true
    }

    deploymentActivitiesImpl(DeploymentActivitiesImpl) {
        it.autowire = "byName"
        it.lazyInit = true
    }

    snsTaskFinishedListener(SnsTaskFinishedListener) { bean ->
        bean.lazyInit = true
    }

    if (application.config.plugin?.authenticationProvider == 'oneLoginAuthenticationProvider') {
        oneLoginAuthenticationProvider(OneLoginAuthenticationProvider) { bean ->
            bean.lazyInit = true
        }
    }

    if (application.config.plugin?.advancedUserDataProvider == 'netflixAdvancedUserDataProvider') {
        netflixAdvancedUserDataProvider(NetflixAdvancedUserDataProvider) { bean ->
            bean.lazyInit = true
        }
    }

    restrictEditAuthorizationProvider(RestrictEditAuthorizationProvider) { bean ->
        bean.lazyInit = true
    }

    //**** Plugin behavior

    xmlns lang:'http://www.springframework.org/schema/lang'

    File pluginDir = new File("${application.config.asgardHome}/plugins/")
    if (pluginDir.exists()) {
        pluginDir.eachFileMatch(FileType.FILES, ~/.*\.groovy/) { File plugin ->
            String beanName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, plugin.name.replace('.groovy', ''))
            lang.groovy(id: beanName, 'script-source': "file:${application.config.asgardHome}/plugins/${plugin.name}",
                    'refresh-check-delay': application.config.plugin.refreshDelay?: -1)
        }
    }
}
