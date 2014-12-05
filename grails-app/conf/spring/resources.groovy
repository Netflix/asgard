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

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.CaseFormat
import com.netflix.asgard.CachedMapBuilder
import com.netflix.asgard.Caches
import com.netflix.asgard.CsiAsgAnalyzer
import com.netflix.asgard.applications.SimpleDBApplicationService
import com.netflix.asgard.applications.SpinnakerApplicationService
import com.netflix.asgard.NoOpAsgAnalyzer
import com.netflix.asgard.Region
import com.netflix.asgard.ServiceInitLoggingBeanPostProcessor
import com.netflix.asgard.SnsTaskFinishedListener
import com.netflix.asgard.ThreadScheduler
import com.netflix.asgard.auth.OneLoginAuthenticationProvider
import com.netflix.asgard.auth.RestrictBrowserAuthorizationProvider
import com.netflix.asgard.auth.RestrictEditAuthorizationProvider
import com.netflix.asgard.deployment.DeploymentActivitiesImpl
import com.netflix.asgard.eureka.EurekaClientHolder
import com.netflix.asgard.model.CsiScheduledAnalysisFactory
import com.netflix.asgard.server.DeprecatedServerNames
import com.netflix.asgard.userdata.DefaultAdvancedUserDataProvider
import com.netflix.asgard.userdata.DefaultUserDataProvider
import com.netflix.asgard.userdata.LocalFileUserDataProvider
import com.netflix.asgard.userdata.NetflixAdvancedUserDataProvider
import com.netflix.asgard.userdata.PropertiesUserDataProvider
import groovy.io.FileType

beans = {
    serviceInitLoggingBeanPostProcessor(ServiceInitLoggingBeanPostProcessor)

    threadScheduler(ThreadScheduler, ref('configService'))

    List<Region> limitedRegions = Region.limitedRegions ?: Region.values()
    cachedMapBuilder(CachedMapBuilder, ref('threadScheduler'), limitedRegions)

    caches(Caches, ref('cachedMapBuilder'), ref('configService'))

    deprecatedServerNames(DeprecatedServerNames) {
        it.autowire = "byName"
    }

    eurekaClientHolder(EurekaClientHolder) {
        it.autowire = "byName"
    }

    objectMapper(ObjectMapper)

    propertiesUserDataProvider(PropertiesUserDataProvider) { bean ->
        bean.lazyInit = true
    }

    defaultUserDataProvider(DefaultUserDataProvider) { bean ->
        bean.lazyInit = true
    }

    defaultAdvancedUserDataProvider(DefaultAdvancedUserDataProvider) { bean ->
        bean.lazyInit = true
    }

    noOpAsgAnalyzer(NoOpAsgAnalyzer)

    deploymentActivitiesImpl(DeploymentActivitiesImpl) {
        it.autowire = "byName"
        it.lazyInit = true
    }

    csiScheduledAnalysisFactory(CsiScheduledAnalysisFactory)

    snsTaskFinishedListener(SnsTaskFinishedListener) { bean ->
        bean.lazyInit = true
    }

    if (application.config.plugin?.authenticationProvider == 'oneLoginAuthenticationProvider') {
        oneLoginAuthenticationProvider(OneLoginAuthenticationProvider) { bean ->
            bean.lazyInit = true
        }
    }

    if (application.config.plugin?.userDataProvider == 'localFileUserDataProvider') {
      localFileUserDataProvider(LocalFileUserDataProvider) { bean ->
        bean.lazyInit = true
      }
    }

    if (application.config.plugin?.advancedUserDataProvider == 'netflixAdvancedUserDataProvider') {
        netflixAdvancedUserDataProvider(NetflixAdvancedUserDataProvider) { bean ->
            bean.lazyInit = true
        }
    }

    if (application.config.plugin?.asgAnalyzer == 'csiAsgAnalyzer') {
        csiAsgAnalyzer(CsiAsgAnalyzer) { bean ->
            bean.lazyInit = true
        }
    }

    restrictEditAuthorizationProvider(RestrictEditAuthorizationProvider) { bean ->
        bean.lazyInit = true
    }

    restrictBrowserAuthorizationProvider(RestrictBrowserAuthorizationProvider)

    if (application.config.spinnaker?.gateUrl) {
        applicationService(
            SpinnakerApplicationService,
            application.config.spinnaker.gateUrl as String,
            application.config.cloud.accountName as String
        ) { bean ->
            bean.lazyInit = true
        }
    } else {
        applicationService(SimpleDBApplicationService) { bean ->
            bean.lazyInit = true
        }
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
